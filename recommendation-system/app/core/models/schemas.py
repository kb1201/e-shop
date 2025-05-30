from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any


class InteractionCreate(BaseModel):
    user_id: int
    product_id: int
    interaction_type: str
    rating: Optional[float] = None
    quantity: int = 1


class RecommendationRequest(BaseModel):
    user_id: int = Field(..., alias="userId")
    # num_recommendations: int = Field(10, alias="numRecommendations")
    # exclude_purchased: bool = Field(True, alias="excludePurchased")
    page: int = Field(default=1, ge=1, description="Page number (1-based)")
    page_size: int = Field(default=10, ge=1, le=100, description="Items per page (max 100)", alias="pageSize")


class PaginatedRecommendationResponse(BaseModel):
    recommendations: List[int]
    pagination: Dict[str, Any]
    metadata: Optional[Dict[str, Any]] = None

# class RecommendationResponse(BaseModel):
#     user_id: int
#     recommendations: List[Dict[str, Any]]
#     algorithm_weights: Dict[str, float]
#     generated_at: datetime
