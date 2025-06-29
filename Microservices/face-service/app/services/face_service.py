from deepface import DeepFace
import numpy as np
from sqlalchemy.orm import Session
from app.models import FaceEmbedding
from app.schemas.face_event import FaceVerificationEvent
from datetime import datetime
import uuid


class FaceService:
    @staticmethod
    def register_face(
        db: Session, student_cic: str, image_path: str, correlation_id: str = None
    ) -> FaceVerificationEvent:
        try:
            # Extract face embedding using DeepFace
            embedding = DeepFace.represent(
                img_path=image_path, model_name="Facenet512"
            )[0]["embedding"]
            embedding_np = np.array(embedding)

            # Check if face already exists, if yes, update it
            existing_face = (
                db.query(FaceEmbedding)
                .filter(FaceEmbedding.student_cic == student_cic)
                .first()
            )
            if existing_face:
                existing_face.embedding = embedding_np
                message = "Cập nhật khuôn mặt thành công"
            else:
                # Save the embedding to the database
                face_embedding = FaceEmbedding(
                    student_cic=student_cic, embedding=embedding_np
                )
                db.add(face_embedding)
                message = "Đăng ký khuôn mặt thành công"

            db.commit()

            return FaceVerificationEvent(
                student_cic=student_cic,
                timestamp=datetime.now(),
                success=True,
                message=message,
                correlation_id=correlation_id,
            )
        except Exception as e:
            db.rollback()
            return FaceVerificationEvent(
                student_cic=student_cic,
                timestamp=datetime.now(),
                success=False,
                message=f"Lỗi đăng ký khuôn mặt: {str(e)}",
                correlation_id=correlation_id,
            )

    @staticmethod
    def verify_face(
        db: Session, student_cic: str, image_path: str, correlation_id: str = None
    ) -> FaceVerificationEvent:
        try:
            # Extract embedding from the input image
            input_embedding = DeepFace.represent(
                img_path=image_path, model_name="Facenet512"
            )[0]["embedding"]
            input_embedding_np = np.array(input_embedding)

            # Retrieve the stored embedding for the student
            stored_embedding = (
                db.query(FaceEmbedding)
                .filter(FaceEmbedding.student_cic == student_cic)
                .first()
            )
            if not stored_embedding:
                return FaceVerificationEvent(
                    student_cic=student_cic,
                    timestamp=datetime.now(),
                    success=False,
                    message="Chưa đăng ký khuôn mặt",
                    correlation_id=correlation_id,
                )

            # Compare embeddings using cosine similarity
            cosine_similarity = np.dot(
                input_embedding_np, stored_embedding.embedding
            ) / (
                np.linalg.norm(input_embedding_np)
                * np.linalg.norm(stored_embedding.embedding)
            )
            confidence = float(cosine_similarity)
            threshold = 0.8

            if confidence >= threshold:
                return FaceVerificationEvent(
                    student_cic=student_cic,
                    timestamp=datetime.now(),
                    success=True,
                    message="Xác thực khuôn mặt thành công",
                    confidence=confidence,
                    correlation_id=correlation_id,
                )
            else:
                return FaceVerificationEvent(
                    student_cic=student_cic,
                    timestamp=datetime.now(),
                    success=False,
                    message="Xác thực khuôn mặt không thành công",
                    confidence=confidence,
                    correlation_id=correlation_id,
                )
        except Exception as e:
            return FaceVerificationEvent(
                student_cic=student_cic,
                timestamp=datetime.now(),
                success=False,
                message=f"Face verification error: {str(e)}",
                correlation_id=correlation_id,
            )
