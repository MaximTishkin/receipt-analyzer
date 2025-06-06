from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import os
from typing import List
import easyocr
import pdf2image
import numpy as np
from PIL import Image
import io
from datetime import datetime
import re

from app.database import SessionLocal, engine
from app.models import receipt as models
from app.schemas import receipt as schemas

# Инициализация FastAPI приложения
app = FastAPI(title="Receipt OCR Service")

# Настройка CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Инициализация OCR reader
reader = easyocr.Reader(['ru', 'en'])

def process_image(image) -> dict:
    """
    Обработка изображения с помощью EasyOCR и извлечение нужной информации
    """
    # Получение текста с изображения
    results = reader.readtext(np.array(image))
    
    # Извлечение текста из результатов
    text = ' '.join([result[1] for result in results])
    
    # Поиск даты (поддерживает несколько форматов)
    date_patterns = [
        r'\d{2}\.\d{2}\.\d{4}',
        r'\d{2}/\d{2}/\d{4}',
        r'\d{4}-\d{2}-\d{2}'
    ]
    
    purchase_date = None
    for pattern in date_patterns:
        dates = re.findall(pattern, text)
        if dates:
            try:
                # Пробуем разные форматы даты
                for date_format in ['%d.%m.%Y', '%d/%m/%Y', '%Y-%m-%d']:
                    try:
                        purchase_date = datetime.strptime(dates[0], date_format)
                        break
                    except ValueError:
                        continue
                if purchase_date:
                    break
            except ValueError:
                continue

    # Поиск суммы (ищем числа с двумя десятичными знаками после точки/запятой)
    amount_pattern = r'\d+[.,]\d{2}'
    amounts = re.findall(amount_pattern, text)
    amount = float(amounts[-1].replace(',', '.')) if amounts else 0.0  # берем последнее число как итоговую сумму

    # Извлечение наименований товаров/услуг
    # Предполагаем, что это строки между датой и суммой
    items = []
    for result in results:
        item_text = result[1].strip()
        # Пропускаем строки, которые похожи на дату или сумму
        if not any(re.match(pattern, item_text) for pattern in date_patterns) and \
           not re.match(amount_pattern, item_text) and \
           len(item_text) > 2:  # пропускаем слишком короткие строки
            items.append(item_text)

    return {
        "items": items,
        "purchase_date": purchase_date,
        "amount": amount
    }

@app.post("/upload/", response_model=schemas.ReceiptResponse)
async def upload_receipt(file: UploadFile = File(...)):
    """
    Загрузка и обработка чека
    """
    # Проверка расширения файла
    if not file.filename.lower().endswith(('.jpg', '.jpeg', '.pdf')):
        raise HTTPException(status_code=400, detail="Поддерживаются только файлы JPG и PDF")

    try:
        content = await file.read()
        
        if file.filename.lower().endswith('.pdf'):
            # Конвертация PDF в изображение
            images = pdf2image.convert_from_bytes(content)
            image = images[0]  # берем первую страницу
        else:
            # Открытие изображения
            image = Image.open(io.BytesIO(content))

        # Обработка изображения
        result = process_image(image)

        # Сохранение в базу данных
        db = SessionLocal()
        try:
            db_receipt = models.Receipt(
                items=result["items"],
                purchase_date=result["purchase_date"],
                amount=result["amount"]
            )
            db.add(db_receipt)
            db.commit()
            db.refresh(db_receipt)
        finally:
            db.close()

        return schemas.ReceiptResponse(
            id=db_receipt.id,
            items=result["items"],
            purchase_date=result["purchase_date"],
            amount=result["amount"]
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# Создание таблиц при запуске приложения
models.Base.metadata.create_all(bind=engine)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True) 