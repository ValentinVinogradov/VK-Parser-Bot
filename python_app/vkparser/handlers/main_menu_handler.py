from aiogram import Router, types
from aiogram.filters import Command
from aiogram.types import CallbackQuery
from aiogram.fsm.context import FSMContext
from aiogram import F
from keyboards.profile_keyboard import profile_menu_keyboard
from keyboards.product_keyboard import product_menu_keyboard
from api_requests.user_requests import *
from api_requests.product_requsts import get_products
from middlewares.main_menu_middleware import MainMenuMiddleware
from states.profile_states import ProfileState


main_menu_router = Router()
main_menu_router.message.middleware(MainMenuMiddleware())


@main_menu_router.message(F.text == "🛍 Просмотр товаров")
async def view_products_handler(message: types.Message, state: FSMContext):
    print(await get_products(message.from_user.id))
    await message.answer("Здесь будут отображаться товары.", reply_markup=product_menu_keyboard())


@main_menu_router.message(F.text == "👤 Профиль")
async def settings_handler(message: types.Message, state: FSMContext):
    await state.set_state(ProfileState.profile)
    
    data = await get_user_info(message.from_user.id)
    vk_markets = data[1]
    
    vk_accounts = data[0]
    
    await state.update_data(vk_accounts=vk_accounts, vk_markets=vk_markets)
    
    
    if len(vk_accounts) < 2:
        vk_accounts_text = "👤 Ваш VK аккаунт:\n"
    else:
        vk_accounts_text = "👤 Ваши VK аккаунты:\n"
    for account in vk_accounts:
        status_emoji = "✨ " if account['active'] else "💤 "
        vk_accounts_text += f"{status_emoji}{account['firstName']} {account['lastName']} ({account['screenName']})\n"
    vk_accounts_text += "\n"
    
    vk_markets_text = ""
    if len(vk_markets) == 0:
        vk_markets_text = "🛒 У вас пока нет магазинов. Создайте их в VK."
    else:
        if len(vk_markets) < 2:
            vk_markets_text = "🏬 Ваш магазин:\n"
        else:
            vk_markets_text = "🏬 Ваши магазины:\n"
        for market in vk_markets:
            status_emoji = "✨ " if market['active'] else "💤 "
            vk_markets_text += f"{status_emoji}{market['name']}\n"
        vk_markets_text += "\n" 
    
    if not any(market["active"] for market in vk_markets):
        vk_markets_text += "🧊 У вас нет активного магазина. \nАктивируйте магазин в настройках.\n\n"
    
    text = vk_accounts_text + vk_markets_text
    
    await message.answer(
        "Ваш профиль:\n\n" + text + "Выберите действие:",
        reply_markup=profile_menu_keyboard()
    )