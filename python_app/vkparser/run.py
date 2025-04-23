from aiogram import Bot, Dispatcher
from aiohttp import web
from handlers.handlers import router  
from webhooks import vk_auth  # Import the vk_auth module
from http_server import create_app  # Import the create_app function from http_server
import asyncio
from os import getenv


# Replace 'YOUR_BOT_TOKEN' with your actual bot token
BOT_TOKEN = getenv('BOT_TOKEN')
if BOT_TOKEN is None:
    raise ValueError("No BOT_TOKEN found in environment variables.")

# Initialize bot and dispatcher
bot = Bot(token=BOT_TOKEN)
dp = Dispatcher()

# Register the router with the dispatcher
dp.include_router(router)

# Main function to start polling
async def main():
    vk_auth_app = create_app()  # Create the aiohttp web application
    
    vk_auth.bot = bot  # Set the bot instance in the vk_auth module
    
    runner = web.AppRunner(vk_auth_app)
    await runner.setup()
    
    print("PORT:", getenv('TG_BOT_PORT'))
    
    site = web.TCPSite(runner, '0.0.0.0', int(getenv('TG_BOT_PORT')))
    
    await site.start()
    print("HTTP server is running on http://localhost:9090")
    
    
    print("Bot is starting...")
    await dp.start_polling(bot)

if __name__ == "__main__":
    asyncio.run(main())