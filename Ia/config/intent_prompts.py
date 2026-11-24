import os

CHAT_MODEL =  "qwen2.5:1.5b"

COOKING_KEYWORDS = {
    "paella", "arroz", "receta", "cocinar", "cocina", "comida", "comer",
    "ingrediente", "producto", "precio", "mercadona", "leche", "pan",
    "carne", "pescado", "verdura", "fruta", "postre", "bebida",
}

NON_COOKING_KEYWORDS = {
    "capital", "presidente", "poema", "python", "javascript", "guerra",
    "historia", "matemática", "ecuación", "chiste",
}


INTENT_OPTIONS = {
    "num_thread": int(os.environ.get("OLLAMA_CHAT_THREADS", "6")),
    "num_ctx": 512,
    "num_predict": 80,
    "temperature": 0,
}

INTENT_SYSTEM_PROMPT = """Eres un clasificador binario. Recibes una pregunta de un usuario y decides si es de cocina o no.

Consideramos "cocina" CUALQUIER pregunta relacionada con:
- recetas, platos, ingredientes, técnicas culinarias 
- alimentación, comida, bebida
- productos de supermercado o precios de productos alimenticios
- compras de comida

Consideramos "no cocina" cualquier tema ajeno: matemáticas, historia, programación, ocio, política, chistes, etc.

Responde EXCLUSIVAMENTE con un JSON válido, sin texto adicional, con este formato exacto:
{"es_cocina": true|false, "motivo": "<frase de máximo 12 palabras>"}

No incluyas markdown, ni explicaciones, ni saltos de línea extra.
"""


__all__ = [
    "CHAT_MODEL",
    "COOKING_KEYWORDS",
    "NON_COOKING_KEYWORDS",
    "INTENT_OPTIONS",
    "INTENT_SYSTEM_PROMPT"
]