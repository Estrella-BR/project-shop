import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent

JSON_PATH = BASE_DIR / "data" / "mercadona.json"
CHROMA_DIR = BASE_DIR / "chroma_db_rag"

CHAT_MODEL = os.environ.get("OLLAMA_CHAT_MODEL", "qwen2.5:1.5b")
EMBED_MODEL = "nomic-embed-text"

COLLECTION_NAME = "productos_mercadona"
EMBED_BATCH_SIZE = 32

EMBED_OPTIONS = {
    "num_thread": int(os.environ.get("OLLAMA_EMBED_THREADS", "2")),
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