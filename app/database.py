from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
import os
from dotenv import load_dotenv

# Загрузка переменных окружения
load_dotenv()

# Получение URL базы данных из переменных окружения
DATABASE_URL = os.getenv("DATABASE_URL")

# Создание движка SQLAlchemy с правильными настройками
engine = create_engine(
    DATABASE_URL,
    pool_pre_ping=True,  # проверка соединения перед использованием
    connect_args={'client_encoding': 'utf8'}
)

# Создание фабрики сессий
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# Создание базового класса для моделей
Base = declarative_base()