from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException, BackgroundTasks
from sqlalchemy.orm import Session
from typing import List

from app.core.database import get_db, UserInteraction
from app.core.models.schemas import RecommendationRequest, InteractionCreate, PaginatedRecommendationResponse
from app.core.security import verify_jwt_token
from app.core.services.recommendation_engine import recommendation_engine
from loguru import logger

router = APIRouter()


@router.post("/recommendations", response_model=PaginatedRecommendationResponse)
async def get_recommendations_optimized(
        request: RecommendationRequest,
        db: Session = Depends(get_db),
        token_payload: dict = Depends(verify_jwt_token)
):
    user_exists = db.query(UserInteraction).filter(UserInteraction.user_id == request.user_id).first()
    if not user_exists:
        raise HTTPException(status_code=404, detail="User has no interactions")

    CACHE_SIZE = 500

    try:
        all_recommendations = recommendation_engine.get_cached_recommendations(
            user_id=request.user_id,
            cache_size=CACHE_SIZE
        )

        if not all_recommendations:
            raise HTTPException(status_code=404, detail="No recommendations available for this user")

    except Exception as e:
        logger.error(f"Error getting recommendations for user {request.user_id}: {e}")
        raise HTTPException(status_code=500, detail="Failed to generate recommendations")

    total_items = len(all_recommendations)
    total_pages = (total_items + request.page_size - 1) // request.page_size

    if request.page < 0:
        raise HTTPException(status_code=400, detail="Page number must be >= 0")

    if request.page >= total_pages and total_pages > 0:
        raise HTTPException(
            status_code=404,
            detail=f"Page {request.page} not found. Total pages available: {total_pages}"
        )

    offset = request.page * request.page_size
    start_idx = offset
    end_idx = min(offset + request.page_size, total_items)

    page_recommendations = all_recommendations[start_idx:end_idx]
    product_ids = [rec["product_id"] for rec in page_recommendations]

    has_next = request.page < total_pages - 1
    has_previous = request.page > 0

    response = PaginatedRecommendationResponse(
        recommendations=product_ids,
        pagination={
            "currentSage": request.page,
            "pageSize": request.page_size,
            "totalItems": total_items,
            "totalPages": total_pages,
            "hasNext": has_next,
            "hasPrevious": has_previous,
            "nextPage": request.page + 1 if has_next else None,
            "previousPage": request.page - 1 if has_previous else None
        },
        metadata={
            "userId": request.user_id,
            "algorithm": "hybrid",
            "generatedAt": datetime.now().isoformat(),
            "cacheSizeUsed": CACHE_SIZE,
            "actualRecommendations": total_items,
            "pageItems": len(product_ids)
        }
    )

    return response


# @router.post("/interactions")
# async def create_interaction(
#         interaction: InteractionCreate,
#         background_tasks: BackgroundTasks,
#         db: Session = Depends(get_db)
# ):
#     db_interaction = UserInteraction(**interaction.dict())
#     db.add(db_interaction)
#     db.commit()
#
#     background_tasks.add_task(check_and_update_models)
#
#     return {"message": "Interaction created"}


# @router.post("/train-models")
# async def train_models(background_tasks: BackgroundTasks):
#     background_tasks.add_task(recommendation_engine.train_models)
#     return {"message": "Treniranje modela pokrenuto u pozadini"}


async def check_and_update_models():
    """Provjeri trebaju li se ažurirati modeli"""
    pass
