"""Authenticated endpoint for bounded app snapshot comparison."""
from __future__ import annotations

from typing import Annotated, Callable

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field, field_validator

from xhubrea_engine.app_comparison import compare_app_snapshots

BoundedLabel = Annotated[str, Field(min_length=1, max_length=300)]
BoundedLabels = Annotated[list[BoundedLabel], Field(max_length=200)]


class AppSnapshotRequest(BaseModel):
    app_label: BoundedLabel = "Application"
    permissions: BoundedLabels = Field(default_factory=list)
    components: BoundedLabels = Field(default_factory=list)
    source_structure: BoundedLabels = Field(default_factory=list)
    dependencies: BoundedLabels = Field(default_factory=list)
    architecture: BoundedLabels = Field(default_factory=list)

    @field_validator("app_label")
    @classmethod
    def reject_blank_label(cls, value: str) -> str:
        if not value.strip():
            raise ValueError("app_label must contain non-whitespace text.")
        return value.strip()


class AppComparisonRequest(BaseModel):
    app_a: AppSnapshotRequest
    app_b: AppSnapshotRequest


def create_app_comparison_router(
    auth_dependency: Callable,
    rate_limit: Callable,
) -> APIRouter:
    router = APIRouter()

    @router.post(
        "/v1/apps/compare",
        dependencies=[Depends(auth_dependency)],
    )
    async def compare_apps(payload: AppComparisonRequest) -> dict:
        """Compare caller-supplied app inventories; no APK/code upload or execution."""
        rate_limit()
        try:
            return compare_app_snapshots(
                payload.app_a.model_dump(),
                payload.app_b.model_dump(),
            )
        except ValueError as exc:
            raise HTTPException(status_code=400, detail=str(exc)) from exc

    return router
