import pandas as pd

from app.core.config import settings
from datetime import datetime

class InteractionScorer:
    def calculate_score(self, interactions: pd.DataFrame) -> float:
        raise NotImplementedError


class PurchaseScorer(InteractionScorer):
    def calculate_score(self, interactions: pd.DataFrame) -> float:
        total_quantity = interactions['quantity'].sum()
        frequency = len(interactions)
        quantity_score = min(total_quantity * 0.4, 2.5)
        frequency_score = min(frequency * 0.8, 2.5)
        return quantity_score + frequency_score


# class GenericScorer(InteractionScorer):
#     def calculate_score(self, interactions: pd.DataFrame) -> float:
#         count = len(interactions)
#         if count == 1:
#             return 1.0
#         elif count <= 5:
#             return 1.0 + (count - 1) * 0.3
#         else:
#             return 2.2 + min((count - 5) * 0.1, 1.3)

class ScorerFactory:
    scorers = {
        'purchase': PurchaseScorer(),
        # 'view': GenericScorer(),
        # 'like': GenericScorer(),
        # 'share': GenericScorer(),
        # 'review': GenericScorer(),
    }

    @staticmethod
    def get_scorer(interaction_type: str) -> InteractionScorer:
        return ScorerFactory.scorers.get(interaction_type)


def calculate_interaction_score(user_id: int, product_id: int, interactions_df: pd.DataFrame) -> float:
    interactions = interactions_df[
        (interactions_df['user_id'] == user_id) &
        (interactions_df['product_id'] == product_id)
        ]

    if interactions.empty:
        return 0.0

    total_score = 0.0
    for interaction_type in interactions['interaction_type'].unique():
        type_df = interactions[interactions['interaction_type'] == interaction_type]
        scorer = ScorerFactory.get_scorer(interaction_type)
        weight = settings.interaction_weights.get(interaction_type, 0.1)
        total_score += scorer.calculate_score(type_df) * weight

    recency_boost = calculate_recency_boost(interactions)
    final_score = total_score * (1 + recency_boost * 0.2)

    return max(0.0, min(final_score, 5.0))


def calculate_recency_boost(interactions: pd.DataFrame) -> float:
    if interactions.empty or 'timestamp' not in interactions.columns:
        return 0.0

    # Convert timestamps to datetime if they are UNIX timestamps in milliseconds
    # If your timestamps are already datetime objects, skip this step
    if pd.api.types.is_numeric_dtype(interactions['timestamp']):
        interactions['timestamp_dt'] = pd.to_datetime(interactions['timestamp'], unit='ms')
    else:
        interactions['timestamp_dt'] = interactions['timestamp']

    now = pd.Timestamp(datetime.utcnow())

    # Calculate time difference in days from now for each interaction
    interactions['days_ago'] = (now - interactions['timestamp_dt']).dt.total_seconds() / 86400

    # Define decay period in days (e.g., 30 days, interactions older than this get zero boost)
    decay_period = 30.0

    # Calculate recency weights using exponential decay: recent = close to 1, old = close to 0
    interactions['recency_weight'] = interactions['days_ago'].apply(
        lambda x: max(0, 1 - (x / decay_period))
    )

    # Average recency weight across all interactions for this user-product pair
    recency_boost = interactions['recency_weight'].mean()

    return float(recency_boost)
