from aiogram import Router, types
from aiogram.filters import Command
from aiogram.types import CallbackQuery
from aiogram import F
from clients.redis_client import redis
import json
from keyboards.accounts_keyboard import accounts_choose_keyboard, account_menu_keyboard, add_account_keyboard
from api_requests.user_requests import *
from aiogram.fsm.context import FSMContext
from states.profile_states import ProfileState
from states.account_states import AccountState
from utils.debounce_manager import DebounceManager
from logging import getLogger


logger = getLogger(__name__)

account_router = Router()
debounce_manager = DebounceManager(delay=5.0)


@account_router.callback_query(F.data == "configure_vk_account", ProfileState.profile)
async def show_account_menu(callback: CallbackQuery, state: FSMContext):
    await state.set_state(ProfileState.configure_vk_account)
    await callback.answer()
    
    await callback.message.answer("Выберите действие:", reply_markup=await account_menu_keyboard())


#todo вынести как то кнопку эту 
@account_router.callback_query(F.data == "back_to_profile", ProfileState.configure_vk_account)
async def back_to_profile(callback: CallbackQuery, state: FSMContext):
    await state.set_state(ProfileState.profile)
    await callback.answer("")
    await callback.message.delete()


@account_router.callback_query(F.data == "back_to_account_settings", AccountState.choose_page)
async def back_to_profile(callback: CallbackQuery, state: FSMContext):
    await callback.answer("")
    await state.set_state(ProfileState.configure_vk_account)
    await callback.message.edit_reply_markup("Выберите действие:", reply_markup=await account_menu_keyboard())


@account_router.callback_query(F.data == "change_vk_account", ProfileState.configure_vk_account)
async def change_vk_account(callback: CallbackQuery, state: FSMContext):
    await callback.answer("")
    await state.set_state(AccountState.choose_page)
    
    user_id = callback.from_user.id
    
    vk_accounts = await get_user_vk_accounts(user_id)
    
    await callback.message.edit_reply_markup("Выберите аккаунт:", reply_markup=await accounts_choose_keyboard(vk_accounts, mode="activate"))
    

@account_router.callback_query(F.data == "add_vk_account", ProfileState.configure_vk_account)
async def add_vk_account(callback: CallbackQuery, state: FSMContext):
    await callback.answer("")
    await state.set_state(AccountState.choose_page)
    user_id = callback.from_user.id
    vk_accounts = await get_user_vk_accounts(user_id)
    if len(vk_accounts) > 2:
        await callback.message.answer("🚫 Нельзя привязать более 3-х аккаунтов!")
    else:
        await callback.message.edit_reply_markup("Войдите в другой аккаунт:", reply_markup=await add_account_keyboard(user_id))


@account_router.callback_query(F.data == "delete_vk_account", ProfileState.configure_vk_account)
async def logout_vk_account(callback: CallbackQuery, state: FSMContext):
    await callback.answer("")
    await state.set_state(AccountState.choose_page)
    user_id = callback.from_user.id
    vk_accounts = await get_user_vk_accounts(user_id)
    
    
    await callback.message.edit_reply_markup("Выберите VK аккаунт для выхода:", reply_markup=await accounts_choose_keyboard(vk_accounts, mode="delete"))


@account_router.callback_query(F.data.startswith("activate_vk_account:"), AccountState.choose_page)
async def activate_vk_account(callback: CallbackQuery, state: FSMContext):
    await callback.answer("")
    await state.set_state(AccountState.choose_page)
    account_id = callback.data.split(":")[1]
    
    logger.info(f"Selected vk account with id: {account_id}")
    
    user_id = callback.from_user.id
    
    vk_accounts = await get_user_vk_accounts(user_id)
    
    logger.info(f"Аккаунты: {vk_accounts}")
    
    for account in vk_accounts:
        logger.debug(f"Цикл обновления активных аккаунтов.")
        if account.get("is_active", None) == True:
            active_account_id = account.get("id")
            if active_account_id == account_id:
                logger.debug(f"Аккаунты совпадают с данными из базы данных.")
                return
        if account.get("id", None) == account_id:
            logger.debug(f"Выбран активный аккаунт: {account_id}")
            account["is_active"] = True
            logger.debug(f"Обновили активный аккаунт в кеше.")
        else:
            logger.debug(f"Аккаунт больше не активен: {account.get("id")}")
            account["is_active"] = False
    await redis.set(f"info:{user_id}:vk_accounts", json.dumps(vk_accounts))
    logger.debug(f"Обновили данные магазинов в кеше.")
    
    await callback.message.edit_reply_markup(reply_markup=await accounts_choose_keyboard(vk_accounts, mode="activate"))
    
    async def debounced_update():
        await update_active_account(user_id, account_id)
    
    debounce_manager.debounce(callback.from_user.id, debounced_update) 


@account_router.callback_query(F.data.startswith("delete_vk_account:"), AccountState.choose_page)
async def delete_vk_account(callback: CallbackQuery, state: FSMContext):
    await callback.answer("")
    await state.set_state(AccountState.choose_page)
    user_id = callback.from_user.id
    account_id = callback.data.split(":")[1]
    
    await delete_user(user_id, account_id)
    
    vk_accounts = await get_user_vk_accounts(user_id)
    
    vk_accounts = [account for account in vk_accounts if account.get("id", None) != account_id]
    
    await redis.set(f"info:{user_id}:vk_accounts", json.dumps(vk_accounts))
    
    await callback.message.edit_reply_markup(reply_markup=await accounts_choose_keyboard(vk_accounts, mode="delete"))
    
