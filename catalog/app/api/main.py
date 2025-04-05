from fastapi import FastAPI
from .database import Base, engine
from .api import product_routes

Base.metadata.create_all(bind=engine)

app = FastAPI()
app.include_router(product_routes.router)
