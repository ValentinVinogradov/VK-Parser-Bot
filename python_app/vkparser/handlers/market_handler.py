from aiogram import Router, types
from aiogram.filters import Command
from aiogram.types import CallbackQuery
from aiogram import F
from keyboards.market_keyboard import market_menu_keyboard
from api_requests.user_requests import *
from aiogram.fsm.context import FSMContext
from states.profile_states import ProfileState
from utils.debounce_manager import DebounceManager

market_router = Router()
debounce_manager = DebounceManager(delay=5.0)


@market_router.callback_query(F.data == "choose_market", ProfileState.profile)
async def show_market_menu(callback: CallbackQuery, state: FSMContext):
    await state.set_state(ProfileState.choose_market)
    data = await state.get_data()
    market_data = data.get("vk_markets")
    await callback.answer("")
    
    # Отправляем меню магазина
    await callback.message.answer("Выберите активный магазин:", reply_markup=await market_menu_keyboard(market_data))

# @market_router.callback_query(F.data == "back_to_profile", ProfileState.choose_market)


## TODO: добавить middleware для синхронизации с базой данных
## TODO: разобраться почему не работает обновление активного магазина в базе данных и не меняется клаваиатура
@market_router.callback_query(F.data.startswith("select_active_market:"), ProfileState.choose_market)
async def select_active_market(callback: CallbackQuery, state: FSMContext):
    
    await callback.answer("")
    market_id = callback.data.split(":")[1]
    
    data = await state.get_data()
    market_data = data.get("vk_markets")
    active_market_id = data.get("active_market_id", None)
    active_vk_account_id = data.get("active_vk_account_id")
    
    if active_market_id == market_id:
        return
    
    for market in market_data:
        if market.get("id") == market_id:
            market["active"] = True
            await state.update_data(active_market_id=market_id)
        else:
            market["active"] = False
    
    await state.update_data(vk_markets=market_data)
    
    await callback.message.edit_reply_markup(reply_markup=await market_menu_keyboard(market_data))
    
    async def debounced_update():
        await update_active_market(active_vk_account_id, market_id)
    
    print(debounced_update)
    
    debounce_manager.debounce(callback.from_user.id, debounced_update)