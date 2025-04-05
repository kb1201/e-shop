from sqlalchemy.orm import Session
from typing import List, Optional
from .models import Product

def get_product_by_id(db: Session, product_id: int):
    return db.query(Product).filter(Product.id == product_id).first()

def search_products(db: Session, query: Optional[str] = None):
    if query:
        return db.query(Product).filter(
            (Product.name.ilike(f"%{query}%")) |
            (Product.category.ilike(f"%{query}%"))
        ).all()
    return db.query(Product).all()

def get_products_by_ids(db: Session, ids: List[int]):
    return db.query(Product).filter(Product.id.in_(ids)).all()
