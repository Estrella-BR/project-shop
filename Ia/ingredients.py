
from config.prompts import (
    INGREDIENTS_SYSTEM_PROMPT
)
from config.config import (
    CHAT_OPTIONS,
    INGREDIENTS_MAX_TOKENS,
    INGREDIENTS_SCHEMA,
)
from llm import chat
from utils import extract_json, safe_value

import re
import unicodedata


def normalize_text(text):
    text = text.lower().strip()

    text = unicodedata.normalize("NFD", text)

    text = "".join(
        c for c in text
        if unicodedata.category(c) != "Mn"
    )

    return re.sub(r"\s+", " ", text)


def extract_excluded_ingredients(question):

    exclusions = []

    patterns = [
        r"\bsin\s+([^,.!?]+)",
        r"\bno\s+quiero\s+([^,.!?]+)",
        r"\bno\s+lleve\s+([^,.!?]+)",
        r"\bno\s+lleva\s+([^,.!?]+)",
        r"\balergia\s+a\s+([^,.!?]+)"
    ]

    for pattern in patterns:
        matches = re.findall(
            pattern,
            question.lower()
        )

        exclusions.extend(matches)

    return [
        normalize_text(item)
        for item in exclusions
    ]


def is_excluded(name, exclusions):

    normalized = normalize_text(name)

    return any(
        exclusion in normalized
        or normalized in exclusion
        for exclusion in exclusions
    )

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
        response_format=INGREDIENTS_SCHEMA,
        options={
            **CHAT_OPTIONS,
            "num_predict": INGREDIENTS_MAX_TOKENS,
        },
    )

    data = extract_json(response["message"]["content"])

    ingredients = data.get("ingredientes_necesarios")

    if not isinstance(ingredients, list) or not ingredients:
        raise ValueError(
            "La IA no ha generado una lista válida de ingredientes."
        )
    
    exclusions = extract_excluded_ingredients(question)
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
        
        if is_excluded(name, exclusions):
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
