"""Bounded, deterministic architecture mapping from supplied source inventory metadata.

This module treats paths and import names as untrusted metadata. It never opens
files, imports project modules, executes code, installs packages, or uses network.
"""
from __future__ import annotations

import re
from typing import Any

MAX_FILES = 200
MAX_PATH_CHARS = 300
MAX_IMPORTS_PER_FILE = 100
MAX_IMPORT_CHARS = 200


def _path(value: Any) -> str:
    if not isinstance(value, str) or not value.strip() or len(value) > MAX_PATH_CHARS:
        raise ValueError("Each path must be a non-empty string up to 300 characters.")
    normalized = value.strip().replace("\\", "/")
    parts = normalized.split("/")
    if (normalized.startswith("/") or re.match(r"^[A-Za-z]:", normalized)
            or any(part in {"", ".", ".."} for part in parts)):
        raise ValueError("Paths must be normalized relative paths without traversal.")
    return normalized


def _imports(value: Any, path: str) -> list[str]:
    if value is None:
        return []
    if not isinstance(value, list) or len(value) > MAX_IMPORTS_PER_FILE:
        raise ValueError(f"imports for {path} must be a list of at most {MAX_IMPORTS_PER_FILE} items.")
    result: set[str] = set()
    for item in value:
        if (not isinstance(item, str) or not item.strip()
                or len(item) > MAX_IMPORT_CHARS):
            raise ValueError(f"Import names for {path} must be non-empty strings up to {MAX_IMPORT_CHARS} characters.")
        result.add(item.strip())
    return sorted(result)


def _component(path: str) -> str:
    parts = path.split("/")
    return parts[0] if len(parts) > 1 else "[root]"


def _module_candidates(import_name: str) -> tuple[str, ...]:
    # Relative imports are retained as unresolved evidence rather than guessed.
    if import_name.startswith("."):
        return ()
    base = import_name.replace(".", "/")
    return (base + ".py", base + "/__init__.py")


def analyze_app_architecture(files: list[dict[str, Any]]) -> dict[str, Any]:
    """Map components and resolvable Python import relationships from metadata."""
    if not isinstance(files, list) or not files or len(files) > MAX_FILES:
        raise ValueError(f"files must be a non-empty list of at most {MAX_FILES} entries.")

    records: dict[str, dict[str, Any]] = {}
    for index, item in enumerate(files, start=1):
        if not isinstance(item, dict):
            raise ValueError(f"File entry {index} must be an object.")
        path = _path(item.get("path"))
        if path in records:
            raise ValueError(f"Duplicate source path: {path}")
        language = item.get("language", "unknown")
        if not isinstance(language, str) or not language.strip() or len(language) > 40:
            raise ValueError(f"Language for {path} must be a short non-empty string.")
        records[path] = {
            "path": path,
            "language": language.strip().lower(),
            "imports": _imports(item.get("imports", []), path),
            "component": _component(path),
        }

    path_set = set(records)
    module_index: dict[str, str] = {}
    for path, record in records.items():
        if record["language"] != "python" and not path.endswith(".py"):
            continue
        if path.endswith(".py"):
            module = path[:-3].replace("/", ".")
            if module.endswith(".__init__"):
                module = module[:-9]
            module_index[module] = path

    edges: set[tuple[str, str, str]] = set()
    unresolved: set[tuple[str, str]] = set()
    external_imports: set[str] = set()
    relative_import_count = 0
    for path, record in records.items():
        for import_name in record["imports"]:
            if import_name.startswith("."):
                relative_import_count += 1
                unresolved.add((path, import_name))
                continue
            target = None
            for candidate in _module_candidates(import_name):
                if candidate in path_set:
                    target = candidate
                    break
            if target is None:
                external_imports.add(import_name.split(".", 1)[0])
                continue
            if target != path:
                edges.add((path, target, import_name))

    component_files: dict[str, list[str]] = {}
    component_languages: dict[str, dict[str, int]] = {}
    for path, record in sorted(records.items()):
        component = record["component"]
        component_files.setdefault(component, []).append(path)
        counts = component_languages.setdefault(component, {})
        language = record["language"]
        counts[language] = counts.get(language, 0) + 1

    components = [
        {
            "name": name,
            "file_count": len(paths),
            "languages": dict(sorted(component_languages[name].items())),
            "files": paths,
        }
        for name, paths in sorted(component_files.items())
    ]
    dependency_edges = [
        {"from": source, "to": target, "import": import_name}
        for source, target, import_name in sorted(edges)
    ]
    return {
        "summary": {
            "files_mapped": len(records),
            "components_detected": len(components),
            "internal_dependency_edges": len(dependency_edges),
            "external_import_roots": len(external_imports),
            "unresolved_relative_imports": relative_import_count,
        },
        "components": components,
        "dependency_edges": dependency_edges,
        "external_import_roots": sorted(external_imports),
        "unresolved_relative_imports": [
            {"from": source, "import": name}
            for source, name in sorted(unresolved)
        ],
        "limitations": [
            "Architecture is inferred only from supplied paths, language labels, and import metadata.",
            "Directory-based components are organizational groupings, not verified runtime services.",
            "Only exact supplied Python module paths are resolved; dynamic imports and other languages are not mapped.",
            "Unresolved imports may be third-party packages, omitted files, or package aliases.",
            "No source files are opened, imported, executed, installed, modified, or sent over a network.",
        ],
    }
