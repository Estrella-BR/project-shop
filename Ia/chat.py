import os
import sys
import json
import argparse
from pathlib import Path
import time
import ollama
import chromadb

from resource_tracker import ResourceTracker
from personality import build_system_prompt, PERSONALITY_PATH
from intent import classify_cooking_intent

# ==========================================================
# CONFIGURACION
# ==========================================================

BASE_DIR = Path(__file__).resolve().parent

JSON_PATH = BASE_DIR / "data" / "mercadona.json"
CHROMA_DIR = BASE_DIR / "chroma_db_rag"

CHAT_MODEL = os.environ.get("OLLAMA_CHAT_MODEL", "qwen2.5:1.5b")
EMBED_MODEL = "all-minilm"

COLLECTION_NAME = "productos_mercadona"

# Productos que se incluyen en el prompt (menos tokens = respuesta mas rapida)
MAX_CONTEXT_PRODUCTS = 5

# Mensaje de rechazo cuando la pregunta no es de cocina
NON_COOKING_ANSWER = (
    "Solo puedo ayudarte con preguntas relacionadas con cocina, "
    "alimentación o productos del catálogo de Mercadona."
)


if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")


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
        raise ValueError("El catálogo debe contener una lista de productos.")

    return products


def get_collection():
    client = chromadb.PersistentClient(
        path=str(CHROMA_DIR)
    )

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
        return float(str(value).replace(",", ".").replace("€", "").strip())
    except (ValueError, TypeError):
        return None


# ==========================================================
# PRODUCTOS
# ==========================================================

def product_to_text(product):
    """Transforma un producto en texto para generar embeddings."""

    name = safe_value(product.get("name"), "Producto sin nombre")
    price = safe_value(product.get("price"), "Precio desconocido")
    quantity = safe_value(
        product.get("cuantity", product.get("quantity")),
        "Formato desconocido"
    )
    seller = safe_value(product.get("seller"), "Supermercado desconocido")

    parts = [
        f"Producto: {name}",
        f"Precio: {price} euros",
        f"Formato: {quantity}",
        f"Supermercado: {seller}"
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
            "Desconocido"
        ),
        "vendedor": safe_value(product.get("seller"), "Desconocido")
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

EMBED_BATCH_SIZE = 32

EMBED_OPTIONS = {"num_thread": int(os.environ.get("OLLAMA_EMBED_THREADS", "2"))}
CHAT_OPTIONS = {
    "num_thread": int(os.environ.get("OLLAMA_CHAT_THREADS", "6")),
    "num_ctx": 2048,
    "num_predict": int(os.environ.get("OLLAMA_CHAT_MAX_TOKENS", "400")),
}


def build_index(force=False, tracker: ResourceTracker | None = None):
    """Construye el índice vectorial de productos."""

    collection = get_collection()

    products = load_products()

    if not products:
        raise ValueError("El catálogo de productos está vacío.")

    if force:
        existing_ids = collection.get()["ids"]

        if existing_ids:
            collection.delete(ids=existing_ids)

    entries = []
    used_ids = set()

    for index, product in enumerate(products):
        original_id = safe_value(product.get("id"), f"product-{index}")
        product_id = original_id

        if product_id in used_ids:
            product_id = f"{original_id}-{index}"

        used_ids.add(product_id)
        entries.append((product_id, product))

    indexed_ids = set(collection.get(include=[])["ids"])
    pending = [(pid, p) for pid, p in entries if pid not in indexed_ids]

    if not pending:
        return

    for start in range(0, len(pending), EMBED_BATCH_SIZE):
        batch = pending[start:start + EMBED_BATCH_SIZE]
        texts = [product_to_text(p) for _, p in batch]

        t0 = time.perf_counter()
        response = ollama.embed(model=EMBED_MODEL, input=texts, options=EMBED_OPTIONS)
        elapsed = time.perf_counter() - t0

        if tracker:
            tracker.track_embed(" ".join(texts), elapsed_s=elapsed)

        collection.upsert(
            ids=[pid for pid, _ in batch],
            documents=texts,
            embeddings=response["embeddings"],
            metadatas=[create_metadata(p) for _, p in batch]
        )


# ==========================================================
# BUSQUEDA DE PRODUCTOS
# ==========================================================

def search_products(question, top_k=5, tracker: ResourceTracker | None = None):
    collection = get_collection()
    total = collection.count()
    if total == 0:
        return []

    t0 = time.perf_counter()
    embedding_response = ollama.embed(model=EMBED_MODEL, input=question, options=EMBED_OPTIONS)
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
        {"texto": d, "metadatos": m, "distancia": dist}
        for d, m, dist in zip(documents, metadatas, distances)
        if d is not None
    ]

    if tracker:
        tracker.track_rag(top_k, parsed, elapsed_s=rag_elapsed)

    return parsed


def build_product_context(results):
    """Construye el contexto que recibirá el modelo."""

    if not results:
        return "No se han encontrado productos en el catálogo."

    context = []

    for result in results[:MAX_CONTEXT_PRODUCTS]:
        context.append(f"- {result['texto']}")

    return "\n\n".join(context)


# ==========================================================
# CHAT CON OLLAMA
# ==========================================================

def send_question(question, top_k=5, tracker: ResourceTracker | None = None):
    """Envía una pregunta a Ollama utilizando RAG."""

    results = search_products(
        question,
        top_k=top_k,
        tracker=tracker
    )

    context = build_product_context(results)
    system_prompt = build_system_prompt(context)

    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": question},
    ]

    t0 = time.perf_counter()
    response = ollama.chat(
        model=CHAT_MODEL,
        messages=messages,
        stream=False,
        options=CHAT_OPTIONS
    )
    chat_elapsed = time.perf_counter() - t0

    answer = response["message"]["content"]

    if tracker:
        tracker.track_chat(response, wall_time_s=chat_elapsed)

    return answer


def answer_with_intent(question, top_k=5, tracker=None, skip_intent=False):
    """
    Orquesta: clasifica intención → si es cocina, RAG+chat; si no, rechaza.

    Devuelve un dict con:
      ok, es_cocina, motivo_intencion, answer
    """

    if skip_intent:
        intent = {"es_cocina": True, "motivo": "intención no analizada (--no-analyze)"}
    else:
        intent = classify_cooking_intent(question, tracker=tracker)

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
        tracker=tracker
    )

    return {
        "ok": True,
        "es_cocina": True,
        "motivo_intencion": intent["motivo"],
        "answer": answer,
    }


# ==========================================================
# MAIN
# ==========================================================

def main():

    parser = argparse.ArgumentParser(
        description="Atenea - Asistente de compras con Ollama y RAG"
    )

    parser.add_argument(
        "--question",
        type=str,
        help="Realiza una pregunta individual"
    )

    parser.add_argument(
        "--top-k",
        type=int,
        default=5,
        help="Número de productos recuperados"
    )

    parser.add_argument(
        "--reindex",
        action="store_true",
        help="Reconstruye el índice de productos"
    )

    parser.add_argument(
        "--show-resources",
        action="store_true",
        help="Muestra el informe de recursos al finalizar"
    )

    parser.add_argument(
        "--no-analyze",
        action="store_true",
        help="Saltarse la clasificación de intención (útil para benchmarks)"
    )

    parser.add_argument(
        "--analyze-only",
        action="store_true",
        help="Solo clasifica la pregunta, sin RAG ni respuesta"
    )

    args = parser.parse_args()

    tracker = ResourceTracker(chroma_dir=CHROMA_DIR)

    try:

        if args.top_k < 1:
            raise ValueError("--top-k debe ser mayor que cero.")

        create_directories()

        # Modo solo análisis de intención
        if args.analyze_only:
            if not args.question:
                raise ValueError("--analyze-only requiere --question.")

            intent = classify_cooking_intent(args.question, tracker=tracker)

            print(json.dumps({
                "ok": True,
                "es_cocina": intent["es_cocina"],
                "motivo_intencion": intent["motivo"],
            }, ensure_ascii=False, indent=4))

            if args.show_resources:
                tracker.print_report()

            return 0

        # Comprobar o construir índice
        build_index(force=args.reindex, tracker=tracker)

        # Modo pregunta individual
        if args.question:

            result = answer_with_intent(
                args.question,
                top_k=args.top_k,
                tracker=tracker,
                skip_intent=args.no_analyze,
            )

            output = dict(result)

            if args.show_resources:
                output["recursos"] = tracker.report()

            print(json.dumps(output, ensure_ascii=False, indent=4))

            if args.show_resources:
                tracker.print_report()

            return 0

        if args.show_resources:
            tracker.print_report()

        return 0

    except Exception as error:

        output = {
            "ok": False,
            "error": str(error),
        }

        if args.show_resources:
            output["recursos"] = tracker.report()

        print(json.dumps(output, ensure_ascii=False, indent=4))

        return 1

    finally:
        tracker.stop()


if __name__ == "__main__":
    sys.exit(main())