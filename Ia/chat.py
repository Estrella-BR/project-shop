import os
import sys
import json
import time
from pathlib import Path

import ollama
import chromadb

from resource_tracker import ResourceTracker
from personality import PERSONALITY_PATH
from intent import classify_cooking_intent


# ==========================================================
# CONFIGURACION
# ==========================================================

BASE_DIR = Path(__file__).resolve().parent

JSON_PATH = BASE_DIR / "data" / "mercadona.json"
CHROMA_DIR = BASE_DIR / "chroma_db_rag"

CHAT_MODEL = os.environ.get("OLLAMA_CHAT_MODEL", "qwen2.5:1.5b")
EMBED_MODEL = "nomic-embed-text"

COLLECTION_NAME = "productos_mercadona"

EMBED_BATCH_SIZE = 32

EMBED_OPTIONS = {
    "num_thread": int(os.environ.get("OLLAMA_EMBED_THREADS", "2"))
}

CHAT_OPTIONS = {
    "num_thread": int(os.environ.get("OLLAMA_CHAT_THREADS", "6")),
    "num_ctx": 4096,
    "num_predict": int(os.environ.get("OLLAMA_CHAT_MAX_TOKENS", "800")),
    "temperature": 0,
}

NON_COOKING_ANSWER = (
    "Solo puedo ayudarte con preguntas relacionadas con cocina, "
    "alimentación o productos del catálogo de Mercadona."
)


# ==========================================================
# PROMPTS
# ==========================================================

INGREDIENTS_SYSTEM_PROMPT = """
Eres un asistente especializado en cocina.

Tu tarea es generar una lista de ingredientes para preparar el plato
solicitado por el usuario.

REGLAS:
- No consultes catálogos ni productos de supermercados.
- Genera únicamente ingredientes necesarios para el plato.
- No repitas ingredientes.
- Utiliza ingredientes habituales de la receta.
- No inventes ingredientes innecesarios.
- Si no se especifican comensales, utiliza 2.
- Calcula cantidades razonables para el número de comensales.
- No incluyas pasos de preparación.
- No incluyas explicaciones.

Devuelve exclusivamente un JSON válido con esta estructura:

{
    "plato": "nombre del plato",
    "comensales": 2,
    "ingredientes": [
        {
            "nombre": "Arroz",
            "cantidad": "160 g"
        }
    ]
}
"""

MATCH_SYSTEM_PROMPT = """
Eres un asistente especializado en identificar productos alimenticios.

OBJETIVO:
Determinar cuáles de los productos del catálogo son equivalentes
al ingrediente solicitado.

REGLAS:
- Utiliza exclusivamente los productos proporcionados.
- No inventes productos ni precios.
- Ten en cuenta el uso culinario del ingrediente.
- Puedes aceptar diferencias de marca y denominación comercial.
- No confundas productos similares pero diferentes.
- No consideres equivalentes productos que no sirvan para
  la misma finalidad culinaria.
- Si no hay equivalentes, devuelve una lista vacía.
- Si existen varias opciones equivalentes, devuelve todos sus índices.
- No selecciones productos únicamente porque contengan palabras similares.

Ejemplos:
- Tomate triturado puede equivaler a tomate triturado de otra marca.
- Leche entera no equivale a leche condensada.
- Aceite de oliva no equivale a aceite de girasol.
- Tomate natural no equivale necesariamente a tomate frito.

Devuelve exclusivamente JSON válido:

{
    "indices": [0, 2],
    "motivo": "Se han encontrado productos equivalentes."
}

Si no hay equivalentes:

{
    "indices": [],
    "motivo": "No se ha encontrado un producto equivalente."
}
"""


# ==========================================================
# UTILIDADES
# ==========================================================

def create_directories():
    """Crea las carpetas necesarias."""

    for directory in [
        JSON_PATH.parent,
        CHROMA_DIR,
        PERSONALITY_PATH.parent,
    ]:
        directory.mkdir(parents=True, exist_ok=True)


def load_products():
    """Carga los productos del JSON."""

    if not JSON_PATH.exists():
        raise FileNotFoundError(
            f"No existe el catálogo: {JSON_PATH}"
        )

    with open(JSON_PATH, "r", encoding="utf-8") as file:
        products = json.load(file)

    if not isinstance(products, list):
        raise ValueError(
            "El catálogo debe contener una lista de productos."
        )

    return products


def get_collection():
    """Obtiene la colección de ChromaDB."""

    client = chromadb.PersistentClient(path=str(CHROMA_DIR))

    collection = client.get_or_create_collection(
        name=COLLECTION_NAME
    )

    return collection


def safe_value(value, default=""):
    """Convierte valores a texto de forma segura."""

    if value is None:
        return default

    return str(value).strip()


def parse_price(value):
    """Convierte un precio a float."""

    if value is None:
        return None

    try:
        return float(
            str(value)
            .replace(",", ".")
            .replace("€", "")
            .strip()
        )
    except (ValueError, TypeError):
        return None


def extract_json(raw):
    """Extrae un objeto JSON de la respuesta del modelo."""

    raw = raw.strip()

    if raw.startswith("```"):
        raw = raw.replace("```json", "", 1)
        raw = raw.replace("```", "").strip()

    try:
        return json.loads(raw)

    except json.JSONDecodeError:
        raise ValueError(
            f"Respuesta JSON inválida de Ollama: {raw}"
        )


# ==========================================================
# PRODUCTOS
# ==========================================================

def product_to_text(product):
    """Transforma un producto en texto para generar embeddings."""

    name = safe_value(
        product.get("name"),
        "Producto sin nombre"
    )

    price = safe_value(
        product.get("price"),
        "Precio desconocido"
    )

    quantity = safe_value(
        product.get("cuantity", product.get("quantity")),
        "Formato desconocido"
    )

    seller = safe_value(
        product.get("seller"),
        "Supermercado desconocido"
    )

    parts = [
        f"Producto: {name}",
        f"Precio: {price} euros",
        f"Formato: {quantity}",
        f"Supermercado: {seller}",
    ]

    if product.get("category"):
        parts.append(f"Categoría: {product['category']}")

    if product.get("description"):
        parts.append(f"Descripción: {product['description']}")

    tags = product.get("tags")

    if isinstance(tags, list) and tags:
        parts.append("Etiquetas: " + ", ".join(map(str, tags)))

    return ". ".join(parts) + "."


def create_metadata(product):
    """Prepara los metadatos compatibles con ChromaDB."""

    metadata = {
        "nombre": safe_value(product.get("name"), "Sin nombre"),
        "formato": safe_value(
            product.get("cuantity", product.get("quantity")),
            "Desconocido",
        ),
        "vendedor": safe_value(
            product.get("seller"),
            "Desconocido",
        ),
    }

    price = parse_price(product.get("price"))

    if price is not None:
        metadata["precio"] = price

    image = product.get("image")

    if image:
        metadata["imagen"] = str(image)

    if product.get("category"):
        metadata["categoria"] = str(product["category"])

    return metadata


# ==========================================================
# INDEXACION RAG
# ==========================================================

def build_index(force=False, tracker: ResourceTracker | None = None):
    """Construye el índice vectorial de productos."""

    collection = get_collection()

    products = load_products()

    if not products:
        raise ValueError(
            "El catálogo de productos está vacío."
        )

    if force:
        existing_ids = collection.get()["ids"]

        if existing_ids:
            collection.delete(ids=existing_ids)

    entries = []
    used_ids = set()

    for index, product in enumerate(products):

        original_id = safe_value(
            product.get("id"),
            f"product-{index}"
        )

        product_id = original_id

        if product_id in used_ids:
            product_id = f"{original_id}-{index}"

        used_ids.add(product_id)

        entries.append((product_id, product))

    indexed_ids = set(collection.get(include=[])["ids"])

    pending = [
        (pid, product)
        for pid, product in entries
        if pid not in indexed_ids
    ]

    if not pending:
        return

    print(
        f"Indexando {len(pending)} productos...",
        file=sys.stderr,
    )

    for start in range(0, len(pending), EMBED_BATCH_SIZE):

        batch = pending[start:start + EMBED_BATCH_SIZE]

        texts = [
            product_to_text(product)
            for _, product in batch
        ]

        t0 = time.perf_counter()

        response = ollama.embed(
            model=EMBED_MODEL,
            input=texts,
            options=EMBED_OPTIONS,
        )

        elapsed = time.perf_counter() - t0

        if tracker:
            tracker.track_embed(
                " ".join(texts),
                elapsed_s=elapsed,
            )

        collection.upsert(
            ids=[pid for pid, _ in batch],
            documents=texts,
            embeddings=response["embeddings"],
            metadatas=[
                create_metadata(product)
                for _, product in batch
            ],
        )


# ==========================================================
# BUSQUEDA DE PRODUCTOS
# ==========================================================

def search_products(
    question,
    top_k=8,
    tracker: ResourceTracker | None = None,
):
    """Busca productos relacionados con una consulta en ChromaDB."""

    collection = get_collection()

    total = collection.count()

    if total == 0:
        return []

    t0 = time.perf_counter()

    embedding_response = ollama.embed(
        model=EMBED_MODEL,
        input=question,
        options=EMBED_OPTIONS,
    )

    embed_elapsed = time.perf_counter() - t0

    if tracker:
        tracker.track_embed(question, elapsed_s=embed_elapsed)

    embedding = embedding_response["embeddings"][0]

    t1 = time.perf_counter()

    results = collection.query(
        query_embeddings=[embedding],
        n_results=min(top_k, total),
        include=["documents", "metadatas", "distances"],
    )

    rag_elapsed = time.perf_counter() - t1

    documents = results["documents"][0] or []
    metadatas = results["metadatas"][0] or []
    distances = results["distances"][0] or []

    parsed = [
        {
            "texto": document,
            "metadatos": metadata,
            "distancia": distance,
        }
        for document, metadata, distance in zip(
            documents,
            metadatas,
            distances,
        )
        if document is not None
    ]

    if tracker:
        tracker.track_rag(top_k, parsed, elapsed_s=rag_elapsed)

    return parsed


# ==========================================================
# FASE 1: GENERACION DE INGREDIENTES
# ==========================================================

def generate_ingredients(question, tracker=None):
    """
    Genera los ingredientes necesarios para un plato.

    IMPORTANTE:
    Esta función no consulta ChromaDB ni recibe productos.
    """

    messages = [
        {"role": "system", "content": INGREDIENTS_SYSTEM_PROMPT},
        {"role": "user", "content": question},
    ]

    t0 = time.perf_counter()

    response = ollama.chat(
        model=CHAT_MODEL,
        messages=messages,
        stream=False,
        format="json",
        options=CHAT_OPTIONS,
    )

    elapsed = time.perf_counter() - t0

    if tracker:
        tracker.track_chat(response, wall_time_s=elapsed)

    raw = response["message"]["content"]

    data = extract_json(raw)

    ingredients = data.get("ingredientes")

    if not isinstance(ingredients, list) or not ingredients:
        raise ValueError(
            "La IA no ha generado una lista válida de ingredientes."
        )

    validated = []

    for ingredient in ingredients:

        if not isinstance(ingredient, dict):
            continue

        name = safe_value(ingredient.get("nombre"))
        quantity = safe_value(ingredient.get("cantidad"))

        if not name:
            continue

        validated.append({
            "nombre": name,
            "cantidad": quantity or "Cantidad no especificada",
        })

    if not validated:
        raise ValueError(
            "No se han podido validar los ingredientes."
        )

    return {
        "plato": safe_value(
            data.get("plato"),
            "Plato solicitado",
        ),
        "comensales": data.get("comensales", 2),
        "ingredientes": validated,
    }


# ==========================================================
# FASE 2: IDENTIFICACION DE PRODUCTOS
# ==========================================================

def match_catalog_product(ingredient, results, tracker=None):
    """
    Identifica productos equivalentes al ingrediente.

    El modelo identifica los candidatos.
    Python selecciona el producto con menor precio.
    """

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
        })

    messages = [
        {"role": "system", "content": MATCH_SYSTEM_PROMPT},
        {
            "role": "user",
            "content": json.dumps(
                {
                    "ingrediente": ingredient["nombre"],
                    "cantidad_necesaria": ingredient["cantidad"],
                    "productos_catalogo": candidates,
                },
                ensure_ascii=False,
            ),
        },
    ]

    t0 = time.perf_counter()

    response = ollama.chat(
        model=CHAT_MODEL,
        messages=messages,
        stream=False,
        format="json",
        options=CHAT_OPTIONS,
    )

    elapsed = time.perf_counter() - t0

    if tracker:
        tracker.track_chat(response, wall_time_s=elapsed)

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
            price = parse_price(metadata.get("precio"))

            if price is None:
                continue

            valid_candidates.append({
                "metadata": metadata,
                "precio": price,
            })

    if not valid_candidates:
        return None

    valid_candidates.sort(key=lambda product: product["precio"])

    return valid_candidates[0]["metadata"]


# ==========================================================
# FASE 2: BUSQUEDA DE TODOS LOS INGREDIENTES
# ==========================================================

def check_catalog(ingredients, top_k=8, tracker=None):
    """
    Busca individualmente cada ingrediente en ChromaDB.
    """

    catalog_results = []

    for ingredient in ingredients:

        results = search_products(
            ingredient["nombre"],
            top_k=top_k,
            tracker=tracker,
        )

        product = match_catalog_product(
            ingredient,
            results,
            tracker=tracker,
        )

        catalog_results.append({
            "ingrediente": ingredient["nombre"],
            "cantidad_necesaria": ingredient["cantidad"],
            "encontrado": product is not None,
            "producto": product,
        })

    return catalog_results


# ==========================================================
# CHAT CON ATENEA
# ==========================================================

def send_question(
    question,
    top_k=8,
    tracker: ResourceTracker | None = None,
):
    """
    Ejecuta las dos fases de Atenea:

    1. Genera ingredientes sin consultar el catálogo.
    2. Busca cada ingrediente en ChromaDB.
    """

    ingredient_data = generate_ingredients(
        question,
        tracker=tracker,
    )

    catalog_results = check_catalog(
        ingredient_data["ingredientes"],
        top_k=top_k,
        tracker=tracker,
    )

    return {
        "plato": ingredient_data["plato"],
        "comensales": ingredient_data["comensales"],
        "ingredientes_necesarios": ingredient_data["ingredientes"],
        "comprobacion_catalogo": catalog_results,
    }


# ==========================================================
# CLASIFICACION Y ORQUESTACION
# ==========================================================

def answer_with_intent(
    question,
    top_k=8,
    tracker=None,
    skip_intent=False,
):
    """
    Clasifica la intención y ejecuta Atenea.

    Si es cocina:
        Genera ingredientes y consulta el catálogo.

    Si no es cocina:
        Devuelve un mensaje de rechazo.
    """

    if skip_intent:

        intent = {
            "es_cocina": True,
            "motivo": "intención no analizada (--no-analyze)",
        }

    else:

        intent = classify_cooking_intent(
            question,
            tracker=tracker,
        )

    if not intent["es_cocina"]:

        return {
            "ok": True,
            "es_cocina": False,
            "motivo_intencion": intent["motivo"],
            "answer": NON_COOKING_ANSWER,
        }

    answer = send_question(
        question,
        top_k=top_k,
        tracker=tracker,
    )

    return {
        "ok": True,
        "es_cocina": True,
        "motivo_intencion": intent["motivo"],
        "answer": answer,
    }


# ==========================================================
# API PARA main.py
# ==========================================================

def create_tracker() -> ResourceTracker:
    """Crea un ResourceTracker apuntando al directorio de ChromaDB."""

    return ResourceTracker(chroma_dir=CHROMA_DIR)


def analyze_intent(question, tracker=None):
    """Solo clasifica la intención de una pregunta."""

    return classify_cooking_intent(
        question,
        tracker=tracker,
    )


__all__ = [
    # Público (usado por main.py)
    "answer_with_intent",
    "analyze_intent",
    "build_index",
    "create_directories",
    "create_tracker",
    "ResourceTracker",
    # Reutilizables programáticamente
    "send_question",
    "generate_ingredients",
    "check_catalog",
    "search_products",
    "match_catalog_product",
]