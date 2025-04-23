from aiogram.types import ReplyKeyboardMarkup, KeyboardButton, InlineKeyboardMarkup, InlineKeyboardButton
from urllib.parse import urlencode, quote

from os import getenv



# Главное меню (reply-кнопки)
main_menu_keyboard = ReplyKeyboardMarkup(
    keyboard=[
        [KeyboardButton(text="🛍 Просмотр товаров")],
        [KeyboardButton(text="⚙️ Настройки")]
    ],
    resize_keyboard=True
)

# Меню настроек (inline-кнопки)
def settings_menu_keyboard(tg_id: int) -> InlineKeyboardMarkup:
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
    
    settings_menu_keyboard = InlineKeyboardMarkup(
    inline_keyboard=[
        [InlineKeyboardButton(text="🔗 Войти через VK", callback_data="login_vk", url=url)],
        [InlineKeyboardButton(text="🔁 Сменить VK аккаунт", callback_data="switch_vk_account")],
        [InlineKeyboardButton(text="🔙 Назад", callback_data="back_from_settings")]
    ])

    return settings_menu_keyboard

product_menu_keyboard = InlineKeyboardMarkup(
    inline_keyboard=[
        [InlineKeyboardButton(text="🛍 Товар «name»", callback_data="view_product")],
        [
            InlineKeyboardButton(text="⬅️ Назад", callback_data="prev_product"),
            InlineKeyboardButton(text="➡️ Далее", callback_data="next_product")
        ]
    ]
)

