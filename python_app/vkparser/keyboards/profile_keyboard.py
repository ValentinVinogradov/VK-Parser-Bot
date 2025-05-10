from aiogram.types import InlineKeyboardMarkup, InlineKeyboardButton
from typing import Any

def profile_menu_keyboard() -> InlineKeyboardMarkup:
    return InlineKeyboardMarkup(
        inline_keyboard=[
            [InlineKeyboardButton(text="🔧 Настроить VK аккаунт", callback_data=f"configure_vk_account")],
            [InlineKeyboardButton(text="🏬 Выбрать магазин", callback_data=f"choose_market")],
            [InlineKeyboardButton(text="🔄 Обновить информацию", callback_data=f"update_profile")]
        ]
    )