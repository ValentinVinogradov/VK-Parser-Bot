from aiogram.types import InlineKeyboardMarkup, InlineKeyboardButton


async def account_menu_keyboard() -> InlineKeyboardMarkup:
    buttons = [
        [InlineKeyboardButton(text="🔄 Сменить аккаунт", callback_data="change_vk_account")],
        [InlineKeyboardButton(text="➕ Добавить аккаунт", callback_data="add_vk_account")],
        [InlineKeyboardButton(text="❌ Выйти из аккаунта", callback_data="logout_vk_account")],
        [InlineKeyboardButton(text="⬅️ Назад", callback_data="back_to_main_menu")]
    ]

    return InlineKeyboardMarkup(inline_keyboard=buttons)

# как то общий интерфейс для всех кнопок, чтобы не дублировать код
# async def accounts_choose_keyboard(vk_accounts: list[dict]) -> InlineKeyboardMarkup:
#     buttons = []

#     for account in vk_accounts:
#         name = account.get("firstName", "Без имени")
#         username = account.get("screenName", "Без имени")
#         account_id = account.get("id")
#         is_active = account.get("isActive", False)

#         emoji = "✨" if is_active else "💤"
#         button_text = f"{emoji} {name} (@{username}) "
#         callback_data = f"select_active_account:{account_id}"

#         buttons.append([InlineKeyboardButton(text=button_text, callback_data=callback_data)])

    # return InlineKeyboardMarkup(inline_keyboard=buttons)

async def accounts_choose_keyboard(
    vk_accounts: list[dict],
    mode: str = "activate", 
) -> InlineKeyboardMarkup:
    buttons = []

    for account in vk_accounts:
        name = account.get("firstName", "Без имени")
        username = account.get("screenName", "no_username")
        account_id = account.get("id")
        is_active = account.get("isActive", False)

        # Настройка текста и коллбэка в зависимости от режима
        if mode == "activate":
            emoji = "✨" if is_active else "💤"
            callback_data = f"activate_vk_account:{account_id}"
        elif mode == "delete":
            emoji = "🗑"
            callback_data = f"delete_vk_account:{account_id}"
        else:
            raise ValueError(f"Unsupported mode: {mode}")
        button_text = f"{emoji} {name} (@{username})"
        buttons.append([InlineKeyboardButton(text=button_text, callback_data=callback_data)])

    return InlineKeyboardMarkup(inline_keyboard=buttons)