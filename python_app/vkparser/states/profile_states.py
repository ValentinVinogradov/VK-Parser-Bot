from aiogram.fsm.state import State, StatesGroup

class ProfileState(StatesGroup):
    profile = State()
    choose_market = State()
    configure_vk_account = State()