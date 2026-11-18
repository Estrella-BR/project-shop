# chat.py
import sys

# UTF-8 ANTES de cualquier print (Windows)
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

import os
import json
import time
import argparse

import ollama
import chromadb

BASE_DIR   = os.path.dirname(os.path.abspath(__file__))
JSON_PATH  = os.path.join(BASE_DIR, "mercadona.json")
CHROMA_DIR = os.path.join(BASE_DIR, "chroma_db_rag")


# ---------- Utilidades ----------

def product_to_text(p):
    parts = [
        f"{p['name']}",
        f"cuesta {p['price']} euros",
        f"formato {p['cuantity']}",
        f"vendido por {p['seller']}",
    ]
    if p.get("category"):
        parts.append(f"categoría {p['category']}")
    if p.get("description"):
        parts.append(p["description"])
    if p.get("tags"):
        parts.append("etiquetas: " + ", ".join(p["tags"]))
    return ". ".join(parts) + "."


def get_collection():
    client = chromadb.PersistentClient(path=CHROMA_DIR)
    return client.get_or_create_collection(name="productos_mercadona")


# ---------- Indexado (solo si hace falta) ----------

def build_index(force=False):
    """Reconstruye el índice. Normalmente NO se llama en cada chat."""
    collection = get_collection()
    if not force and collection.count() > 0:
        return  # ya está indexado

    with open(JSON_PATH, "r", encoding="utf-8") as f:
        products = json.load(f)

    textos = [product_to_text(p) for p in products]

    vectores = []
    for texto in textos:
        v = ollama.embed(model="nomic-embed-text", input=texto)["embeddings"][0]
        vectores.append(v)
        time.sleep(0.05)

    ids = [p["id"] for p in products]
    metadata = [
        {
            "nombre":   p["name"],
            "precio":   float(p["price"]),
            "formato":  p["cuantity"],
            "vendedor": p["seller"],
            "imagen":   p["image"],
        }
        for p in products
    ]

    collection.upsert(
        ids=ids, embeddings=vectores, documents=textos, metadatas=metadata
    )


# ---------- Consulta ----------

def searchProducts(question, top_k=3):
    collection = get_collection()

    embedding_pregunta = ollama.embed(
        model="nomic-embed-text",
        input=question,
    )["embeddings"][0]

    resultados = collection.query(
        query_embeddings=[embedding_pregunta],
        n_results=top_k,
    )

    return [
        {"texto": doc, "metadatos": meta}
        for doc, meta in zip(
            resultados["documents"][0],
            resultados["metadatas"][0],
        )
    ]


def sendQuestion(pregunta, top_k=3):
    resultados = searchProducts(pregunta, top_k=top_k)

    contexto = "\n\n".join(
        f"- {r['texto']} (Precio: {r['metadatos']['precio']} €, "
        f"Formato: {r['metadatos']['formato']})"
        for r in resultados
    )

    prompt_sistema = f"""Eres un asistente de compras.
Responde al usuario basándote ÚNICAMENTE en los siguientes productos:
{contexto}

Si el usuario pregunta por algo que no está en la lista, dilo claramente.
Cuando compares productos, indica siempre el precio y el formato.
"""

    respuesta = ollama.chat(
        model="qwen2.5",
        messages=[
            {"role": "system", "content": prompt_sistema},
            {"role": "user",   "content": pregunta},
        ],
    )
    return respuesta["message"]["content"]


# ---------- Entrypoint (al final, siempre) ----------

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--question", required=True)
    parser.add_argument("--top-k", type=int, default=3, dest="top_k")
    parser.add_argument("--reindex", action="store_true",
                        help="Reconstruye el índice antes de responder")
    args = parser.parse_args()

    try:
        if args.reindex:
            build_index(force=True)
        else:
            build_index(force=False)  # crea si está vacío, si no no hace nada

        answer = sendQuestion(args.question, top_k=args.top_k)
        print(json.dumps({"ok": True, "answer": answer}, ensure_ascii=False))
        return 0
    except Exception as e:
        print(json.dumps({"ok": False, "error": str(e)}, ensure_ascii=False))
        return 1


if __name__ == "__main__":
    sys.exit(main())