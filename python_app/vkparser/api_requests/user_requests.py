import aiohttp
from os import getenv
from logging import getLogger


logger = getLogger(__name__)

BASE_URL = getenv('PARSER_CONTAINER_URL')

async def get_user_info(user_id):
    url = BASE_URL + f'/users/info?tg_id={user_id}'
    async with aiohttp.ClientSession() as session:
        async with session.get(url) as response:
            data = await response.json()
            logger.debug(f"Response url: {url}")
            logger.debug(f"Response status: {response.status}")
            logger.debug(f"Response data: {data}")
            if response.status == 200:
                vk_accounts_raw = data.get('accounts')
                user_markets_raw = data.get('markets')
                
                return vk_accounts_raw, user_markets_raw
            else:
                return None


async def create_user(user_id):
    url = BASE_URL + f'/users/create-user?tg_id={user_id}'
    async with aiohttp.ClientSession() as session:
        async with session.post(url) as response:
            logger.debug(f"Response url: {url}")
            logger.debug(f"Response status: {response.status}")
            if response.status == 200:
                return
            else:
                return None


async def check_login(user_id):
    url = BASE_URL + f'/users/check-login?tg_id={user_id}'
    async with aiohttp.ClientSession() as session:
        async with session.get(url) as response:
            data = await response.json()
            logger.debug(f"Response url: {url}")
            logger.debug(f"Response status: {response.status}")
            logger.debug(f"Response data: {data}")
            if response.status == 200:
                return data
            else:
                return False


async def check_active_market(user_id):
    url = BASE_URL + f'/users/check-active-market?tg_id={user_id}'
    async with aiohttp.ClientSession() as session:
        async with session.get(url) as response:
            data = await response.json()
            logger.debug(f"Response url: {url}")
            logger.debug(f"Response status: {response.status}")
            logger.debug(f"Response data: {data}")
            if response.status == 200:
                return data
            else:
                return False


async def update_active_market(tg_id, market_id):
    url = BASE_URL + f'/users/update-active-market?tg_id={tg_id}&market_id={market_id}'
    async with aiohttp.ClientSession() as session:
        async with session.patch(url) as response:
            data = await response.json()
            logger.debug(f"Response url: {url}")
            logger.debug(f"Response status: {response.status}")
            logger.debug(f"Response data: {data}")
            if response.status == 200:
                return
            else:
                return False


async def check_active_vk(tg_id: int):
    url = BASE_URL + f'/users/check-active-account?tg_id={tg_id}'
    async with aiohttp.ClientSession() as session:
        async with session.get(url) as response:
            data = await response.json()
            logger.debug(f"Response url: {url}")
            logger.debug(f"Response status: {response.status}")
            logger.debug(f"Response data: {data}")
            if response.status == 200:
                return data
            else:
                return False


async def get_user_vk_accounts(tg_id):
    url = BASE_URL + f'/users/vk-accounts?tg_id={tg_id}'
    async with aiohttp.ClientSession() as session:
        async with session.get(url) as response:
            data = await response.json()
            logger.debug(f"Response url: {url}")
            logger.debug(f"Response status: {response.status}")
            logger.debug(f"Response data: {data}")
            if response.status == 200:
                return data
            else:
                return None


async def get_user_markets(tg_id):
    url = BASE_URL + f'/users/markets?tg_id={tg_id}'
    async with aiohttp.ClientSession() as session:
        async with session.get(url) as response:
            data = await response.json()
            logger.debug(f"Response url: {url}")
            logger.debug(f"Response status: {response.status}")
            logger.debug(f"Response data: {data}")
            if response.status == 200:
                return data
            else:
                return None