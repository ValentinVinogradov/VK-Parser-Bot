from aiogram import Bot, Dispatcher
from aiogram.fsm.storage.redis import RedisStorage
from aiohttp import web
from handlers.main_menu_handler import main_menu_router
from handlers.start_handler import start_router
from handlers.market_handler import market_router
from handlers.account_handler import account_router
from handlers.product_handler import product_menu_handler
from api_responses import vk_auth
from http_server import create_app
import asyncio
from os import getenv
from logging import basicConfig, INFO


BOT_TOKEN = getenv('BOT_TOKEN')
if BOT_TOKEN is None:
    raise ValueError("No BOT_TOKEN found in environment variables.")

bot = Bot(token=BOT_TOKEN)
redis_url = getenv('REDIS_URL')
if redis_url is None:
    raise ValueError("No REDIS_URL found in environment variables.")

storage = RedisStorage.from_url(redis_url, state_ttl=getenv("TTL"), data_ttl=getenv("TTL"))
dp = Dispatcher(storage=storage)

dp.include_routers(main_menu_router, 
                   start_router, 
                   market_router, 
                   account_router,
                   product_menu_handler)

async def main():
    vk_auth_app = create_app() 
    
    vk_auth.bot = bot 
    
    runner = web.AppRunner(vk_auth_app)
    await runner.setup()
    
    site = web.TCPSite(runner, '0.0.0.0', int(getenv('TG_BOT_PORT')))
    
    await site.start()
    print("HTTP server is running on http://tg-bot:9090")
    
    
    print("Bot is starting...")
    await dp.start_polling(bot)

if __name__ == "__main__":
    basicConfig(level=INFO, format='%(asctime)s - %(levelname)s - %(message)s')
    asyncio.run(main())