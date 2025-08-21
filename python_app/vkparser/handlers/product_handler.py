from aiogram import Router, types
from aiogram.filters import Command
from aiogram.types import CallbackQuery, Message
from aiogram.fsm.context import FSMContext
from aiogram import F
from keyboards.product_keyboard import pages_keyboard
from keyboards.product_keyboard import product_menu_keyboard
from api_requests.product_requsts import get_products, get_product
from api_requests.user_requests import *
from aiogram.types.input_media_photo import InputMediaPhoto
from states.product_states import ProductState
from keyboards.product_keyboard import pages_keyboard
from utils.utils import format_product_caption_md
from logging import getLogger


logger = getLogger(__name__)

product_menu_handler = Router()

@product_menu_handler.callback_query(F.data.startswith("page:"), ProductState.choose_page)
async def show_pages(callback: CallbackQuery, state: FSMContext):
    
    logger.info(f"Нажата кнопка с выбором страницы")
    await callback.answer("")
    await callback.message.delete()
    await state.set_state(ProductState.main_show)
    logger.info(f"Установлено состояние main_show")
    
    user_id = callback.from_user.id
    
    data = await state.get_data()
    page = int(callback.data.split(":")[1])
    count = 5
    first_page_index = (page - 1) * count + 1
    current_index = data.get("current_index", None)
    current_page = data.get("current_page", None)
    total_count = data.get("total_count", None)
    if current_index == first_page_index:
        return
    product_data = await get_products(user_id, count, page)
    products = product_data.get("products", None)
    product = products[0]
    await state.update_data(current_index=first_page_index,
                            current_page=page)
    media = [InputMediaPhoto(media=url) for url in product.get('photo_urls', None)]
    photo_text = format_product_caption_md(product, first_page_index)
    media[0].caption = photo_text
    media[0].parse_mode = "MarkdownV2"
    await callback.message.answer_media_group(media=media)
        
    await callback.message.answer("Выберите действие:", reply_markup=product_menu_keyboard(first_page_index, total_count))

    
    #TODO: может пригодится
    # first_product = products[0] if len(products) > 0 else None


@product_menu_handler.callback_query(F.data.startswith("product:"), ProductState.main_show)
async def show_product(callback: CallbackQuery, state: FSMContext):
    await state.set_state(ProductState.main_show)
    await callback.answer("")
    await callback.message.delete()
    user_id = callback.from_user.id
    count = 5
    new_index = int(callback.data.split(":")[1])
    logger.info(f"Новый индекс товара: {new_index}")

    data = await state.get_data()
    
    total_count = data.get("total_count", None)
    current_index = data.get("current_index", None)
    current_page = data.get("current_page", None)
    
    #TODO можно локальный индекс тоже в кеш засунуть и потом проверку на него
    
    current_index = new_index
    current_page = ((current_index - 1) // count) + 1
    logger.info(f"Текущая страница: {current_page}")
    
    local_index = ((current_index - 1) % count) + 1
    logger.info(f"Локальный индекс товара: {local_index}")

    product_data = await get_products(user_id, count, current_page)
    products = product_data.get("products", None)
    logger.info(f"Товарная инфа: {products}")


    await state.update_data(
        current_index=current_index,
        current_page=current_page
    )
    product = products[local_index-1]

    media = [InputMediaPhoto(media=url) for url in product['photo_urls']]
    media[0].caption = format_product_caption_md(product, new_index)
    media[0].parse_mode = "MarkdownV2"

    await callback.message.answer_media_group(media=media)
    await callback.message.answer("Выберите действие:", reply_markup=product_menu_keyboard(new_index, total_count))


# TODO: подумать как реализовать разделение на страницы сами страницы
@product_menu_handler.callback_query(F.data == "choose_page", ProductState.main_show)
async def choose_page(callback: CallbackQuery, state: FSMContext):
    await state.set_state(ProductState.choose_page)
    await callback.answer("")
    await callback.message.delete()
    data = await state.get_data()
    total_count = data.get("total_count", 0)
    await callback.message.answer("Выберите страницу:", reply_markup=pages_keyboard(total_count))



@product_menu_handler.callback_query(F.data.startswith("set:"), ProductState.choose_page)
async def choose_set(callback: CallbackQuery, state: FSMContext):
    await callback.answer("")
    current_set = int(callback.data.split(":")[1])
    await state.update_data(current_set=current_set)
    data = await state.get_data()
    total_count = data.get("total_count", 0)
    await callback.message.edit_reply_markup(reply_markup=pages_keyboard(total_count, current_set=current_set))