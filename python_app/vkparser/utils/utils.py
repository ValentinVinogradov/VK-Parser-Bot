from datetime import datetime
from aiogram.fsm.context import FSMContext
from api_requests.user_requests import get_user_info

async def get_user_data(state: FSMContext, user_id: int):
    cache_data = await state.get_data()
    if cache_data:
        return cache_data

    vk_accounts, vk_markets = await get_user_info(user_id)
    await state.update_data(vk_accounts=vk_accounts, vk_markets=vk_markets)

    # Устанавливаем активные элементы
    for market in vk_markets:
        if market.get("is_active"):
            await state.update_data(active_market_id=market.get("id"))
    for account in vk_accounts:
        if account.get("is_active"):
            await state.update_data(active_vk_account_id=account.get("id"))

    return await state.get_data()



def format_vk_accounts(vk_accounts: list[dict]) -> str:
    if len(vk_accounts) < 2:
        result = "👤 Ваш VK аккаунт:\n"
    else:
        result = "👤 Ваши VK аккаунты:\n"

    for account in vk_accounts:
        status_emoji = "✨ " if account['is_active'] else "💤 "
        result += f"{status_emoji}{account['first_name']} {account['last_name']} ({account['screen_name']})\n"
    return result + "\n"


def format_vk_markets(vk_markets: list[dict]) -> str:
    if not vk_markets:
        return "🛒 У вас пока нет магазинов. Создайте их в VK.\n\n"

    result = "🏬 Ваш магазин:\n" if len(vk_markets) < 2 else "🏬 Ваши магазины:\n"
    for market in vk_markets:
        status_emoji = "✨ " if market['is_active'] else "💤 "
        result += f"{status_emoji} {market['name']} ({market['members_count']} подп.)\n"

    result += "\n"
    if not any(market["is_active"] for market in vk_markets):
        result += "🧊 У вас нет активного магазина. \nАктивируйте магазин в настройках.\n\n"

    return result


def escape_md(text: str) -> str:
    """Экранирует спецсимволы под MarkdownV2"""
    return (
        text.replace("_", "\\_")
            .replace("*", "\\*")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("~", "\\~")
            .replace("`", "\\`")
            .replace(">", "\\>")
            .replace("#", "\\#")
            .replace("+", "\\+")
            .replace("-", "\\-")
            .replace("=", "\\=")
            .replace("|", "\\|")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace(".", "\\.")
            .replace("!", "\\!")
    )


def format_availability(code: int) -> str:
    return {
        0: "✅ В наличии",
        1: "❌ Удалён",
        2: "🚫 Нет в наличии",
    }.get(code, "⚠️ Неизвестно")


def format_product_caption_md(product: dict, index: int) -> str:
    title = escape_md(product['title'])
    description = escape_md(product['description'])
    category = escape_md(product['category'])
    price = escape_md(str(product['price']))
    availability = escape_md(format_availability(product['availability']))
    stock = escape_md(str(product['stock_quantity']))
    likes = product.get('likes_count', 0)
    reposts = product.get('repost_count', 0)
    views = product.get('views_count', 0)
    reviews = product.get('reviews_count', 0)
    created_at = escape_md(str(product.get('created_at')))

    return (
        f"🛍️ Товар \\#{index} \\- *{title}*\n\n"
        f"💬 {description}\n\n"
        f"┌ 🗂️ Категория: `{category}`\n"
        f"├ 💵 Цена: `{price}`\n"
        f"├ 📶 Наличие: `{availability}`\n"
        f"├ 📊 Осталось: `{stock}` шт\\.\n"
        f"├ ❤️‍🔥 Лайков: `{likes}`\n"
        f"├ 🔄 Репостов: `{reposts}`\n"
        f"├ 👀 Просмотров: `{views}`\n"
        f"├ 📝 Отзывов: `{reviews}`\n"
        f"└ 🕒 Добавлен: `{created_at}`\n"
    )
