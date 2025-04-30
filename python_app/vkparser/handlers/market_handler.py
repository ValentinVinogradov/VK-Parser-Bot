from aiogram import Router, types
from aiogram.filters import Command
from aiogram.types import CallbackQuery
from aiogram import F
from keyboards.market_keyboard import market_menu_keyboard
from api_requests.user_requests import *
from aiogram.fsm.context import FSMContext
from states.profile_states import ProfileState

market_router = Router()


@market_router.callback_query(F.data == "choose_market", ProfileState.profile)
async def show_market_menu(callback: CallbackQuery, state: FSMContext):
    await state.set_state(ProfileState.choose_market)
    data = await state.get_data()
    market_data = data.get("vk_markets")
    await callback.answer("")
    
    # Отправляем меню магазина
    await callback.message.answer("Выберите активный магазин:", reply_markup=await market_menu_keyboard(market_data))
