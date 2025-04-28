import aiohttp
from os import getenv

BASE_URL = getenv('PARSER_CONTAINER_URL')

async def get_user_info(user_id):
    url = BASE_URL + f'/users/info?tg_id={user_id}'
    print("User info URL:", url)
    async with aiohttp.ClientSession() as session:
        async with session.get(url) as response:
            if response.status == 200:
                # Возвращаем информацию о пользователе
                data = await response.json()
                print("User data:", data)
                vk_accounts_raw = data.get('vkAccounts')
                print("VK accounts:", vk_accounts_raw)
                user_markets_raw = data.get('userMarkets')
                print("User markets:", user_markets_raw)
                
                return vk_accounts_raw, user_markets_raw
            else:
                return None

async def create_user(user_id):
    url = BASE_URL + f'/users/create-user?tg_id={user_id}'
    async with aiohttp.ClientSession() as session:
        async with session.get(url) as response:
            if response.status == 200:
                return
            else:
                return None


async def check_login(user_id):
    url = BASE_URL + f'/users/check-login?tg_id={user_id}'
    async with aiohttp.ClientSession() as session:
        async with session.get(url) as response:
            print("Check login response:", response.status)
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