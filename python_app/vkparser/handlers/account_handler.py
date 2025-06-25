from aiogram import Router, types
from aiogram.filters import Command
from aiogram.types import CallbackQuery
from aiogram import F
from keyboards.accounts_keyboard import accounts_choose_keyboard, account_menu_keyboard, vk_login_button
from api_requests.user_requests import *
from aiogram.fsm.context import FSMContext
from states.profile_states import ProfileState
from states.account_states import AccountState

account_router = Router()


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


@account_router.callback_query(F.data == "change_vk_account", ProfileState.configure_vk_account)
async def change_vk_account(callback: CallbackQuery, state: FSMContext):
    await callback.answer("")
    await state.set_state(AccountState.choose_page)
    
    cached_data = await state.get_data()
    
    if not cached_data:
        vk_accounts = get_user_vk_accounts(callback.from_user.id)
        # await state.update_data("vk_accounts", vk_accounts_data)
    else: 
        vk_accounts = cached_data.get("vk_accounts", [])
    
    await callback.message.answer("Выберите аккаунт:", reply_markup=await accounts_choose_keyboard(vk_accounts, mode="activate"))


@account_router.callback_query(F.data == "add_vk_account", ProfileState.configure_vk_account)
async def add_vk_account(callback: CallbackQuery, state: FSMContext):
    await callback.answer("")
    
    await callback.message.answer("Войдите в другой аккаунт:", reply_markup=await vk_login_button(callback.from_user.id))


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
@account_router.callback_query(F.data.startswith("activate_vk_account:"), AccountState.choose_page)
async def activate_vk_account(callback: CallbackQuery, state: FSMContext):
    await callback.answer("")
    await state.set_state(AccountState.activate_page)
    
    account_id = callback.data.split(":")[1]
    
    cached_data = await state.get_data()
    vk_accounts_data = cached_data.get("vk_accounts", [])
    
    #TODO
    if not vk_accounts_data:
        vk_accounts = await get_user_vk_accounts(callback.from_user.id)
        await state.update_data(vk_accounts=vk_accounts)
        vk_accounts_data = vk_accounts
    
    
    for account in vk_accounts_data:
        if account.get("is_active") == True:
            active_account_id = account.get("id")
            if active_account_id == account_id:
                return
        if account.get("id") == account_id:
            account["is_active"] = True
            await state.update_data(active_account_id=account_id)
        else:
            account["is_active"] = False 
        
    
    await state.update_data(vk_accounts=vk_accounts)
    
    await callback.message.edit_reply_markup(reply_markup=await accounts_choose_keyboard(vk_accounts, mode="activate"))
    
    await callback.message.answer(f"Вы успешно вошли в аккаунт {"name"}")


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
    
