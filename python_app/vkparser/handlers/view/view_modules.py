from aiogram.fsm.context import FSMContext
from utils.utils import format_vk_accounts, format_vk_markets
from aiogram.types import Message, CallbackQuery
from aiogram.fsm.context import FSMContext
from logging import getLogger

logger = getLogger(__name__)



async def build_profile_text(vk_accounts: dict, vk_markets: dict) -> str:
    vk_accounts_text = format_vk_accounts(vk_accounts)
    logger.debug(f"Отформатировали текст для аккаунтов.")
    
    vk_markets_text = format_vk_markets(vk_markets)
    logger.debug(f"Отформатировали текст дла магазинов.")
    
    logger.debug(f"Текст готов к отображению.")

    return "Ваш профиль:\n\n" + vk_accounts_text + vk_markets_text + "Выберите действие:"