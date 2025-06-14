from aiogram import BaseMiddleware
from aiogram.types import Message
from typing import Callable, Awaitable, Dict, Any
from api_requests.user_requests import *
from keyboards.main_menu_keyboard import vk_login_button

class MainMenuMiddleware(BaseMiddleware):
    async def __call__(
        self,
        handler: Callable[[Message, Dict[str, Any]], Awaitable[Any]],
        event: Message,
        data: Dict[str, Any]
    ) -> Any:
        
        user_id = event.from_user.id
        
        if event.text == "👤 Профиль":
            if not await self.exists_active_vk(user_id):
                
                await event.answer("🚫 Вы не вошли ни в один VK аккаунт!", reply_markup=await vk_login_button(user_id))
                return
            return await handler(event, data)

        elif event.text == "🛍 Просмотр товаров":
            if not await self.is_user_logged_in(user_id):
                await event.answer("🚫 Вы не вошли ни в один VK аккаунт!", reply_markup=await vk_login_button(user_id))
                return

            if not await self.is_active_market(user_id):
                await event.answer("🔒 У вас пока нет активного магазина.\nВыберите его в настройках, чтобы увидеть товары!")
                return
            return await handler(event, data)


    async def exists_active_vk(self, tg_id) -> bool:
        return await check_active_vk(tg_id)
    

    # async def is_user_logged_in(self, user_id: int) -> bool:
    #     return await check_login(user_id)
    
    
    async def is_active_market(self, user_id: int) -> bool:
        return await check_active_market(user_id)
    
    async def get_all_vk_accounts(tg_id):
        return await get_user_vk_accounts(tg_id)