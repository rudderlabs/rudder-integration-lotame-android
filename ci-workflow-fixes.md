# CI Workflow Fixes: PAT to GitHub App Token Migration

## Background

PR [SEC-58] migrated the release workflows from using a Personal Access Token (PAT) to GitHub App tokens via `actions/create-github-app-token`. It also replaced local `git tag`/`git push` operations with `ryancyq/github-signed-commit`, which creates tags and commits via the GitHub API instead of locally.

These changes introduced two bugs that need to be fixed in every repo that received this migration.

---

## Fix 1: `draft_new_release.yml` — Missing `permission-members: read`

### Symptom

The "Draft new release" action fails with a **422 error** when the `repo-sync/pull-request` action tries to assign a team (e.g., `@rudderlabs/sdk-android`) as a reviewer on the PR.

```
Reviews validation failed: Could not resolve to a node with the global id of '<team_id>'
```

### Root Cause

The GitHub App token is generated with only `permission-contents: write` and `permission-pull-requests: write`. Resolving a **team** as a reviewer requires the `members:read` permission on the organization. Without it, the GitHub API cannot resolve the team slug to a node ID.

Note: The PR itself is created successfully — the failure occurs in a **separate API call** that assigns reviewers after PR creation. This is why you may see a PR created but the action still fails.

### Fix

Add `permission-members: read` to the `Generate GitHub App Token` step in `draft_new_release.yml`:

```yaml
- name: Generate GitHub App Token
  id: generate-token
  uses: actions/create-github-app-token@67018539274d69449ef7c02e8e71183d1719ab42 # v2.1.4
  with:
    app-id: ${{ vars.RELEASE_APP_ID }}
    private-key: ${{ secrets.RELEASE_PRIVATE_KEY }}
    permission-contents: write # to create commits and push changes
    permission-pull-requests: write # to create and update PRs
    permission-members: read # to resolve team reviewers   <-- ADD THIS LINE
```

### Prerequisite

The GitHub App (configured via `RELEASE_APP_ID`) must have the **Organization > Members: Read** permission enabled in its app settings on GitHub. If the permission isn't granted at the app level, requesting it in the workflow will fail.

---

## Fix 2: `publish-new-github-release.yml` — Missing `git fetch --tags`

### Symptom

The "Publish new github release" action completes **successfully** (all steps green, exit code 0), but **no GitHub release is actually created**. The tag (e.g., `v3.6.0`) exists on the remote but the release page on GitHub shows nothing new.

### Root Cause

The migration replaced local tag creation (`git tag -a ... && git push origin refs/tags/...`) with `ryancyq/github-signed-commit`, which creates the tag via the **GitHub API**. This means:

1. **Checkout** (step 4) — clones the repo with full history (`fetch-depth: 0`)
2. **Create verified tag via API** (step 7) — creates the tag on the **remote** via the GitHub API, but **not in the local clone**
3. **Create GitHub Release** (step 8) — runs `conventional-github-releaser`, which internally uses `git-semver-tags` to find the latest tag from the **local** git repo

Since the tag only exists on the remote, `conventional-github-releaser` cannot find it locally. It silently does nothing and exits with code 0 — no error, no release.

### Fix

Add a `git fetch --tags` step between "Create verified tag via API" and "Create GitHub Release":

```yaml
- name: Create verified tag via API
  uses: ryancyq/github-signed-commit@e9f3b28c80da7be66d24b8f501a5abe82a6b855f # v1.2.0
  env:
    GH_TOKEN: ${{ steps.generate-token.outputs.token }}
  with:
    branch-name: master
    commit-message: 'chore: release v${{ steps.extract-version.outputs.release_version }}'
    tag: 'v${{ steps.extract-version.outputs.release_version }}'
    files: ''

- name: Fetch remote tags           # <-- ADD THIS STEP
  run: git fetch --tags

- name: Create GitHub Release
  id: create_release
  env:
    CONVENTIONAL_GITHUB_RELEASER_TOKEN: ${{ steps.generate-token.outputs.token }}
  run: |
    DEBUG=conventional-github-releaser npx conventional-github-releaser -p angular
```

### Why this is sufficient

The checkout already has the full commit history (`fetch-depth: 0`). The only missing piece is the tag reference itself. `git fetch --tags` pulls just the tag ref pointing to the already-present commit.

---

## How to identify affected repos

Look for repos where PR [SEC-58] (or similar PAT-to-GitHub-App-token migration PRs) modified:
- `draft_new_release.yml` — check if `permission-members: read` is missing from the token generation step
- `publish-new-github-release.yml` — check if there's a `git fetch --tags` between the `ryancyq/github-signed-commit` tag creation step and the `conventional-github-releaser` step

## Verification

- **Fix 1**: Run the "Draft new release" action and confirm the PR is created with team reviewers assigned without errors.
- **Fix 2**: Merge a release PR into master and confirm both the tag and the GitHub release are created. The release list (`gh release list`) should show the new version.
