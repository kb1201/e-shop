import json
import os

import pandas as pd
from sklearn.decomposition import TruncatedSVD
from sklearn.preprocessing import StandardScaler

from typing import List, Dict, Tuple
from loguru import logger
import redis
from collections import defaultdict

from app.core.config import settings
from app.core.database import get_db, UserInteraction
import joblib
from scipy.sparse import vstack
from sklearn.metrics.pairwise import cosine_similarity

from app.core.services.score_service import calculate_interaction_score


class HybridRecommendationEngine:
    def __init__(self):
        self.redis_client = redis.from_url(settings.redis_url)
        self.content_vectorizer = None
        self.content_similarity_matrix = None
        self.collaborative_model = None
        self.user_item_matrix = None
        self.scaler = StandardScaler()
        self.tfidf_matrix = None
        self.product_id_to_index = None
        self.content_is_trained = False
        self.collaborative_is_trained = False
        self.path = os.path.join(os.path.dirname(__file__), '../../../models')

    def load_models(self):
        try:
            self.content_vectorizer = joblib.load(os.path.join(self.path, 'tfidf_vectorizer_final.joblib'))
            self.tfidf_matrix = joblib.load(os.path.join(self.path, 'tfidf_matrix_final.joblib'))
            self.content_similarity_matrix = joblib.load(
                os.path.join(self.path, 'cosine_similarity_matrix_final.joblib'))
            self.product_id_to_index = joblib.load(os.path.join(self.path, 'product_id_to_index_final.joblib'))
            logger.info("Content-based model successfully loaded")
            self.content_is_trained = True
        except FileNotFoundError:
            logger.warning("Models not found")
            self.content_is_trained = False

        try:
            self.collaborative_model = joblib.load(os.path.join(self.path, 'collaborative_model.joblib'))
            self.user_item_matrix = joblib.load(os.path.join(self.path, 'user_item_matrix.joblib'))
            self.scaler = joblib.load(os.path.join(self.path, 'scaler.joblib'))
            logger.info("Collaborative based model successfully loaded")
            self.collaborative_is_trained = True
        except FileNotFoundError:
            logger.warning("Models not found")
            self.collaborative_is_trained = False

    def prepare_collaborative_data(self, interactions_df: pd.DataFrame) -> pd.DataFrame:
        logger.info("Preparing collaborative data")
        # Filter out invalid rows
        valid_interactions = interactions_df.dropna(subset=['user_id', 'product_id'])
        if valid_interactions.empty:
            logger.warning("No valid interactions found")
            return pd.DataFrame()

        # Get unique user-product pairs
        user_product_pairs = valid_interactions[['user_id', 'product_id']].drop_duplicates()

        # Calculate implicit rating for each user-product pair by scoring all interactions for that pair
        user_product_pairs['implicit_rating'] = user_product_pairs.apply(
            lambda row: calculate_interaction_score(
                row['user_id'],
                row['product_id'],
                valid_interactions
            ), axis=1
        )

        # Filter pairs with zero or low scores (optional)
        meaningful_pairs = user_product_pairs[user_product_pairs['implicit_rating'] > 0]

        # Create user-item pivot table
        user_item = meaningful_pairs.pivot_table(
            index='user_id',
            columns='product_id',
            values='implicit_rating',
            fill_value=0
        )

        logger.info("Finished preparing")
        # (Additional checks for data sufficiency, logging etc.)
        return user_item

    def train_collaborative(self, interactions_df: pd.DataFrame):
        logger.info("Training collaborative filtering model...")

        self.user_item_matrix = self.prepare_collaborative_data(interactions_df)

        user_item_scaled = self.scaler.fit_transform(self.user_item_matrix.fillna(0))
        n_features = self.user_item_matrix.shape[1]
        n_components = min(settings.svd_components, n_features)
        self.collaborative_model = TruncatedSVD(
            n_components=n_components,
            random_state=42
        )
        self.collaborative_model.fit(user_item_scaled)

        joblib.dump(self.collaborative_model, os.path.join(self.path, 'collaborative_model.joblib'))
        joblib.dump(self.user_item_matrix, os.path.join(self.path, 'user_item_matrix.joblib'))
        joblib.dump(self.scaler, os.path.join(self.path, 'scaler.joblib'))

        logger.info("Collaborative filtering model successfully saved")

    def get_content_based_recommendations(
            self, user_id: int, num_recommendations: int = 10, top_n_per_product: int = None
    ) -> List[Tuple[int, float]]:
        if not self.content_is_trained or not self.content_similarity_matrix.any():
            return []

        # FIXED: Dynamic top_n based on desired recommendations
        if top_n_per_product is None:
            # Calculate how many similar items per product we need to get enough total recommendations
            top_n_per_product = max(5, num_recommendations // 2)  # At least 5, but scale with num_recommendations

        db = next(get_db())
        try:
            logger.info("108")
            user_interactions = (
                db.query(UserInteraction)
                .filter(UserInteraction.user_id == user_id)
                .order_by(UserInteraction.timestamp.desc())
                .limit(10)
                .all()
            )

            if not user_interactions:
                return []

            user_product_ids = [interaction.product_id for interaction in user_interactions]
            product_indices = self.product_id_to_index
            idx_to_pid = {idx: pid for pid, idx in product_indices.items()}

            candidate_scores = defaultdict(float)

            for product_id in user_product_ids:
                if product_id not in product_indices:
                    continue

                product_idx = product_indices[product_id]
                sim_scores = list(enumerate(self.content_similarity_matrix[product_idx]))

                # FIXED: Use dynamic top_n_per_product instead of fixed top_n=5
                sim_scores = sorted(sim_scores, key=lambda x: x[1], reverse=True)[1:top_n_per_product + 1]

                for idx, score in sim_scores:
                    similar_pid = idx_to_pid[idx]
                    if similar_pid not in user_product_ids:  # skip already seen
                        candidate_scores[similar_pid] += score

            # Sort candidates by score and return as list of (product_id, score)
            sorted_candidates = sorted(candidate_scores.items(), key=lambda x: x[1], reverse=True)
            return sorted_candidates[:num_recommendations]

        finally:
            db.close()

    def get_collaborative_recommendations(self, user_id: int, num_recommendations: int = 10) -> List[Tuple[int, float]]:
        if not self.collaborative_is_trained or self.user_item_matrix is None:
            return []

        if user_id not in self.user_item_matrix.index:
            return []

        user_row = self.user_item_matrix.loc[user_id].values.reshape(1, -1)
        user_row_scaled = self.scaler.transform(user_row)

        user_factors = self.collaborative_model.transform(user_row_scaled)
        predicted_ratings = self.collaborative_model.inverse_transform(user_factors)
        predicted_ratings = self.scaler.inverse_transform(predicted_ratings)[0]

        unrated_items = self.user_item_matrix.loc[user_id] == 0
        recommendations = []

        for i, (product_id, is_unrated) in enumerate(zip(self.user_item_matrix.columns, unrated_items)):
            if is_unrated:
                recommendations.append((product_id, predicted_ratings[i]))

        recommendations.sort(key=lambda x: x[1], reverse=True)
        return recommendations[:num_recommendations]
    def normalize_scores(self, scores: List[Tuple[int, float]]) -> List[Tuple[int, float]]:
        if not scores:
            return scores
        
        score_values = [score for _, score in scores]
        min_score = min(score_values)
        max_score = max(score_values)
        
        # Avoid division by zero
        if max_score == min_score:
            return [(product_id, 1.0) for product_id, _ in scores]
        
        normalized = [
            (product_id, (score - min_score) / (max_score - min_score))
            for product_id, score in scores
        ]
        return normalized
    def get_hybrid_recommendations(self, user_id: int, num_recommendations: int = 10) -> List[Dict]:
        # FIXED: Scale the individual recommendation calls to get more diverse results
        content_recs = self.get_content_based_recommendations(
            user_id,
            num_recommendations * 2,  # Get more content-based recommendations
            top_n_per_product=max(10, num_recommendations // 5)  # Scale similar items per product
        )
        collaborative_recs = self.get_collaborative_recommendations(user_id, num_recommendations * 2)
        
        content_recs_normalized = self.normalize_scores(content_recs)
        collaborative_recs_normalized = self.normalize_scores(collaborative_recs)
        
        all_recommendations = {}
        logger.info("content recs: {}", len(content_recs))
        logger.info("collaborative recs: {}", len(collaborative_recs))

        for product_id, score in content_recs_normalized:
            all_recommendations[product_id] = {
                'content_score': score,
                'collaborative_score': 0.0
            }

        for product_id, score in collaborative_recs_normalized:
            if product_id in all_recommendations:
                all_recommendations[product_id]['collaborative_score'] = score
            else:
                all_recommendations[product_id] = {
                    'content_score': 0.0,
                    'collaborative_score': score
                }

        final_recommendations = []
        for product_id, scores in all_recommendations.items():
            hybrid_score = (
                    settings.content_weight * scores['content_score'] +
                    settings.collaborative_weight * scores['collaborative_score']
            )

            final_recommendations.append({
                'product_id': product_id,
                'hybrid_score': hybrid_score,
                'content_score': scores['content_score'],
                'collaborative_score': scores['collaborative_score']
            })

        final_recommendations.sort(key=lambda x: x['hybrid_score'], reverse=True)

        logger.info(f"Generated {len(final_recommendations)} total recommendations, returning {min(num_recommendations, len(final_recommendations))}")
        return final_recommendations[:num_recommendations]

    def train_models(self):
        db = next(get_db())
        try:

            interactions_query = db.query(UserInteraction).filter(
                UserInteraction.user_id.isnot(None),
                UserInteraction.product_id.isnot(None),
                UserInteraction.quantity >= 1
            )
            interactions_df = pd.read_sql(interactions_query.statement, db.bind)

            # todo when creating products is enabled train both
            # self.train_content_based(products_df)

            logger.info(len(interactions_df))
            if len(interactions_df) >= settings.min_interactions:
                self.train_collaborative(interactions_df)

            self.collaborative_is_trained = True
            logger.info("Model trained")

        finally:
            db.close()

    def get_cached_recommendations(self, user_id: int, cache_size: int = 200) -> List[Dict]:
        """Get recommendations with Redis caching for pagination"""
        cache_key = f"recs:user:{user_id}"

        # Try to get from Redis cache first
        try:
            cached_recs = self.redis_client.get(cache_key)
            if cached_recs:
                cached_data = json.loads(cached_recs)
                logger.info(f"Retrieved {len(cached_data)} cached recommendations for user {user_id}")
                return cached_data
        except Exception as e:
            logger.warning(f"Redis cache error: {e}")

        # FIXED: Use cache_size parameter properly
        logger.info(f"Generating {cache_size} fresh recommendations for user {user_id}")
        recommendations = self.get_hybrid_recommendations(
            user_id=user_id,
            num_recommendations=cache_size  # Now properly uses cache_size
        )

        logger.info(f"Generated {len(recommendations)} recommendations for caching")

        # Cache for 1 hour
        try:
            self.redis_client.setex(
                cache_key,
                3600,  # 1 hour TTL
                json.dumps(recommendations, default=str)
            )
            logger.info(f"Cached {len(recommendations)} recommendations for user {user_id}")
        except Exception as e:
            logger.warning(f"Redis cache write error: {e}")

        return recommendations

    def invalidate_user_cache(self, user_id: int):
        """Invalidate user's recommendation cache (call after new interactions)"""
        cache_key = f"recs:user:{user_id}"
        try:
            deleted = self.redis_client.delete(cache_key)
            if deleted:
                logger.info(f"Invalidated cache for user {user_id}")
            else:
                logger.info(f"No cache found to invalidate for user {user_id}")
        except Exception as e:
            logger.warning(f"Cache invalidation error: {e}")

    # todo use after
    def update_tfidf_with_new_products(
            self,
            new_products: list[dict],
            vectorizer_path: str,
            tfidf_matrix_path: str,
            sim_matrix_path: str
    ):
        """
        Adds multiple new products to an existing TF-IDF matrix and updates cosine similarity matrix.

        Args:
            new_products (list): List of dicts, each with keys: 'name', 'category', 'about_product'.
            vectorizer_path (str): Path to the saved TfidfVectorizer (.joblib).
            tfidf_matrix_path (str): Path to the saved tfidf_matrix (.joblib).
            sim_matrix_path (str): Path to save the updated cosine similarity matrix (.joblib).
        """
        # Load existing vectorizer and TF-IDF matrix
        vectorizer = joblib.load(vectorizer_path)
        tfidf_matrix = joblib.load(tfidf_matrix_path)

        # Prepare text data for all new products
        new_texts = [
            f"{p['name']} {p['category']} {p.get('about_product', '')}"
            for p in new_products
        ]

        # Transform using existing vectorizer
        new_vectors = vectorizer.transform(new_texts)

        # Stack new vectors to the existing matrix
        updated_tfidf_matrix = vstack([tfidf_matrix, new_vectors])

        # Recalculate cosine similarity matrix
        updated_cosine_sim = cosine_similarity(updated_tfidf_matrix)

        # Save updated matrices
        joblib.dump(updated_tfidf_matrix, tfidf_matrix_path)
        joblib.dump(updated_cosine_sim, sim_matrix_path)

        logger.info(f"Added {len(new_products)} new products. TF-IDF and cosine similarity matrices updated.")


# Singleton instance
recommendation_engine = HybridRecommendationEngine()