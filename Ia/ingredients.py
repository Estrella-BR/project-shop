
from config.prompts import (
    INGREDIENTS_SYSTEM_PROMPT
)
from config.config import INGREDIENTS_SCHEMA
from llm import chat
from utils import extract_json, safe_value


def generate_ingredients(question, tracker=None):

    messages = [
        {
            "role": "system",
            "content": INGREDIENTS_SYSTEM_PROMPT
        },
        {
            "role": "user",
            "content": question
        }
    ]

    response = chat(
        messages,
        tracker=tracker,
        response_format=INGREDIENTS_SCHEMA
    )

    data = extract_json(response["message"]["content"])

    ingredients = data.get("ingredientes_necesarios")

    if not isinstance(ingredients, list) or not ingredients:
        raise ValueError(
            "La IA no ha generado una lista válida de ingredientes."
        )

    validated = []
    seen = set()

    for ingredient in ingredients:

        if not isinstance(ingredient, dict):
            continue

        name = safe_value(ingredient.get("nombre"))
        quantity = safe_value(ingredient.get("cantidad"))

        if not name:
            continue

        # Evitar ingredientes repetidos.
        normalized = name.casefold().strip()

        if normalized in seen:
            continue

        seen.add(normalized)

        validated.append({
            "nombre": name,
            "cantidad": quantity or "Cantidad no especificada"
        })

    if not validated:
        raise ValueError(
            "No se han podido validar los ingredientes."
        )

    try:
        comensales = int(data.get("comensales", 2))
    except (ValueError, TypeError):
        comensales = 2

    if comensales < 1:
        comensales = 2

    return {
        "plato": safe_value(data.get("plato"), "Plato solicitado"),
        "comensales": comensales,
        "ingredientes_necesarios": validated
    }
