/**
 * Inked MD / MD Studio — Core Offline Document Engine
 * Offline KaTeX LaTeX Mathematical Typesetting & Mermaid Diagram Rendering
 */

(function () {
  'use strict';

  let currentRawMarkdown = '';
  let currentCoverConfig = null;
  let searchMatches = [];
  let currentSearchIndex = -1;
  let lastScrollY = 0;
  let scrollTicking = false;
  let mermaidCounter = 0;

  // Initialize Mermaid if available
  function initMermaidSafe() {
    const m = window.mermaid || (window.mermaid && window.mermaid.default);
    if (m && typeof m.initialize === 'function') {
      try {
        m.initialize({
          startOnLoad: false,
          theme: 'default',
          securityLevel: 'loose',
          fontFamily: 'Inter, sans-serif'
        });
      } catch (e) {
        console.warn('Mermaid init warning:', e);
      }
    }
  }

  // --- Unicode-Safe Slugify for Headings ---
  function unicodeSafeSlug(text) {
    if (!text) return 'heading';
    return text
      .trim()
      .toLowerCase()
      .replace(/[\s\t\n]+/g, '-')
      .replace(/[^\w\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF-]/g, '');
  }

  // --- Automated Direction Detection ---
  function detectDirection(text) {
    if (!text) return 'ltr';
    let rtlChars = 0;
    let ltrChars = 0;
    const rtlRegex = /[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF]/g;
    const ltrRegex = /[A-Za-z]/g;

    const rtlMatches = text.match(rtlRegex);
    if (rtlMatches) rtlChars = rtlMatches.length;

    const ltrMatches = text.match(ltrRegex);
    if (ltrMatches) ltrChars = ltrMatches.length;

    const total = rtlChars + ltrChars;
    if (total === 0) return 'ltr';

    return (rtlChars / total >= 0.25) ? 'rtl' : 'ltr';
  }

  // --- Syntax Highlighter ---
  const HighlightEngine = {
    keywords: {
      kotlin: ['fun', 'val', 'var', 'class', 'interface', 'object', 'sealed', 'data', 'when', 'if', 'else', 'for', 'while', 'return', 'package', 'import', 'private', 'public', 'protected', 'internal', 'override', 'companion', 'suspend', 'by'],
      python: ['def', 'class', 'import', 'from', 'return', 'if', 'elif', 'else', 'for', 'while', 'try', 'except', 'finally', 'with', 'as', 'lambda', 'yield', 'async', 'await', 'pass', 'True', 'False', 'None'],
      javascript: ['const', 'let', 'var', 'function', 'class', 'return', 'if', 'else', 'for', 'while', 'import', 'export', 'default', 'from', 'async', 'await', 'try', 'catch', 'new', 'this', 'true', 'false', 'null', 'undefined'],
      typescript: ['const', 'let', 'var', 'function', 'class', 'interface', 'type', 'enum', 'return', 'if', 'else', 'import', 'export', 'async', 'await', 'true', 'false', 'null', 'undefined'],
      java: ['public', 'private', 'protected', 'class', 'interface', 'enum', 'extends', 'implements', 'return', 'if', 'else', 'for', 'while', 'try', 'catch', 'finally', 'new', 'static', 'final', 'void', 'int', 'boolean', 'String'],
      sql: ['SELECT', 'FROM', 'WHERE', 'INSERT', 'INTO', 'UPDATE', 'DELETE', 'CREATE', 'TABLE', 'JOIN', 'LEFT', 'RIGHT', 'INNER', 'GROUP', 'BY', 'ORDER', 'LIMIT', 'AND', 'OR', 'NOT', 'NULL', 'PRIMARY', 'KEY'],
      bash: ['echo', 'cd', 'ls', 'mkdir', 'rm', 'cp', 'mv', 'cat', 'grep', 'sudo', 'chmod', 'chown', 'curl', 'wget', 'export', 'if', 'then', 'else', 'fi', 'for', 'do', 'done']
    },

    highlight: function (code, lang) {
      lang = (lang || '').toLowerCase().trim();
      const kwList = this.keywords[lang] || this.keywords['kotlin'] || [];

      let escaped = code
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');

      // Comments
      escaped = escaped.replace(/(\/\/[^\n]*|#[^\n]*)/g, '<span style="color:#868e96;font-style:italic;">$1</span>');
      // Multi-line comments
      escaped = escaped.replace(/(\/\*[\s\S]*?\*\/)/g, '<span style="color:#868e96;font-style:italic;">$1</span>');
      // Strings
      escaped = escaped.replace(/(["'`].*?["'`])/g, '<span style="color:#2b8a3e;">$1</span>');
      // Numbers
      escaped = escaped.replace(/\b(\d+(\.\d+)?)\b/g, '<span style="color:#d9480f;">$1</span>');

      // Keywords
      if (kwList.length > 0) {
        const regex = new RegExp(`\\b(${kwList.join('|')})\\b`, 'g');
        escaped = escaped.replace(regex, '<span style="color:#7048e8;font-weight:600;">$1</span>');
      }

      return escaped;
    }
  };

  // --- KaTeX Mathematical Typesetting Engine ---
  const MathEngine = {
    renderInline: function (formula) {
      let clean = (formula || '').trim();
      if (!clean) return '';
      if (typeof window.katex !== 'undefined' && window.katex && typeof window.katex.renderToString === 'function') {
        try {
          return `<span class="math-inline" dir="ltr">${window.katex.renderToString(clean, {
            displayMode: false,
            throwOnError: false
          })}</span>`;
        } catch (e) {
          console.warn('KaTeX inline render error:', e);
        }
      }
      return `<span class="math-inline" dir="ltr">${clean}</span>`;
    },

    renderBlock: function (formula) {
      let clean = (formula || '').trim();
      if (!clean) return '';
      if (typeof window.katex !== 'undefined' && window.katex && typeof window.katex.renderToString === 'function') {
        try {
          return `<div class="math-display" dir="ltr">${window.katex.renderToString(clean, {
            displayMode: true,
            throwOnError: false
          })}</div>`;
        } catch (e) {
          console.warn('KaTeX block render error:', e);
        }
      }
      return `<div class="math-display" dir="ltr">${clean}</div>`;
    }
  };

  // --- Mermaid Diagram Engine (Hybrid: Mermaid.js + Built-in Native SVG Engine) ---
  const MermaidEngine = {
    renderBuiltinSvg: function (source) {
      source = source.trim();
      const isLR = source.includes(' LR') || source.includes('LR');

      // 1. Sequence Diagram
      if (source.startsWith('sequenceDiagram')) {
        let participants = new Set();
        let messages = [];
        source.split('\n').forEach(l => {
          let line = l.trim();
          let pMatch = line.match(/(?:participant|actor)\s+([a-zA-Z0-9_-]+)(?:\s+as\s+(.*))?/);
          if (pMatch) {
            participants.add(pMatch[2] || pMatch[1]);
          }
          let mMatch = line.match(/([a-zA-Z0-9_-]+)\s*(?:->>|-->>|-+>>|->)\s*([a-zA-Z0-9_-]+)\s*:\s*(.*)/);
          if (mMatch) {
            participants.add(mMatch[1]);
            participants.add(mMatch[2]);
            messages.push({ from: mMatch[1], to: mMatch[2], text: mMatch[3] });
          }
        });

        if (participants.size === 0) {
          participants.add('Client');
          participants.add('Server');
          messages.push({ from: 'Client', to: 'Server', text: 'Request' });
          messages.push({ from: 'Server', to: 'Client', text: 'Response 200 OK' });
        }

        const pList = Array.from(participants);
        const colWidth = 140;
        const totalWidth = Math.max(340, pList.length * colWidth + 40);
        const rowHeight = 48;
        const totalHeight = 80 + Math.max(1, messages.length) * rowHeight;
        let pX = new Map();
        let svg = `<svg viewBox="0 0 ${totalWidth} ${totalHeight}" width="${totalWidth}" height="${totalHeight}" style="max-width:100%;height:auto;"><defs><marker id="m-arrow" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse"><path d="M 0 1 L 10 5 L 0 9 z" fill="#4c6ef5"/></marker></defs>`;

        pList.forEach((p, idx) => {
          let x = 50 + idx * colWidth;
          pX.set(p, x);
          svg += `
            <rect x="${x - 48}" y="12" width="96" height="30" rx="6" fill="#4c6ef5" fill-opacity="0.15" stroke="#4c6ef5" stroke-width="1.5"/>
            <text x="${x}" y="32" font-size="11" font-weight="700" text-anchor="middle" fill="currentColor">${p}</text>
            <line x1="${x}" y1="44" x2="${x}" y2="${totalHeight - 14}" stroke="#ced4da" stroke-dasharray="4 4" stroke-width="1.5"/>
          `;
        });

        messages.forEach((m, idx) => {
          let y = 68 + idx * rowHeight;
          let x1 = pX.get(m.from) || 50;
          let x2 = pX.get(m.to) || (50 + colWidth);
          svg += `
            <line x1="${x1}" y1="${y}" x2="${x2}" y2="${y}" stroke="#4c6ef5" stroke-width="2" marker-end="url(#m-arrow)"/>
            <text x="${(x1 + x2) / 2}" y="${y - 6}" font-size="11" fill="currentColor" text-anchor="middle">${m.text}</text>
          `;
        });
        svg += `</svg>`;
        return svg;
      }

      // 2. Pie Chart
      if (source.startsWith('pie')) {
        let slices = [];
        let total = 0;
        let title = 'Distribution';
        source.split('\n').forEach(l => {
          let m = l.trim().match(/pie\s+title\s+(.*)/);
          if (m) title = m[1];
          let sm = l.trim().match(/"([^"]+)"\s*:\s*([\d.]+)/);
          if (sm) {
            let val = parseFloat(sm[2]);
            slices.push({ label: sm[1], val: val });
            total += val;
          }
        });
        if (slices.length === 0) {
          slices = [{ label: 'Section A', val: 50 }, { label: 'Section B', val: 30 }, { label: 'Section C', val: 20 }];
          total = 100;
        }
        const colors = ['#4c6ef5', '#2f9e44', '#f76707', '#ae3ec9', '#1098ad', '#fab005'];
        const cx = 100, cy = 100, r = 72;
        let startAngle = 0;
        let paths = '';
        let legend = '';
        slices.forEach((s, idx) => {
          let sliceAngle = (s.val / total) * 2 * Math.PI;
          let endAngle = startAngle + sliceAngle;
          let x1 = cx + r * Math.cos(startAngle);
          let y1 = cy + r * Math.sin(startAngle);
          let x2 = cx + r * Math.cos(endAngle);
          let y2 = cy + r * Math.sin(endAngle);
          let largeArc = sliceAngle > Math.PI ? 1 : 0;
          let color = colors[idx % colors.length];
          paths += `<path d="M ${cx} ${cy} L ${x1} ${y1} A ${r} ${r} 0 ${largeArc} 1 ${x2} ${y2} Z" fill="${color}" fill-opacity="0.88" stroke="var(--bg)" stroke-width="2"/>`;
          let percent = Math.round((s.val / total) * 100);
          legend += `
            <g transform="translate(190, ${36 + idx * 22})">
              <rect width="12" height="12" rx="2" fill="${color}"/>
              <text x="18" y="10" font-size="11" fill="currentColor">${s.label} (${percent}%)</text>
            </g>
          `;
          startAngle = endAngle;
        });
        return `
          <svg viewBox="0 0 360 200" width="360" height="200" style="max-width:100%;height:auto;">
            <text x="180" y="20" font-size="13" font-weight="700" text-anchor="middle" fill="currentColor">${title}</text>
            ${paths}
            ${legend}
          </svg>
        `;
      }

      // 3. Default: Flowchart / State / Mindmap / Graph
      let nodes = new Map();
      let edges = [];
      source.split('\n').forEach(l => {
        let trimmed = l.trim();
        let edgeMatch = trimmed.match(/([a-zA-Z0-9_-]+)(?:\[(.*?)\]|\((.*?)\)|\{(.*?)\})?\s*(-+>|==>|-->)\s*(?:\|([^|]+)\|)?\s*([a-zA-Z0-9_-]+)(?:\[(.*?)\]|\((.*?)\)|\{(.*?)\})?/);
        if (edgeMatch) {
          let fromId = edgeMatch[1];
          let fromLabel = edgeMatch[2] || edgeMatch[3] || edgeMatch[4] || fromId;
          let edgeLabel = edgeMatch[6] || '';
          let toId = edgeMatch[7];
          let toLabel = edgeMatch[8] || edgeMatch[9] || edgeMatch[10] || toId;
          nodes.set(fromId, fromLabel);
          nodes.set(toId, toLabel);
          edges.push({ from: fromId, to: toId, label: edgeLabel });
        } else {
          let nodeMatch = trimmed.match(/([a-zA-Z0-9_-]+)\[(.*?)\]/);
          if (nodeMatch) {
            nodes.set(nodeMatch[1], nodeMatch[2]);
          }
        }
      });

      if (nodes.size === 0) {
        nodes.set('A', 'Client Application');
        nodes.set('B', 'API Gateway');
        nodes.set('C', 'Microservice');
        edges.push({ from: 'A', to: 'B', label: 'HTTPS' });
        edges.push({ from: 'B', to: 'C', label: 'gRPC' });
      }

      const nodeList = Array.from(nodes.entries());
      const nodeWidth = 140;
      const nodeHeight = 40;
      const gap = 55;
      let totalWidth = isLR ? (nodeList.length * (nodeWidth + gap) + 40) : (nodeWidth + 60);
      let totalHeight = isLR ? (nodeHeight + 60) : (nodeList.length * (nodeHeight + gap) + 30);
      let nodePositions = new Map();
      let svgContent = '';

      nodeList.forEach((entry, idx) => {
        let x = isLR ? (idx * (nodeWidth + gap) + 20) : 30;
        let y = isLR ? 20 : (idx * (nodeHeight + gap) + 20);
        nodePositions.set(entry[0], { x: x + nodeWidth / 2, y: y + nodeHeight / 2 });
        svgContent += `
          <g class="flow-node">
            <rect x="${x}" y="${y}" width="${nodeWidth}" height="${nodeHeight}" rx="6" fill="#4c6ef5" fill-opacity="0.12" stroke="#4c6ef5" stroke-width="1.8"/>
            <text x="${x + nodeWidth / 2}" y="${y + nodeHeight / 2 + 4}" font-family="Inter, sans-serif" font-size="12" font-weight="600" fill="currentColor" text-anchor="middle">${entry[1]}</text>
          </g>
        `;
      });

      edges.forEach(edge => {
        let fromPos = nodePositions.get(edge.from);
        let toPos = nodePositions.get(edge.to);
        if (fromPos && toPos) {
          svgContent += `
            <g class="flow-edge">
              <line x1="${fromPos.x}" y1="${fromPos.y}" x2="${toPos.x}" y2="${toPos.y}" stroke="#868e96" stroke-width="1.8" marker-end="url(#f-arrow)"/>
              ${edge.label ? `<text x="${(fromPos.x + toPos.x) / 2}" y="${(fromPos.y + toPos.y) / 2 - 5}" font-size="10" font-weight="600" fill="#4c6ef5" text-anchor="middle">${edge.label}</text>` : ''}
            </g>
          `;
        }
      });

      return `
        <svg viewBox="0 0 ${totalWidth} ${totalHeight}" width="${totalWidth}" height="${totalHeight}" style="max-width:100%;height:auto;">
          <defs>
            <marker id="f-arrow" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
              <path d="M 0 1 L 10 5 L 0 9 z" fill="#868e96" />
            </marker>
          </defs>
          ${svgContent}
        </svg>
      `;
    },

    renderPlaceholder: function (source, id) {
      let encoded = encodeURIComponent(source.trim());
      // Render immediate native SVG
      let initialSvg = this.renderBuiltinSvg(source);

      return `
        <div class="mermaid-container" id="mermaid-container-${id}" data-source="${encoded}" onclick="window.InkedBridge && window.InkedBridge.onMermaidClick(this.querySelector('.mermaid-svg-wrapper').innerHTML, decodeURIComponent(this.getAttribute('data-source')))">
          <div class="mermaid-zoom-badge">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="11" y1="8" x2="11" y2="14"/><line x1="8" y1="11" x2="14" y2="11"/></svg>
            Tap to Zoom
          </div>
          <div class="mermaid-svg-wrapper mermaid-target" id="mermaid-target-${id}" data-code="${encoded}">
            ${initialSvg}
          </div>
        </div>
      `;
    },

    renderAllAsync: async function () {
      initMermaidSafe();
      const m = window.mermaid || (window.mermaid && window.mermaid.default);
      if (!m || typeof m.render !== 'function') return;

      const targets = document.querySelectorAll('.mermaid-target');
      for (let i = 0; i < targets.length; i++) {
        const el = targets[i];
        const code = decodeURIComponent(el.getAttribute('data-code'));
        const renderId = `m-svg-${Date.now()}-${i}`;
        try {
          const res = await m.render(renderId, code);
          const svgCode = typeof res === 'string' ? res : (res && res.svg ? res.svg : '');
          if (svgCode) {
            el.innerHTML = svgCode;
          }
        } catch (err) {
          console.warn('Mermaid render notice (keeping native SVG):', err);
        }
      }
    }
  };

  // --- Cover Page Generator ---
  function renderCoverPage(config) {
    if (!config || !config.style || config.style === 'none') return '';

    let title = config.title || 'Document Title';
    let subtitle = config.subtitle || '';
    let author = config.author || '';
    let date = config.date || '';
    let status = config.status || 'FINAL';
    let eyebrow = config.eyebrow || 'TECHNICAL REPORT';

    switch (config.style) {
      case 'classic':
        return `
          <div class="cover-page cover-classic">
            <h1 class="cover-title">${title}</h1>
            ${subtitle ? `<div class="cover-subtitle">${subtitle}</div>` : ''}
            <div class="cover-divider"></div>
            <div class="cover-meta">${author}${author && date ? ' • ' : ''}${date}</div>
          </div>
        `;
      case 'modern':
        return `
          <div class="cover-page cover-modern">
            <div class="cover-eyebrow">${eyebrow}</div>
            <h1 class="cover-title">${title}</h1>
            ${subtitle ? `<div class="cover-subtitle">${subtitle}</div>` : ''}
            <div class="cover-footer">
              ${author ? `<div><strong>Author:</strong> ${author}</div>` : ''}
              ${date ? `<div><strong>Date:</strong> ${date}</div>` : ''}
            </div>
          </div>
        `;
      case 'academic':
        return `
          <div class="cover-page cover-academic">
            <div class="cover-eyebrow">${eyebrow}</div>
            <h1 class="cover-title">${title}</h1>
            ${subtitle ? `<div class="cover-subtitle">${subtitle}</div>` : ''}
            <div style="margin-top:2.5em;font-size:0.95em;">
              <div>${author}</div>
              <div style="color:var(--text-muted);margin-top:4px;">${date}</div>
            </div>
          </div>
        `;
      case 'executive':
        return `
          <div class="cover-page cover-executive">
            <div class="cover-badge">${status}</div>
            <h1 class="cover-title">${title}</h1>
            ${subtitle ? `<div class="cover-subtitle" style="color:var(--text-secondary);">${subtitle}</div>` : ''}
            <div class="cover-capsules">
              ${author ? `<div class="cover-capsule">${author}</div>` : ''}
              ${date ? `<div class="cover-capsule">${date}</div>` : ''}
            </div>
          </div>
        `;
      case 'technical':
        return `
          <div class="cover-page cover-technical">
            <div class="spec-badge">SPEC-${status}</div>
            <h1 class="cover-title">${title}</h1>
            ${subtitle ? `<div style="color:var(--text-secondary);font-size:1.1em;">${subtitle}</div>` : ''}
            <div class="cover-spec-grid">
              <div><div class="spec-label">Author</div><div class="spec-value">${author || 'Engineering'}</div></div>
              <div><div class="spec-label">Date</div><div class="spec-value">${date || 'Current'}</div></div>
              <div><div class="spec-label">Status</div><div class="spec-value">${status}</div></div>
            </div>
          </div>
        `;
      default:
        return '';
    }
  }

  // --- Complete Markdown Parser ---
  function parseMarkdown(md) {
    if (!md) return '';

    let toc = [];
    let headingCounter = 0;
    let footnotes = [];

    // Step 1: Normalize line endings
    let text = md.replace(/\r\n/g, '\n');

    // Step 2: Extract and protect Code Blocks & Mermaid
    // Note: Placeholders deliberately contain NO underscores to prevent corruption by markdown italic syntax (_..._)
    let codeBlocks = [];
    text = text.replace(/```([a-zA-Z0-9_-]*)\n([\s\S]*?)```/g, (match, lang, code) => {
      let idx = codeBlocks.length;
      codeBlocks.push({ lang: lang.trim(), code: code });
      return `%%CODEBLOCK${idx}%%`;
    });

    // Step 3: Extract and protect Display Math ($$ ... $$)
    let displayMath = [];
    text = text.replace(/\$\$([\s\S]*?)\$\$/g, (match, formula) => {
      let idx = displayMath.length;
      displayMath.push(formula.trim());
      return `%%DISPLAYMATH${idx}%%`;
    });

    // Step 4: Extract and protect Inline Math ($ ... $)
    let inlineMath = [];
    text = text.replace(/\$([^\$\n]+?)\$/g, (match, formula) => {
      let trimmed = formula.trim();
      if (!trimmed) return match;
      let idx = inlineMath.length;
      inlineMath.push(trimmed);
      return `%%INLINEMATH${idx}%%`;
    });

    // Step 5: Page Breaks (---pagebreak---)
    text = text.replace(/^---pagebreak---$/gm, `
      <div class="page-break-container pagebreak">
        <div class="page-break-indicator">
          <span>PAGE BREAK</span>
        </div>
      </div>
    `);

    // Step 6: Custom MD Studio Semantic Callouts
    const calloutTypes = [
      { name: 'definition', icon: '📖', label: 'Definition' },
      { name: 'important', icon: '⚠️', label: 'Important' },
      { name: 'example', icon: '💡', label: 'Example' },
      { name: 'tip', icon: '✨', label: 'Tip' },
      { name: 'warning', icon: '🛑', label: 'Warning' },
      { name: 'note', icon: '📝', label: 'Note' },
      { name: 'exam', icon: '🎯', label: 'Exam Point' },
      { name: 'reminder', icon: '🔔', label: 'Reminder' },
      { name: 'secondary', icon: '📌', label: 'Reference' }
    ];

    calloutTypes.forEach(c => {
      let regex = new RegExp(`\\[${c.name}\\]([\\s\\S]*?)\\[\\/${c.name}\\]`, 'g');
      text = text.replace(regex, (match, content) => {
        return `
          <div class="callout callout-${c.name}">
            <div class="callout-title">
              <span class="callout-icon">${c.icon}</span>
              <span>${c.label}</span>
            </div>
            <div class="callout-content">${content.trim()}</div>
          </div>
        `;
      });
    });

    // Step 7: Footnotes syntax ([^1])
    text = text.replace(/^\[\^(\d+)\]:\s*(.*)$/gm, (match, num, def) => {
      footnotes.push({ num: num, def: def.trim() });
      return '';
    });
    text = text.replace(/\[\^(\d+)\]/g, (match, num) => {
      return `<sup class="footnote-ref"><a href="#fn-${num}" id="fnref-${num}">[${num}]</a></sup>`;
    });

    // Step 8: Headings with Anchors & Table of Contents Collection
    text = text.replace(/^(#{1,6})\s+(.*)$/gm, (match, hashes, title) => {
      let level = hashes.length;
      let cleanTitle = title.replace(/<[^>]*>/g, '').trim();
      headingCounter++;
      let slug = unicodeSafeSlug(cleanTitle) + '-' + headingCounter;

      toc.push({
        id: slug,
        title: cleanTitle,
        level: level
      });

      return `<h${level} id="${slug}">${cleanTitle}</h${level}>`;
    });

    // Step 9: Task Lists (- [ ] and - [x]) — MUST BE PARSED BEFORE UNORDERED LISTS
    text = text.replace(/^\s*[-*+]\s+\[\s\]\s+(.*)$/gm, `
      <li class="task-list-item"><input type="checkbox" class="task-checkbox" disabled> <span>$1</span></li>
    `);
    text = text.replace(/^\s*[-*+]\s+\[[xX]\]\s+(.*)$/gm, `
      <li class="task-list-item"><input type="checkbox" class="task-checkbox" checked disabled> <span class="task-done">$1</span></li>
    `);

    // Step 10: Lists (Unordered & Ordered)
    text = text.replace(/^\s*[-*+]\s+(.*)$/gm, '<li>$1</li>');
    text = text.replace(/(<li.*<\/li>\n?)+/g, '<ul>$&</ul>');

    text = text.replace(/^\s*\d+\.\s+(.*)$/gm, '<li class="ol-item">$1</li>');
    text = text.replace(/(<li class="ol-item">.*<\/li>\n?)+/g, '<ol>$&</ol>');

    // Step 11: Definition lists (Term\n: Definition)
    text = text.replace(/^([^\n]+)\n:\s+([^\n]+)$/gm, `
      <dl><dt>$1</dt><dd>$2</dd></dl>
    `);

    // Step 12: Horizontal Rules
    text = text.replace(/^---$/gm, '<hr style="border:none;border-top:1px solid var(--border);margin:1.8em 0;">');

    // Step 13: Blockquotes
    text = text.replace(/^>\s+(.*)$/gm, '<blockquote>$1</blockquote>');

    // Step 14: Tables
    let tableRegex = /((?:\|[^\n]+\|\r?\n)+)/g;
    text = text.replace(tableRegex, (match) => {
      let lines = match.trim().split('\n');
      if (lines.length < 2) return match;

      let headers = lines[0].split('|').map(s => s.trim()).filter((s, i, a) => i > 0 && i < a.length - 1);
      let separator = lines[1];
      if (!separator.includes('---')) return match;

      let alignments = separator.split('|').map(s => s.trim()).filter((s, i, a) => i > 0 && i < a.length - 1).map(s => {
        if (s.startsWith(':') && s.endsWith(':')) return 'center';
        if (s.endsWith(':')) return 'right';
        return 'left';
      });

      let thead = '<thead><tr>' + headers.map((h, i) => `<th style="text-align:${alignments[i] || 'left'}">${h}</th>`).join('') + '</tr></thead>';

      let tbody = '<tbody>';
      for (let i = 2; i < lines.length; i++) {
        let cells = lines[i].split('|').map(s => s.trim()).filter((s, idx, a) => idx > 0 && idx < a.length - 1);
        tbody += '<tr>' + cells.map((c, idx) => `<td style="text-align:${alignments[idx] || 'left'}">${c}</td>`).join('') + '</tr>';
      }
      tbody += '</tbody>';

      return `<div class="table-wrapper"><table>${thead}${tbody}</table></div>`;
    });

    // Step 15: Inline formatting
    text = text.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, (match, alt, src) => {
      return `<img src="${src}" alt="${alt}" loading="lazy" onclick="window.InkedBridge && window.InkedBridge.onImageClick(this.src, this.alt)"/>`;
    });
    text = text.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>');

    text = text.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    text = text.replace(/__([^_]+)__/g, '<strong>$1</strong>');
    text = text.replace(/\*([^*]+)\*/g, '<em>$1</em>');
    text = text.replace(/_([^_]+)_/g, '<em>$1</em>');
    text = text.replace(/~~([^~]+)~~/g, '<del>$1</del>');
    text = text.replace(/---/g, '—');
    text = text.replace(/--/g, '–');
    text = text.replace(/\.\.\./g, '…');
    text = text.replace(/(https?:\/\/[^\s<]+)/g, '<a href="$1">$1</a>');

    // Paragraph wrapping
    let paragraphs = text.split(/\n{2,}/);
    text = paragraphs.map(p => {
      p = p.trim();
      if (!p) return '';
      if (p.startsWith('<h') || p.startsWith('<div') || p.startsWith('<table') ||
          p.startsWith('<blockquote') || p.startsWith('<dl') || p.startsWith('%%') ||
          p.startsWith('<li') || p.startsWith('<hr') || p.startsWith('<ul') || p.startsWith('<ol')) {
        return p;
      }
      return `<p>${p}</p>`;
    }).join('\n\n');

    // Step 16: Restore Display Math (KaTeX)
    text = text.replace(/%%DISPLAYMATH(\d+)%%/g, (match, idx) => {
      return MathEngine.renderBlock(displayMath[idx]);
    });

    // Step 17: Restore Inline Math (KaTeX)
    text = text.replace(/%%INLINEMATH(\d+)%%/g, (match, idx) => {
      return MathEngine.renderInline(inlineMath[idx]);
    });

    // Step 18: Restore Code Blocks & Mermaid
    text = text.replace(/%%CODEBLOCK(\d+)%%/g, (match, idx) => {
      let item = codeBlocks[idx];
      let lang = item.lang.toLowerCase();

      if (lang === 'mermaid' || lang === 'mmd') {
        mermaidCounter++;
        return MermaidEngine.renderPlaceholder(item.code, mermaidCounter);
      }

      let highlighted = HighlightEngine.highlight(item.code, lang);
      let encodedCode = encodeURIComponent(item.code);
      let displayLang = lang ? lang.toUpperCase() : 'CODE';

      return `
        <div class="code-card">
          <div class="code-header">
            <span>${displayLang}</span>
            <button class="copy-btn" onclick="copyCode(this, decodeURIComponent('${encodedCode}'))">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              Copy
            </button>
          </div>
          <pre class="code-body"><code>${highlighted}</code></pre>
        </div>
      `;
    });

    // Step 19: Append Footnotes if present
    if (footnotes.length > 0) {
      let fnHtml = '<div class="footnotes"><hr><ol>';
      footnotes.forEach(fn => {
        fnHtml += `<li id="fn-${fn.num}">${fn.def} <a href="#fnref-${fn.num}">↩</a></li>`;
      });
      fnHtml += '</ol></div>';
      text += fnHtml;
    }

    // Step 20: Send Table of Contents to Android Bridge
    if (window.InkedBridge && window.InkedBridge.onTableOfContentsReady) {
      window.InkedBridge.onTableOfContentsReady(JSON.stringify(toc));
    }

    return text;
  }

  // --- Copy Code Handler ---
  window.copyCode = function (btn, code) {
    if (window.InkedBridge && window.InkedBridge.onCodeCopy) {
      window.InkedBridge.onCodeCopy(code);
    } else if (navigator.clipboard) {
      navigator.clipboard.writeText(code);
    }
    const oldText = btn.innerHTML;
    btn.innerHTML = '✓ Copied';
    setTimeout(() => { btn.innerHTML = oldText; }, 1800);
  };

  // --- Scroll Tracking for Reading Progress ---
  window.addEventListener('scroll', () => {
    if (!scrollTicking) {
      window.requestAnimationFrame(() => {
        let scrollY = window.scrollY || document.documentElement.scrollTop;
        let totalHeight = document.documentElement.scrollHeight - window.innerHeight;
        let progress = totalHeight > 0 ? (scrollY / totalHeight) * 100 : 0;
        let isScrollingDown = scrollY > lastScrollY && scrollY > 40;

        if (window.InkedBridge && window.InkedBridge.onReadingProgress) {
          window.InkedBridge.onReadingProgress(Math.min(100, Math.max(0, progress)), isScrollingDown);
        }

        lastScrollY = scrollY <= 0 ? 0 : scrollY;
        scrollTicking = false;
      });
      scrollTicking = true;
    }
  });

  // --- Public APIs Exposed to Android WebView ---
  window.setMarkdown = function (markdown, coverConfigJson) {
    currentRawMarkdown = markdown || '';

    if (coverConfigJson) {
      try {
        currentCoverConfig = JSON.parse(coverConfigJson);
      } catch (e) {
        currentCoverConfig = null;
      }
    } else {
      currentCoverConfig = null;
    }

    // Automated direction detection
    let dir = detectDirection(currentRawMarkdown);
    document.documentElement.setAttribute('dir', dir);
    document.documentElement.setAttribute('lang', 'en');

    let coverHtml = renderCoverPage(currentCoverConfig);
    let bodyHtml = parseMarkdown(currentRawMarkdown);

    let container = document.getElementById('document-container');
    if (container) {
      container.innerHTML = coverHtml + bodyHtml;
      // Trigger asynchronous Mermaid rendering for all diagrams
      MermaidEngine.renderAllAsync();
    }
  };

  window.setReadingPreferences = function (theme, fontFamily, fontSizePt, lineHeight) {
    document.body.className = '';
    document.body.classList.add(`theme-${theme || 'light'}`);

    let dir = detectDirection(currentRawMarkdown);
    document.documentElement.setAttribute('dir', dir);

    let root = document.documentElement;
    if (fontFamily) {
      root.style.setProperty('--font-family', `"${fontFamily}", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif`);
    }
    if (fontSizePt) {
      root.style.setProperty('--font-size', `${fontSizePt}pt`);
    }
    if (lineHeight) {
      root.style.setProperty('--line-height', `${lineHeight}`);
    }
  };

  window.scrollToHeading = function (id) {
    let el = document.getElementById(id);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

  window.searchInDocument = function (query, forward) {
    if (!query || query.trim().length === 0) {
      window.clearSearch();
      return;
    }

    let container = document.getElementById('document-container');
    if (!container) return;

    let existingMarks = container.querySelectorAll('mark.search-highlight');
    existingMarks.forEach(m => {
      let parent = m.parentNode;
      parent.replaceChild(document.createTextNode(m.textContent), m);
      parent.normalize();
    });

    searchMatches = [];
    currentSearchIndex = -1;

    let walker = document.createTreeWalker(container, NodeFilter.SHOW_TEXT, null, false);
    let node;
    let nodesToReplace = [];

    let lowerQuery = query.toLowerCase();

    while (node = walker.nextNode()) {
      if (node.parentElement && (node.parentElement.tagName === 'SCRIPT' || node.parentElement.tagName === 'STYLE')) {
        continue;
      }
      let idx = node.nodeValue.toLowerCase().indexOf(lowerQuery);
      if (idx !== -1) {
        nodesToReplace.push(node);
      }
    }

    nodesToReplace.forEach(textNode => {
      let val = textNode.nodeValue;
      let lowerVal = val.toLowerCase();
      let parent = textNode.parentNode;
      let frag = document.createDocumentFragment();
      let lastIdx = 0;
      let matchIdx;

      while ((matchIdx = lowerVal.indexOf(lowerQuery, lastIdx)) !== -1) {
        if (matchIdx > lastIdx) {
          frag.appendChild(document.createTextNode(val.substring(lastIdx, matchIdx)));
        }
        let mark = document.createElement('mark');
        mark.className = 'search-highlight';
        mark.textContent = val.substring(matchIdx, matchIdx + query.length);
        frag.appendChild(mark);
        searchMatches.push(mark);
        lastIdx = matchIdx + query.length;
      }

      if (lastIdx < val.length) {
        frag.appendChild(document.createTextNode(val.substring(lastIdx)));
      }

      parent.replaceChild(frag, textNode);
    });

    if (searchMatches.length > 0) {
      if (forward) {
        currentSearchIndex = (currentSearchIndex + 1) % searchMatches.length;
      } else {
        currentSearchIndex = (currentSearchIndex - 1 + searchMatches.length) % searchMatches.length;
      }
      searchMatches.forEach(m => m.classList.remove('active-match'));
      let active = searchMatches[currentSearchIndex];
      active.classList.add('active-match');
      active.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    if (window.InkedBridge && window.InkedBridge.onSearchResults) {
      window.InkedBridge.onSearchResults(searchMatches.length, currentSearchIndex + 1);
    }
  };

  window.clearSearch = function () {
    let container = document.getElementById('document-container');
    if (!container) return;
    let marks = container.querySelectorAll('mark.search-highlight');
    marks.forEach(m => {
      let parent = m.parentNode;
      parent.replaceChild(document.createTextNode(m.textContent), m);
      parent.normalize();
    });
    searchMatches = [];
    currentSearchIndex = -1;
    if (window.InkedBridge && window.InkedBridge.onSearchResults) {
      window.InkedBridge.onSearchResults(0, 0);
    }
  };

  // Signal that engine is ready
  function notifyReady() {
    if (window.InkedBridge && window.InkedBridge.onEngineReady) {
      window.InkedBridge.onEngineReady();
    }
  }

  if (document.readyState === 'complete' || document.readyState === 'interactive') {
    setTimeout(notifyReady, 0);
  } else {
    window.addEventListener('DOMContentLoaded', notifyReady);
  }

})();
