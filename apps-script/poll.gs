// Paste this into Apps Script (Extensions > Apps Script) bound to your Google Sheet.
// Set triggers: onEdit (installable) and time-driven every 1 minute.

const CONFIG = {
  BACKEND_URL: 'https://ann.prnmjpngst.pw',
  API_KEY: 'panggilan-dev-key-2026-change-me',
  SHEET_NAME: 'IDENTIFIKASI',
  KEY_COLUMN: 'NOMOR UJI',
};

function poll() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sheet = ss.getSheetByName(CONFIG.SHEET_NAME) || ss.getActiveSheet();
  const values = sheet.getDataRange().getValues();
  if (values.length < 2) return;
  const headers = values[0].map(h => String(h).trim());
  const rows = values.slice(1).map(r => {
    const obj = {};
    headers.forEach((h, i) => { obj[h] = r[i]; });
    return obj;
  }).filter(r => Object.values(r).some(v => v !== '' && v != null));

  const payload = JSON.stringify({ headers: headers, rows: rows, keyColumn: CONFIG.KEY_COLUMN });
  const res = UrlFetchApp.fetch(CONFIG.BACKEND_URL + '/api/ingest', {
    method: 'post',
    contentType: 'application/json',
    headers: { 'X-API-Key': CONFIG.API_KEY, 'User-Agent': 'Mozilla/5.0 (compatible; Google-Apps-Script)' },
    payload: payload,
    muteHttpExceptions: true,
  });
  Logger.log(res.getResponseCode() + ' ' + res.getContentText());
}

function onEdit(e) { poll(); }