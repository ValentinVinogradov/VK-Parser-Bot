from aiogram import BaseMiddleware
from aiogram.types import Message
from typing import Callable, Awaitable, Dict, Any
from api_requests.user_requests import *
from keyboards.main_menu_keyboard import vk_login_button
from keyboards.accounts_keyboard import accounts_choose_keyboard
from aiogram.fsm.context import FSMContext
from states.account_states import AccountState
from clients.redis_client import redis
from logging import getLogger
import json


logger = getLogger(__name__)

class MainMenuMiddleware(BaseMiddleware):
    async def __call__(
        self,
        handler: Callable[[Message, Dict[str, Any]], Awaitable[Any]],
        event: Message,
        data: Dict[str, Any]
    ) -> Any:
        
        user_id = event.from_user.id
        state: FSMContext = data.get("state")
        
        if event.text == "👤 Профиль":
            if not await self.exists_active_vk(user_id):
                return await self.__show_auth_message(event, user_id, state)
            logger.info("Пользователь вошел")
            return await handler(event, data)

        elif event.text == "🛍 Просмотр товаров" or event.text == "📊 Аналитика товаров с ИИ":
            if not await self.exists_active_vk(user_id):
                return await self.__show_auth_message(event, user_id, state)
            if not await self.is_active_market(user_id):
                await event.answer("🔒 У вас пока нет активного магазина.\nВыберите его в настройках, чтобы увидеть товары!")
                return
            return await handler(event, data)


    async def __show_auth_message(self, event, user_id, state):
        vk_accounts = await self.get_all_vk_accounts(user_id)
        if len(vk_accounts) < 1:
            await event.answer("🚫 Вы не вошли ни в один VK аккаунт!", 
                            reply_markup=await vk_login_button(user_id))
        else:
            await state.set_state(AccountState.choose_page)
            await event.answer("Войдите в аккаунт:", reply_markup=await accounts_choose_keyboard(vk_accounts, "activate"))
    
    async def exists_active_vk(self, tg_id) -> bool:
        return await check_active_vk(tg_id)
    
    
    async def is_active_market(self, user_id: int) -> bool:
        return await check_active_market(user_id)
    
    
    async def get_all_vk_accounts(self, tg_id: int) -> dict:
        return await get_user_vk_accounts(tg_id)