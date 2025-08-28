from aiogram.types import InlineKeyboardMarkup, InlineKeyboardButton

def product_menu_keyboard(current_index: int, total_count: int) -> InlineKeyboardMarkup:
    keyboard = InlineKeyboardMarkup(inline_keyboard=[])
    keyboard.inline_keyboard.append([
        InlineKeyboardButton(text="🗂️ Выбрать страницу", callback_data="choose_page")
    ])

    nav_buttons = []
    if current_index > 1:
        nav_buttons.append(InlineKeyboardButton(text="⬅️", callback_data=f"product:{current_index - 1}"))
    if current_index < total_count:
        nav_buttons.append(InlineKeyboardButton(text="➡️", callback_data=f"product:{current_index + 1}"))

    if nav_buttons:
        keyboard.inline_keyboard.append(nav_buttons)

    return keyboard



def pages_keyboard(total_count: int, current_set: int = 1, per_page: int = 5, buttons_per_set: int = 5) -> InlineKeyboardMarkup:
    total_pages = (total_count + per_page - 1) // per_page
    total_sets = (total_pages + buttons_per_set - 1) // buttons_per_set

    # Вычисляем, какие страницы показывать на текущем "экране"
    start_page = (current_set - 1) * buttons_per_set + 1

    end_page = min(start_page + buttons_per_set - 1, total_pages)

    keyboard = InlineKeyboardMarkup(inline_keyboard=[])
    page_buttons = []

    for page in range(start_page, end_page + 1):
        start = (page - 1) * per_page + 1
        end = min(page * per_page, total_count)
        text = f"📦 {start}–{end}"
        callback_data = f"page:{page}"
        page_buttons.append(InlineKeyboardButton(text=text, callback_data=callback_data))

    keyboard.inline_keyboard.append(page_buttons)

    nav_buttons = []

    if current_set > 1:
        nav_buttons.append(InlineKeyboardButton(text="⬅️ Назад", callback_data=f"set:{current_set - 1}"))

    if current_set < total_sets:
        nav_buttons.append(InlineKeyboardButton(text="➡️ Далее", callback_data=f"set:{current_set + 1}"))

    if nav_buttons:
        keyboard.inline_keyboard.append(nav_buttons)

    return keyboard
