
import json

from config.prompts import MATCH_SYSTEM_PROMPT
from llm import chat
from catalog import search_products
from utils import extract_json, parse_price


def match_catalog_product(ingredient, results, tracker=None):

    if not results:
        return None

    candidates = [
        {
            "indice": index,
            "nombre": r["metadatos"].get("nombre"),
            "precio": r["metadatos"].get("precio"),
            "formato": r["metadatos"].get("formato"),
            "vendedor": r["metadatos"].get("vendedor"),
        }
        for index, r in enumerate(results)
    ]

    messages = [
        {
            "role": "system",
            "content": MATCH_SYSTEM_PROMPT
        },
        {
            "role": "user",
            "content": json.dumps(
                {
                    "ingrediente": ingredient["nombre"],
                    "cantidad_necesaria": ingredient["cantidad"],
                    "productos_catalogo": candidates
                },
                ensure_ascii=False
            )
        }
    ]

    response = chat(
        messages,
        tracker=tracker,
        response_format="json"
    )

    selection = extract_json(response["message"]["content"])

    indices = selection.get("indices", [])

    if not isinstance(indices, list):
        return None

    valid_candidates = []

    for index in indices:

        if (
            isinstance(index, int)
            and not isinstance(index, bool)
            and 0 <= index < len(results)
        ):

            metadata = results[index]["metadatos"]

            valid_candidates.append({
                "metadata": metadata,
                "precio": parse_price(metadata.get("precio"))
            })

    if not valid_candidates:
        return None

    # Los productos con precio conocido aparecen primero.
    valid_candidates.sort(
        key=lambda p: (
            p["precio"] is None,
            p["precio"] if p["precio"] is not None else float("inf")
        )
    )

    metadata = valid_candidates[0]["metadata"]

    return {
        "nombre": metadata.get("nombre"),
        "precio": parse_price(metadata.get("precio")),
        "formato": metadata.get("formato"),
        "vendedor": metadata.get("vendedor"),
        "imagen": metadata.get("imagen")
    }


def check_catalog(ingredients, top_k=8, tracker=None):

    catalog_results = []

    for ingredient in ingredients:

        results = search_products(
            ingredient["nombre"],
            top_k=top_k,
            tracker=tracker
        )

        product = match_catalog_product(
            ingredient,
            results,
            tracker=tracker
        )

        catalog_results.append({
            "ingrediente": ingredient["nombre"],
            "cantidad_necesaria": ingredient["cantidad"],
            "encontrado": product is not None,
            "producto": product
        })

    return catalog_results
