from aiohttp import web
from api_requests.vk_auth import handle_vk_auth

def create_app():
    app = web.Application()
    print("Creating aiohttp web application...")
    app.router.add_post("/vk/user_info", handle_vk_auth)
    return app