import aiohttp
from os import getenv

BASE_URL = getenv('PARSER_CONTAINER_URL')

# TODO: подумать как разделить на запрос используя данные из кеша и запрос на данные из бд по айди обычному
async def get_products(user_id, count, page):
    url = BASE_URL + f'/products/get-page?tg_id={user_id}&count={count}&page={page}'
    async with aiohttp.ClientSession() as session:
        async with session.get(url) as response:
            if response.status == 200:
                data = await response.json()
                return data
            else:
                return None


async def get_product(product_id):
    url = BASE_URL + f'/products/get/{product_id}'
    async with aiohttp.ClientSession() as session:
        async with session.get(url) as response:
            if response.status == 200:
                data = await response.json()
                return data
            else:
                return None
