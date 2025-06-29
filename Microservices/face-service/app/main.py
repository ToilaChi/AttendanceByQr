from fastapi import FastAPI, File, UploadFile, Depends, HTTPException, Form
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from app.database import get_db, create_tables
from app.services.face_service import FaceService
from app.services.nats_service import NatsService
from app.schemas import FaceRegisterResponse, FaceVerifyResponse, FaceCheckResponse
from app.config import Config
import os
import asyncio
import logging
from contextlib import asynccontextmanager
import tempfile
import aiofiles
import socket
from py_eureka_client import eureka_client
from app.config import Config

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

nats_service = NatsService()


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    logger.info("Starting Face Service...")
    try:
        # Create temp directory if not exists
        os.makedirs("temp", exist_ok=True)
        # Create database tables
        create_tables()
        # Connect to NATS
        await nats_service.connect()
        # Register service with Eureka
        await eureka_client.init_async(
            eureka_server="http://localhost:8761/eureka/",
            app_name=Config.APP_NAME.upper(),
            instance_port=Config.APP_PORT,
            instance_ip=Config.APP_ADDRESS,
            instance_id=f"{Config.APP_ADDRESS}:{Config.APP_NAME.lower()}:{Config.APP_PORT}",
        )
        logger.info("Face Service started successfully!")
    except Exception as e:
        logger.error(f"Failed to start Face Service: {str(e)}")
        raise

    yield

    # Shutdown
    logger.info("Shutting down Face Service...")
    await nats_service.close()
    logger.info("Face Service shut down successfully!")


app = FastAPI(
    title="Face Recognition Service",
    description="Face Recognition Service for Smart Attendance System",
    version="1.0.0",
    lifespan=lifespan,
)

# Add CORS middleware for development
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:3000",
        "http://127.0.0.1:3000",
    ],  # React dev server
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/")
async def root():
    return {"message": "Face Recognition Service is running", "version": "1.0.0"}


@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "face-service", "port": Config.APP_PORT}


@app.post("/face/register", response_model=FaceRegisterResponse)
async def register_face(
    student_cic: str = Form(...),
    file: UploadFile = File(...),
    correlation_id: str = Form(None),
    db: Session = Depends(get_db),
):
    """Register a face for a student"""
    temp_path = None
    try:
        # Validate file type
        if not file.content_type.startswith("image/"):
            raise HTTPException(status_code=400, detail="File phải là định dạng ảnh")

        # Create temp file
        with tempfile.NamedTemporaryFile(
            delete=False, suffix=".jpg", dir="temp"
        ) as temp_file:
            temp_path = temp_file.name
            # Write uploaded file content
            content = await file.read()
            temp_file.write(content)

        # Register face - now returns FaceVerificationEvent
        face_event = FaceService.register_face(
            db, student_cic, temp_path, correlation_id
        )

        # Publish the event to NATS
        subject = (
            Config.FACE_VERIFICATION_SUCCESS_SUBJECT
            if face_event.success
            else Config.FACE_VERIFICATION_FAILED_SUBJECT
        )

        await nats_service.publish(subject, face_event.model_dump())
        logger.info(
            f"Published face registration event: {subject} for student {student_cic}"
        )

        return FaceRegisterResponse(
            success=face_event.success,
            message=face_event.message,
            student_cic=face_event.student_cic,
            correlation_id=face_event.correlation_id,
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error registering face for {student_cic}: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Lỗi đăng ký khuôn mặt: {str(e)}")
    finally:
        # Clean up temporary file
        if temp_path and os.path.exists(temp_path):
            os.remove(temp_path)


@app.post("/face/verify", response_model=FaceVerifyResponse)
async def verify_face(
    student_cic: str = Form(...),
    file: UploadFile = File(...),
    correlation_id: str = Form(None),
    db: Session = Depends(get_db),
):
    """Verify a face for attendance"""
    temp_path = None
    try:
        # Validate file type
        if not file.content_type.startswith("image/"):
            raise HTTPException(status_code=400, detail="File phải là định dạng ảnh")

        # Create temp file
        with tempfile.NamedTemporaryFile(
            delete=False, suffix=".jpg", dir="temp"
        ) as temp_file:
            temp_path = temp_file.name
            # Write uploaded file content
            content = await file.read()
            temp_file.write(content)

        # Verify the face
        face_event = FaceService.verify_face(db, student_cic, temp_path, correlation_id)

        # Publish the event to NATS
        subject = (
            Config.FACE_VERIFICATION_SUCCESS_SUBJECT
            if face_event.success
            else Config.FACE_VERIFICATION_FAILED_SUBJECT
        )

        await nats_service.publish(subject, face_event.model_dump())
        logger.info(
            f"Published face verification event: {subject} for student {student_cic}"
        )

        return FaceVerifyResponse(
            success=face_event.success,
            message=face_event.message,
            student_cic=face_event.student_cic,
            confidence=face_event.confidence,
            correlation_id=face_event.correlation_id,
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error verifying face for {student_cic}: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Lỗi xác thực khuôn mặt: {str(e)}")
    finally:
        # Clean up temporary file
        if temp_path and os.path.exists(temp_path):
            os.remove(temp_path)


@app.get("/face/check/{student_cic}", response_model=FaceCheckResponse)
async def check_face_registration(student_cic: str, db: Session = Depends(get_db)):
    """Check if a student has registered their face"""
    try:
        from app.models import FaceEmbedding

        existing_face = (
            db.query(FaceEmbedding)
            .filter(FaceEmbedding.student_cic == student_cic)
            .first()
        )

        return FaceCheckResponse(
            student_cic=student_cic,
            registered=existing_face is not None,
            message=(
                "Đã đăng ký khuôn mặt" if existing_face else "Chưa đăng ký khuôn mặt"
            ),
        )
    except Exception as e:
        logger.error(f"Error checking face registration for {student_cic}: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Lỗi kiểm tra đăng ký: {str(e)}")


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host=Config.APP_ADDRESS, port=Config.APP_PORT)
