// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from "vitest";
import { checkBackendHealth, sendResearchMessage, structureResearchEvidence, compareAppSnapshots } from "./api.js";

afterEach(() => vi.restoreAllMocks());

describe("XhubREA backend client", () => {
  it("rejects remote HTTP URLs", async () => {
    await expect(checkBackendHealth({ baseUrl: "http://example.com" }))
      .rejects.toThrow("Use HTTPS");
  });

  it("checks backend health", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true, json: async () => ({ status: "ok" })
    }));
    await expect(checkBackendHealth({ baseUrl: "https://api.example.com" }))
      .resolves.toEqual({ status: "ok" });
  });

  it("sends an authenticated research prompt", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true, json: async () => ({ answer: "Evidence-based response" })
    });
    vi.stubGlobal("fetch", fetchMock);
    await expect(sendResearchMessage({
      baseUrl: "https://api.example.com", token: "temporary-token"
    }, "  Explain this finding  ")).resolves.toBe("Evidence-based response");
    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe("Bearer temporary-token");
    expect(fetchMock.mock.calls[0][1].body).toBe(JSON.stringify({ prompt: "Explain this finding" }));
  });

  it("allows HTTP only for localhost development", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true, json: async () => ({ status: "ok" })
    }));
    await expect(checkBackendHealth({ baseUrl: "http://localhost:8000" }))
      .resolves.toEqual({ status: "ok" });
  });

  it("surfaces sanitized backend configuration errors", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: false, status: 503, json: async () => ({ detail: "AI agent is not configured." })
    }));
    await expect(sendResearchMessage({
      baseUrl: "https://api.example.com", token: "temporary-token"
    }, "Test agent")).rejects.toThrow("AI agent is not configured.");
  });

  it("rejects malformed agent responses", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true, json: async () => ({ unexpected: true })
    }));
    await expect(sendResearchMessage({
      baseUrl: "https://api.example.com", token: "temporary-token"
    }, "Test agent")).rejects.toThrow("invalid response");
  });

  it("rejects empty prompts and invalid evidence counts", async () => {
    await expect(sendResearchMessage({ baseUrl: "https://api.example.com" }, "  "))
      .rejects.toThrow("cannot be empty");
    expect(() => structureResearchEvidence({ baseUrl: "https://api.example.com" }, []))
      .toThrow("between 1 and 12");
  });

  it("posts two validated app snapshots with bearer authentication", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true, json: async () => ({ app_a: { app_label: "A" }, app_b: { app_label: "B" } })
    });
    vi.stubGlobal("fetch", fetchMock);
    const appA = { app_label: " App A ", permissions: ["camera"] };
    const appB = { app_label: "App B", architecture: ["MVVM"] };
    await expect(compareAppSnapshots({
      baseUrl: "https://api.example.com", token: "session-token"
    }, appA, appB)).resolves.toHaveProperty("app_a.app_label", "A");
    expect(fetchMock.mock.calls[0][0]).toBe("https://api.example.com/v1/apps/compare");
    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe("Bearer session-token");
    const body = JSON.parse(fetchMock.mock.calls[0][1].body);
    expect(body.app_a.app_label).toBe("App A");
    expect(body.app_a.permissions).toEqual(["camera"]);
    expect(body.app_b.architecture).toEqual(["MVVM"]);
  });

  it("rejects oversized and blank app comparison labels before fetch", () => {
    expect(() => compareAppSnapshots({ baseUrl: "https://api.example.com" },
      { app_label: " " }, { app_label: "B" })).toThrow("App A label");
    expect(() => compareAppSnapshots({ baseUrl: "https://api.example.com" },
      { app_label: "A", permissions: Array(201).fill("camera") },
      { app_label: "B" })).toThrow("at most 200");
    expect(() => compareAppSnapshots({ baseUrl: "https://api.example.com" },
      { app_label: "A", components: ["x".repeat(301)] },
      { app_label: "B" })).toThrow("up to 300");
  });

});
