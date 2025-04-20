from aiogram import Router, types
from aiogram.filters import Command
from aiogram.types import CallbackQuery
from aiogram import F
from keyboards import main_menu_keyboard, settings_menu_keyboard, product_menu_keyboard

router = Router()

@router.message(Command("start"))
async def start_handler(message: types.Message):
    await message.answer("Добро пожаловать! Выберите действие:", reply_markup=main_menu_keyboard)

@router.message(F.text == "🛍 Просмотр товаров")
async def view_products_handler(message: types.Message):
    await message.answer("Здесь будут отображаться товары.", reply_markup=product_menu_keyboard)
    

@router.message(F.text == "⚙️ Настройки")
async def settings_handler(message: types.Message):
    await message.answer(
        "Настройки аккаунта:\n\nВы можете войти через VK или изменить привязку.",
        reply_markup=settings_menu_keyboard(message.from_user.id)
    )

# Обработка колбеков с кнопок настроек
@router.callback_query(F.data == "login_vk")
async def login_vk_callback(callback: CallbackQuery):
    await callback.answer()
    await callback.message.answer("🔗 Переход к авторизации VK...")

@router.callback_query(F.data == "switch_vk_account")
async def switch_vk_callback(callback: CallbackQuery):
    await callback.answer()
    await callback.message.answer("🔁 Смена аккаунта VK...")

@router.callback_query(F.data == "back_from_settings")
async def back_from_settings_callback(callback: CallbackQuery):
    await callback.answer()
    await callback.message.answer("🔙 Возвращаемся в главное меню...", reply_markup=main_menu_keyboard)


