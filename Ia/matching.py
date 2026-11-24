
import json
import re
import unicodedata

from config.prompts import MATCH_SYSTEM_PROMPT
from llm import chat
from catalog import search_products
from utils import extract_json, parse_price


# ==========================================================
# NORMALIZACIÓN
# ==========================================================

def normalize_text(text):
    text = str(text or "").lower().strip()

    text = unicodedata.normalize("NFD", text)

    text = "".join(
        char for char in text
        if unicodedata.category(char) != "Mn"
    )

    return re.sub(r"[^a-z0-9]+", " ", text).strip()


def get_tokens(text):
    ignored = {"de", "del", "la", "el", "los", "las", "y", "a"}

    return {
        token
        for token in normalize_text(text).split()
        if token not in ignored
    }


# ==========================================================
# VALIDACIÓN SEMÁNTICA DETERMINISTA
# ==========================================================

# Tipos de productos que no deben confundirse con ingredientes.
INVALID_PRODUCT_TERMS = {
    "aceite de oliva": {
        "pan", "barra", "galleta", "bizcocho",
        "mantecado", "snack", "patatas",
    },
    "limon": {
        "mantecado", "helado", "galleta", "bizcocho",
        "caramelo", "refresco", "yogur", "pastel",
        "zumo", "bebida",
    },
    "tomate": {
        "triturado", "frito", "salsa", "ketchup",
        "concentrado", "pure", "zumo", "sopa",
    },
}


def is_valid_product_match(ingredient, product_name):

    ingredient_normalized = normalize_text(ingredient)
    product_normalized = normalize_text(product_name)

    if not ingredient_normalized or not product_normalized:
        return False

    ingredient_tokens = get_tokens(ingredient_normalized)
    product_tokens = get_tokens(product_normalized)

    # No aceptar productos que no contengan los términos
    # principales del ingrediente.
    if not ingredient_tokens.issubset(product_tokens):
        return False

    # Evitar confusiones entre ingredientes y productos procesados.
    forbidden_terms = INVALID_PRODUCT_TERMS.get(
        ingredient_normalized,
        set()
    )

    if forbidden_terms.intersection(product_tokens):
        return False

    return True


# ==========================================================
# SELECCIÓN MEDIANTE OLLAMA
# ==========================================================

def match_catalog_product(ingredient, results, plato=None, tracker=None):

    if not results:
        return None

    candidates = []

    for index, result in enumerate(results):

        metadata = result["metadatos"]

        candidates.append({
            "indice": index,
            "nombre": metadata.get("nombre"),
            "precio": metadata.get("precio"),
            "formato": metadata.get("formato"),
            "vendedor": metadata.get("vendedor"),
            "distancia": result.get("distancia"),
        })

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

    selection = extract_json(
        response["message"]["content"]
    )

    indices = selection.get("indices", [])

    if not isinstance(indices, list):
        return None

    # Mantener el orden de relevancia propuesto por Ollama.
    # No ordenar por precio antes de validar.

    for index in indices:

        if (
            not isinstance(index, int)
            or isinstance(index, bool)
            or not 0 <= index < len(results)
        ):
            continue

        metadata = results[index]["metadatos"]

        product_name = metadata.get("nombre", "")

        # Segunda barrera: validación independiente de Ollama.
        if not is_valid_product_match(
            ingredient["nombre"],
            product_name
        ):
            continue

        return {
            "nombre": product_name,
            "precio": parse_price(metadata.get("precio")),
            "formato": metadata.get("formato"),
            "vendedor": metadata.get("vendedor"),
            "imagen": metadata.get("imagen")
        }

    return None


# ==========================================================
# COMPROBACIÓN DEL CATÁLOGO
# ==========================================================

def check_catalog(ingredients, plato=None, top_k=8, tracker=None):

    catalog_results = []

    for ingredient in ingredients:

        # Construimos una búsqueda teniendo en cuenta la receta.
        search_query = ingredient["nombre"]

        if plato:
            search_query = (
                f"{ingredient['nombre']} "
                f"para preparar {plato}"
            )

        # Recuperamos los productos más relevantes de ChromaDB.
        results = search_products(
            search_query,
            top_k=top_k,
            tracker=tracker
        )

        # Ollama selecciona teniendo en cuenta el contexto.
        product = match_catalog_product(
            ingredient,
            results,
            plato=plato,
            tracker=tracker
        )

        catalog_results.append({
            "ingrediente": ingredient["nombre"],
            "cantidad_necesaria": ingredient["cantidad"],
            "encontrado": product is not None,
            "producto": product
        })

    return catalog_results