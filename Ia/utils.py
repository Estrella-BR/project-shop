import json

from config.config import JSON_PATH, CHROMA_DIR


def safe_value(value, default=""):
    if value is None:
        return default
    return str(value).strip()


def parse_price(value):
    if value is None:
        return None
    try:
        return float(str(value).replace(",", ".").replace("€", "").strip())
    except (ValueError, TypeError):
        return None


def extract_json(raw):
    raw = raw.strip()
    if raw.startswith("```"):
        raw = raw.replace("```json", "", 1).replace("```", "").strip()
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        raise ValueError(f"Respuesta JSON inválida de Ollama: {raw}")


def create_directories():
    """Crea las carpetas necesarias (catálogo y ChromaDB)."""

    for directory in [JSON_PATH.parent, CHROMA_DIR]:
        directory.mkdir(parents=True, exist_ok=True)