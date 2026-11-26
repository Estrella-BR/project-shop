import sys
import json
import time

import chromadb

from config.config import (
    JSON_PATH,
    CHROMA_DIR,
    COLLECTION_NAME,
    EMBED_BATCH_SIZE,
)
from utils import safe_value, parse_price
from llm import embed


def load_products():

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


# ==========================================================
# CHROMADB
# ==========================================================

def get_collection():

    client = chromadb.PersistentClient(path=str(CHROMA_DIR))

    return client.get_or_create_collection(
        name=COLLECTION_NAME,
    )


def product_to_text(product):

    name = safe_value(
        product.get("name"),
        "Producto sin nombre",
    )

    price = safe_value(
        product.get("price"),
        "Precio desconocido",
    )

    quantity = safe_value(
        product.get("cuantity", product.get("quantity")),
        "Formato desconocido",
    )

    seller = safe_value(
        product.get("seller"),
        "Supermercado desconocido",
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


def build_index(force=False, tracker=None):

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
            f"product-{index}",
        )

        product_id = original_id

        if product_id in used_ids:
            product_id = f"{original_id}-{index}"

        used_ids.add(product_id)

        entries.append((product_id, product))

    # Detect products not indexed.
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

    # Index
    for start in range(0, len(pending), EMBED_BATCH_SIZE):

        batch = pending[start:start + EMBED_BATCH_SIZE]

        texts = [
            product_to_text(product)
            for _, product in batch
        ]

        response = embed(texts, tracker=tracker)

        collection.upsert(
            ids=[pid for pid, _ in batch],
            documents=texts,
            embeddings=response["embeddings"],
            metadatas=[
                create_metadata(product)
                for _, product in batch
            ],
        )

def search_products_batch(
    queries,
    top_k=8,
    tracker=None,
):
    """Busca varias consultas con una sola llamada de embeddings
    y una sola consulta a ChromaDB."""

    if not queries:
        return []

    collection = get_collection()

    total = collection.count()

    if total == 0:
        return [[] for _ in queries]

    # Mismo modelo de embeddings con el que se creó el índice.
    embeddings = embed(list(queries), tracker=tracker)["embeddings"]

    t0 = time.perf_counter()

    results = collection.query(
        query_embeddings=embeddings,
        n_results=min(top_k, total),
        include=["documents", "metadatas", "distances"],
    )

    batch = [
        [
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
        for documents, metadatas, distances in zip(
            results["documents"],
            results["metadatas"],
            results["distances"],
        )
    ]

    if tracker:
        tracker.track_rag(
            queries=len(queries),
            results=sum(len(items) for items in batch),
            elapsed_s=time.perf_counter() - t0,
        )

    return batch


def search_products(
    question,
    top_k=8,
    tracker=None,
):

    return search_products_batch([question], top_k=top_k, tracker=tracker)[0]

__all__ = [
    "build_index",
    "get_collection",
    "load_products",
    "search_products",
    "search_products_batch",
]