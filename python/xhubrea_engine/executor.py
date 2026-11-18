"""Bounded executor contract for XhubREA.

The default executor is deliberately disabled. This module defines a narrow
interface and validates task eligibility; it does not launch processes, call
providers, access repositories, or perform network/file operations.
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Protocol

from .task_store import TaskStoreError


MAX_EXECUTION_TIMEOUT_SECONDS = 60
MAX_GOAL_LENGTH = 2000
MAX_RESULT_LENGTH = 2000


@dataclass(frozen=True)
class ExecutionRequest:
    task_id: str
    goal: str
    timeout_seconds: int = 30


@dataclass(frozen=True)
class ExecutionResult:
    succeeded: bool
    summary: str


class Executor(Protocol):
    """Contract for a future explicitly reviewed executor implementation."""

    def execute(self, request: ExecutionRequest) -> ExecutionResult:
        """Execute one bounded request and return a short result summary."""


class ExecutorDisabledError(RuntimeError):
    """Raised when execution has not been explicitly enabled."""


class DisabledExecutor:
    """Safe default: no-op executor that refuses all execution requests."""

    def execute(self, request: ExecutionRequest) -> ExecutionResult:
        raise ExecutorDisabledError(
            "No executor is configured. Agent execution remains disabled."
        )


class GuardedExecutor:
    """Gate an injected executor using persisted task state and strict bounds.

    Injecting a concrete executor is an explicit application-level decision.
    This wrapper never creates one and defaults to DisabledExecutor.
    """

    def __init__(
        self,
        task_store,
        *,
        executor: Executor | None = None,
        execution_enabled: bool = False,
    ) -> None:
        self.task_store = task_store
        self.executor = executor or DisabledExecutor()
        self.execution_enabled = bool(execution_enabled)

    def execute_task(
        self,
        task_id: str,
        *,
        actor_id: str,
        timeout_seconds: int = 30,
    ) -> ExecutionResult:
        if not self.execution_enabled:
            raise ExecutorDisabledError("Execution is disabled by configuration.")
        if not isinstance(timeout_seconds, int) or not (
            1 <= timeout_seconds <= MAX_EXECUTION_TIMEOUT_SECONDS
        ):
            raise ValueError(
                f"timeout_seconds must be between 1 and {MAX_EXECUTION_TIMEOUT_SECONDS}"
            )

        task = self.task_store.get_task(task_id)
        if task["approval_gates"] and task["status"] != "approved":
            raise TaskStoreError("owner approval is required before execution")
        if task["status"] not in {"queued", "approved", "failed"}:
            raise TaskStoreError("task is not eligible for execution")
        if task["attempts"] > task["max_retries"]:
            raise TaskStoreError("task retry limit has been reached")

        goal = task["goal"]
        if not goal or len(goal) > MAX_GOAL_LENGTH:
            raise ValueError("task goal is empty or exceeds the execution bound")

        if isinstance(self.executor, DisabledExecutor):
            raise ExecutorDisabledError("No executor is configured. Agent execution remains disabled.")

        # Start/audit through the task store before handing off to an injected executor.
        self.task_store.start_task(task_id, actor_id=actor_id)
        request = ExecutionRequest(
            task_id=task_id, goal=goal, timeout_seconds=timeout_seconds
        )
        try:
            result = self.executor.execute(request)
            if not isinstance(result, ExecutionResult):
                raise TypeError("executor must return an ExecutionResult")
            if not isinstance(result.succeeded, bool):
                raise TypeError("executor result succeeded flag must be boolean")
            if not isinstance(result.summary, str):
                raise TypeError("executor result summary must be text")
            summary = result.summary[:MAX_RESULT_LENGTH]
            self.task_store.finish_task(
                task_id, actor_id=actor_id, succeeded=result.succeeded, summary=summary
            )
            return ExecutionResult(result.succeeded, summary)
        except Exception as exc:
            # Do not leak exception content (which could contain secrets) into task records.
            self.task_store.finish_task(
                task_id,
                actor_id="xhubrea-executor-guard",
                succeeded=False,
                summary=f"Executor failed: {type(exc).__name__}",
            )
            raise
