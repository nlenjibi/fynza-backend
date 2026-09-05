# GitHub Branch Protection Rules

Configure these rules under **Settings → Branches → Add rule**.

---

## `develop` branch

| Setting | Value |
|---|---|
| Require a pull request before merging | ✅ |
| Required approvals | **1** |
| Dismiss stale reviews on new commits | ✅ |
| Require review from code owners | — |
| Require status checks to pass | ✅ |
| Require branches to be up to date | ✅ |
| Require conversation resolution | ✅ |
| Include administrators | ✅ |

**Required status checks:**
- `CI Complete`

---

## `main` branch

| Setting | Value |
|---|---|
| Require a pull request before merging | ✅ |
| Required approvals | **2** |
| Dismiss stale reviews on new commits | ✅ |
| Require review from code owners | ✅ |
| Require status checks to pass | ✅ |
| Require branches to be up to date | ✅ |
| Require conversation resolution | ✅ |
| Require signed commits | ✅ |
| Include administrators | ✅ |
| Restrict who can push | ✅ (release automation only) |

**Required status checks:**
- `CI Complete`

---

## `feature/**` and `fix/**` branches

| Setting | Value |
|---|---|
| Require a pull request before merging | ✅ |
| Required approvals | **1** |
| Dismiss stale reviews on new commits | ✅ |
| Require status checks to pass | ✅ |
| Require branches to be up to date | ✅ |

**Required status checks:**
- `CI Complete`

---

## Required GitHub Secrets

Configure under **Settings → Secrets and variables → Actions**.

| Secret | Purpose |
|---|---|
| `GITLEAKS_LICENSE` | Gitleaks secret scanning (required for private org repos) |

---

## How the gate works

The single `CI Complete` status check aggregates every required job in the pipeline.
Branch protection only needs to watch one check name — the gate job fails the entire
pipeline if any required upstream job fails or is cancelled.

Jobs that are **skipped** for a given event type (e.g. `dependency-review` on push,
`sbom`/`provenance`/`sign-image` on PRs) are treated as acceptable by the gate and
do not block the merge.

---

## Branch flow

```
feature/auth → PR → develop  (1 approval + CI Complete)
                        ↓
                    PR → main  (2 approvals + CI Complete)
                                    ↓
                             Trusted immutable artifact
                                    ↓
                              CD / Deployment (separate pipeline)
```
