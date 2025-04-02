from sqlalchemy import Column, Integer, String, DECIMAL, ForeignKey, Text
from .database import Base

class Product(Base):
    __tablename__ = "products"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(255), nullable=False)
    category = Column(String(100), nullable=False)
    discounted_price = Column(DECIMAL(10, 2), nullable=False)
    actual_price = Column(DECIMAL(10, 2), nullable=False)
    discount_percentage = Column(DECIMAL(5, 2), nullable=False)
    rating = Column(DECIMAL(3, 2))
    rating_count = Column(Integer)
    about_product = Column(Text)
    img_link = Column(Text)
    product_link = Column(Text)
    specific_category = Column(String(100))
    combined_text = Column(Text)
    image_name = Column(String(255))
    image_path = Column(Text)

