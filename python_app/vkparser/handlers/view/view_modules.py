from aiogram.fsm.context import FSMContext
from states.profile_states import ProfileState
from utils.utils import get_user_data, format_vk_accounts, format_vk_markets
from keyboards.profile_keyboard import profile_menu_keyboard
from keyboards.product_keyboard import product_menu_keyboard
from aiogram.types.input_media_photo import InputMediaPhoto
from aiogram.types import Message, CallbackQuery
from aiogram.fsm.context import FSMContext
from api_requests.product_requsts import get_products



async def build_profile_text(user_id: int, state: FSMContext) -> str:
    user_data = await get_user_data(state, user_id)
    vk_accounts = user_data.get("vk_accounts", [])
    vk_markets = user_data.get("vk_markets", [])

    vk_accounts_text = format_vk_accounts(vk_accounts)
    vk_markets_text = format_vk_markets(vk_markets)

    return "Ваш профиль:\n\n" + vk_accounts_text + vk_markets_text + "Выберите действие:"


async def show_products_view(user_id: int, count: int, page: int, send_method: Message | CallbackQuery, state: FSMContext):
    pass

async def show_that_product(user_id: int, count: int, page: int):
    pass