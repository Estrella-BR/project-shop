"""Selección de productos del catálogo mediante reglas (sin LLM).

El catálogo no tiene categorías, así que la validación se basa en el nombre.
"""

import re
import unicodedata

from catalog import search_products_batch
from utils import parse_price


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


def same_word(a, b):
    """Compara dos palabras admitiendo singular y plural."""

    return a == b or a in (b + "s", b + "es") or b in (a + "s", a + "es")


# ==========================================================
# REGLAS DE VALIDACIÓN
# ==========================================================

# Palabras del ingrediente que no ayudan a identificar el producto.
IGNORED_MODIFIERS = {
    "fresco", "fresca", "frescos", "frescas",
    "picado", "picada", "picados", "picadas",
    "maduro", "madura", "maduros", "maduras",
}

# Variante que se busca cuando el ingrediente es genérico.
PREFERRED_VARIANTS = {
    "aceite": "aceite de oliva",
    "sal": "sal fina",
    "harina": "harina de trigo",
    "azucar": "azucar blanco",
    "leche": "leche entera",
    "espagueti": "spaghetti",
    "espaguetis": "spaghetti",
}

# Productos elaborados o no alimentarios. Un término solo descarta el
# producto si el usuario no lo ha pedido en el ingrediente.
BLOCKED_TERMS = {
    "sorpresa", "chocolate", "chocolateado", "galleta", "bizcocho",
    "mantecado", "helado", "caramelo", "caramelizada", "snack", "tortas",
    "frito", "frita", "fritos", "fritas", "prefritas", "crujiente",
    "sabor", "relleno", "rellenos", "rellenas", "rebozado", "listo",
    "facial", "corporal", "lactantes", "continuacion", "lavavajillas",
    "limpieza", "condensada", "codorniz", "cocido", "cocidos", "cocida",
    "polvo", "salsa", "light",
}

# Términos que además descartan un ingrediente concreto.
INVALID_PRODUCT_TERMS = {
    "limon": {"refresco", "yogur", "pastel", "zumo", "bebida"},
    "tomate": {"triturado", "concentrado", "pure", "zumo", "sopa", "ketchup"},
    "patata": {"tortilla"},
}


def search_phrase(ingredient):
    """Nombre del ingrediente tal y como se busca en el catálogo."""

    # Quitar aclaraciones: "huevos (grandes)", "aceite, para freír".
    text = re.split(r"[(,]", str(ingredient or ""))[0]

    words = [
        word for word in normalize_text(text).split()
        if word not in IGNORED_MODIFIERS
    ]
    phrase = " ".join(words)

    return PREFERRED_VARIANTS.get(phrase, phrase)


def is_valid_product_match(ingredient, product_name):

    wanted = search_phrase(ingredient).split()
    product = normalize_text(product_name).split()

    if not wanted or len(product) < len(wanted):
        return False

    # El nombre del producto debe EMPEZAR por el ingrediente:
    # descarta "Tortas de aceite" al buscar "aceite".
    if not all(same_word(w, p) for w, p in zip(wanted, product)):
        return False

    # "Ajo y perejil" o "Patatas con jamón" son otro producto.
    rest = product[len(wanted):]
    if rest and rest[0] in {"y", "con", "sabor"}:
        return False

    blocked = set(BLOCKED_TERMS)
    for key, terms in INVALID_PRODUCT_TERMS.items():
        if any(same_word(key, word) for word in wanted):
            blocked |= terms

    return not any(
        word in blocked and not any(same_word(word, w) for w in wanted)
        for word in rest
    )


# ==========================================================
# SELECCIÓN
# ==========================================================

def match_catalog_product(ingredient_name, results):
    """Devuelve el mejor producto válido o None.

    Prefiere el nombre más simple y, en caso de empate,
    el más cercano en ChromaDB.
    """

    valid = [
        result for result in results
        if is_valid_product_match(
            ingredient_name,
            result["metadatos"].get("nombre", ""),
        )
    ]

    if not valid:
        return None

    best = min(
        valid,
        key=lambda r: (
            len(normalize_text(r["metadatos"].get("nombre")).split()),
            r.get("distancia", 0),
        ),
    )
    metadata = best["metadatos"]

    return {
        "nombre": metadata.get("nombre", ""),
        "precio": parse_price(metadata.get("precio")),
        "formato": metadata.get("formato"),
        "vendedor": metadata.get("vendedor"),
        "imagen": metadata.get("imagen")
    }


# ==========================================================
# COMPROBACIÓN DEL CATÁLOGO
# ==========================================================

# Los candidatos no pasan por el LLM, así que revisar muchos es barato.
MIN_CANDIDATES = 200


def check_catalog(ingredients, plato=None, top_k=8, tracker=None):
    # `plato` no se usa en la búsqueda: añadirlo hacía que se
    # recuperasen platos preparados en lugar de ingredientes.

    all_results = search_products_batch(
        [search_phrase(item["nombre"]) or item["nombre"] for item in ingredients],
        top_k=max(top_k, MIN_CANDIDATES),
        tracker=tracker,
    )

    catalog_results = []

    for ingredient, results in zip(ingredients, all_results):

        product = match_catalog_product(ingredient["nombre"], results)

        if tracker and product:
            tracker.track_match()

        catalog_results.append({
            "ingrediente": ingredient["nombre"],
            "cantidad_necesaria": ingredient["cantidad"],
            "encontrado": product is not None,
            "producto": product
        })

    return catalog_results
