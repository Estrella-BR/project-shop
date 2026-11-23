"""Clasificador de intención: ¿la pregunta es de cocina?"""

import json
import os
import re
import time

import ollama

CHAT_MODEL = os.environ.get("OLLAMA_CHAT_MODEL", "qwen2.5:1.5b")

INTENT_OPTIONS = {
    "num_thread": int(os.environ.get("OLLAMA_CHAT_THREADS", "6")),
    "num_ctx": 512,
    "num_predict": 80,
    "temperature": 0,
}

INTENT_SYSTEM_PROMPT = """Eres un clasificador binario. Recibes una pregunta de un usuario y decides si es de cocina o no.

Consideramos "cocina" CUALQUIER pregunta relacionada con:
- recetas, platos, ingredientes, técnicas culinarias 
- alimentación, comida, bebida
- productos de supermercado o precios de productos alimenticios
- compras de comida

Consideramos "no cocina" cualquier tema ajeno: matemáticas, historia, programación, ocio, política, chistes, etc.

Responde EXCLUSIVAMENTE con un JSON válido, sin texto adicional, con este formato exacto:
{"es_cocina": true|false, "motivo": "<frase de máximo 12 palabras>"}

No incluyas markdown, ni explicaciones, ni saltos de línea extra.
"""


def _extract_json(text: str) -> dict:
    """Extrae el primer objeto JSON del texto del modelo."""
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
    """
    Devuelve {'es_cocina': bool, 'motivo': str, 'raw': str}.

    Si el modelo falla o responde algo raro, se asume es_cocina=False
    para no responder cosas raras al usuario.
    """

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