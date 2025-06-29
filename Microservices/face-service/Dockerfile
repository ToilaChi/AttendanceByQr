FROM python:3.11-slim-bullseye

# WORKDIR /app

# # Install system dependencies
# RUN apt-get update && apt-get upgrade -y && \
#     apt-get install -y \
#     libgl1-mesa-glx \
#     libglib2.0-0 \
#     libsm6 \
#     libxext6 \
#     libxrender-dev \
#     libgomp1 \
#     libpq-dev \
#     gcc \
#     && apt-get clean \
#     && rm -rf /var/lib/apt/lists/*

# # Copy and install Python dependencies
# RUN 
# COPY requirements.txt .
# RUN pip install --no-cache-dir -r requirements.txt

# # Copy application code
# COPY . .

# # Expose port
# EXPOSE 8086

# # Run the application
# CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8086"]