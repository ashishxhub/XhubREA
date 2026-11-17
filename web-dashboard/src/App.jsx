import { useEffect, useMemo, useState } from "react";
import { checkBackendHealth, sendResearchMessage } from "./api.js";
import {
  Activity, AppWindow, ArrowUp, Bot, ChevronDown, CircleHelp,
  FileSearch, FolderOpen, LockKeyhole, Menu, Plus, Search,
  Settings, ShieldCheck, Sparkles, Terminal, X, Command, Clock3,
  Layers3, PanelLeftClose
} from "lucide-react";

const navGroups = [
  { title: "WORKSPACE", items: [
    { label: "New analysis", icon: Plus, description: "Start a fresh, evidence-led research session." },
    { label: "Application intelligence", icon: AppWindow, description: "Explore application structure, components and dependencies." },
    { label: "Research assistant", icon: Bot, description: "Organize technical questions and research notes." },
    { label: "Reports", icon: FolderOpen, description: "Review and organize analysis reports." }
  ]},
  { title: "INTELLIGENCE", items: [
    { label: "Security center", icon: ShieldCheck, description: "Review permissions and security configuration indicators." },
    { label: "Behaviour analysis", icon: Activity, description: "Study observable behaviour and supporting evidence." },
    { label: "DEX & resources", icon: FileSearch, description: "Inspect package metadata, files and resources." },
    { label: "API intelligence", icon: Terminal, description: "Map API surfaces and integration notes." }
  ]}
];

const suggestions = [
  { icon: AppWindow, title: "Understand an application", text: "Explore app structure, components and dependencies.", target: "Application intelligence" },
  { icon: ShieldCheck, title: "Review security posture", text: "Inspect permissions and configuration indicators.", target: "Security center" },
  { icon: Activity, title: "Study app behaviour", text: "Organize observable behaviour and evidence.", target: "Behaviour analysis" },
  { icon: FileSearch, title: "Explore resources", text: "Review files, assets and package metadata.", target: "DEX & resources" }
];

export default function App() {
  const [active, setActive] = useState("New analysis");
  const [prompt, setPrompt] = useState("");
  const [messages, setMessages] = useState([]);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [helpOpen, setHelpOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [backendUrl, setBackendUrl] = useState("");
  const [apiToken, setApiToken] = useState("");
  const [connectionStatus, setConnectionStatus] = useState("Not connected");
  const [isConnecting, setIsConnecting] = useState(false);
  const [isSending, setIsSending] = useState(false);

  useEffect(() => {
    if (!helpOpen) return undefined;
    const handleKeyDown = event => { if (event.key === "Escape") setHelpOpen(false); };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [helpOpen]);

  const activeItem = useMemo(
    () => navGroups.flatMap(group => group.items).find(item => item.label === active),
    [active]
  );

  const filteredGroups = useMemo(() => navGroups.map(group => ({
    ...group,
    items: group.items.filter(item => item.label.toLowerCase().includes(search.toLowerCase()))
  })).filter(group => group.items.length), [search]);

  async function connectBackend() {
    setIsConnecting(true);
    setConnectionStatus("Checking connection…");
    try {
      const result = await checkBackendHealth({ baseUrl: backendUrl });
      setConnectionStatus(result.status === "ok" ? "Backend reachable · authentication required for AI" : "Unexpected health response");
    } catch (error) {
      setConnectionStatus(error.message || "Could not connect to backend.");
    } finally {
      setIsConnecting(false);
    }
  }

  async function sendMessage(value = prompt) {
    const text = value.trim();
    if (!text || isSending) return;
    setMessages(current => [...current, { role: "user", text }]);
    setPrompt("");
    if (!backendUrl.trim() || !apiToken.trim()) return;
    setIsSending(true);
    try {
      const answer = await sendResearchMessage({ baseUrl: backendUrl, token: apiToken }, text);
      setMessages(current => [...current, { role: "assistant", text: answer }]);
    } catch (error) {
      setMessages(current => [...current, { role: "assistant", text: `Backend error: ${error.message}` }]);
    } finally {
      setIsSending(false);
    }
  }

  function selectPage(label) {
    setActive(label);
    setMessages([]);
    setPrompt("");
    setSidebarOpen(false);
  }

  function startNew() {
    setMessages([]);
    setPrompt("");
    setActive("New analysis");
    setSearch("");
    setSidebarOpen(false);
  }

  const isHome = active === "New analysis";

  return (
    <div className="app-shell">
      {sidebarOpen && <button className="scrim" aria-label="Close navigation" onClick={() => setSidebarOpen(false)} />}
      <aside className={`sidebar ${sidebarOpen ? "sidebar-open" : ""}`} aria-label="Main navigation">
        <div className="brand-row">
          <div className="brand-mark"><Sparkles size={18} /></div>
          <span className="brand-name">Xhub<span>REA</span></span>
          <button className="icon-button mobile-close" onClick={() => setSidebarOpen(false)} aria-label="Close navigation"><X size={18} /></button>
        </div>

        <button className="new-analysis" onClick={startNew}><Plus size={16} /> New analysis <span className="shortcut">N</span></button>

        <label className="sidebar-search">
          <Search size={15} />
          <input value={search} onChange={event => setSearch(event.target.value)} placeholder="Find a workspace" aria-label="Find a workspace" />
          <kbd>/</kbd>
        </label>

        <div className="nav-scroll">
          {filteredGroups.map(group => <section className="nav-group" key={group.title}>
            <p className="nav-heading">{group.title}</p>
            {group.items.map(item => {
              const Icon = item.icon;
              return <button key={item.label} onClick={() => selectPage(item.label)} className={`nav-item ${active === item.label ? "active" : ""}`} aria-current={active === item.label ? "page" : undefined}>
                <Icon size={16} /><span>{item.label}</span>
              </button>;
            })}
          </section>)}
          {filteredGroups.length === 0 && <p className="empty-search">No matching workspace.</p>}
        </div>

        <div className="sidebar-bottom">
          <button className={`nav-item ${active === "Settings" ? "active" : ""}`} onClick={() => selectPage("Settings")}><Settings size={16} /><span>Settings</span></button>
          <div className="profile-card">
            <div className="avatar">X</div>
            <div className="profile-copy"><strong>XhubREA Workspace</strong><small>Personal workspace</small></div>
            <ChevronDown size={15} />
          </div>
        </div>
      </aside>

      <main className="main-panel">
        <header className="topbar">
          <div className="topbar-left">
            <button className="icon-button menu-button" onClick={() => setSidebarOpen(true)} aria-label="Open navigation" aria-expanded={sidebarOpen}><Menu size={19} /></button>
            <span className="breadcrumb">Workspace</span><span className="crumb-divider">/</span><strong>{active}</strong>
          </div>
          <div className="topbar-actions">
            <span className="workspace-status"><i /> Preview environment</span>
            <button className="help-button" onClick={() => setHelpOpen(true)}><CircleHelp size={16} /> <span>Help</span></button>
          </div>
        </header>

        <div className="content-wrap">
          {messages.length > 0 ? (
            <section className="conversation">
              <div className="page-heading compact-heading"><div className="heading-icon"><Bot size={19} /></div><div><span className="eyebrow-text">RESEARCH SESSION</span><h1>Research workspace</h1><p>Your question is saved in this local preview session.</p></div></div>
              {messages.map((message, index) => <div className="message-row" key={index}><div className="message-avatar">{message.role === "assistant" ? "REA" : "You"}</div><div className="message-bubble">{message.text}</div></div>)}
              <div className="notice"><Sparkles size={16} /><span>{backendUrl && apiToken ? (isSending ? "REA is processing your research request…" : "Connected mode is enabled for this session.") : "Local preview: configure a backend URL and temporary access token in Settings to enable AI chat."}</span></div>
              <Composer prompt={prompt} setPrompt={setPrompt} sendMessage={sendMessage} placeholder="Continue your research..." />
            </section>
          ) : isHome ? (
            <section className="welcome">
              <div className="hero-topline"><span className="live-pill"><i /> WORKSPACE READY</span><span className="version-tag">V2 DESIGN PREVIEW</span></div>
              <div className="hero-icon"><Sparkles size={24} /></div>
              <h1>Understand technology.<br /><span>See the bigger picture.</span></h1>
              <p className="welcome-copy">A focused workspace for exploring application architecture, resources, behaviour and security — with evidence at the center.</p>
              <Composer prompt={prompt} setPrompt={setPrompt} sendMessage={sendMessage} placeholder="Ask a technical question or describe an app to research..." />
              <div className="section-heading"><div><span className="eyebrow-text">GET STARTED</span><h2>Explore your workspace</h2></div><span className="section-count">04 TOOLS</span></div>
              <div className="suggestion-grid">
                {suggestions.map(item => { const Icon = item.icon; return <button className="suggestion-card" key={item.title} onClick={() => { setActive(item.target); setPrompt(item.title + ": "); }}>
                  <div className="suggestion-icon"><Icon size={18} /></div><div className="card-arrow">↗</div><strong>{item.title}</strong><span>{item.text}</span>
                </button>; })}
              </div>
              <div className="trust-strip"><LockKeyhole size={14} /><span>Designed for authorized, evidence-based application research.</span><span className="trust-divider">•</span><span>Analysis stays local in this preview.</span></div>
            </section>
          ) : (
            <section className="section-view">
              <div className="page-heading"><div className="heading-icon">{active === "Settings" ? <Settings size={20} /> : activeItem ? <activeItem.icon size={20} /> : <Layers3 size={20} />}</div><div><span className="eyebrow-text">XHUBREA WORKSPACE</span><h1>{active}</h1><p>{activeItem?.description || "Manage your workspace preferences and preview settings."}</p></div></div>
              {active === "Settings" ? (
                <div className="coming-card backend-settings">
                  <div className="coming-icon"><LockKeyhole size={20} /></div>
                  <div>
                    <span className="coming-label">OPTIONAL BACKEND CONNECTION</span>
                    <h2>Connect your XhubREA API</h2>
                    <p>Use your own deployed HTTPS backend. The access token is kept only in this page session and is not saved to browser storage.</p>
                    <label className="settings-field">Backend URL<input aria-label="Backend URL" value={backendUrl} onChange={event => setBackendUrl(event.target.value)} placeholder="https://your-api.example.com" /></label>
                    <label className="settings-field">Temporary API access token<input aria-label="Temporary API access token" type="password" autoComplete="off" value={apiToken} onChange={event => setApiToken(event.target.value)} placeholder="Paste your backend token" /></label>
                    <button className="modal-primary" onClick={connectBackend} disabled={isConnecting || !backendUrl.trim()}>{isConnecting ? "Checking…" : "Check backend connection"}</button>
                    <p role="status" aria-live="polite">{connectionStatus}</p>
                  </div>
                </div>
              ) : (
                <div className="coming-card"><div className="coming-icon"><Clock3 size={20} /></div><div><span className="coming-label">MODULE PLACEHOLDER</span><h2>This workspace is being prepared</h2><p>The V2 interface is in place. Its analysis engine and connected data workflows will be added in later development stages.</p><button className="text-button" onClick={startNew}>Back to new analysis <span>→</span></button></div></div>
              )}
              <div className="module-composer"><span className="eyebrow-text">START A LOCAL RESEARCH DRAFT</span><Composer prompt={prompt} setPrompt={setPrompt} sendMessage={sendMessage} placeholder={`Ask something about ${active.toLowerCase()}...`} /></div>
            </section>
          )}
          <footer className="page-footer"><span>© XhubREA</span><span className="footer-dot">•</span><span>Application intelligence workspace</span><span className="footer-spacer" /><span>Preview build · V2</span></footer>
        </div>
      </main>

      {helpOpen && <div className="modal-backdrop" role="presentation" onClick={() => setHelpOpen(false)}><section className="help-modal" role="dialog" aria-modal="true" aria-labelledby="help-title" onClick={event => event.stopPropagation()}><button className="icon-button modal-close" onClick={() => setHelpOpen(false)} aria-label="Close help"><X size={18} /></button><div className="heading-icon"><CircleHelp size={20} /></div><span className="eyebrow-text">QUICK GUIDE</span><h2 id="help-title">Welcome to XhubREA</h2><p>This is the V2 frontend preview. Choose a workspace from the sidebar, or enter a research question to create a local draft.</p><div className="help-note"><LockKeyhole size={15} /> Only analyze applications and systems you own or are authorized to test.</div><button className="modal-primary" onClick={() => setHelpOpen(false)}>Got it</button></section></div>}
    </div>
  );
}

function Composer({ prompt, setPrompt, sendMessage, placeholder }) {
  return <div className="composer">
    <textarea value={prompt} disabled={isSending} onChange={event => setPrompt(event.target.value)} onKeyDown={event => { if (event.key === "Enter" && !event.shiftKey) { event.preventDefault(); sendMessage(); } }} placeholder={placeholder} rows={2} aria-label="Research message" />
    <div className="composer-footer"><span className="composer-hint">Enter to send <span>·</span> Shift + Enter for a new line</span><button className="send-button" onClick={() => sendMessage()} disabled={!prompt.trim() || isSending} aria-label="Send message"><ArrowUp size={18} /></button></div>
  </div>;
}
