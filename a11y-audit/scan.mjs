#!/usr/bin/env node
// WCAG accessibility scanner: Playwright + axe-core -> HTML/PDF/JSON report.
//
// Usage:
//   node scan.mjs <url> [url ...] [--out reports/client] [--client "Client name"] [--manual findings.json]
//
// --manual: optional JSON array of findings from manual testing:
//   [{ "title", "impact", "wcag", "description", "fix" }]
//
// Env:
//   CHROME_PATH  optional path to a Chromium/Chrome binary
import { chromium } from 'playwright';
import { AxeBuilder } from '@axe-core/playwright';
import fs from 'node:fs/promises';
import path from 'node:path';

const WCAG_TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'];
const IMPACT_ORDER = ['critical', 'serious', 'moderate', 'minor'];
const IMPACT_WEIGHT = { critical: 10, serious: 5, moderate: 2, minor: 1 };

function parseArgs(argv) {
  const args = { urls: [], out: 'reports/audit', client: 'Website', manual: null };
  for (let i = 0; i < argv.length; i++) {
    if (argv[i] === '--out') args.out = argv[++i];
    else if (argv[i] === '--manual') args.manual = argv[++i];
    else if (argv[i] === '--client') args.client = argv[++i];
    else args.urls.push(argv[i]);
  }
  if (!args.urls.length) {
    console.error('Usage: node scan.mjs <url> [url ...] [--out dir] [--client name]');
    process.exit(1);
  }
  return args;
}

const esc = (s = '') =>
  String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

/** wcag143 -> 1.4.3 ; wcag2aa -> null */
function criteria(tags) {
  return tags
    .map((t) => /^wcag(\d)(\d)(\d+)$/.exec(t))
    .filter(Boolean)
    .map((m) => `${m[1]}.${m[2]}.${m[3]}`);
}

function level(tags) {
  if (tags.some((t) => /^wcag2(1|2)?aa$/.test(t))) return 'AA';
  if (tags.some((t) => /^wcag2(1|2)?a$/.test(t))) return 'A';
  return '';
}

function score(violations) {
  const penalty = violations.reduce(
    (sum, v) => sum + (IMPACT_WEIGHT[v.impact] ?? 1) * Math.min(v.nodes.length, 5),
    0,
  );
  return Math.max(0, 100 - penalty);
}

async function scanPage(browser, input, outDir, index) {
  let url = input;
  const context = await browser.newContext({ viewport: { width: 1280, height: 900 } });
  const page = await context.newPage();
  if (!/^(https?|file):\/\//.test(url)) url = 'file://' + path.resolve(url); // allow local files
  await page.goto(url, { waitUntil: 'networkidle', timeout: 60_000 });
  const shot = `page-${index + 1}.png`;
  await page.screenshot({ path: path.join(outDir, shot), fullPage: false });
  const result = await new AxeBuilder({ page }).withTags(WCAG_TAGS).analyze();
  const title = await page.title();
  await context.close();
  return { url: input, title, shot, result };
}

function renderReport({ client, pages, date, manual = [] }) {
  const allViolations = pages.flatMap((p) => p.result.violations);
  const totals = Object.fromEntries(IMPACT_ORDER.map((i) => [i, 0]));
  for (const v of allViolations) totals[v.impact] = (totals[v.impact] ?? 0) + v.nodes.length;
  for (const m of manual) totals[m.impact] = (totals[m.impact] ?? 0) + 1;
  const issueCount = Object.values(totals).reduce((a, b) => a + b, 0);
  const avgScore = Math.round(pages.reduce((s, p) => s + score(p.result.violations), 0) / pages.length);

  const pageSections = pages
    .map((p) => {
      const violations = [...p.result.violations].sort(
        (a, b) => IMPACT_ORDER.indexOf(a.impact) - IMPACT_ORDER.indexOf(b.impact),
      );
      const rows = violations
        .map(
          (v) => `
        <article class="issue ${v.impact}">
          <header>
            <span class="badge ${v.impact}">${esc(v.impact)}</span>
            <h4>${esc(v.help)}</h4>
          </header>
          <p>${esc(v.description)}</p>
          <p class="meta">
            WCAG ${criteria(v.tags).map(esc).join(', ') || '—'} ${level(v.tags) ? `(Level ${level(v.tags)})` : ''}
            · Rule <code>${esc(v.id)}</code> · ${v.nodes.length} element(s)
            · <a href="${esc(v.helpUrl)}">How to fix</a>
          </p>
          <details>
            <summary>Affected elements</summary>
            ${v.nodes
              .slice(0, 10)
              .map(
                (n) => `<div class="node"><code>${esc(n.target.join(' '))}</code>
                <pre>${esc(n.html)}</pre>
                <p class="fix">${esc(n.failureSummary).replace(/\n/g, '<br>')}</p></div>`,
              )
              .join('')}
            ${v.nodes.length > 10 ? `<p>…and ${v.nodes.length - 10} more.</p>` : ''}
          </details>
        </article>`,
        )
        .join('');
      return `
      <section class="page">
        <h3>${esc(p.title || p.url)}</h3>
        <p class="meta">${esc(p.url)} · Score ${score(p.result.violations)}/100 ·
          ${p.result.violations.length} failed rules · ${p.result.passes.length} passed rules ·
          ${p.result.incomplete.length} need manual review</p>
        <img class="shot" src="${esc(p.shot)}" alt="Screenshot of ${esc(p.title || p.url)}">
        ${rows || '<p class="ok">No automated violations found on this page.</p>'}
      </section>`;
    })
    .join('');

  return `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Accessibility Audit — ${esc(client)}</title>
<style>
  body { font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; color:#1f2937; margin:0; line-height:1.5; }
  main { max-width: 960px; margin: 0 auto; padding: 40px 24px; }
  h1 { margin: 0 0 4px; } h2 { margin-top: 40px; border-bottom: 2px solid #e5e7eb; padding-bottom: 6px; }
  .meta { color:#4b5563; font-size: 14px; }
  .cards { display:grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 12px; margin: 24px 0; }
  .card { border:1px solid #e5e7eb; border-radius: 10px; padding: 14px; }
  .card b { display:block; font-size: 28px; }
  .badge { display:inline-block; padding: 2px 10px; border-radius: 999px; font-size: 12px; font-weight: 700; text-transform: uppercase; color:#fff; }
  .badge.critical { background:#991b1b; } .badge.serious { background:#c2410c; }
  .badge.moderate { background:#a16207; } .badge.minor { background:#374151; }
  .issue { border:1px solid #e5e7eb; border-left-width: 6px; border-radius: 8px; padding: 12px 16px; margin: 14px 0; break-inside: avoid; }
  .issue.critical { border-left-color:#991b1b; } .issue.serious { border-left-color:#c2410c; }
  .issue.moderate { border-left-color:#a16207; } .issue.minor { border-left-color:#374151; }
  .issue header { display:flex; gap: 10px; align-items:center; } .issue h4 { margin: 0; }
  pre { background:#f3f4f6; padding: 8px; border-radius: 6px; white-space: pre-wrap; word-break: break-all; font-size: 12px; }
  .node { margin: 10px 0; } .fix { font-size: 13px; color:#374151; }
  .shot { width: 100%; border:1px solid #e5e7eb; border-radius: 8px; margin: 8px 0 16px; }
  .ok { color:#166534; font-weight: 600; }
  .note { background:#f9fafb; border:1px solid #e5e7eb; border-radius: 8px; padding: 12px 16px; font-size: 14px; }
  a { color:#1d4ed8; }
</style>
</head>
<body>
<main>
  <h1>Accessibility Audit Report</h1>
  <p class="meta">${esc(client)} · ${esc(date)} · Standard: WCAG 2.2 Level AA (EN 301 549 / European Accessibility Act)</p>

  <h2>Executive summary</h2>
  <div class="cards">
    <div class="card"><span>Score</span><b>${avgScore}/100</b></div>
    <div class="card"><span>Pages tested</span><b>${pages.length}</b></div>
    <div class="card"><span>Issues found</span><b>${issueCount}</b></div>
    ${IMPACT_ORDER.map((i) => `<div class="card"><span class="badge ${i}">${i}</span><b>${totals[i] ?? 0}</b></div>`).join('')}
  </div>
  <p>Issues are ordered by impact. <b>Critical</b> and <b>serious</b> issues block or seriously hinder users of screen readers,
  keyboards or low-vision settings and should be fixed first.</p>

  <h2>Findings by page</h2>
  ${pageSections}

  ${manual.length ? `<h2>Manual review findings</h2>
  <p>Barriers found by keyboard-only and screen reader testing that automated tools cannot detect.</p>
  ${manual.map((m) => `
    <article class="issue ${esc(m.impact)}">
      <header><span class="badge ${esc(m.impact)}">${esc(m.impact)}</span><h4>${esc(m.title)}</h4></header>
      <p>${esc(m.description)}</p>
      <p class="meta">WCAG ${esc(m.wcag)} · Manual test</p>
      <p class="fix"><b>Fix:</b> ${esc(m.fix)}</p>
    </article>`).join('')}` : ''}

  <h2>Methodology & limitations</h2>
  <div class="note">
    Automated testing with axe-core ${esc(pages[0]?.result.testEngine?.version ?? '')} in Chromium against WCAG 2.0/2.1/2.2 A and AA rules.
    Automated tools detect only part of all accessibility barriers; items flagged as "need manual review" and checks such as
    keyboard navigation order, focus visibility, screen reader announcements and content meaning require manual testing.
    This report is a technical assessment, not a legal certification of compliance.
  </div>
</main>
</body>
</html>`;
}

async function main() {
  const { urls, out, client, manual: manualPath } = parseArgs(process.argv.slice(2));
  const manual = manualPath ? JSON.parse(await fs.readFile(manualPath, 'utf8')) : [];
  await fs.mkdir(out, { recursive: true });

  const browser = await chromium.launch(process.env.CHROME_PATH ? { executablePath: process.env.CHROME_PATH } : {});
  try {
    const pages = [];
    for (const [i, url] of urls.entries()) {
      console.log(`Scanning ${url} ...`);
      pages.push(await scanPage(browser, url, out, i));
    }

    const date = new Date().toISOString().slice(0, 10);
    const html = renderReport({ client, pages, date, manual });
    const htmlPath = path.join(out, 'report.html');
    await fs.writeFile(htmlPath, html);
    await fs.writeFile(
      path.join(out, 'results.json'),
      JSON.stringify(pages.map((p) => ({ url: p.url, title: p.title, violations: p.result.violations })), null, 2),
    );

    const pdfPage = await browser.newPage();
    await pdfPage.goto('file://' + path.resolve(htmlPath), { waitUntil: 'load' });
    await pdfPage.evaluate(() => document.querySelectorAll('details').forEach((d) => (d.open = true)));
    await pdfPage.pdf({ path: path.join(out, 'report.pdf'), format: 'A4', printBackground: true,
      margin: { top: '16mm', bottom: '16mm', left: '12mm', right: '12mm' } });
    await pdfPage.close();

    for (const p of pages) {
      console.log(`${p.url}: score ${score(p.result.violations)}/100, ${p.result.violations.length} failed rules`);
    }
    console.log(`Report: ${htmlPath}`);
  } finally {
    await browser.close();
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
