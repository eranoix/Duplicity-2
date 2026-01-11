#!/usr/bin/env node
/* eslint-disable no-console */
const http = require("node:http");
const fs = require("node:fs");
const path = require("node:path");
const { spawn } = require("node:child_process");

const PUBLIC_BASE = "/oni-duplicity/";

function logPath(appHome) {
  return path.join(appHome, "duplicity.log");
}

function appendLog(appHome, line) {
  try {
    fs.appendFileSync(logPath(appHome), `[${new Date().toISOString()}] ${line}\n`);
  } catch (_) {}
}

const MIME = {
  ".html": "text/html; charset=utf-8",
  ".js": "application/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".map": "application/json; charset=utf-8",
  ".png": "image/png",
  ".svg": "image/svg+xml",
  ".woff": "font/woff",
  ".woff2": "font/woff2",
  ".ttf": "font/ttf",
  ".eot": "application/vnd.ms-fontobject",
  ".txt": "text/plain; charset=utf-8",
};

function openBrowser(url) {
  const plat = process.platform;
  if (plat === "win32") {
    // "start" is a cmd builtin.
    spawn("cmd", ["/c", "start", "", url], { detached: true, stdio: "ignore" }).unref();
    return;
  }
  if (plat === "darwin") {
    spawn("open", [url], { detached: true, stdio: "ignore" }).unref();
    return;
  }
  spawn("xdg-open", [url], { detached: true, stdio: "ignore" }).unref();
}

function safeJoin(base, rel) {
  const full = path.normalize(path.join(base, rel));
  if (!full.startsWith(base)) return null;
  return full;
}

function serveFile(res, filePath) {
  const ext = path.extname(filePath).toLowerCase();
  res.statusCode = 200;
  res.setHeader("Content-Type", MIME[ext] || "application/octet-stream");
  fs.createReadStream(filePath).pipe(res);
}

function main() {
  // IMPORTANT:
  // When packed with pkg, __dirname points inside the virtual snapshot.
  // We need the real folder where the .exe is located.
  const appHome = process.pkg ? path.dirname(process.execPath) : path.resolve(__dirname);
  const distDir = path.join(appHome, "dist");
  if (!fs.existsSync(distDir)) {
    const msg = `ERRO: não encontrei a pasta dist ao lado do launcher: ${distDir}`;
    console.error(msg);
    appendLog(appHome, msg);
    process.exit(2);
  }

  appendLog(appHome, `appHome=${appHome}`);
  appendLog(appHome, `distDir=${distDir}`);

  const server = http.createServer((req, res) => {
    try {
      const u = new URL(req.url || "/", "http://127.0.0.1");
      let p = u.pathname || "/";

      if (p === "/") {
        res.statusCode = 302;
        res.setHeader("Location", PUBLIC_BASE);
        return res.end();
      }

      if (!p.startsWith(PUBLIC_BASE)) {
        res.statusCode = 404;
        return res.end("Not Found");
      }

      let rel = p.substring(PUBLIC_BASE.length);
      if (rel === "" || rel.endsWith("/")) rel = rel + "index.html";

      const filePath = safeJoin(distDir, rel);
      if (!filePath || !fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
        // SPA fallback
        const indexPath = path.join(distDir, "index.html");
        if (fs.existsSync(indexPath)) return serveFile(res, indexPath);
        res.statusCode = 404;
        return res.end("Not Found");
      }

      return serveFile(res, filePath);
    } catch (e) {
      appendLog(appHome, `server error: ${e && e.stack ? e.stack : String(e)}`);
      res.statusCode = 500;
      return res.end("Server Error");
    }
  });

  server.on("error", (e) => {
    const msg = `server listen error: ${e && e.stack ? e.stack : String(e)}`;
    console.error(msg);
    appendLog(appHome, msg);
    process.exit(2);
  });

  server.listen(0, "127.0.0.1", () => {
    const addr = server.address();
    const port = addr && typeof addr === "object" ? addr.port : 0;
    const url = `http://127.0.0.1:${port}${PUBLIC_BASE}`;
    console.log("Duplicity UI em:", url);
    appendLog(appHome, `url=${url}`);
    openBrowser(url);
  });
}

main();
