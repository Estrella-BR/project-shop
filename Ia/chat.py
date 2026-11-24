
from resource_tracker import ResourceTracker
from intent import classify_cooking_intent

from config.config import CHROMA_DIR, NON_COOKING_ANSWER
from ingredients import generate_ingredients
from matching import check_catalog

def create_tracker() -> ResourceTracker:

    return ResourceTracker(chroma_dir=CHROMA_DIR)


def analyze_intent(question, tracker=None):

    return classify_cooking_intent(
        question,
        tracker=tracker,
    )

def send_question(
    question,
    top_k=8,
    tracker: ResourceTracker | None = None,
):

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

def answer_with_intent(
    question,
    top_k=8,
    tracker=None,
    skip_intent=False,
):
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


__all__ = [
    "ResourceTracker",
    "analyze_intent",
    "answer_with_intent",
    "create_tracker",
    "send_question",
]