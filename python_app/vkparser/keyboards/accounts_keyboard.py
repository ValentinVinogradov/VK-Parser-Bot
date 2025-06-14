from aiogram.types import InlineKeyboardMarkup, InlineKeyboardButton
from os import getenv
from urllib.parse import urlencode, quote


async def account_menu_keyboard() -> InlineKeyboardMarkup:
    buttons = [
        [InlineKeyboardButton(text="🔄 Сменить аккаунт", callback_data="change_vk_account")],
        [InlineKeyboardButton(text="➕ Добавить аккаунт", callback_data="add_vk_account")],
        [InlineKeyboardButton(text="❌ Удалить аккаунт", callback_data="delete_vk_account")],
        [InlineKeyboardButton(text="⬅️ Назад", callback_data="back_to_profile")]
    ]

    return InlineKeyboardMarkup(inline_keyboard=buttons)


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
    
    
# как то общий интерфейс для всех кнопок, чтобы не дублировать код
# async def accounts_choose_keyboard(vk_accounts: list[dict]) -> InlineKeyboardMarkup:
#     buttons = []

#     for account in vk_accounts:
#         name = account.get("first_name", "Без имени")
#         username = account.get("screen_name", "Без имени")
#         account_id = account.get("id")
#         is_active = account.get("is_active", False)

#         emoji = "✨" if is_active else "💤"
#         button_text = f"{emoji} {name} (@{username}) "
#         callback_data = f"select_active_account:{account_id}"

#         buttons.append([InlineKeyboardButton(text=button_text, callback_data=callback_data)])

#     return InlineKeyboardMarkup(inline_keyboard=buttons)

async def accounts_choose_keyboard(
    vk_accounts: list[dict],
    mode: str = "activate", 
) -> InlineKeyboardMarkup:
    buttons = []

    for account in vk_accounts:
        name = account.get("first_name", "Без имени")
        username = account.get("screen_name", "no_username")
        account_id = account.get("id")
        is_active = account.get("is_active", False)

        # Настройка текста и коллбэка в зависимости от режима
        if mode == "activate":
            emoji = "✨" if is_active else "💤"
            callback_data = f"activate_vk_account:{account_id}"
        elif mode == "delete":
            emoji = "🗑"
            callback_data = f"delete_account:{account_id}"
        else:
            raise ValueError(f"Unsupported mode: {mode}")
        button_text = f"{emoji} {name} (@{username})"
        buttons.append([InlineKeyboardButton(text=button_text, callback_data=callback_data)])

    return InlineKeyboardMarkup(inline_keyboard=buttons)