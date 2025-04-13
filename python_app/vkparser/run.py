from aiogram import Bot, Dispatcher
from handlers import router  
import asyncio
from os import getenv
from dotenv import load_dotenv

load_dotenv()

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
    print("Bot is starting...")
    await dp.start_polling(bot)

if __name__ == "__main__":
    asyncio.run(main())