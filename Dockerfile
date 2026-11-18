FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

COPY python/requirements-backend.txt /app/requirements-backend.txt
COPY python/requirements-antigravity.txt /app/requirements-antigravity.txt
RUN python -m pip install --upgrade pip \
    && python -m pip install -r /app/requirements-backend.txt \
    && python -m pip install -r /app/requirements-antigravity.txt

COPY python/xhubrea_engine /app/xhubrea_engine

RUN useradd --system --uid 10001 --create-home xhubrea \
    && chown -R xhubrea:xhubrea /app
USER xhubrea

EXPOSE 8000
CMD ["sh", "-c", "uvicorn xhubrea_engine.api:app --app-dir /app --host 0.0.0.0 --port ${PORT:-8000}"]
