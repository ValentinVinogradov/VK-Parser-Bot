import aiohttp
from os import getenv

BASE_URL = getenv('PARSER_CONTAINER_URL')

async def get_products(user_id):
    url = BASE_URL + f'/products/all?tg_id={user_id}'
    async with aiohttp.ClientSession() as session:
        async with session.get(url) as response:
            if response.status == 200:
                # Возвращаем информацию о пользователе
                data = await response.json()
                return data
            else:
                return None
