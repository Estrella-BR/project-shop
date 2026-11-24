import sys
import json
import argparse

from chat import (
    answer_with_intent,
    analyze_intent,
    create_tracker,
    warm_up_all_parallel,
)
from catalog import build_index
from utils import create_directories


if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")


def interactive_mode(top_k=8, skip_intent=False, show_resources=False, tracker=None):
    """Pregunta en bucle hasta que el usuario escriba 'salir' o envíe EOF."""
    print("Modo interactivo. Escribe tu pregunta ('salir' para terminar).")

    while True:
        try:
            question = input("\n> ").strip()
        except (EOFError, KeyboardInterrupt):
            print()
            break

        if not question:
            continue
        if question.lower() in {"salir", "exit", "quit"}:
            break

        try:
            result = answer_with_intent(
                question,
                top_k=top_k,
                tracker=tracker,
                skip_intent=skip_intent,
            )

            output = dict(result)
            if show_resources:
                output["recursos"] = tracker.report()

            print(json.dumps(output, ensure_ascii=False, indent=4))

        except Exception as error:
            print(json.dumps(
                {"ok": False, "error": str(error)},
                ensure_ascii=False,
                indent=4,
            ))


def main():

    parser = argparse.ArgumentParser(
        description="Atenea - Asistente de cocina con Ollama y RAG"
    )

    parser.add_argument(
        "--question",
        type=str,
        help="Realiza una pregunta individual",
    )

    parser.add_argument(
        "--interactive",
        action="store_true",
        help="Modo interactivo: pregunta varias veces sin reiniciar",
    )

    parser.add_argument(
        "--top-k",
        type=int,
        default=8,
        help="Número de productos recuperados por ingrediente",
    )

    parser.add_argument(
        "--reindex",
        action="store_true",
        help="Reconstruye el índice de productos",
    )

    parser.add_argument(
        "--show-resources",
        action="store_true",
        help="Muestra el informe de recursos al finalizar",
    )

    parser.add_argument(
        "--no-analyze",
        action="store_true",
        help="Saltarse la clasificación de intención",
    )

    parser.add_argument(
        "--analyze-only",
        action="store_true",
        help="Solo clasifica la pregunta, sin RAG ni respuesta",
    )

    args = parser.parse_args()

    tracker = create_tracker()

    try:

        if args.top_k < 1:
            raise ValueError("--top-k debe ser mayor que cero.")

        create_directories()
        warm_up_all_parallel()

        if args.analyze_only:

            if not args.question:
                raise ValueError(
                    "--analyze-only requiere --question."
                )

            intent = analyze_intent(
                args.question,
                tracker=tracker,
            )

            print(json.dumps(
                {
                    "ok": True,
                    "es_cocina": intent["es_cocina"],
                    "motivo_intencion": intent["motivo"],
                },
                ensure_ascii=False,
                indent=4,
            ))

            if args.show_resources:
                tracker.print_report()

            return 0

        build_index(
            force=args.reindex,
            tracker=tracker,
        )

        if args.interactive:

            interactive_mode(
                top_k=args.top_k,
                skip_intent=args.no_analyze,
                show_resources=args.show_resources,
                tracker=tracker,
            )

            if args.show_resources:
                tracker.print_report()

            return 0

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

            print(json.dumps(
                output,
                ensure_ascii=False,
                indent=4,
            ))

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

        print(json.dumps(
            output,
            ensure_ascii=False,
            indent=4,
        ))

        return 1

    finally:
        tracker.stop()


if __name__ == "__main__":
    sys.exit(main())