from resource_tracker import ResourceTracker
from intent import classify_cooking_intent

from config.config import CHROMA_DIR, NON_COOKING_ANSWER
from catalog import build_index
from allergens import allergy_warning, extract_excluded_ingredients
from ingredients import generate_ingredients
from matching import check_catalog
from utils import create_directories


def create_tracker() -> ResourceTracker:
    return ResourceTracker(chroma_dir=CHROMA_DIR)


def analyze_intent(question, tracker=None):
    return classify_cooking_intent(question, tracker=tracker)


def send_question(question, top_k=8, tracker=None):
    exclusions = extract_excluded_ingredients(question)

    ingredient_data = generate_ingredients(
        question,
        tracker=tracker,
    )

    catalog_results = check_catalog(
        ingredient_data["ingredientes_necesarios"],
        top_k=top_k,
        tracker=tracker,
        exclusions=exclusions,
    )

    result = {
        "plato": ingredient_data["plato"],
        "comensales": ingredient_data["comensales"],
        "ingredientes_necesarios": ingredient_data["ingredientes_necesarios"],
        "comprobacion_catalogo": catalog_results,
    }

    if exclusions:
        result["excluidos"] = exclusions
        result["aviso"] = allergy_warning(exclusions)

    return result


def answer_with_intent(question, top_k=8, tracker=None, skip_intent=False):
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

    return {
        "ok": True,
        "es_cocina": True,
        "motivo_intencion": intent["motivo"],
        "answer": send_question(question, top_k=top_k, tracker=tracker),
    }


__all__ = [
    "ResourceTracker",
    "analyze_intent",
    "answer_with_intent",
    "build_index",
    "create_directories",
    "create_tracker",
    "send_question",
]