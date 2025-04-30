from aiohttp import web
from api_responses.vk_auth import handle_vk_auth
from api_responses.healthcheck_response import healthcheck

def create_app():
    app = web.Application()
    print("Creating aiohttp web application...")
    app.router.add_post("/vk/user_info", handle_vk_auth)
    app.router.add_get("/healthcheck", healthcheck)
    return app