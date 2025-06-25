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
            if await redis.get(f"user:{user_id}:active_vk_account") is None:
                if not await self.exists_active_vk(user_id):
                    if len(await self.get_all_vk_accounts(user_id)) < 1:
                        await event.answer("🚫 Вы не вошли ни в один VK аккаунт!", 
                                        reply_markup=await vk_login_button(user_id))
                    else:
                        await event.answer("Войдите в аккаунт:")
                        # await event.answer("Войдите в аккаунт:", reply_markup=accounts_choose_keyboard("activate"))
                        #TODO: состояние
                        # await state.set_state(AccountState.choose_page)
                    return
            logger.info("Пользователь вошел")
            return await handler(event, data)

        elif event.text == "🛍 Просмотр товаров":
            if await redis.get(f"user:{user_id}:active_vk_account") is None:
                if not await self.exists_active_vk(user_id):
                    if len(await self.get_all_vk_accounts(user_id)) < 1:
                        await event.answer("🚫 Вы не вошли ни в один VK аккаунт!", 
                                        reply_markup=await vk_login_button(user_id))
                    else:
                        await event.answer("Войдите в аккаунт:", reply_markup=accounts_choose_keyboard("activate"))
                        #TODO: состояние
                        # await state.set_state(AccountState.choose_page)
                    return
            vk_account_data_raw = await redis.get(f"user:{user_id}:active_vk_account")
            vk_account_data = json.loads(vk_account_data_raw)
            vk_account_id = vk_account_data.get("id", None)
            # if await redis.get(f"user:{vk_account_id}:active_vk_market") is None:
            if await redis.get(f"user:{user_id}:active_vk_market") is None:
                if not await self.is_active_market(user_id):
                    await event.answer("🔒 У вас пока нет активного магазина.\nВыберите его в настройках, чтобы увидеть товары!")
                    return
            return await handler(event, data)


    async def exists_active_vk(self, tg_id) -> bool:
        return await check_active_vk(tg_id)
    
    
    
    async def is_active_market(self, user_id: int) -> bool:
        return await check_active_market(user_id)
    
    
    async def get_all_vk_accounts(self, tg_id: int) -> dict:
        return await get_user_vk_accounts(tg_id)