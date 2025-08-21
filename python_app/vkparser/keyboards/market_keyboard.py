from aiogram.types import InlineKeyboardMarkup, InlineKeyboardButton

async def market_menu_keyboard(markets: list[dict]) -> InlineKeyboardMarkup:
    buttons = []
    for market in markets:
        name = market.get("name", "Без названия")
        market_id = market.get("id")
        is_active = market.get("is_active", False)

        emoji = "✨" if is_active else "💤"
        button_text = f"{emoji} {name}"
        callback_data = f"select_active_market:{market_id}"

        buttons.append([InlineKeyboardButton(text=button_text, callback_data=callback_data)])
    
    buttons.append([InlineKeyboardButton(text="⬅️ Назад", callback_data="back_to_profile")])

    return InlineKeyboardMarkup(inline_keyboard=buttons)