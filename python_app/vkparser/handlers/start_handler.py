from aiogram import Router, types
from aiogram.filters import Command
from keyboards.main_menu_keyboard import main_menu_keyboard
from api_requests.user_requests import *
from aiogram.fsm.context import FSMContext

start_router = Router()

@start_router.message(Command("start"))
async def start_handler(message: types.Message, state: FSMContext):
    await state.clear()
    await create_user(message.from_user.id)
    
    username = message.from_user.username
    
    await message.answer(f"Привет, @{username}! Я VK PARSERR! Выбери действие:", 
                        reply_markup=main_menu_keyboard())