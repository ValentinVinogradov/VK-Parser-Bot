import asyncio
from typing import Callable, Hashable


class DebounceManager:
    def __init__(self, delay: float = 10.0):
        self._tasks: dict[Hashable, asyncio.Task] = {}
        self._delay = delay

    def debounce(self, key: Hashable, callback: Callable[[], asyncio.Future]):
        if task := self._tasks.get(key):
            print("Отменяем задачу для пользователя:", key)
            task.cancel()

        task = asyncio.create_task(self._run(key, callback))
        self._tasks[key] = task

    async def _run(self, key: Hashable, callback):
        try:
            print("Запускаем задачу для пользователя:", key)
            await asyncio.sleep(self._delay)
            print(callback)
            await callback()
        except asyncio.CancelledError:
            pass
        finally:
            self._tasks.pop(key, None)

    def cancel(self, key: Hashable):
        if task := self._tasks.get(key):
            task.cancel()
            self._tasks.pop(key, None)

