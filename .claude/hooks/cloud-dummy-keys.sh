#!/bin/bash
# SessionStart hook for Claude Code cloud sessions. app/build.gradle reads
# treetracker.keys.properties and fails without it, so write dummy keys, the same
# values CI uses for tests. Does nothing locally or when the file already exists.
# The SDK and JDK come from the cloud environment: see docs/getting-started/claude-cloud-sessions.md.
[ "${CLAUDE_CODE_REMOTE:-}" = "true" ] || exit 0
cd "$CLAUDE_PROJECT_DIR" || exit 0
[ -f treetracker.keys.properties ] && exit 0
cat > treetracker.keys.properties <<'KEYS'
#Treetracker API Keys (dummy, for Claude cloud sessions)
treetracker_client_id=dummy-id
treetracker_client_secret=dummy-secret
s3_dev_identity_pool_id=dummy-pool-id-dev
s3_test_identity_pool_id=dummy-pool-id-test
prod_treetracker_client_id=dummy-prod-id
prod_treetracker_client_secret=dummy-prod-secret
s3_production_identity_pool_id=dummy-pool-id-prod
KEYS
