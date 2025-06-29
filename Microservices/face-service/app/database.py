from sqlalchemy import create_engine, text
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from app.config import Config
import logging

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Create engine with proper PostgreSQL connection string
engine = create_engine(
    Config.DATABASE_URL, 
    echo=True,
    pool_pre_ping=True,  # Verify connections before use
    pool_recycle=300,    # Recycle connections every 5 minutes
    # Set default schema using connect_args
    connect_args={
        "options": f"-csearch_path={Config.DATABASE_SCHEMA}"
    } if Config.DATABASE_SCHEMA else {}
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine) 
Base = declarative_base()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

def create_tables():
    """Create all tables defined in models"""
    try:
        # Import models để SQLAlchemy biết về các table
        from app.models import FaceEmbedding
        
        logger.info("Creating database tables...")
        
        # Test connection first
        with engine.connect() as conn:
            # Test basic connection
            result = conn.execute(text("SELECT 1"))
            logger.info(f"Database connection test successful: {result.fetchone()}")
            
            # Create schema if it doesn't exist
            if Config.DATABASE_SCHEMA:
                logger.info(f"Creating schema: {Config.DATABASE_SCHEMA}")
                conn.execute(text(f"CREATE SCHEMA IF NOT EXISTS {Config.DATABASE_SCHEMA}"))
            
            # Create vector extension if it doesn't exist
            logger.info("Creating vector extension...")
            conn.execute(text("CREATE EXTENSION IF NOT EXISTS vector"))
            
            # Set search path for this session
            if Config.DATABASE_SCHEMA:
                conn.execute(text(f"SET search_path TO {Config.DATABASE_SCHEMA}, public"))
            
            conn.commit()
        
        # Set the schema for Base metadata
        if Config.DATABASE_SCHEMA:
            Base.metadata.schema = Config.DATABASE_SCHEMA
        
        # Create tables
        logger.info("Creating tables...")
        Base.metadata.create_all(bind=engine)
        logger.info("Database tables created successfully!")
        
    except Exception as e:
        logger.error(f"Error creating tables: {str(e)}")
        # Sanitize URL for logging (hide password)
        try:
            url_parts = Config.DATABASE_URL.split('@')
            if len(url_parts) > 1:
                user_pass_part = url_parts[0].split('://')[-1]
                if ':' in user_pass_part:
                    user_part = user_pass_part.split(':')[0]
                    sanitized_url = f"postgresql://{user_part}:***@{url_parts[1]}"
                else:
                    sanitized_url = f"postgresql://***@{url_parts[1]}"
                logger.error(f"Database URL (sanitized): {sanitized_url}")
            else:
                logger.error(f"Database URL format unexpected: {Config.DATABASE_URL}")
        except:
            logger.error("Could not sanitize URL for logging")
            
        # Additional debug info
        logger.error(f"Username: {Config.username if hasattr(Config, 'username') else 'Not found'}")
        logger.error(f"Schema: {Config.DATABASE_SCHEMA}")
        raise