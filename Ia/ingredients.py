
from config.prompts import (
    INGREDIENTS_SYSTEM_PROMPT
)
from config.config import (
    CHAT_OPTIONS,
    INGREDIENTS_MAX_TOKENS,
    INGREDIENTS_SCHEMA,
)
from allergens import (
    extract_excluded_ingredients,
    is_excluded,
    is_zero_quantity,
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


NUMBER_WORDS = {
    "una": 1, "uno": 1, "dos": 2, "tres": 3, "cuatro": 4, "cinco": 5,
    "seis": 6, "siete": 7, "ocho": 8, "nueve": 9, "diez": 10,
}


def extract_servings(question):
    """Número de comensales indicado en la pregunta, o None."""

    text = normalize_text(question)
    number = r"(\d{1,3}|" + "|".join(NUMBER_WORDS) + r")"
    people = r"(?:personas?|comensales?|raciones|racion)"

    match = (
        re.search(rf"\b{number}\s+{people}\b", text)
        or re.search(rf"\bpara\s+{number}\b(?!\s*(?:minutos?|horas?|dias?))", text)
    )

    if not match:
        return None

    value = match.group(1)
    servings = int(value) if value.isdigit() else NUMBER_WORDS[value]

    return servings if servings > 0 else None


def generate_ingredients(question, tracker=None):

    servings = extract_servings(question)

    messages = [
        {
            "role": "system",
            "content": INGREDIENTS_SYSTEM_PROMPT
        },
        {
            "role": "user",
            "content": f"{question}\nComensales: {servings or 2}"
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

        if not name or is_zero_quantity(quantity):
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
        comensales = servings or int(data.get("comensales", 2))
    except (ValueError, TypeError):
        comensales = 2

    if comensales < 1:
        comensales = 2

    return {
        "plato": safe_value(data.get("plato"), "Plato solicitado"),
        "comensales": comensales,
        "ingredientes_necesarios": validated
    }
