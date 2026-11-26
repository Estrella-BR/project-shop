"""Detección y filtrado de alérgenos e ingredientes excluidos.

El filtro solo mira NOMBRES: el catálogo no tiene la lista de
ingredientes ni de alérgenos de cada producto.
"""

import re
import unicodedata


def _normalize(text):
    text = unicodedata.normalize("NFD", str(text or "").lower())
    text = "".join(c for c in text if unicodedata.category(c) != "Mn")
    # Se conservan los separadores de frases y listas.
    text = re.sub(r"[^a-z0-9,.;:!?()]+", " ", text)
    return text.strip()


# Alimentos que contienen (o suelen contener) cada alérgeno.
# Ante la duda se excluye.
FAMILIES = {
    "huevo": "huevo yema clara mayonesa alioli merengue mahonesa cesar",
    "leche": (
        "leche lacteo lactosa queso mantequilla nata yogur cuajada requeson "
        "bechamel parmesano pecorino mozzarella gruyere manchego emmental "
        "mascarpone ricotta cheddar gouda feta chocolate cesar"
    ),
    "gluten": (
        "gluten trigo harina pan pasta espagueti spaghetti macarron fideo "
        "tallarin lasana cebada centeno avena espelta semola cuscus galleta "
        "bizcocho panko picatostes crutones cerveza panqueque"
    ),
    "frutos secos": (
        "frutos secos nuez nueces almendra avellana pistacho pistache "
        "anacardo pinon pecana macadamia praline turron mazapan pesto"
    ),
    "cacahuete": "cacahuete mani",
    "marisco": (
        "marisco gamba gambon langostino langosta bogavante cigala cangrejo "
        "necora almeja mejillon berberecho ostra vieira navaja calamar "
        "chipiron sepia pulpo surimi"
    ),
    "pescado": (
        "pescado anchoa boqueron atun bonito bacalao salmon merluza sardina "
        "caballa rape dorada lubina trucha surimi cesar worcestershire"
    ),
    "soja": "soja tofu edamame miso tempeh",
    "sesamo": "sesamo ajonjoli tahini",
}

# Formas de nombrar cada familia en la pregunta.
ALIASES = {
    "huevo": "huevo",
    "leche": "leche", "lactosa": "leche", "lacteo": "leche",
    "proteina de la leche": "leche",
    "gluten": "gluten", "trigo": "gluten",
    "frutos secos": "frutos secos", "fruto seco": "frutos secos",
    "cacahuete": "cacahuete", "mani": "cacahuete",
    "marisco": "marisco", "crustaceo": "marisco", "molusco": "marisco",
    "pescado": "pescado",
    "soja": "soja",
    "sesamo": "sesamo",
}

TRIGGERS = (
    r"(?:alergic[oa]s?|alergias?|intolerantes?|intolerancia)"
    r"(?: alimentaria)?(?: (?:a|al|con)\b| ?:)(?: (?:la|el|los|las)\b)?",
    r"\bsin\b",
    r"\bno (?:quiero|lleve|lleva|puedo (?:comer|tomar)|"
    r"puede (?:comer|tomar)|como|tomo)\b",
    r"\b(?:celiac[oa]s?)\b",
)

# Palabras que marcan el final de lo excluido.
STOP = r"\b(?:para|porque|pero|que|por favor|gracias|soy|somos|es|con|quiero)\b"


def _same_word(a, b):
    return a == b or a in (b + "s", b + "es") or b in (a + "s", a + "es")


def _contains(words, phrase):
    wanted = phrase.split()
    return any(
        all(_same_word(w, words[i + j]) for j, w in enumerate(wanted))
        for i in range(len(words) - len(wanted) + 1)
    )


def _clean_term(text):
    text = re.sub(r"^(?:(?:a|al|la|el|los|las|de|del|ni|y|o|tambien)\b ?)+",
                  "", text.strip())
    return text.strip()


def extract_excluded_ingredients(question):
    """Alimentos que el usuario no quiere o no puede tomar."""

    text = _normalize(question)
    exclusions = []

    if re.search(r"\bceliac[oa]s?\b", text):
        exclusions.append("gluten")

    for trigger in TRIGGERS[:-1]:
        for match in re.finditer(trigger, text):
            tail = re.split(r"[.;!?()]", text[match.end():])[0]
            tail = re.split(STOP, tail)[0]

            # Listas: "al huevo, a la leche y a la soja".
            for part in re.split(r",|\by\b|\bni\b|\bo\b|\be\b", tail):
                term = _clean_term(part)
                if not term:
                    continue

                # Un fragmento largo no se usa tal cual, pero los
                # alérgenos que nombre se excluyen igualmente.
                if len(term.split()) <= 4:
                    found = [term]
                else:
                    found = [
                        alias for alias in ALIASES
                        if _contains(term.split(), alias)
                    ]

                exclusions += [t for t in found if t not in exclusions]

    return exclusions


def _banned_terms(exclusion):
    words = exclusion.split()
    families = {
        family for alias, family in ALIASES.items() if _contains(words, alias)
    }

    if not families:
        return [exclusion]

    terms = [exclusion]
    for family in families:
        terms += FAMILIES[family].split()
        if family == "frutos secos":
            terms.remove("frutos")
            terms.remove("secos")
            terms.append("frutos secos")

    return terms


def is_excluded(name, exclusions):
    """True si el nombre corresponde a algo excluido."""

    text = re.sub(r"[^a-z0-9]+", " ", _normalize(name)).strip()

    for exclusion in exclusions:
        # "pan sin gluten" es válido para quien evita el gluten.
        if re.search(rf"\bsin {re.escape(exclusion)}s?\b", text):
            continue

        words = text.split()

        if any(_contains(words, term) for term in _banned_terms(exclusion)):
            return True

    return False


def is_zero_quantity(quantity):
    """True para "0", "0 g" o "0 (opcional)", pero no para "0,5"."""

    return bool(re.match(r"\s*0+(?![.,]?\d)", str(quantity or "")))


def allergy_warning(exclusions):
    return (
        "Se han filtrado por nombre: " + ", ".join(exclusions) + ". "
        "El filtro puede fallar y no comprueba la composición de los "
        "productos. Si tienes una alergia, revisa la etiqueta de cada uno."
    )
