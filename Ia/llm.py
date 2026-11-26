import time
import ollama

from config.config import (
    CHAT_MODEL, CHAT_OPTIONS,
    EMBED_MODEL, EMBED_OPTIONS, INGREDIENTS_SCHEMA
)


def chat(messages, tracker=None, response_format=None, **overrides):
    t0 = time.perf_counter()


    params = {
        "model": CHAT_MODEL,
        "messages": messages,
        "stream": False,
        "options": CHAT_OPTIONS,
        "keep_alive": -1,
        **overrides,
    }

    # El formato solamente se aplica cuando se solicita.
    if response_format is not None:
        params["format"] = response_format

    response = ollama.chat(**params)

    if tracker:
        tracker.track_chat(
            response,
            wall_time_s=time.perf_counter() - t0,
        )

    return response


def embed(text_or_texts, tracker=None):
    t0 = time.perf_counter()

    response = ollama.embed(
        model=EMBED_MODEL,
        input=text_or_texts,
        options=EMBED_OPTIONS,
        keep_alive=-1,          # pin embed model in memory (top-level!)
    )

    if tracker:
        tracker.track_embed(elapsed_s=time.perf_counter() - t0)

    return response


def warm_up_chat_model():
    try:
        ollama.chat(
            model=CHAT_MODEL,
            messages=[{"role": "user", "content": "hi"}],
            stream=False,
            options={"num_predict": 1},
            keep_alive=-1,
        )
    except Exception as e:
        print(f"[warmup] chat warm-up failed (non-critical): {e}")


def warm_up_embed_model():
    try:
        ollama.embed(
            model=EMBED_MODEL,
            input="warmup",
            keep_alive=-1,
        )
    except Exception as e:
        print(f"[warmup] embed warm-up failed (non-critical): {e}")
