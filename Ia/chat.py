
from resource_tracker import ResourceTracker
from intent import classify_cooking_intent
from concurrent.futures import ThreadPoolExecutor
from config.config import CHROMA_DIR, NON_COOKING_ANSWER
from allergens import allergy_warning, extract_excluded_ingredients
from ingredients import generate_ingredients
from matching import check_catalog
from llm import warm_up_chat_model, warm_up_embed_model

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

    exclusions = extract_excluded_ingredients(question)

    ingredient_data = generate_ingredients(
        question,
        tracker=tracker,
    )

    catalog_results = check_catalog(
        ingredient_data["ingredientes_necesarios"],
        plato=ingredient_data["plato"],
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


def warm_up_all_parallel():
    with ThreadPoolExecutor(max_workers=2) as ex:
        f1 = ex.submit(warm_up_chat_model)
        f2 = ex.submit(warm_up_embed_model)
        f1.result(); f2.result()

__all__ = [
    "ResourceTracker",
    "analyze_intent",
    "answer_with_intent",
    "create_tracker",
    "send_question",
    "warm_up_all"
]