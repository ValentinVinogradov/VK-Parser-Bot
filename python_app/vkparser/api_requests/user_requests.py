import aiohttp
from os import getenv

BASE_URL = getenv('PARSER_CONTAINER_URL')

async def get_user_info(user_id):
    url = BASE_URL + f'/users/info?tg_id={user_id}'
    async with aiohttp.ClientSession() as session:
        async with session.get(url) as response:
            if response.status == 200:
                data = await response.json()
                vk_accounts_raw = data.get('accounts')
                user_markets_raw = data.get('markets')
                
                return vk_accounts_raw, user_markets_raw
            else:
                return None


async def create_user(user_id):
    url = BASE_URL + f'/users/create-user?tg_id={user_id}'
    async with aiohttp.ClientSession() as session:
        async with session.post(url) as response:
            if response.status == 200:
                return
            else:
                return None


async def check_login(user_id):
    url = BASE_URL + f'/users/check-login?tg_id={user_id}'
    async with aiohttp.ClientSession() as session:
        async with session.get(url) as response:
            if response.status == 200:
                return await response.json()
            else:
                return False


async def check_active_market(user_id):
    url = BASE_URL + f'/users/check-active-market?tg_id={user_id}'
    async with aiohttp.ClientSession() as session:
        async with session.get(url) as response:
            if response.status == 200:
                return await response.json()
            else:
                return False


async def update_active_market(active_vk_account_id, market_id):
    url = BASE_URL + f'/users/update-active-market?account_id={active_vk_account_id}&market_id={market_id}'
    async with aiohttp.ClientSession() as session:
        async with session.patch(url) as response:
            if response.status == 200:
                return
            else:
                return False


async def check_active_vk(tg_id: int):
    url = BASE_URL + f'/users/check-active-account/{tg_id}'
    async with aiohttp.ClientSession() as session:
        async with session.patch(url) as response:
            if response.status == 200:
                return await response.json()
            else:
                return False


async def get_user_vk_accounts(tg_id):
    url = BASE_URL + f'/users/vk-accounts?tg_id={tg_id}'
    async with aiohttp.ClientSession() as session:
        async with session.patch(url) as response:
            if response.status == 200:
                return await response.json()
            else:
                return None