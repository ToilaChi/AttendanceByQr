from pydantic import BaseModel
from datetime import datetime
from typing import Optional


class FaceVerificationEvent(BaseModel):
    student_cic: str
    timestamp: datetime
    success: bool
    message: str
    confidence: Optional[float] = None
    correlation_id: Optional[str] = None
