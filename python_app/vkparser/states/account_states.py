from aiogram.fsm.state import State, StatesGroup

class AccountState(StatesGroup):
    choose_page = State()