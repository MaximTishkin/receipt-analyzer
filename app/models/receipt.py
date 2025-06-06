from sqlalchemy import Column, Integer, String, DateTime, Float, ARRAY
from app.database import Base

class Receipt(Base):
    __tablename__ = "receipts"

    id = Column(Integer, primary_key=True, index=True)
    items = Column(ARRAY(String))  # Список товаров/услуг
    purchase_date = Column(DateTime)  # Дата покупки
    amount = Column(Float)  # Сумма покупки 