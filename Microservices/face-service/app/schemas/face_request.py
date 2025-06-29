from pydantic import BaseModel, Field
from typing import Optional


class FaceRegisterRequest(BaseModel):
    student_cic: str = Field(..., description="Student CIC number")
    correlation_id: Optional[str] = None


class FaceRegisterResponse(BaseModel):
    success: bool
    message: str
    student_cic: str
    correlation_id: Optional[str] = None


class FaceVerifyRequest(BaseModel):
    student_cic: str = Field(..., description="Student CIC number")
    correlation_id: str = Field(..., description="Correlation ID for the request")


class FaceVerifyResponse(BaseModel):
    success: bool
    message: str
    student_cic: str
    confidence: Optional[float] = None
    correlation_id: Optional[str] = None


class FaceCheckResponse(BaseModel):
    student_cic: str
    registered: bool
    message: str
