import pytest

from xhubrea_engine.architecture_research import analyze_app_architecture


def test_maps_components_and_internal_python_imports():
    report = analyze_app_architecture([
        {"path": "app/main.py", "language": "python", "imports": ["app.services.auth", "fastapi"]},
        {"path": "app/services/auth.py", "language": "python", "imports": ["os"]},
        {"path": "web/src/App.kt", "language": "kotlin", "imports": []},
    ])
    assert report["summary"]["files_mapped"] == 3
    assert report["summary"]["components_detected"] == 2
    assert report["dependency_edges"] == [
        {"from": "app/main.py", "to": "app/services/auth.py", "import": "app.services.auth"}
    ]
    assert report["external_import_roots"] == ["fastapi", "os"]


def test_resolves_python_package_init():
    report = analyze_app_architecture([
        {"path": "pkg/__init__.py", "language": "python", "imports": []},
        {"path": "consumer.py", "language": "python", "imports": ["pkg"]},
    ])
    assert report["dependency_edges"][0]["to"] == "pkg/__init__.py"


def test_relative_import_is_reported_not_guessed():
    report = analyze_app_architecture([
        {"path": "pkg/a.py", "language": "python", "imports": [".b"]},
    ])
    assert report["summary"]["unresolved_relative_imports"] == 1
    assert report["unresolved_relative_imports"][0]["import"] == ".b"


@pytest.mark.parametrize("path", ["../secret.py", "/tmp/a.py", "a//b.py", "C:/a.py"])
def test_rejects_unsafe_or_non_normalized_paths(path):
    with pytest.raises(ValueError):
        analyze_app_architecture([{"path": path, "imports": []}])


def test_rejects_duplicate_paths():
    with pytest.raises(ValueError, match="Duplicate"):
        analyze_app_architecture([
            {"path": "a.py", "imports": []},
            {"path": "a.py", "imports": []},
        ])


def test_enforces_file_limit():
    with pytest.raises(ValueError, match="at most 200"):
        analyze_app_architecture([{"path": f"f{i}.py"} for i in range(201)])


def test_enforces_import_limit_and_shape():
    with pytest.raises(ValueError):
        analyze_app_architecture([{"path": "a.py", "imports": ["x"] * 101}])
    with pytest.raises(ValueError):
        analyze_app_architecture([{"path": "a.py", "imports": "os"}])


def test_rejects_invalid_language_metadata():
    with pytest.raises(ValueError):
        analyze_app_architecture([{"path": "a.py", "language": "", "imports": []}])


def test_deterministic_ordering_and_no_runtime_claims():
    files = [
        {"path": "z/b.py", "language": "python", "imports": []},
        {"path": "a.py", "language": "python", "imports": []},
    ]
    first = analyze_app_architecture(files)
    second = analyze_app_architecture(list(reversed(files)))
    assert first == second
    assert any("not verified runtime services" in item for item in first["limitations"])
