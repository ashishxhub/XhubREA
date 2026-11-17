const DEFAULT_TIMEOUT_MS = 20000;

function normalizeBaseUrl(value) {
  const trimmed = String(value || "").trim().replace(/\/+$/, "");
  if (!trimmed) throw new Error("Backend URL is required.");
  let parsed;
  try { parsed = new URL(trimmed); } catch { throw new Error("Enter a valid backend URL."); }
  const localHost = ["localhost", "127.0.0.1"].includes(parsed.hostname);
  if (parsed.protocol !== "https:" && !(localHost && parsed.protocol === "http:")) {
    throw new Error("Use HTTPS for remote backend connections.");
  }
  return trimmed;
}

async function request(path, { baseUrl, token, method = "GET", body, timeoutMs = DEFAULT_TIMEOUT_MS }) {
  const base = normalizeBaseUrl(baseUrl);
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(`${base}${path}`, {
      method, headers, body: body === undefined ? undefined : JSON.stringify(body),
      signal: controller.signal
    });
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) {
      const message = typeof payload.detail === "string" ? payload.detail : `Backend request failed (${response.status}).`;
      throw new Error(message);
    }
    return payload;
  } catch (error) {
    if (error.name === "AbortError") throw new Error("Backend request timed out.");
    throw error;
  } finally {
    clearTimeout(timeout);
  }
}

export function checkBackendHealth(config) {
  return request("/health", config);
}

export async function sendResearchMessage(config, prompt) {
  const value = String(prompt || "").trim();
  if (!value) throw new Error("Research prompt cannot be empty.");
  const result = await request("/v1/agent/chat", { ...config, method: "POST", body: { prompt: value } });
  if (typeof result.answer !== "string") throw new Error("Backend returned an invalid response.");
  return result.answer;
}

export function structureResearchEvidence(config, findings) {
  if (!Array.isArray(findings) || findings.length < 1 || findings.length > 12) {
    throw new Error("Provide between 1 and 12 evidence findings.");
  }
  return request("/v1/research/structure", { ...config, method: "POST", body: { findings } });
}
