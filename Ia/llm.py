import time

import ollama

from config.config import (
    CHAT_MODEL, CHAT_OPTIONS,
    EMBED_MODEL, EMBED_OPTIONS,
)


def chat(messages, tracker=None, **overrides):

    t0 = time.perf_counter()

    response = ollama.chat(
        model=CHAT_MODEL,
        messages=messages,
        stream=False,
        format="json",
        options=CHAT_OPTIONS,
        **overrides,
    )

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
    )

    if tracker:
        text_for_log = (
            text_or_texts
            if isinstance(text_or_texts, str)
            else " ".join(text_or_texts)
        )
        tracker.track_embed(
            text_for_log,
            elapsed_s=time.perf_counter() - t0,
        )

    return response