/*
 * Copyright 2026 Enrico Olivelli
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
"use strict";

const state = {
    source: "cassandra",
    selection: null, // {type:'table',keyspace,table,view} | {type:'index',index,view}
    expanded: new Set(),
    rows: [],          // last loaded cassandra rows (for the detail drawer)
    primaryKey: [],    // primary-key columns of the current table
};

const el = (id) => document.getElementById(id);

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".tab").forEach((tab) => {
        tab.addEventListener("click", () => switchSource(tab.dataset.source));
    });
    el("refresh-btn").addEventListener("click", loadTree);
    el("limit").addEventListener("change", () => render());
    el("copy-btn").addEventListener("click", () => copyText(el("request-text").textContent, el("copy-btn")));
    el("drawer-close").addEventListener("click", closeDrawer);
    el("drawer-overlay").addEventListener("click", (e) => {
        if (e.target === el("drawer-overlay")) closeDrawer();
    });
    document.addEventListener("keydown", (e) => { if (e.key === "Escape") closeDrawer(); });
    loadTree();
});

function switchSource(source) {
    if (source === state.source) return;
    state.source = source;
    state.selection = null;
    state.expanded.clear();
    document.querySelectorAll(".tab").forEach((t) =>
        t.classList.toggle("active", t.dataset.source === source));
    el("sidebar-title").textContent = source === "cassandra" ? "Keyspaces" : "Indices";
    el("breadcrumb").textContent = "Select an item on the left to begin.";
    hide("request-panel");
    hide("controls");
    hide("view-switch");
    hide("message");
    el("result").innerHTML = "";
    loadTree();
}

async function api(path) {
    const res = await fetch(path);
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.message || `HTTP ${res.status}`);
    return data;
}

// ---------- Sidebar tree ----------

async function loadTree() {
    const tree = el("tree");
    tree.innerHTML = `<div class="spinner">Loading…</div>`;
    try {
        if (state.source === "cassandra") {
            const result = await api("/api/cassandra/keyspaces");
            renderKeyspaces(result.rows.map((r) => r.keyspace_name).sort());
        } else {
            const result = await api("/api/opensearch/indices");
            renderIndices(result.indices);
        }
    } catch (e) {
        tree.innerHTML = `<div class="spinner" style="color:var(--danger)">${escapeHtml(e.message)}</div>`;
    }
}

function renderKeyspaces(keyspaces) {
    const tree = el("tree");
    tree.innerHTML = "";
    keyspaces.forEach((ks) => {
        const node = makeNode(ks, "keyspace");
        const twisty = document.createElement("span");
        twisty.className = "twisty";
        twisty.textContent = state.expanded.has(ks) ? "▾" : "▸";
        node.prepend(twisty);
        node.addEventListener("click", () => toggleKeyspace(ks));
        tree.appendChild(node);
        if (state.expanded.has(ks)) renderTablesPlaceholder(ks);
    });
}

async function toggleKeyspace(ks) {
    if (state.expanded.has(ks)) {
        state.expanded.delete(ks);
        renderKeyspaces(currentKeyspaces());
        return;
    }
    state.expanded.add(ks);
    renderKeyspaces(currentKeyspaces());
    renderTablesPlaceholder(ks);
    try {
        const result = await api(`/api/cassandra/keyspaces/${encodeURIComponent(ks)}/tables`);
        renderTables(ks, result.rows.map((r) => r.table_name).sort());
    } catch (e) {
        showMessage(e.message, "error");
    }
}

function currentKeyspaces() {
    return Array.from(el("tree").querySelectorAll(".tree-node.keyspace")).map((n) => n.dataset.name);
}

function renderTablesPlaceholder(ks) {
    const anchor = keyspaceNode(ks);
    if (anchor && !anchor.nextSibling?.classList?.contains("child")) {
        const loading = document.createElement("div");
        loading.className = "tree-node child";
        loading.dataset.parent = ks;
        loading.textContent = "loading…";
        anchor.after(loading);
    }
}

function renderTables(ks, tables) {
    el("tree").querySelectorAll(`.tree-node.child[data-parent="${cssEscape(ks)}"]`).forEach((n) => n.remove());
    const anchor = keyspaceNode(ks);
    if (!anchor) return;
    let prev = anchor;
    if (tables.length === 0) {
        const empty = makeChild(ks, "(no tables)");
        empty.style.fontStyle = "italic";
        prev.after(empty);
        return;
    }
    tables.forEach((t) => {
        const child = makeChild(ks, t);
        child.addEventListener("click", (ev) => { ev.stopPropagation(); selectTable(ks, t); });
        prev.after(child);
        prev = child;
    });
}

function renderIndices(indices) {
    const tree = el("tree");
    tree.innerHTML = "";
    if (!indices || indices.length === 0) {
        tree.innerHTML = `<div class="spinner">No indices found.</div>`;
        return;
    }
    indices.forEach((idx) => {
        const node = makeNode(idx.index, "keyspace");
        const badge = document.createElement("span");
        badge.className = "badge";
        badge.textContent = (idx["docs.count"] ?? "0") + " docs";
        node.appendChild(badge);
        node.addEventListener("click", () => selectIndex(idx.index));
        tree.appendChild(node);
    });
}

function makeNode(name, cls) {
    const node = document.createElement("div");
    node.className = `tree-node ${cls}`;
    node.dataset.name = name;
    const label = document.createElement("span");
    label.textContent = name;
    node.appendChild(label);
    return node;
}

function makeChild(parent, name) {
    const node = document.createElement("div");
    node.className = "tree-node child";
    node.dataset.parent = parent;
    node.dataset.name = name;
    node.textContent = name;
    return node;
}

function keyspaceNode(ks) {
    return el("tree").querySelector(`.tree-node.keyspace[data-name="${cssEscape(ks)}"]`);
}

function markSelected(predicate) {
    document.querySelectorAll(".tree-node").forEach((n) => n.classList.toggle("selected", predicate(n)));
}

// ---------- Selection & views ----------

function selectTable(ks, table) {
    state.selection = { type: "table", keyspace: ks, table, view: "rows" };
    markSelected((n) => n.classList.contains("child") && n.dataset.parent === ks && n.dataset.name === table);
    render();
}

function selectIndex(index) {
    state.selection = { type: "index", index, view: "documents" };
    markSelected((n) => n.dataset.name === index);
    render();
}

function renderViewSwitch() {
    const sel = state.selection;
    const views = sel.type === "table"
        ? [["rows", "Rows"], ["schema", "Schema"]]
        : [["documents", "Documents"], ["mapping", "Mapping"]];
    const sw = el("view-switch");
    sw.innerHTML = "";
    views.forEach(([key, label]) => {
        const b = document.createElement("button");
        b.textContent = label;
        b.classList.toggle("active", sel.view === key);
        b.addEventListener("click", () => { sel.view = key; render(); });
        sw.appendChild(b);
    });
    show("view-switch");
}

async function render() {
    const sel = state.selection;
    if (!sel) return;
    renderViewSwitch();
    hide("message");
    hide("request-panel");
    hide("controls");
    el("result").innerHTML = `<div class="spinner">Loading…</div>`;
    el("result-meta").textContent = "";
    try {
        if (sel.type === "table" && sel.view === "rows") await viewTableRows(sel);
        else if (sel.type === "table" && sel.view === "schema") await viewTableSchema(sel);
        else if (sel.type === "index" && sel.view === "documents") await viewIndexDocuments(sel);
        else if (sel.type === "index" && sel.view === "mapping") await viewIndexMapping(sel);
    } catch (e) {
        el("result").innerHTML = "";
        showMessage(e.message, "error");
    }
}

async function viewTableRows(sel) {
    el("breadcrumb").innerHTML = `Cassandra › <b>${escapeHtml(sel.keyspace)}.${escapeHtml(sel.table)}</b>`;
    show("controls");
    const limit = el("limit").value;
    const r = await api(`/api/cassandra/keyspaces/${encodeURIComponent(sel.keyspace)}`
        + `/tables/${encodeURIComponent(sel.table)}/rows?limit=${limit}`);
    showRequest("CQL", r.cql);
    state.rows = r.rows;
    state.primaryKey = r.primaryKey || [];
    el("result-meta").textContent = `${r.rowCount} row(s) · click a row for detail`;
    renderTable(r.columns, r.rows, (i) => openCassandraRow(sel, i));
}

async function viewTableSchema(sel) {
    el("breadcrumb").innerHTML = `Cassandra › <b>${escapeHtml(sel.keyspace)}.${escapeHtml(sel.table)}</b> › schema`;
    const s = await api(`/api/cassandra/keyspaces/${encodeURIComponent(sel.keyspace)}`
        + `/tables/${encodeURIComponent(sel.table)}/schema`);
    renderCassandraSchema(s);
}

async function viewIndexDocuments(sel) {
    el("breadcrumb").innerHTML = `OpenSearch › <b>${escapeHtml(sel.index)}</b>`;
    show("controls");
    const size = el("limit").value;
    const r = await api(`/api/opensearch/indices/${encodeURIComponent(sel.index)}/documents?size=${size}`);
    showRequest("REST API", formatOsRequest(r.request));
    el("result-meta").textContent = `${r.documents.length} of ${r.total} document(s) · click a row for detail`;
    renderTable(r.columns, r.documents, (i) => openOsDocument(sel, r.documents[i]._id));
}

async function viewIndexMapping(sel) {
    el("breadcrumb").innerHTML = `OpenSearch › <b>${escapeHtml(sel.index)}</b> › mapping`;
    const s = await api(`/api/opensearch/indices/${encodeURIComponent(sel.index)}/schema`);
    showRequest("REST API", formatOsRequest(s.request));
    let html = `<div class="section-title">Mappings</div>`;
    html += `<pre class="code-block json">${escapeHtml(JSON.stringify(s.mappings, null, 2))}</pre>`;
    html += `<div class="section-title">Settings</div>`;
    html += `<pre class="code-block json">${escapeHtml(JSON.stringify(s.settings, null, 2))}</pre>`;
    el("result").innerHTML = html;
}

// ---------- Schema rendering ----------

function renderCassandraSchema(s) {
    let html = "";

    html += `<div class="section-title">Columns</div>`;
    html += `<table><thead><tr><th>Column</th><th>Type</th><th>Kind</th></tr></thead><tbody>`;
    s.columns.forEach((c) => {
        html += `<tr><td>${escapeHtml(c.name)}</td><td>${escapeHtml(c.type)}</td><td>${kindPill(c)}</td></tr>`;
    });
    html += `</tbody></table>`;

    html += `<div class="section-title">Indexes</div>`;
    if (s.indexes.length === 0) {
        html += `<div class="message info">No indexes.</div>`;
    } else {
        html += `<table><thead><tr><th>Name</th><th>Kind</th><th>Target</th><th>Class</th></tr></thead><tbody>`;
        s.indexes.forEach((i) => {
            const kind = i.kind === "SAI" ? `<span class="pill sai">SAI</span>` : escapeHtml(i.kind);
            html += `<tr><td>${escapeHtml(i.name)}</td><td>${kind}</td><td>${escapeHtml(i.target)}</td>`
                + `<td>${escapeHtml(i.className || "—")}</td></tr>`;
        });
        html += `</tbody></table>`;
    }

    if (s.types && s.types.length) {
        html += `<div class="section-title">User-defined types</div>`;
        s.types.forEach((t) => {
            html += `<pre class="code-block">${escapeHtml(t.createStatement)}</pre>`;
        });
    }

    html += `<div class="section-title">CREATE statement</div>`;
    html += `<pre class="code-block">${escapeHtml(s.createStatement)}</pre>`;

    el("result").innerHTML = html;
}

function kindPill(c) {
    if (c.kind === "PARTITION_KEY") return `<span class="pill pk">partition key</span>`;
    if (c.kind === "CLUSTERING") return `<span class="pill ck">clustering ${escapeHtml(c.clusteringOrder || "")}</span>`;
    if (c.kind === "STATIC") return `<span class="pill">static</span>`;
    return `<span class="pill">regular</span>`;
}

// ---------- Detail drawer ----------

function openCassandraRow(sel, index) {
    const row = state.rows[index];
    if (!row) return;
    const cql = cqlForRow(sel.keyspace, sel.table, state.primaryKey, row);
    let body = "";
    if (cql) {
        body += `<div class="section-title">CQL — fetch this row</div>`;
        body += `<pre class="code-block">${escapeHtml(cql)}</pre>`;
    }
    body += `<div class="section-title">Columns</div>`;
    body += kvGrid(row);
    openDrawer(`${sel.keyspace}.${sel.table}`, body);
}

async function openOsDocument(sel, id) {
    openDrawer(`${sel.index} / ${id}`, `<div class="spinner">Loading…</div>`);
    try {
        const d = await api(`/api/opensearch/indices/${encodeURIComponent(sel.index)}`
            + `/documents/${encodeURIComponent(id)}`);
        let body = `<div class="section-title">REST API</div>`;
        body += `<pre class="code-block">${escapeHtml(formatOsRequest(d.request))}</pre>`;
        body += `<div class="section-title">_source</div>`;
        body += `<pre class="code-block json">${escapeHtml(JSON.stringify(d.source, null, 2))}</pre>`;
        el("drawer-body").innerHTML = body;
    } catch (e) {
        el("drawer-body").innerHTML = `<div class="message error">${escapeHtml(e.message)}</div>`;
    }
}

function kvGrid(obj) {
    let html = `<div class="kv">`;
    Object.keys(obj).forEach((k) => {
        const v = obj[k];
        if (v === null || v === undefined) {
            html += `<div class="k">${escapeHtml(k)}</div><div class="v null">null</div>`;
        } else {
            const text = typeof v === "object" ? JSON.stringify(v, null, 2) : String(v);
            html += `<div class="k">${escapeHtml(k)}</div><div class="v">${escapeHtml(text)}</div>`;
        }
    });
    html += `</div>`;
    return html;
}

function openDrawer(title, bodyHtml) {
    el("drawer-title").textContent = title;
    el("drawer-body").innerHTML = bodyHtml;
    show("drawer-overlay");
}

function closeDrawer() {
    hide("drawer-overlay");
}

// ---------- Table rendering ----------

function renderTable(columns, rows, onRowClick) {
    const result = el("result");
    if (!rows || rows.length === 0) {
        result.innerHTML = `<div class="message info">No rows to display.</div>`;
        return;
    }
    const cols = columns && columns.length ? columns : Object.keys(rows[0]);
    const thead = `<thead><tr>${cols.map((c) => `<th>${escapeHtml(c)}</th>`).join("")}</tr></thead>`;
    const body = rows.map((row) =>
        `<tr class="clickable">${cols.map((c) => formatCell(row[c])).join("")}</tr>`).join("");
    result.innerHTML = `<table>${thead}<tbody>${body}</tbody></table>`;
    if (onRowClick) {
        Array.from(result.querySelectorAll("tbody tr")).forEach((tr, i) =>
            tr.addEventListener("click", () => onRowClick(i)));
    }
}

function formatCell(value) {
    if (value === null || value === undefined) return `<td class="null">null</td>`;
    if (typeof value === "number") return `<td class="num">${value}</td>`;
    const str = typeof value === "object" ? JSON.stringify(value) : String(value);
    return `<td title="${escapeHtml(str)}">${escapeHtml(str)}</td>`;
}

// ---------- Helpers ----------

function showRequest(label, text) {
    el("request-label").textContent = label;
    el("request-text").textContent = text;
    show("request-panel");
}

function formatOsRequest(req) {
    let s = `${req.method} ${req.path}`;
    if (req.body) s += `\n${req.body}`;
    return s;
}

function cqlForRow(ks, table, pk, row) {
    if (!pk || !pk.length) return null;
    const where = pk.map((c) => `"${c}" = ${cqlLiteral(row[c])}`).join(" AND ");
    return `SELECT * FROM "${ks}"."${table}"\nWHERE ${where};`;
}

function cqlLiteral(v) {
    if (v === null || v === undefined) return "null";
    if (typeof v === "number" || typeof v === "boolean") return String(v);
    const s = String(v);
    if (/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/.test(s)) return s;
    if (/^0x[0-9a-fA-F]+$/.test(s)) return s;
    return "'" + s.replace(/'/g, "''") + "'";
}

function copyText(text, btn) {
    navigator.clipboard.writeText(text).then(() => {
        const old = btn.textContent;
        btn.textContent = "Copied!";
        setTimeout(() => (btn.textContent = old), 1200);
    });
}

function showMessage(msg, kind) {
    const m = el("message");
    m.textContent = msg;
    m.className = `message ${kind || "info"}`;
    show("message");
}

function show(id) { el(id).classList.remove("hidden"); }
function hide(id) { el(id).classList.add("hidden"); }

function escapeHtml(s) {
    return String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;")
        .replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function cssEscape(s) {
    return String(s).replace(/["\\]/g, "\\$&");
}
