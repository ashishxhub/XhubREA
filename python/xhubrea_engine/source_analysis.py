"""Bounded, read-only source-code inventory and Python AST analysis.

Source text is treated strictly as untrusted data. No project code is imported,
executed, evaluated, installed, or modified by this module.
"""
from __future__ import annotations

import ast
import re
from typing import Any

MAX_FILES = 40
MAX_PATH_CHARS = 300
MAX_SOURCE_CHARS = 64_000
MAX_SYMBOLS_PER_FILE = 300
SUPPORTED_AST_SUFFIXES = {".py"}


def _validate_path(path: Any) -> str:
    if not isinstance(path, str) or not path.strip() or len(path) > MAX_PATH_CHARS:
        raise ValueError("Each file path must be a non-empty string up to 300 characters.")
    normalized = path.strip().replace("\\", "/")
    parts = normalized.split("/")
    if normalized.startswith("/") or re.match(r"^[A-Za-z]:", normalized) or any(
        part in {"", ".", ".."} for part in parts
    ):
        raise ValueError("File paths must be normalized relative paths without traversal.")
    return normalized


def _python_summary(source: str) -> dict[str, Any]:
    try:
        tree = ast.parse(source)
    except SyntaxError as exc:
        return {
            "parse_status": "syntax_error",
            "syntax_error": {
                "line": exc.lineno,
                "offset": exc.offset,
                "message": (exc.msg or "Syntax error")[:200],
            },
            "imports": [],
            "symbols": [],
        }

    imports: set[str] = set()
    symbols: list[dict[str, Any]] = []
    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            imports.update(alias.name for alias in node.names)
        elif isinstance(node, ast.ImportFrom):
            imports.add("." * (node.level or 0) + (node.module or ""))
        elif isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef)):
            symbols.append({
                "kind": "class" if isinstance(node, ast.ClassDef) else "function",
                "name": node.name[:120],
                "line": node.lineno,
            })
    symbols.sort(key=lambda item: (item["line"], item["name"]))
    return {
        "parse_status": "ok",
        "syntax_error": None,
        "imports": sorted(imports)[:MAX_SYMBOLS_PER_FILE],
        "symbols": symbols[:MAX_SYMBOLS_PER_FILE],
    }


def analyze_source_inventory(files: list[dict[str, Any]]) -> dict[str, Any]:
    """Analyze supplied source snapshots without executing or importing them."""
    if not isinstance(files, list) or not files or len(files) > MAX_FILES:
        raise ValueError(f"files must be a non-empty list of at most {MAX_FILES} entries.")

    results: list[dict[str, Any]] = []
    seen_paths: set[str] = set()
    total_chars = 0
    language_counts: dict[str, int] = {}
    syntax_error_count = 0

    for index, item in enumerate(files, start=1):
        if not isinstance(item, dict):
            raise ValueError(f"File entry {index} must be an object.")
        path = _validate_path(item.get("path"))
        if path in seen_paths:
            raise ValueError(f"Duplicate source path: {path}")
        seen_paths.add(path)

        source = item.get("content")
        if not isinstance(source, str):
            raise ValueError(f"Source content for {path} must be text.")
        if len(source) > MAX_SOURCE_CHARS:
            raise ValueError(f"Source content for {path} exceeds {MAX_SOURCE_CHARS} characters.")
        total_chars += len(source)
        if total_chars > 512_000:
            raise ValueError("Combined source content exceeds 512,000 characters.")

        suffix = "." + path.rsplit(".", 1)[-1].lower() if "." in path.rsplit("/", 1)[-1] else ""
        language = "python" if suffix == ".py" else (suffix.lstrip(".") or "unknown")
        language_counts[language] = language_counts.get(language, 0) + 1
        line_count = len(source.splitlines())
        result: dict[str, Any] = {
            "path": path,
            "language": language,
            "line_count": line_count,
            "character_count": len(source),
        }
        if suffix in SUPPORTED_AST_SUFFIXES:
            summary = _python_summary(source)
            result.update(summary)
            if summary["parse_status"] == "syntax_error":
                syntax_error_count += 1
        else:
            result.update({
                "parse_status": "inventory_only",
                "imports": [],
                "symbols": [],
            })
        results.append(result)

    results.sort(key=lambda item: item["path"])
    return {
        "summary": {
            "files_analyzed": len(results),
            "total_characters": total_chars,
            "language_counts": dict(sorted(language_counts.items())),
            "python_syntax_errors": syntax_error_count,
        },
        "files": results,
        "limitations": [
            "Source text was parsed as data; no project code was executed or imported.",
            "AST summaries are structural and do not prove runtime behavior.",
            "Only Python files receive syntax-tree analysis; other languages receive inventory-only summaries.",
            "No dependency installation, network access, filesystem writes, or external commands are performed.",
        ],
    }
