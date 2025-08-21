from aiohttp import web
from aiogram import Bot
import json


bot: Bot = None  # Field 'bot' with type None

async def handle_vk_auth(request: web.Request):
    try:
        data = await request.json()
        tg_id = int(data.get("tg_id", None))
        user_info = data.get("user_info", None)
        
        msg = f"✅ Вы вошли как {user_info.get("first_name", None)} " \
            + f"{user_info.get("last_name")}" +"!"

        await bot.send_message(tg_id, msg)
        return web.Response(text="OK")

    except (KeyError, json.JSONDecodeError):
        return web.Response(status=400, text="Bad request")
    except Exception as e:
        return web.Response(status=500, text=f"Internal error: {str(e)}")
