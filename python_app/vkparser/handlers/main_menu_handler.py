from aiogram import Router, types
from aiogram.filters import Command
from aiogram.types import CallbackQuery, Message
from aiogram.fsm.context import FSMContext
from aiogram import F
from keyboards.product_keyboard import product_menu_keyboard
from keyboards.profile_keyboard import profile_menu_keyboard
from api_requests.product_requsts import get_products, get_product
from api_requests.user_requests import *
from middlewares.main_menu_middleware import MainMenuMiddleware
from aiogram.types.input_media_photo import InputMediaPhoto
from states.profile_states import ProfileState
from states.product_states import ProductState
from collections import defaultdict
from handlers.view.view_modules import build_profile_text
from utils.utils import format_product_caption_md


main_menu_router = Router()
main_menu_router.message.middleware(MainMenuMiddleware())


# TODO: сделать как то универсальным этот метод
@main_menu_router.message(F.text == "🛍 Просмотр товаров")
async def view_first_products(message: Message, state: FSMContext):
    await state.set_state(ProductState.main_show)
    count = 5
    data = await state.get_data()
    current_index = data.get("current_index", None)
    current_page = data.get("current_page", None)
    product_list = data.get("product_list", None)
    total_count = data.get("total_count", None)
    
    if current_index is None or \
        current_page is None or \
            product_list is None or \
                total_count is None:
        current_index = 1
        current_page = 1    
        product_data = await get_products(message.from_user.id, 
                                          count, 
                                          current_page)
        product_ids = product_data.get("uuids", [])
        total_count = product_data.get("count", None)
        product_list = {i + 1: uuid for i, uuid in enumerate(product_ids)}
        await state.update_data(current_index=current_index,
                                current_page=current_page,
                                product_list=product_list,
                                total_count=total_count)
        product = await get_product(product_list[current_index])
        media = [InputMediaPhoto(media=url) for url in product['photoUrls']]
        photo_text = format_product_caption_md(product, current_index)
        media[0].caption = photo_text
        media[0].parse_mode = "MarkdownV2"
        await message.answer_media_group(media=media)
            
        await message.answer("Выберите действие:", reply_markup=product_menu_keyboard(current_index, total_count))

    else:
        product = await get_product(product_list[str(current_index)])
        media = [InputMediaPhoto(media=url) for url in product['photoUrls']]
        photo_text = format_product_caption_md(product, current_index)
        media[0].caption = photo_text
        media[0].parse_mode = "MarkdownV2"
        await message.answer_media_group(media=media)
            
        await message.answer("Выберите действие:", reply_markup=product_menu_keyboard(current_index, total_count))



# TODO: добавить кеширование для получения информации о пользователе
@main_menu_router.message(F.text == "👤 Профиль")
async def settings_handler(message: types.Message, state: FSMContext):
    await state.set_state(ProfileState.profile)
    text = await build_profile_text(message.from_user.id, state)
    await message.answer(text, reply_markup=profile_menu_keyboard())


@main_menu_router.callback_query(F.data == "update_profile")
async def update_profile_handler(callback: CallbackQuery, state: FSMContext):
    await state.set_state(ProfileState.profile)
    await callback.answer("")
    text = await build_profile_text(callback.from_user.id, state)
    if text == callback.message.text:
        return
    await callback.message.edit_text(text, reply_markup=profile_menu_keyboard())
