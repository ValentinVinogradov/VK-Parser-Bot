from aiohttp import web
from aiogram import Bot
import json


bot: Bot = None 

async def healthcheck(request: web.Request):
    try:
        if request.method == "POST":
            data = await request.json()
            print(f"tg-bot: ping -> parser: {data}")
        else:
            print("tg-bot: healthcheck GET received")
        return web.Response(text="pong")

    except (KeyError, json.JSONDecodeError):
        return web.Response(status=400, text="Bad request")
    except Exception as e:
        return web.Response(status=500, text=f"Internal error: {str(e)}")