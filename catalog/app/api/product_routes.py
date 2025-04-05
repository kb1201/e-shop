from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from typing import List, Optional

from .. import crud, schemas
from ..database import SessionLocal

router = APIRouter()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

@router.get("/products/{product_id}", response_model=schemas.ProductSchema)
def read_product(product_id: int, db: Session = Depends(get_db)):
    product = crud.get_product_by_id(db, product_id)
    if not product:
        raise HTTPException(status_code=404, detail="Product not found")
    return product

@router.get("/products/search/", response_model=List[schemas.ProductSchema])
def search_products(q: Optional[str] = None, db: Session = Depends(get_db)):
    return crud.search_products(db, query=q)

@router.get("/products/by_ids/", response_model=List[schemas.ProductSchema])
def get_products_by_ids(ids: List[int] = Query(...), db: Session = Depends(get_db)):
    return crud.get_products_by_ids(db, ids)
