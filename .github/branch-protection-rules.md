# GitHub Branch Protection Rules

This document outlines the branch protection rules that must be configured in GitHub for this repository.

## Configuration Steps

1. Go to **Settings** → **Branches**
2. Click **Add rule** for each branch pattern below
3. Configure the settings as specified

---

## Development Branch (`dev`)

**Branch name pattern:** `dev`

### Required Settings:
- ✅ **Require a pull request before merging**
  - Require approvals: **1**
  - Dismiss stale pull request approvals when new commits are pushed: ✓
  - Require review from code owners: ✗

- ✅ **Require status checks to pass before merging**
  - Require branches to be up to date before merging: ✓
  - Status checks that must pass:
    - `backend-ci`
    - `frontend-ci`
    - `docker-build`

- ✅ **Require conversation resolution before merging**

- ✅ **Include administrators** (enforce for admins too)

---

## Production Branch (`main`)

**Branch name pattern:** `main`

### Required Settings:
- ✅ **Require a pull request before merging**
  - Require approvals: **2**
  - Dismiss stale pull request approvals when new commits are pushed: ✓
  - Require review from code owners: ✓

- ✅ **Require status checks to pass before merging**
  - Require branches to be up to date before merging: ✓
  - Status checks that must pass:
    - `backend-ci`
    - `frontend-ci`
    - `docker-build`

- ✅ **Require conversation resolution before merging**

- ✅ **Require signed commits**

- ✅ **Include administrators** (enforce for admins too)

- ✅ **Restrict who can push to matching branches**
  - Allow specified actors to bypass required pull requests: (leave empty or specify release automation)

---

## Feature Branches (`feature/*`)

**Branch name pattern:** `feature/*`

### Required Settings:
- ✅ **Require a pull request before merging**
  - Require approvals: **1**
  - Dismiss stale pull request approvals when new commits are pushed: ✓

- ✅ **Require status checks to pass before merging**
  - Require branches to be up to date before merging: ✓
  - Status checks that must pass:
    - `backend-ci`
    - `frontend-ci`
    - `docker-build`

---

## Bug Fix Branches (`fix/*`)

**Branch name pattern:** `fix/*`

### Required Settings:
- ✅ **Require a pull request before merging**
  - Require approvals: **1**
  - Dismiss stale pull request approvals when new commits are pushed: ✓

- ✅ **Require status checks to pass before merging**
  - Require branches to be up to date before merging: ✓
  - Status checks that must pass:
    - `backend-ci`
    - `frontend-ci`
    - `docker-build`

---

## GitHub Secrets Required

Configure these secrets in **Settings** → **Secrets and variables** → **Actions**:

| Secret Name | Description | Example |
|---|---|---|
| `VERCEL_TOKEN` | Vercel authentication token | `vercel_...` |
| `VERCEL_ORG_ID` | Vercel organization ID | `team_...` |
| `VERCEL_PROJECT_ID` | Vercel project ID | `prj_...` |

---

## Workflow

```
feature/auth → PR to dev → 1 approval + CI pass → merge to dev
                                                        ↓
                                                   Deploy preview
                                                        ↓
                                                   PR to main
                                                        ↓
                                            2 approvals + CI pass
                                                        ↓
                                                   Merge to main
                                                        ↓
                                            Deploy to production
```

---

## Notes

- All status checks must pass before merging
- Branches must be up-to-date with the target branch
- Stale approvals are dismissed when new commits are pushed
- Administrators are subject to the same rules
- Production deployments require 2 approvals for extra safety
