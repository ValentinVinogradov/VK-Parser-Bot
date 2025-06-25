from redis.asyncio import Redis
from os import getenv


redis_url = getenv("REDIS_URL", "redis://localhost:6379/0")
redis = Redis.from_url(redis_url)