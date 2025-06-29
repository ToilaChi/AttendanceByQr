from nats.aio.client import Client as NATS
from app.config import Config
import json
import asyncio
import logging
from datetime import datetime


class NatsService:
    def __init__(self):
        self.nc = NATS()
        self.is_connected = False

    async def connect(self):
        try:
            await self.nc.connect(Config.NATS_URL)
            self.is_connected = True
            logging.info("Connected to NATS server")
        except Exception as e:
            logging.error(f"Failed to connect to NATS: {str(e)}")

    async def publish(self, subject: str, message: dict):
        if not self.is_connected:
            logging.error("NATS client is not connected")
            return

        try:
            # Custom json encoder handle datetime
            def json_serializer(obj):
                if isinstance(obj, datetime):
                    return obj.isoformat()
                raise TypeError(f"Object of type {type(obj)} is not JSON serializable")

            message_json = json.dumps(
                message, default=json_serializer, ensure_ascii=False
            )
            logging.info(f"About to publish to {subject}: {message_json}")
            await self.nc.publish(subject, message_json.encode())
            logging.info(f"Published message to {subject}: {message}")
        except Exception as e:
            logging.error(f"Failed to publish message to {subject}: {str(e)}")

    async def close(self):
        if self.is_connected:
            await self.nc.close()
            self.is_connected = False
            logging.info("NATS connection closed")
