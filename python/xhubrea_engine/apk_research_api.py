"""Authenticated, bounded APK inventory endpoint; metadata-only and read-only."""
from __future__ import annotations

from typing import Annotated, Callable

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field, field_validator

from xhubrea_engine.apk_research import analyze_apk_inventory

BoundedInventoryText = Annotated[str, Field(min_length=1, max_length=300)]


class ApkResearchRequest(BaseModel):
    """Client-extracted APK metadata only; never accepts or executes an APK file."""
    package_name: str = Field(default="unknown", min_length=1, max_length=300)
    version_name: str = Field(default="unknown", min_length=1, max_length=300)
    permissions: list[BoundedInventoryText] = Field(default_factory=list, max_length=200)
    exported_components: list[BoundedInventoryText] = Field(default_factory=list, max_length=200)
    native_libraries: list[BoundedInventoryText] = Field(default_factory=list, max_length=200)

    @field_validator("package_name", "version_name")
    @classmethod
    def reject_blank_metadata(cls, value: str) -> str:
        if not value.strip():
            raise ValueError("Metadata fields must contain non-whitespace text.")
        return value.strip()


def create_apk_research_router(
    auth_dependency: Callable,
    rate_limit: Callable,
) -> APIRouter:
    router = APIRouter()

    @router.post(
        "/v1/apk/research",
        dependencies=[Depends(auth_dependency)],
    )
    async def apk_research(payload: ApkResearchRequest) -> dict:
        """Analyze bounded APK metadata supplied by a client."""
        rate_limit()
        try:
            return analyze_apk_inventory(payload.model_dump())
        except ValueError as exc:
            raise HTTPException(status_code=400, detail=str(exc)) from exc

    return router
