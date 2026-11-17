// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from "vitest";
import { checkBackendHealth, sendResearchMessage, structureResearchEvidence } from "./api.js";

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

  it("rejects empty prompts and invalid evidence counts", async () => {
    await expect(sendResearchMessage({ baseUrl: "https://api.example.com" }, "  "))
      .rejects.toThrow("cannot be empty");
    expect(() => structureResearchEvidence({ baseUrl: "https://api.example.com" }, []))
      .toThrow("between 1 and 12");
  });
});
