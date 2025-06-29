import yaml
from pathlib import Path
import urllib.parse


class Config:
    # Load YAML configuration
    with open(Path(__file__).parent.parent / "application.yml", "r") as file:
        config = yaml.safe_load(file)

    # Application
    APP_NAME = config["app"]["name"]
    APP_PORT = config["app"]["port"]
    APP_ADDRESS = config["app"]["address"]

    # Database
    jdbc_url = config["database"]["url"]
    username = config["database"]["username"]
    password = config["database"]["password"]
    schema = config["database"]["schema"]

    # URL encode username and password to handle special characters
    encoded_username = urllib.parse.quote_plus(username)
    encoded_password = urllib.parse.quote_plus(password)

    if jdbc_url.startswith("jdbc:postgresql://"):
        # Extract host, port, database from JDBC URL
        url_part = jdbc_url.replace("jdbc:postgresql://", "")

        if "?" in url_part:
            host_port_db, params = url_part.split("?", 1)

            # Parse parameters and filter out PostgreSQL-incompatible ones
            parsed_params = urllib.parse.parse_qs(params)

            # Remove currentSchema and other JDBC-specific parameters
            filtered_params = {}
            for key, value in parsed_params.items():
                if key.lower() not in ["currentschema"]:
                    filtered_params[key] = (
                        value[0]
                        if isinstance(value, list) and len(value) == 1
                        else value
                    )

            # Rebuild parameter string
            if filtered_params:
                param_str = "&".join([f"{k}={v}" for k, v in filtered_params.items()])
                DATABASE_URL = f"postgresql://{encoded_username}:{encoded_password}@{host_port_db}?{param_str}"
            else:
                DATABASE_URL = (
                    f"postgresql://{encoded_username}:{encoded_password}@{host_port_db}"
                )
        else:
            DATABASE_URL = (
                f"postgresql://{encoded_username}:{encoded_password}@{url_part}"
            )
    else:
        # Fallback for other formats
        DATABASE_URL = f"postgresql://{encoded_username}:{encoded_password}@localhost:5432/postgres"

    DATABASE_SCHEMA = schema

    # Redis
    REDIS_HOST = config["redis"]["host"]
    REDIS_PORT = config["redis"]["port"]
    REDIS_TIMEOUT = config["redis"]["timeout"]

    # NATS
    NATS_URL = config["nats"]["server"]["url"]
    FACE_VERIFICATION_SUCCESS_SUBJECT = "face.verification.success"
    FACE_VERIFICATION_FAILED_SUBJECT = "face.verification.failed"
