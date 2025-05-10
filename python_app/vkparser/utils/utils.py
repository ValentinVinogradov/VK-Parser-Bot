from datetime import datetime
from aiogram.fsm.context import FSMContext
from api_requests.user_requests import get_user_info

async def get_user_data(state: FSMContext, user_id: int):
    cache_data = await state.get_data()
    if cache_data:
        return cache_data

    # если нет в кеше — загружаем из БД
    vk_accounts, vk_markets = await get_user_info(user_id)
    await state.update_data(vk_accounts=vk_accounts, vk_markets=vk_markets)

    # Устанавливаем активные элементы
    for market in vk_markets:
        if market.get("active"):
            await state.update_data(active_market_id=market.get("id"))
    for account in vk_accounts:
        if account.get("active"):
            await state.update_data(active_vk_account_id=account.get("id"))

    return await state.get_data()



def format_vk_accounts(vk_accounts: list[dict]) -> str:
    if len(vk_accounts) < 2:
        result = "👤 Ваш VK аккаунт:\n"
    else:
        result = "👤 Ваши VK аккаунты:\n"

    for account in vk_accounts:
        status_emoji = "✨ " if account['active'] else "💤 "
        result += f"{status_emoji}{account['firstName']} {account['lastName']} ({account['screenName']})\n"
    return result + "\n"


def format_vk_markets(vk_markets: list[dict]) -> str:
    if not vk_markets:
        return "🛒 У вас пока нет магазинов. Создайте их в VK."

    result = "🏬 Ваш магазин:\n" if len(vk_markets) < 2 else "🏬 Ваши магазины:\n"
    for market in vk_markets:
        status_emoji = "✨ " if market['active'] else "💤 "
        result += f"{status_emoji}{market['name']}\n"

    result += "\n"
    if not any(market["active"] for market in vk_markets):
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
    stock = escape_md(str(product['stockQuantity']))
    likes = product.get('likesCount', 0)
    reposts = product.get('repostCount', 0)
    views = product.get('viewsCount', 0)
    reviews = product.get('reviewsCount', 0)
    created_at = escape_md(str(product.get('createdAt')))

    return (
        f"🛍️Товар \\#{index} \\- *{title}*\n\n"
        f"💬 {description}\n\n"
        f"┌📦 Категория: `{category}`\n"
        f"├ 💰 Цена: `{price}`\n"
        f"├`{availability}`\n"
        f"├ 📦 Осталось: `{stock}` шт\\.\n"
        f"├ 👍 Лайков: `{likes}`\n"
        f"├ 🔁 Репостов: `{reposts}`\n"
        f"├ 👁️ Просмотров: `{views}`\n"
        f"├ ⭐ Отзывов: `{reviews}`\n"
        f"└ 🕒 Добавлен: `{created_at}`\n"
    )
