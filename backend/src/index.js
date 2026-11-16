import express from 'express';
import cors from 'cors';
import fs from 'node:fs';
import path from 'node:path';

const PORT = process.env.PORT || 8050;
const BASE_URL = process.env.BASE_URL || '';
const API_KEY = process.env.API_KEY || '';
const DATA_FILE = process.env.DATA_FILE || path.resolve(process.cwd(), 'data', 'pemanggilan.json');

fs.mkdirSync(path.dirname(DATA_FILE), { recursive: true });
if (!fs.existsSync(DATA_FILE)) {
  fs.writeFileSync(DATA_FILE, JSON.stringify({ headers: [], rows: {}, order: [], lastSync: null }, null, 2));
}

function loadData() {
  try { return JSON.parse(fs.readFileSync(DATA_FILE, 'utf8')); }
  catch { return { headers: [], rows: {}, order: [], lastSync: null }; }
}
function saveData(d) { fs.writeFileSync(DATA_FILE, JSON.stringify(d, null, 2)); }

function auth(req, res, next) {
  if (!API_KEY) return next();
  const key = req.header('X-API-Key') || req.query.apiKey;
  if (key !== API_KEY) return res.status(401).json({ error: 'unauthorized' });
  next();
}

const app = express();
app.use(cors());
app.use(express.json({ limit: '10mb' }));

app.get('/health', (req, res) => res.json({ ok: true, service: 'pemanggilan' }));
// also under /pemanggilan? handled by path prefix? cloudflared strips? Actually service receives full path /pemanggilan/...
// We'll support both by mounting routers? Simpler: make all routes work with optional prefix.

function normalizePrefix(req, res, next) { next(); }

// Helper to get data
app.get(['/pemanggilan/health', '/health'], (req, res) => res.json({ ok: true, service: 'pemanggilan' }));

// Meta
function metaHandler(req, res) {
  const d = loadData();
  res.json({ headers: d.headers, count: d.order.length, lastSync: d.lastSync });
}
app.get(['/pemanggilan/api/meta', '/api/meta'], auth, metaHandler);

// Ingest
function ingestHandler(req, res) {
  const body = req.body || {};
  const headers = Array.isArray(body.headers) ? body.headers : [];
  const rows = Array.isArray(body.rows) ? body.rows : [];
  const keyColumn = body.keyColumn || null;
  const d = loadData();
  if (headers.length) d.headers = headers;
  let created = 0, updated = 0;
  const now = new Date().toISOString();
  for (const raw of rows) {
    if (!raw || typeof raw !== 'object') continue;
    let id = null;
    if (keyColumn && raw[keyColumn] != null && String(raw[keyColumn]).trim() !== '') id = String(raw[keyColumn]).trim();
    if (!id) {
      // try common id columns
      for (const c of ['NOMOR UJI', 'ID', 'id', 'Id']) {
        if (raw[c] != null && String(raw[c]).trim() !== '') { id = String(raw[c]).trim(); break; }
      }
    }
    if (!id) {
      // fallback: plate + name
      const plate = raw['NOMOR KENDARAAN'] || raw['plate'] || raw['Plate'] || '';
      id = String(plate).trim() + '|' + (raw['NOMOR WA'] || '');
    }
    if (!id || id === '|') id = 'row-' + Math.random().toString(36).slice(2, 10);
    const existing = d.rows[id];
    const record = {
      id,
      raw,
      status: existing ? existing.status : 'pending',
      calledCount: existing ? (existing.calledCount || 0) : 0,
      lastCalledAt: existing ? existing.lastCalledAt : null,
      doneAt: existing ? existing.doneAt : null,
      createdAt: existing ? existing.createdAt : now,
      updatedAt: now,
    };
    if (existing) updated++; else created++;
    d.rows[id] = record;
    if (!d.order.includes(id)) d.order.push(id);
  }
  d.lastSync = now;
  saveData(d);
  res.json({ ok: true, received: rows.length, created, updated, total: d.order.length });
}
app.post(['/pemanggilan/api/ingest', '/api/ingest'], auth, ingestHandler);

// List vehicles
function listHandler(req, res) {
  const d = loadData();
  const status = req.query.status;
  const ids = (d.order || []).slice().reverse();
  let items = ids.map(id => d.rows[id]).filter(Boolean);
  if (status) items = items.filter(v => v.status === status);
  // attach normalized convenience fields? keep raw; client maps. But provide id.
  res.json({ headers: d.headers, lastSync: d.lastSync, items });
}
app.get(['/pemanggilan/api/vehicles', '/api/vehicles'], auth, listHandler);

function getOneHandler(req, res) {
  const d = loadData();
  const v = d.rows[req.params.id];
  if (!v) return res.status(404).json({ error: 'not found' });
  res.json(v);
}
app.get(['/pemanggilan/api/vehicles/:id', '/api/vehicles/:id'], auth, getOneHandler);

function callHandler(req, res) {
  const d = loadData();
  const v = d.rows[req.params.id];
  if (!v) return res.status(404).json({ error: 'not found' });
  v.status = v.status === 'done' ? 'done' : 'called';
  v.calledCount = (v.calledCount || 0) + 1;
  v.lastCalledAt = new Date().toISOString();
  v.updatedAt = new Date().toISOString();
  saveData(d);
  res.json({ ok: true, vehicle: v });
}
app.post(['/pemanggilan/api/vehicles/:id/call', '/api/vehicles/:id/call'], auth, callHandler);

function doneHandler(req, res) {
  const d = loadData();
  const v = d.rows[req.params.id];
  if (!v) return res.status(404).json({ error: 'not found' });
  v.status = 'done';
  v.doneAt = new Date().toISOString();
  v.updatedAt = new Date().toISOString();
  saveData(d);
  res.json({ ok: true, vehicle: v });
}
app.post(['/pemanggilan/api/vehicles/:id/done', '/api/vehicles/:id/done'], auth, doneHandler);

// reset status (optional) back to pending
function resetHandler(req, res) {
  const d = loadData();
  const v = d.rows[req.params.id];
  if (!v) return res.status(404).json({ error: 'not found' });
  v.status = 'pending';
  v.updatedAt = new Date().toISOString();
  saveData(d);
  res.json({ ok: true, vehicle: v });
}
app.post(['/pemanggilan/api/vehicles/:id/reset', '/api/vehicles/:id/reset'], auth, resetHandler);

// Clear all (for testing)
app.post(['/pemanggilan/api/clear', '/api/clear'], auth, (req, res) => {
  saveData({ headers: [], rows: {}, order: [], lastSync: null });
  res.json({ ok: true });
});

app.listen(PORT, () => console.log(`pemanggilan backend listening on :${PORT}`));