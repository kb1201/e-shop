from pydantic import BaseModel
from typing import Optional

class ProductSchema(BaseModel):
    id: int
    name: str
    category: str
    discounted_price: float
    actual_price: float
    discount_percentage: float
    rating: Optional[float]
    rating_count: Optional[int]
    about_product: Optional[str]
    img_link: Optional[str]
    product_link: Optional[str]
    specific_category: Optional[str]
    combined_text: Optional[str]
    image_name: Optional[str]
    image_path: Optional[str]

    class Config:
        orm_mode = True
