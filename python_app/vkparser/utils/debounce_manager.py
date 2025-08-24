import asyncio
from typing import Callable, Hashable


class DebounceManager:
    def __init__(self, delay: float = 3.0):
        self._tasks: dict[Hashable, asyncio.Task] = {}
        self._delay = delay

    def debounce(self, key: Hashable, callback: Callable[[], asyncio.Future]):
        existing_task = self._tasks.get(key)
        if existing_task:
            print(f"Отменяем задачу для пользователя: {key}")
            existing_task.cancel()

        new_task = asyncio.create_task(self._run(key, callback))
        self._tasks[key] = new_task
        print(f"Запускаем задачу для пользователя: {key}")

    async def _run(self, key: Hashable, callback):
        try:
            await asyncio.sleep(self._delay)
            # Только если задача не была перезаписана
            if self._tasks.get(key) is asyncio.current_task():
                print(f"Выполняем callback для пользователя: {key}")
                await callback()
        except asyncio.CancelledError:
            print(f"Задача отменена для пользователя: {key}")
        finally:
            if self._tasks.get(key) is asyncio.current_task():
                self._tasks.pop(key, None)

    def cancel(self, key: Hashable):
        if task := self._tasks.get(key):
            task.cancel()
            self._tasks.pop(key, None)
