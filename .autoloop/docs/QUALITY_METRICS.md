# Quality Metrics — stoic-journal

Seeded 2026-07-05T12:28:29. Owned by the Tester. Metrics tracked per iteration; source of truth = `.autoloop/runs/NNNN/gates_*.json` + baseline.json.

| Metric | Source | Baseline | Latest | Direction |
|---|---|---|---|---|
| Required gates passing | gates | TBD | TBD | must stay ✅ |
| Gate wall-time (s) | gates total_seconds | TBD | TBD | ↓ better, ≤ +25% tolerated |
| Test count | test gate output | TBD | TBD | ↑ better |
| Lint findings | lint gate | TBD | TBD | ↓ better |

Update the Baseline column only when `keep --promote-baseline` is used.
