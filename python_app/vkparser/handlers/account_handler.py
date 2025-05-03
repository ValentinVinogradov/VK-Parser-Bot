from aiogram import Router, types
from aiogram.filters import Command
from aiogram.types import CallbackQuery
from aiogram import F
from keyboards.market_keyboard import market_menu_keyboard
from keyboards.accounts_keyboard import accounts_choose_keyboard, account_menu_keyboard
from api_requests.user_requests import *
from aiogram.fsm.context import FSMContext
from states.profile_states import ProfileState

account_router = Router()


@account_router.callback_query(F.data == "configure_vk_account", ProfileState.profile)
async def show_account_menu(callback: CallbackQuery, state: FSMContext):
    await state.set_state(ProfileState.configure_vk_account)
    await callback.answer("")
    
    await callback.message.answer("Выберите действие:", reply_markup=await account_menu_keyboard())


@account_router.callback_query(F.data == "change_vk_account", ProfileState.configure_vk_account)
async def change_vk_account(callback: CallbackQuery, state: FSMContext):
    await callback.answer("")
    
    data = await state.get_data()
    vk_accounts = data.get("vk_accounts")
    
    if len(vk_accounts) == 0:
        await callback.message.answer("У вас нет VK аккаунтов. Добавьте их в настройках.")
        return
    
    await callback.message.answer("Выберите VK аккаунт:", reply_markup=await accounts_choose_keyboard(vk_accounts, mode="activate"))

@account_router.callback_query(F.data == "add_vk_account", ProfileState.configure_vk_account)
async def add_vk_account(callback: CallbackQuery, state: FSMContext):
    await callback.answer("")
    
    await callback.message.answer("Добавление VK аккаунта. Пожалуйста, следуйте инструкциям на экране.")


@account_router.callback_query(F.data == "logout_vk_account", ProfileState.configure_vk_account)
async def logout_vk_account(callback: CallbackQuery, state: FSMContext):
    await callback.answer("")
    
    data = await state.get_data()
    vk_accounts = data.get("vk_accounts")
    
    if len(vk_accounts) == 0:
        await callback.message.answer("У вас нет VK аккаунтов.")
        return
    
    await callback.message.answer("Выберите VK аккаунт для выхода:", reply_markup=await accounts_choose_keyboard(vk_accounts, mode="delete"))


#TODO: добавить логику для смены аккаунта со связью с базой данных
@account_router.callback_query(F.data.startswith("activate_vk_account:"), ProfileState.configure_vk_account)
async def activate_vk_account(callback: CallbackQuery, state: FSMContext):
    await callback.answer("")
    
    account_id = callback.data.split(":")[1]
    
    data = await state.get_data()
    vk_accounts = data.get("vk_accounts")
    
    for account in vk_accounts:
        if account.get("id") == account_id:
            account["isActive"] = True  
        else:
            account["isActive"] = False 
    
    await state.update_data(vk_accounts=vk_accounts)
    
    await callback.message.edit_reply_markup(reply_markup=await accounts_choose_keyboard(vk_accounts, mode="activate"))
    
    await callback.message.answer("Аккаунт активирован.")


#TODO: добавить логику для удаления аккаунта из базы данных
@account_router.callback_query(F.data.startswith("delete_vk_account:"), ProfileState.configure_vk_account)
async def delete_vk_account(callback: CallbackQuery, state: FSMContext):
    await callback.answer("")
    
    account_id = callback.data.split(":")[1]
    
    data = await state.get_data()
    vk_accounts = data.get("vk_accounts")
    
    # Удаляем аккаунт из списка
    vk_accounts = [account for account in vk_accounts if account.get("id") != account_id]
    
    await state.update_data(vk_accounts=vk_accounts)
    
    await callback.message.edit_reply_markup(reply_markup=await accounts_choose_keyboard(vk_accounts, mode="activate"))
    
    await callback.message.answer("Аккаунт удален.")



## TODO: добавить middleware для синхронизации с базой данных
# @account_router.callback_query(F.data.startswith("select_active_account:"), ProfileState.configure_vk_account)
# async def select_active_market(callback: CallbackQuery, state: FSMContext):
#     # await 
#     await callback.answer("")
#     market_id = callback.data.split(":")[1]
    
#     data = await state.get_data()
#     market_data = data.get("vk_markets")
    
#     for market in market_data:
#         if market.get("id") == market_id:
#             market["isActive"] = True  
#         else:
#             market["isActive"] = False 
    
#     await state.update_data(vk_markets=market_data)
    
#     await callback.message.edit_reply_markup(reply_markup=await market_menu_keyboard(market_data))
    
    
