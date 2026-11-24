"""Clasificador de intención: ¿la pregunta es de cocina?"""

import json
import os
import re
import time
from config.intent_prompts import CHAT_MODEL,INTENT_OPTIONS,INTENT_SYSTEM_PROMPT

import ollama


def _extract_json(text: str) -> dict:
    text = text.strip()
    text = re.sub(r"^```(?:json)?\s*", "", text)
    text = re.sub(r"\s*```$", "", text)

    try:
        return json.loads(text)
    except json.JSONDecodeError:
        pass

    match = re.search(r"\{.*\}", text, re.DOTALL)
    if match:
        try:
            return json.loads(match.group(0))
        except json.JSONDecodeError:
            pass

    return {}


def classify_cooking_intent(question: str, tracker=None) -> dict:

    messages = [
        {"role": "system", "content": INTENT_SYSTEM_PROMPT},
        {"role": "user", "content": question},
    ]

    t0 = time.perf_counter()
    response = ollama.chat(
        model=CHAT_MODEL,
        messages=messages,
        stream=False,
        options=INTENT_OPTIONS,
    )
    elapsed = time.perf_counter() - t0

    if tracker:
        tracker.track_chat(response, wall_time_s=elapsed)

    raw = response["message"]["content"]
    data = _extract_json(raw)

    es_cocina = data.get("es_cocina")
    if isinstance(es_cocina, str):
        es_cocina = es_cocina.strip().lower() in ("true", "1", "sí", "si", "yes")

    return {
        "es_cocina": bool(es_cocina) if es_cocina is not None else False,
        "motivo": str(data.get("motivo", "sin motivo")).strip(),
        "raw": raw,
    }