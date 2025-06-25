from aiogram.fsm.state import State, StatesGroup

class AccountState(StatesGroup):
    choose_page = State()
    activate_page = State()
    delete_page = State()