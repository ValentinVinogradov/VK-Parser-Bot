from aiogram.types import InlineKeyboardMarkup, InlineKeyboardButton

def product_menu_keyboard() -> InlineKeyboardMarkup:
    return InlineKeyboardMarkup(
        inline_keyboard=[
            [InlineKeyboardButton(text="🛍 Товар «name»", callback_data="view_product")],
            [
                InlineKeyboardButton(text="⬅️ Назад", callback_data="prev_product"),
                InlineKeyboardButton(text="➡️ Далее", callback_data="next_product")
            ]
        ]
    )


