from apscheduler.schedulers.asyncio import AsyncIOScheduler
from app.core.config import settings
from loguru import logger

from app.core.services.recommendation_engine import recommendation_engine

scheduler = AsyncIOScheduler()


async def update_recommendation_models():
    logger.info("Starting model training...")
    try:
        recommendation_engine.train_models()
        logger.info("Models trained")
    except Exception as e:
        logger.error(f"Error while training models {e}")


async def start_scheduler():
    scheduler.add_job(
        update_recommendation_models,
        trigger='interval',
        seconds=settings.model_update_interval,
        id='update_models'
    )
    scheduler.start()
    logger.info(f"Scheduler started - every {settings.model_update_interval} ")
