"""Optional authenticated HTTP API for XhubREA's read-only AI agent.

Run from the repository root with:
  uvicorn xhubrea_engine.api:app --app-dir python --host 127.0.0.1 --port 8000

Keep this service behind HTTPS in deployment. It is not part of the Android app.
"""
from __future__ import annotations

import hmac
import os
import time
from collections import deque
from typing import Annotated

from fastapi import Depends, FastAPI, Header, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field, field_validator

from xhubrea_engine.antigravity_adapter import ask_antigravity
from xhubrea_engine.research_workflow import build_research_prompt
from xhubrea_engine.intelligence import structure_findings

app = FastAPI(
    title="XhubREA Research API",
    version="0.1.0",
    description="Authenticated, read-only AI research endpoint.",
)

# Browser access is disabled unless an explicit origin allowlist is configured.
_allowed_origins = [
    origin.strip()
    for origin in os.getenv("XHUBREA_CORS_ORIGINS", "").split(",")
    if origin.strip()
]
if _allowed_origins:
    app.add_middleware(
        CORSMiddleware,
        allow_origins=_allowed_origins,
        allow_credentials=False,
        allow_methods=["GET", "POST"],
        allow_headers=["Authorization", "Content-Type"],
    )

MAX_REQUESTS_PER_MINUTE = 10
_request_times: deque[float] = deque()


class AgentChatRequest(BaseModel):
    prompt: str = Field(min_length=1, max_length=8000)

    @field_validator("prompt")
    @classmethod
    def reject_blank_prompt(cls, value: str) -> str:
        if not value.strip():
            raise ValueError("Prompt must contain non-whitespace text.")
        return value.strip()


class AgentChatResponse(BaseModel):
    answer: str


class EvidenceFinding(BaseModel):
    evidence_id: str = Field(min_length=1, max_length=80)
    source: str = Field(min_length=1, max_length=500)
    observation: str = Field(min_length=1, max_length=500)
    confidence: str = Field(min_length=1, max_length=40)


class ResearchAnalyzeRequest(BaseModel):
    project_name: str = Field(min_length=1, max_length=120)
    objective: str = Field(min_length=1, max_length=500)
    findings: list[EvidenceFinding] = Field(min_length=1, max_length=12)


class ResearchAnalyzeResponse(BaseModel):
    report: str


class ResearchStructureRequest(BaseModel):
    findings: list[EvidenceFinding] = Field(min_length=1, max_length=12)


def require_api_token(authorization: Annotated[str | None, Header()] = None) -> None:
    """Require a server-configured bearer token; never accept a client API key."""
    expected = os.getenv("XHUBREA_API_TOKEN", "")
    if not expected:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="XhubREA API authentication is not configured.",
        )
    scheme, _, supplied = (authorization or "").partition(" ")
    if scheme.lower() != "bearer" or not supplied or not hmac.compare_digest(supplied, expected):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Valid bearer token required.",
            headers={"WWW-Authenticate": "Bearer"},
        )


def _enforce_rate_limit(now: float | None = None) -> None:
    """Apply a conservative process-local cap; deployments should add gateway limits."""
    current = time.monotonic() if now is None else now
    while _request_times and current - _request_times[0] >= 60:
        _request_times.popleft()
    if len(_request_times) >= MAX_REQUESTS_PER_MINUTE:
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail="Rate limit reached. Try again in a minute.",
        )
    _request_times.append(current)


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok", "service": "xhubrea-research-api"}


@app.post(
    "/v1/agent/chat",
    response_model=AgentChatResponse,
    dependencies=[Depends(require_api_token)],
)
async def agent_chat(payload: AgentChatRequest) -> AgentChatResponse:
    _enforce_rate_limit()
    try:
        answer = await ask_antigravity(payload.prompt)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except RuntimeError as exc:
        # Do not expose stack traces, SDK internals, or credentials to clients.
        message = str(exc)
        if "disabled" in message.lower():
            raise HTTPException(status_code=503, detail="AI agent is disabled.") from exc
        if "not configured" in message.lower() or "not installed" in message.lower():
            raise HTTPException(status_code=503, detail="AI agent is not configured.") from exc
        raise HTTPException(status_code=502, detail="AI agent request failed.") from exc
    except Exception as exc:  # SDK/network failures are deliberately sanitized.
        raise HTTPException(status_code=502, detail="AI agent request failed.") from exc
    return AgentChatResponse(answer=answer)


@app.post(
    "/v1/research/analyze",
    response_model=ResearchAnalyzeResponse,
    dependencies=[Depends(require_api_token)],
)
async def research_analyze(payload: ResearchAnalyzeRequest) -> ResearchAnalyzeResponse:
    """Turn supplied authorized observations into a structured research report."""
    _enforce_rate_limit()
    try:
        prompt = build_research_prompt(
            payload.project_name,
            payload.objective,
            [finding.model_dump() for finding in payload.findings],
        )
        report = await ask_antigravity(prompt)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except RuntimeError as exc:
        message = str(exc).lower()
        if "disabled" in message:
            raise HTTPException(status_code=503, detail="AI agent is disabled.") from exc
        if "not configured" in message or "not installed" in message:
            raise HTTPException(status_code=503, detail="AI agent is not configured.") from exc
        raise HTTPException(status_code=502, detail="AI agent request failed.") from exc
    except Exception as exc:
        raise HTTPException(status_code=502, detail="AI agent request failed.") from exc
    return ResearchAnalyzeResponse(report=report)


@app.post(
    "/v1/research/structure",
    dependencies=[Depends(require_api_token)],
)
async def research_structure(payload: ResearchStructureRequest) -> dict:
    """Return deterministic evidence labels and source groupings; no AI inference."""
    try:
        return structure_findings([finding.model_dump() for finding in payload.findings])
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


# Keep APK metadata analysis in a separate bounded, read-only router.
from xhubrea_engine.apk_research_api import create_apk_research_router

app.include_router(create_apk_research_router(require_api_token, _enforce_rate_limit))
