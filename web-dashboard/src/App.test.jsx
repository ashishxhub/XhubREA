// @vitest-environment jsdom
import { afterEach, describe, expect, it } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom/vitest";
import App from "./App.jsx";

afterEach(() => cleanup());

describe("XhubREA dashboard interactions", () => {
  it("filters workspace navigation", () => {
    render(<App />);
    fireEvent.change(screen.getByRole("textbox", { name: "Find a workspace" }), {
      target: { value: "Security" }
    });
    expect(screen.getByRole("button", { name: /Security center/ })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /API intelligence/ })).not.toBeInTheDocument();
  });

  it("shows a local draft after sending a question", () => {
    render(<App />);
    fireEvent.change(screen.getByRole("textbox", { name: "Research message" }), {
      target: { value: "Explain this app architecture" }
    });
    fireEvent.click(screen.getByRole("button", { name: "Send message" }));
    expect(screen.getByText("Explain this app architecture")).toBeInTheDocument();
    expect(screen.getByText(/configure a backend URL and temporary access token/)).toBeInTheDocument();
  });

  it("opens and closes the help dialog", () => {
    render(<App />);
    fireEvent.click(screen.getByRole("button", { name: /Help/ }));
    expect(screen.getByRole("dialog", { name: "Welcome to XhubREA" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Got it" }));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("closes the help dialog with Escape", () => {
    render(<App />);
    fireEvent.click(screen.getByRole("button", { name: /Help/ }));
    fireEvent.keyDown(window, { key: "Escape" });
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("opens and closes mobile navigation", () => {
    render(<App />);
    fireEvent.click(screen.getByRole("button", { name: "Open navigation" }));
    expect(screen.getByRole("button", { name: "Open navigation" })).toHaveAttribute("aria-expanded", "true");
    fireEvent.click(screen.getByRole("button", { name: "Close navigation" }));
    expect(screen.getByRole("button", { name: "Open navigation" })).toHaveAttribute("aria-expanded", "false");
  });

  it("opens a module placeholder from a suggestion card", () => {
    render(<App />);
    fireEvent.click(screen.getByRole("button", { name: /Review security posture/ }));
    expect(screen.getByRole("heading", { name: "Security center" })).toBeInTheDocument();
    expect(screen.getByText("This workspace is being prepared")).toBeInTheDocument();
  });
});

