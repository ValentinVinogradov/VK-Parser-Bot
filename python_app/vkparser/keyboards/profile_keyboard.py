from aiogram.types import InlineKeyboardMarkup, InlineKeyboardButton
from typing import Any

def profile_menu_keyboard(vk_markets) -> InlineKeyboardMarkup:
    keyboard = [
        [InlineKeyboardButton(text="🔧 Настроить VK аккаунт", callback_data="configure_vk_account")]
    ]

    # Добавляем кнопку выбора магазина, если список не пустой
    if vk_markets:
        keyboard.append(
            [InlineKeyboardButton(text="🏬 Выбрать магазин", callback_data="choose_market")]
        )

    keyboard.append(
        [InlineKeyboardButton(text="🔄 Обновить информацию", callback_data="update_profile")]
    )

    return InlineKeyboardMarkup(inline_keyboard=keyboard)