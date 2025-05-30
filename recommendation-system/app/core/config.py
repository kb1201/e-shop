from pydantic_settings import BaseSettings
from pydantic import Field


class Settings(BaseSettings):
    database_url: str = Field(..., alias="DATABASE_URL")
    redis_url: str = Field(..., alias="REDIS_URL")
    kafka_bootstrap_servers: str = Field(..., alias="KAFKA_BOOTSTRAP_SERVERS")
    model_update_interval: int = Field(600, alias="MODEL_UPDATE_INTERVAL")
    api_host: str = Field("0.0.0.0", alias="API_HOST")
    api_port: int = Field(8085, alias="API_PORT")
    kafka_topic_name: str = Field("recommendation-events", alias="KAFKA_TOPIC_NAME")

    content_weight: float = Field(0.4, alias="CONTENT_WEIGHT")
    collaborative_weight: float = Field(0.6, alias="COLLABORATIVE_WEIGHT")
    svd_components: int = Field(50, alias="SVD_COMPONENTS")
    min_interactions: int = Field(10, alias="MIN_INTERACTIONS")
    interaction_weights: dict = {
        "purchase": 1.0,
        # "view": 0.2,
        # "cart_add": 0.5,
        # "rating": 1.0,
        # # add your interaction types and weights here
    }
    class Config:
        env_file = ".env"
        populate_by_name = True


settings = Settings()
