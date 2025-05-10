from aiogram.fsm.state import State, StatesGroup

class ProductState(StatesGroup):
    main_show = State()
    choose_page = State()