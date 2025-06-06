from pydantic import BaseModel
from datetime import datetime
from typing import List

class ReceiptBase(BaseModel):
    items: List[str]
    purchase_date: datetime
    amount: float

class ReceiptCreate(ReceiptBase):
    pass

class ReceiptResponse(ReceiptBase):
    id: int

    class Config:
        from_attributes = True 