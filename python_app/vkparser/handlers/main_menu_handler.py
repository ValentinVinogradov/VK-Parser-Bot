from aiogram import Router, types
from aiogram.filters import Command
from aiogram.types import CallbackQuery, Message
from aiogram.fsm.context import FSMContext
from aiogram import F
from clients.redis_client import redis
import json
from keyboards.product_keyboard import product_menu_keyboard
from keyboards.profile_keyboard import profile_menu_keyboard
from api_requests.product_requsts import get_products, get_product
from api_requests.user_requests import *
from middlewares.main_menu_middleware import MainMenuMiddleware
from aiogram.types.input_media_photo import InputMediaPhoto
from states.profile_states import ProfileState
from states.product_states import ProductState
from handlers.view.view_modules import build_profile_text
from utils.utils import format_product_caption_md
from logging import getLogger


logger = getLogger(__name__)


main_menu_router = Router()
main_menu_router.message.middleware(MainMenuMiddleware())


# TODO: сделать как то универсальным этот метод
@main_menu_router.message(F.text == "🛍 Просмотр товаров")
async def view_first_products(message: Message, state: FSMContext):
    logger.info(f"Нажата кнопка 🛍 Просмотр товаров")
    
    user_id = message.from_user.id
    
    await state.set_state(ProductState.main_show)
    logger.debug(f"Установили состояние main_show")
    
    count = 5
    data = await state.get_data()
    logger.debug("Текущие данные с кеша FSM: %s", data)

    current_index = data.get("current_index", None)
    logger.debug(f"Current index from cache: {current_index}")
    
    current_page = data.get("current_page", None)
    logger.debug(f"Current page from cache: {current_page}")
    
    total_count = data.get("total_count", None)
    logger.debug(f"Total count from cache: {total_count}")
    
    
    if current_index is None or \
        current_page is None or \
        total_count is None:
        
        logger.info("Первый запуск просмотра товаров — загружаем первую страницу")

        current_index = 1
        current_page = 1
        
        await state.update_data(current_index=current_index,
                                current_page=current_page,
                                total_count=total_count)
        
        logger.debug(f"Обновили данные в состоянии.")

    logger.info("Показ товара (страница: %s, индекс: %s)", current_page, current_index)
    try:
        product_data = await get_products(message.from_user.id, count, current_page)
        logger.debug(f"Получили товары: {product_data}")
        products = product_data.get("products", None)
    except Exception as e:
        logger.exception("Ошибка при получении списка товаров: %s", e)
        await message.answer("Не удалось загрузить товары. Попробуйте позже.")
        return

    logger.debug("Загружено %d айди товаров на странице %d", len(products), current_page)
    
    try:
        product = products[current_index-1]
    except Exception as e:
        logger.exception("Ошибка при получении карточки товара: %s", e)
        await message.answer("Не удалось загрузить товар. Попробуйте позже.")
        return
    
    
    logger.info(f"Получили товар: {product}")
        
    media = [InputMediaPhoto(media=url) for url in product['photo_urls']]
    photo_text = format_product_caption_md(product, current_index)
    media[0].caption = photo_text
    media[0].parse_mode = "MarkdownV2"
    
    await message.answer_media_group(media=media)
    logger.debug("Отправлены фото товара %s", product['id'])

    await message.answer("Выберите действие:", reply_markup=product_menu_keyboard(current_index, total_count))



@main_menu_router.message(F.text == "👤 Профиль")
async def profile_handler(message: types.Message, state: FSMContext):
    logger.info(f"Нажата кнопка 👤 Профиль")
    
    await state.set_state(ProfileState.profile)
    logger.debug(f"Установили состояние profile")
    
    vk_accounts, vk_markets = await get_user_info(message.from_user.id)
    logger.info(f"Получили данные для отображения.")
    logger.debug(f"Получили данные для отображения. Аккаунты: {vk_accounts}, магазины: {vk_markets}")
    
    logger.debug(f"Полученные аккаунты: {vk_accounts}, магазины: {vk_markets}")
    
    text = await build_profile_text(vk_accounts, vk_markets)
    await message.answer(text, reply_markup=profile_menu_keyboard())


@main_menu_router.callback_query(F.data == "update_profile")
async def update_profile_handler(callback: CallbackQuery, state: FSMContext):
    logger.info(f"Нажата кнопка update_profile")
    
    await state.set_state(ProfileState.profile)
    logger.debug(f"Установили состояние profile")

    await callback.answer("")
    
    vk_accounts, vk_markets = await get_user_info(callback.from_user.id)
    
    text = await build_profile_text(vk_accounts, vk_markets)
    if text == callback.message.text:
        logger.debug(f"Текст совпадает.")
        return
    await callback.message.edit_text(text, reply_markup=profile_menu_keyboard())
