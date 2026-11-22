# resource_tracker.py
"""
Monitor de recursos para Atenea — versión alineada con métricas de Ollama.

Aprovecha lo que Ollama devuelve en cada respuesta:
  - prompt_eval_count / prompt_eval_duration  (tokens y tiempo del prompt)
  - eval_count / eval_duration                (tokens y tiempo de generación)
  - total_duration / load_duration

Con eso calculamos tok/s REALES, que es lo que importa para evaluar
modelos y comparar configuraciones.
"""

import sys
import time
import threading
from pathlib import Path
from typing import Optional

try:
    import psutil
    HAS_PSUTIL = True
except ImportError:
    HAS_PSUTIL = False

try:
    import resource
    HAS_RESOURCE = True
except ImportError:
    HAS_RESOURCE = False


class ResourceTracker:
    def __init__(
        self,
        chroma_dir: Optional[Path] = None,
        history_path: Optional[Path] = None,
        sample_interval: float = 0.5,
    ):
        self.start_time = time.perf_counter()
        self._lock = threading.Lock()

        self.chroma_dir = Path(chroma_dir) if chroma_dir else None
        self.history_path = Path(history_path) if history_path else None

        # Ollama — datos reales
        self.embed_calls = 0
        self.embed_time_s = 0.0
        self.chat_calls = 0
        self.chat_time_s = 0.0
        self.input_tokens_real = 0
        self.output_tokens_real = 0
        self.prompt_eval_time_ns = 0
        self.eval_time_ns = 0
        self.load_time_ns = 0

        # Estimaciones de respaldo (solo si Ollama no devuelve campos)
        self.est_input = 0
        self.est_output = 0

        # RAG
        self.rag_queries = 0
        self.rag_results_total = 0
        self.rag_time_s = 0.0

        # Sampling en background
        self._sample_interval = sample_interval
        self._stop_event = threading.Event()
        self._sampler = None
        self.peak_rss_mb = 0.0
        self.peak_cpu_pct = 0.0
        self.samples = 0

        self._process = psutil.Process() if HAS_PSUTIL else None
        if self._process:
            self._process.cpu_percent()  # reset del contador

        self._start_sampler()

    # ---------- Sampling background ----------

    def _start_sampler(self):
        if not HAS_PSUTIL:
            return
        self._sampler = threading.Thread(target=self._sample_loop, daemon=True)
        self._sampler.start()

    def _sample_loop(self):
        while not self._stop_event.wait(self._sample_interval):
            try:
                rss_mb = self._process.memory_info().rss / (1024 * 1024)
                cpu = self._process.cpu_percent()
                with self._lock:
                    if rss_mb > self.peak_rss_mb:
                        self.peak_rss_mb = rss_mb
                    if cpu > self.peak_cpu_pct:
                        self.peak_cpu_pct = cpu
                    self.samples += 1
            except Exception:
                pass

    def stop(self):
        self._stop_event.set()
        if self._sampler:
            self._sampler.join(timeout=1)

    # ---------- Tracking ----------

    def track_embed(self, text: str, elapsed_s: float = 0.0):
        with self._lock:
            self.embed_calls += 1
            self.embed_time_s += elapsed_s
            self.est_input += max(1, len(text) // 4)

    def track_chat(self, response: dict, wall_time_s: float):
        """
        Registra métricas REALES que Ollama devuelve en el dict `response`.
        Ollama incluye: prompt_eval_count, eval_count,
        prompt_eval_duration, eval_duration, total_duration, load_duration.
        """
        with self._lock:
            self.chat_calls += 1
            self.chat_time_s += wall_time_s

            self.input_tokens_real += response.get("prompt_eval_count", 0) or 0
            self.output_tokens_real += response.get("eval_count", 0) or 0
            self.prompt_eval_time_ns += response.get("prompt_eval_duration", 0) or 0
            self.eval_time_ns += response.get("eval_duration", 0) or 0
            self.load_time_ns += response.get("load_duration", 0) or 0

    def track_rag(self, top_k: int, results: list, elapsed_s: float = 0.0):
        with self._lock:
            self.rag_queries += 1
            self.rag_results_total += len(results)
            self.rag_time_s += elapsed_s

    # ---------- Informe ----------

    def report(self) -> dict:
        elapsed = time.perf_counter() - self.start_time

        with self._lock:
            tok_s_gen = (
                self.output_tokens_real / (self.eval_time_ns / 1e9)
                if self.output_tokens_real and self.eval_time_ns else 0.0
            )
            tok_s_prompt = (
                self.input_tokens_real / (self.prompt_eval_time_ns / 1e9)
                if self.input_tokens_real and self.prompt_eval_time_ns else 0.0
            )

            stats = {
                "tiempo_total_s": round(elapsed, 2),
                "ollama": {
                    "embed_calls": self.embed_calls,
                    "chat_calls": self.chat_calls,
                    "embed_tiempo_s": round(self.embed_time_s, 2),
                    "chat_tiempo_s": round(self.chat_time_s, 2),
                    "tokens_prompt_reales": self.input_tokens_real,
                    "tokens_generados_reales": self.output_tokens_real,
                    "prefill_time_s": round(self.prompt_eval_time_ns / 1e9, 3),
                    "generacion_time_s": round(self.eval_time_ns / 1e9, 3),
                    "load_modelo_s": round(self.load_time_ns / 1e9, 3),
                    "tokens_por_segundo_generacion": round(tok_s_gen, 2),
                    "tokens_por_segundo_prompt": round(tok_s_prompt, 2),
                },
                "tokens_estimados": {
                    "input": self.est_input,
                    "output": self.est_output,
                },
                "rag": {
                    "consultas": self.rag_queries,
                    "productos_recuperados": self.rag_results_total,
                    "tiempo_s": round(self.rag_time_s, 2),
                },
            }

        # CPU / memoria / sistema
        if HAS_PSUTIL and self._process:
            mem = self._process.memory_info()
            stats["memoria_rss_actual_mb"] = round(mem.rss / (1024 * 1024), 1)
            stats["memoria_rss_pico_mb"] = round(self.peak_rss_mb, 1)
            stats["cpu_pico_pct"] = round(self.peak_cpu_pct, 1)

            cpu_times = self._process.cpu_times()
            cpu_user_s = cpu_times.user + cpu_times.system
            stats["cpu_tiempo_total_s"] = round(cpu_user_s, 2)

            stats["nucleos_logicos"] = psutil.cpu_count(logical=True)
            stats["nucleos_fisicos"] = psutil.cpu_count(logical=False)
            try:
                stats["loadavg_1_5_15"] = [round(x, 2) for x in psutil.getloadavg()]
            except (AttributeError, OSError):
                stats["loadavg_1_5_15"] = None
            stats["memoria_sistema_usada_pct"] = psutil.virtual_memory().percent
        elif HAS_RESOURCE:
            usage = resource.getrusage(resource.RUSAGE_SELF)
            divisor = (1024 * 1024) if sys.platform == "darwin" else 1024
            stats["memoria_rss_pico_mb"] = round(usage.ru_maxrss / divisor, 1)

        # Disco
        def dir_size_bytes(path):
            if path is None or not path.exists():
                return 0
            if path.is_file():
                return path.stat().st_size
            return sum(f.stat().st_size for f in path.rglob("*") if f.is_file())

        stats["disco_indice_chroma_mb"] = round(
            dir_size_bytes(self.chroma_dir) / (1024 * 1024), 2
        )
        stats["disco_historial_kb"] = round(
            dir_size_bytes(self.history_path) / 1024, 1
        )

        return stats

    def print_report(self):
        stats = self.report()
        print("\n" + "=" * 55)
        print("  RECURSOS UTILIZADOS")
        print("=" * 55)

        def _print(d, indent=2):
            for k, v in d.items():
                if isinstance(v, dict):
                    print(" " * indent + f"{k}:")
                    _print(v, indent + 4)
                else:
                    print(" " * indent + f"{k:32s}: {v}")

        _print(stats)
        print("=" * 55 + "\n")

# Añadir cerca del inicio de resource_tracker.py, antes de la clase

def _to_int(value, default=0):
    if value is None:
        return default
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def _to_float(value, default=0.0):
    if value is None:
        return default
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def _get_field(response, key, default=None):
    """
    Lee un campo de `response`, sea dict, ChatResponse (pydantic) o similar.
    Compatible con .get(), getattr y model_dump().
    """
    if response is None:
        return default
    if isinstance(response, dict):
        return response.get(key, default)
    # pydantic v2
    if hasattr(response, "model_dump"):
        return response.model_dump().get(key, default)
    # pydantic v1
    if hasattr(response, "dict"):
        return response.dict().get(key, default)
    # fallback
    return getattr(response, key, default)