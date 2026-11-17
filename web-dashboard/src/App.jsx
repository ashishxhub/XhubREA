import { useState } from "react";
import {
  Activity, AppWindow, ArrowUp, Bot, ChevronDown, CircleHelp,
  FileSearch, FolderOpen, LockKeyhole, Menu, Plus, Search,
  Settings, ShieldCheck, Sparkles, Terminal, X
} from "lucide-react";

const navGroups = [
  { title: "WORKSPACE", items: [
    { label: "New analysis", icon: Plus },
    { label: "Application intelligence", icon: AppWindow },
    { label: "Research assistant", icon: Bot },
    { label: "Reports", icon: FolderOpen }
  ]},
  { title: "INTELLIGENCE", items: [
    { label: "Security center", icon: ShieldCheck },
    { label: "Behaviour analysis", icon: Activity },
    { label: "DEX & resources", icon: FileSearch },
    { label: "API intelligence", icon: Terminal }
  ]}
];

const suggestions = [
  { icon: AppWindow, title: "Understand an application", text: "Explore app structure, components and dependencies." },
  { icon: ShieldCheck, title: "Review security posture", text: "Inspect permissions and configuration indicators." },
  { icon: Activity, title: "Study app behaviour", text: "Organize observable behaviour and evidence." },
  { icon: FileSearch, title: "Explore resources", text: "Review files, assets and package metadata." }
];

export default function App() {
  const [active, setActive] = useState("New analysis");
  const [prompt, setPrompt] = useState("");
  const [messages, setMessages] = useState([]);
  const [sidebarOpen, setSidebarOpen] = useState(false);

  function sendMessage(value = prompt) {
    const text = value.trim();
    if (!text) return;
    setMessages(current => [...current, { role: "user", text }]);
    setPrompt("");
  }

  function startNew() {
    setMessages([]);
    setPrompt("");
    setActive("New analysis");
    setSidebarOpen(false);
  }

  return (
    <div className="app-shell">
      {sidebarOpen && <button className="scrim" aria-label="Close menu" onClick={() => setSidebarOpen(false)} />}
      <aside className={`sidebar ${sidebarOpen ? "sidebar-open" : ""}`}>
        <div className="brand-row">
          <div className="brand-mark"><Sparkles size={19} /></div>
          <span className="brand-name">Xhub<span>REA</span></span>
          <button className="icon-button mobile-close" onClick={() => setSidebarOpen(false)} aria-label="Close menu"><X size={18} /></button>
        </div>
        <button className="new-analysis" onClick={startNew}><Plus size={17} /> New analysis</button>
        <div className="nav-scroll">
          {navGroups.map(group => <section className="nav-group" key={group.title}>
            <p className="nav-heading">{group.title}</p>
            {group.items.map(item => {
              const Icon = item.icon;
              return <button key={item.label} onClick={() => { setActive(item.label); setSidebarOpen(false); }} className={`nav-item ${active === item.label ? "active" : ""}`}>
                <Icon size={17} /><span>{item.label}</span>
              </button>;
            })}
          </section>)}
        </div>
        <div className="sidebar-bottom">
          <button className="nav-item"><Settings size={17} /><span>Settings</span></button>
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
            <button className="icon-button menu-button" onClick={() => setSidebarOpen(true)} aria-label="Open menu"><Menu size={20} /></button>
            <span className="breadcrumb">Workspace</span><span className="crumb-divider">/</span><strong>{active}</strong>
          </div>
          <button className="help-button"><CircleHelp size={16} /> <span>Help</span></button>
        </header>

        <div className="content-wrap">
          {messages.length === 0 ? <section className="welcome">
            <div className="eyebrow"><span className="status-dot" /> APPLICATION INTELLIGENCE WORKSPACE</div>
            <div className="hero-icon"><Sparkles size={25} /></div>
            <h1>Understand technology.<br /><span>See the bigger picture.</span></h1>
            <p className="welcome-copy">Your workspace for exploring application architecture, resources, behaviour and security — with evidence at the center.</p>

            <div className="composer">
              <textarea value={prompt} onChange={e => setPrompt(e.target.value)} onKeyDown={e => { if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); sendMessage(); } }} placeholder="Ask about an app, its structure, or an analysis..." rows={2} />
              <div className="composer-footer">
                <span className="composer-hint">Start with a question or choose a workspace</span>
                <button className="send-button" onClick={() => sendMessage()} disabled={!prompt.trim()} aria-label="Send message"><ArrowUp size={18} /></button>
              </div>
            </div>

            <div className="section-label">EXPLORE YOUR WORKSPACE</div>
            <div className="suggestion-grid">
              {suggestions.map(item => { const Icon = item.icon; return <button className="suggestion-card" key={item.title} onClick={() => { setActive(item.title); setPrompt(item.title + ": "); }}>
                <div className="suggestion-icon"><Icon size={18} /></div><strong>{item.title}</strong><span>{item.text}</span>
              </button>; })}
            </div>
          </section> : <section className="conversation">
            <div className="conversation-heading"><div className="hero-icon small"><Bot size={20} /></div><div><h2>Research workspace</h2><p>Your question has been added to this local draft.</p></div></div>
            {messages.map((message, index) => <div className="message-row" key={index}><div className="message-avatar">You</div><div className="message-bubble">{message.text}</div></div>)}
            <div className="notice"><Sparkles size={16} /><span>This is the frontend preview. AI responses and APK analysis will be connected in a later backend integration step.</span></div>
            <div className="composer conversation-composer">
              <textarea value={prompt} onChange={e => setPrompt(e.target.value)} onKeyDown={e => { if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); sendMessage(); } }} placeholder="Continue your research..." rows={2} />
              <div className="composer-footer"><span className="composer-hint">Enter to send · Shift + Enter for a new line</span><button className="send-button" onClick={() => sendMessage()} disabled={!prompt.trim()} aria-label="Send message"><ArrowUp size={18} /></button></div>
            </div>
          </section>}
          <footer className="page-footer"><LockKeyhole size={13} /> XhubREA is designed for authorized, evidence-based application research.</footer>
        </div>
      </main>
    </div>
  );
}
