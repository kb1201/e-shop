import asyncio
import json
from datetime import datetime

from aiokafka import AIOKafkaConsumer
from typing import Dict, Any
from app.core.config import settings
from app.core.database import SessionLocal, UserInteraction
from loguru import logger
from json import JSONDecoder


async def process_user_product_event(event_data: Dict[str, Any]):
    """Process UserProductEvent and create interaction"""
    db = SessionLocal()
    try:
        logger.info("kafka_consumer.process_user_product_event started")
        user_id = event_data.get('userId')
        product_id = event_data.get('productId')
        quantity = event_data.get('quantity', 1)
        timestamp = event_data.get('timestamp')

        if not user_id or not product_id:
            logger.warning(f"Invalid event data - missing userId or productId: {event_data}")
            return

        interaction = UserInteraction(
            user_id=user_id,
            product_id=product_id,
            interaction_type='purchase',
            quantity=quantity,
            rating=None,
            timestamp=datetime.fromtimestamp(timestamp / 1000.0)
        )

        db.add(interaction)
        db.commit()

        logger.info(f"Processed UserProductEvent - User: {user_id}, Product: {product_id}, Quantity: {quantity}")

    except Exception as e:
        logger.error(f"Error processing UserProductEvent: {e}")
        db.rollback()
    finally:
        db.close()


def safe_json_deserializer(data: bytes):
    try:
        decoded_data = data.decode('utf-8')
        obj, _ = JSONDecoder().raw_decode(decoded_data)
        return obj
    except Exception as e:
        logger.error(f"Safe JSON deserialization failed: {e}; raw message: {data}")
        return {}  # Or None, depending on how you want to handle errors


class AsyncUserProductEventKafkaConsumer:
    def __init__(self):
        self.consumer: AIOKafkaConsumer | None = None
        self.running = False

    async def start(self):
        self.consumer = AIOKafkaConsumer(
            settings.kafka_topic_name,
            bootstrap_servers=settings.kafka_bootstrap_servers,
            group_id='recommendation_service',
            auto_offset_reset='latest',
            enable_auto_commit=True,
            value_deserializer=safe_json_deserializer,
        )

        await self.consumer.start()
        self.running = True
        logger.info("Async Kafka consumer started")

        try:
            async for message in self.consumer:
                if not self.running:
                    break

                try:
                    event_data = message.value
                    logger.debug(f"Received message: {event_data}")
                    await process_user_product_event(event_data)

                except json.JSONDecodeError as e:
                    logger.error(f"JSON decode error: {e}")
                except Exception as e:
                    logger.error(f"Message processing error: {e}")

        except Exception as e:
            logger.error(f"Kafka consumer error: {e}")
        finally:
            await self.consumer.stop()
            logger.info("Kafka consumer stopped")

    async def stop(self):
        self.running = False
        if self.consumer:
            await self.consumer.stop()


consumer = AsyncUserProductEventKafkaConsumer()


async def start_kafka_consumer():
    asyncio.create_task(consumer.start())
