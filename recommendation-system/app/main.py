from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
from contextlib import asynccontextmanager

from app.core.api.routes import router
from app.core.config import settings
from app.core.database import Base, engine

from loguru import logger

from app.core.services.kafka_consumer import start_kafka_consumer
from app.core.services.recommendation_engine import recommendation_engine
from app.core.services.scheduler import start_scheduler

Base.metadata.create_all(bind=engine)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Application started")

    recommendation_engine.load_models()

    #todo training better
    if not recommendation_engine.collaborative_is_trained:
        recommendation_engine.train_models()

    await start_kafka_consumer()

    await start_scheduler()

    logger.info("Application started")

    yield

    # Shutdown
    logger.info("Shutdown...")


app = FastAPI(
    title="Hibridni preporučivački sustav",
    description="API za hibridne preporuke proizvoda",
    version="1.0.0",
    lifespan=lifespan
)

# CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Register routes
app.include_router(router, prefix="/api/v1")


# Root endpoint
@app.get("/")
async def root():
    return {"message": "Hybrid recommendation system"}


# Health check
@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "models_trained": recommendation_engine.is_trained
    }


# Uvicorn startup
if __name__ == "__main__":
    uvicorn.run(
        "app.main:app",
        host=settings.api_host,
        port=settings.api_port,
        reload=True
    )
