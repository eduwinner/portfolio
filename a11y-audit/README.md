# Accessibility Audit — WCAG 2.2 AA scanner + sample report

A command-line scanner that audits web pages against **WCAG 2.0/2.1/2.2 Level A and AA** (the standard behind EN 301 549 and the **European Accessibility Act**) and produces a client-ready **HTML + PDF report**, plus raw JSON.

Built with **Playwright** (real Chromium rendering) and **axe-core**, with support for adding **manual-test findings** (keyboard and screen reader checks that automated tools cannot catch).

## Sample: before → after

A fictional coffee e-commerce page ("Nordic Brew") with typical real-world issues was audited, fixed and re-audited:

| | Automated score | Failed rules | Issues (automated + manual) |
|---|---|---|---|
| [Before](demo-site/before/index.html) | **28/100** | 5 | 23 |
| [After](demo-site/after/index.html) | **100/100** | 0 | — |

Reports: [before (PDF)](reports/demo-before/report.pdf) · [before (HTML)](reports/demo-before/report.html) · [after (PDF)](reports/demo-after/report.pdf)

**Found and fixed** — missing image alt text, icon-only cart button with no name, low color contrast (11 elements), missing page language, zoom disabled in viewport, `<div>` used as a button (not keyboard accessible), focus outline removed, placeholder-only form fields, no landmarks/H1/skip link, vague "here" and "+" controls, generic page title.

## Usage

```bash
npm install
npx playwright install chromium

# one or more pages of a client site
node scan.mjs https://example.com https://example.com/checkout \
  --client "Example Ltd" --out reports/example

# add findings from manual keyboard / screen reader testing
node scan.mjs https://example.com --client "Example Ltd" --out reports/example \
  --manual manual-findings.json

# reproduce the demo
npm run demo
```

Output in `--out`: `report.html`, `report.pdf`, `results.json`, page screenshots.

## Report contents

- Executive summary: score, pages tested, issue counts by impact (critical / serious / moderate / minor)
- Per page: screenshot, failed rules ordered by impact, WCAG success criteria and level, affected elements (selector + HTML), how to fix
- Manual review findings (optional)
- Methodology and limitations — automated testing covers only part of WCAG; the report is a technical assessment, not a legal certification

---
Built by **Eduardo — Winner Tecnologia**. Available for accessibility audits and remediation (WCAG 2.2 AA / EAA).
