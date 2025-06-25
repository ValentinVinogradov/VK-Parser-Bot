from aiogram import Router, types
from aiogram.filters import Command
from aiogram.types import CallbackQuery
from aiogram import F
from keyboards.market_keyboard import market_menu_keyboard
from api_requests.user_requests import *
from aiogram.fsm.context import FSMContext
from states.profile_states import ProfileState
from utils.debounce_manager import DebounceManager
from clients.redis_client import redis
import json
from logging import getLogger


logger = getLogger(__name__)

market_router = Router()
debounce_manager = DebounceManager(delay=5.0)


@market_router.callback_query(F.data == "choose_market", ProfileState.profile)
async def show_market_menu(callback: CallbackQuery, state: FSMContext):
    logger.info(f"Нажата кнопка choose_market")
    await callback.answer("")
    
    await state.set_state(ProfileState.choose_market)
    logger.debug(f"Установили состояние choose_market")
    
    #!
    markets = await get_user_markets(callback.from_user.id)
    
    
    
    await callback.message.answer("Выберите активный магазин:", reply_markup=await market_menu_keyboard(markets))



@market_router.callback_query(F.data.startswith("select_active_market:"), ProfileState.choose_market)
async def select_active_market(callback: CallbackQuery, state: FSMContext):
    logger.info(f"Была нажата кнопка с выбором магазина.")
    await callback.answer("")
    
    user_id = callback.from_user.id 

    market_id = callback.data.split(":")[1]
    logger.debug(f"Выбранный магазин: {market_id}")
    
    
    #????????????????????????????????????????????????????????????????????????????
    prev_active_vk_account = await redis.get(f"user:{user_id}:active_vk_account")
    logger.info(f"Предыдущий активный магазин из кеша: {prev_active_vk_account}")
    
    
    prev_active_vk_account_id = json.loads(prev_active_vk_account).get("id", None)
    logger.info(f"Айди предыдущего активного магазина из кеша: {prev_active_vk_account}")
    
    
    if prev_active_vk_account != None and \
        prev_active_vk_account_id == market_id:
        logger.info(f"Магазины совпали.")
        return
    
    
    #?????????????????????
    
    
    
    vk_markets_raw = await redis.get(f"info:{user_id}:vk_markets")
    vk_markets = json.loads(vk_markets_raw)
    logger.info(f"Магазины из кеша: {vk_markets}")
    
    if vk_markets is None or len(vk_markets) == 0:
        vk_markets = await get_user_markets(user_id)
        logger.info(f"Магазины из бд: {vk_markets}")
    
    for market in vk_markets:
        logger.debug(f"Цикл обновления активных магазинов.")
        if market.get("is_active") == True:
            active_market_id = market.get("id")
            if active_market_id == market_id:
                logger.debug(f"Магазины совпадают с данными из базы данных.")
                return
        if market.get("id") == market_id:
            logger.debug(f"Выбран активный магазин: {market_id}")
            market["is_active"] = True
            logger.debug(f"Обновили активный магазин в кеше.")
        else:
            logger.debug(f"Магазин больше не активен: {market.get("id")}")
            market["is_active"] = False
    logger.debug(f"Обновили данные магазинов в кеше.")
    await redis.set(f"info:{user_id}:vk_markets", json.dumps(vk_markets))
    
    await callback.message.edit_reply_markup(reply_markup=await market_menu_keyboard(vk_markets))
    
    async def debounced_update():
        await update_active_market(user_id, market_id)
    
    debounce_manager.debounce(callback.from_user.id, debounced_update)


@market_router.callback_query(F.data == "back_to_profile", ProfileState.choose_market)
async def back_to_profile(callback: CallbackQuery, state: FSMContext):
    await state.set_state(ProfileState.profile)
    await callback.answer("")
    await callback.message.delete()