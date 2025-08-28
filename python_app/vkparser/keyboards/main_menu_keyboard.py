from aiogram.types import ReplyKeyboardMarkup, KeyboardButton, InlineKeyboardMarkup, InlineKeyboardButton
from urllib.parse import urlencode, quote

from os import getenv


def main_menu_keyboard() -> ReplyKeyboardMarkup:
    return ReplyKeyboardMarkup(
        keyboard=[
            [KeyboardButton(text="👤 Профиль")],
            [KeyboardButton(text="🛍 Просмотр товаров")],
            [KeyboardButton(text="📊 Аналитика товаров с ИИ")]
        ],
        resize_keyboard=True
    )

async def vk_login_button(tg_id: int) -> InlineKeyboardMarkup:
    
    vk_auth_url = getenv("VK_AUTH_URL")
    
    params = {
        "response_type": getenv("VK_RESPONSE_TYPE"),
        "client_id": getenv("VK_CLIENT_ID"),
        "redirect_uri": getenv("VK_CLEAN_REDIRECT_URI") + f"?tg_id={tg_id}",
        "scope": getenv("VK_SCOPE"),
        "state": getenv("VK_STATE"),
        "code_challenge": getenv("VK_CODE_CHALLENGE"),
        "code_challenge_method": getenv("VK_CODE_CHALLENGE_METHOD")
    }
    
    url = "?".join([vk_auth_url, urlencode(params, quote_via=quote, safe="?=")])
    
    return InlineKeyboardMarkup(
        inline_keyboard=[
            [InlineKeyboardButton(text="🔗 Войти через VK", url=url)]
        ]
    )

