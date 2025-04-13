from aiogram.types import ReplyKeyboardMarkup, KeyboardButton, InlineKeyboardMarkup, InlineKeyboardButton
from dotenv import load_dotenv

import os


load_dotenv()



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
    vk_main_url = os.environ.get("VK_MAIN_URL")
    vk_client_id = os.getenv("VK_CLIENT_ID")
    vk_redirect_uri = os.getenv("VK_REDIRECT_URI")
    vk_scope = os.getenv("VK_SCOPE")
    vk_state = os.getenv("VK_STATE")
    code_challenge = os.getenv("CODE_CHALLENGE")
    code_challenge_method = os.getenv("CODE_CHALLENGE_METHOD")
    response_type = os.getenv("RESPONSE_TYPE")
    
    vk_redirect_uri = vk_redirect_uri + f"?tg_id={tg_id}"
    
    vk_id_url = f"{vk_main_url}?response_type={response_type}"
    vk_id_url += f"&client_id={vk_client_id}&redirect_uri={vk_redirect_uri}"
    vk_id_url += f"&scope={vk_scope}&state={vk_state}&response_type={response_type}"
    vk_id_url += f"&code_challenge={code_challenge}&code_challenge_method={code_challenge_method}"
    
    settings_menu_keyboard = InlineKeyboardMarkup(
    inline_keyboard=[
        [InlineKeyboardButton(text="🔗 Войти через VK", callback_data="login_vk", url=vk_id_url)],
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

