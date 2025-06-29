from sqlalchemy import Column, String
from pgvector.sqlalchemy import Vector
from app.database import Base

class FaceEmbedding(Base):
    __tablename__ = 'face_embeddings'
    __table_args__ = {'schema': 'face_db'}
    
    student_cic = Column(String, primary_key=True)
    embedding = Column(Vector(512), nullable=False)