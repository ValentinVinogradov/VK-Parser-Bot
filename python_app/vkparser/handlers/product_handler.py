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


product_menu_handler = Router()

@product_menu_handler.callback_query(F.data.startswith("page:"), ProductState.choose_page)
async def show_pages(callback: CallbackQuery, state: FSMContext):
    await callback.answer("")
    await callback.message.delete()
    await state.set_state(ProductState.main_show)
    data = await state.get_data()
    page = int(callback.data.split(":")[1])
    count = 5
    first_index = (page - 1) * count + 1
    current_index = data.get("current_index", None)
    current_page = data.get("current_page", None)
    product_list = data.get("product_list", None)
    total_count = data.get("total_count", None)
    if current_index == first_index:
        return
    if current_page == page:
        product = await get_product(product_list[str(first_index)])
        await state.update_data(current_index=first_index)
        media = [InputMediaPhoto(media=url) for url in product['photoUrls']]
        await callback.message.answer_media_group(media=media)
        
        await callback.message.answer("Выберите действие:", reply_markup=product_menu_keyboard(first_index, total_count))

    else:
        product_data = await get_products(callback.from_user.id, 
                                      count, 
                                      page)
        product_ids = product_data.get("uuids", [])
        product_list = {
            str((page - 1) * count + i + 1): uuid
            for i, uuid in enumerate(product_ids)
            }
        product = await get_product(product_list[str(first_index)])
        await state.update_data(product_list=product_list, 
                                current_index=first_index,
                                current_page=page,
                                current_set=None)
        media = [InputMediaPhoto(media=url) for url in product['photoUrls']]
        photo_text = format_product_caption_md(product, first_index)
        media[0].caption = photo_text
        media[0].parse_mode = "MarkdownV2"
        await callback.message.answer_media_group(media=media)
            
        await callback.message.answer("Выберите действие:", reply_markup=product_menu_keyboard(first_index, total_count))

    
    #TODO: может пригодится
    # first_product = products[0] if len(products) > 0 else None


@product_menu_handler.callback_query(F.data.startswith("product:"), ProductState.main_show)
async def show_product(callback: CallbackQuery, state: FSMContext):
    await state.set_state(ProductState.main_show)
    await callback.answer("")
    await callback.message.delete()
    count = 5
    new_index = int(callback.data.split(":")[1])

    data = await state.get_data()
    product_list = data.get("product_list", {})
    
    total_count = data.get("total_count", 0)
    current_index = data.get("current_index", 1)
    current_page = data.get("current_page", 1)
    

    if str(new_index) not in product_list.keys():
        new_page = current_page - 1 if new_index < current_index else current_page + 1

        product_data = await get_products(callback.from_user.id, count, new_page)
        product_ids = product_data.get("uuids", [])
        product_list = {
            str((new_page - 1) * count + i + 1): uuid
            for i, uuid in enumerate(product_ids)
        }

        await state.update_data(current_page=new_page)

    await state.update_data(
        product_list=product_list,
        current_index=new_index
    )
    product = await get_product(product_list[str(new_index)])

    media = [InputMediaPhoto(media=url) for url in product['photoUrls']]
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