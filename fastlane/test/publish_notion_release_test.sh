#!/usr/bin/env bash

# Verify lookup parsing and Notion filter payloads without external requests.

set -uo pipefail

readonly TEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly FASTLANE_DIR="$(cd "$TEST_DIR/.." && pwd)"

export NOTION_TITLE_PROPERTY_ID="title"
export NOTION_BUG_TITLE_PROPERTY_ID="Bug title"

# shellcheck source=../publish_notion_release.sh
source "$FASTLANE_DIR/publish_notion_release.sh"

PASS_COUNT=0
FAIL_COUNT=0
CAPTURED_PAYLOAD=""

# Record one equality assertion.
assert_equal() {
  local description="$1"
  local expected="$2"
  local actual="$3"

  if [[ "$actual" == "$expected" ]]; then
    PASS_COUNT=$((PASS_COUNT + 1))
    printf 'ok   %s\n' "$description"
  else
    FAIL_COUNT=$((FAIL_COUNT + 1))
    printf 'FAIL %s (expected: %s, actual: %s)\n' \
      "$description" "$expected" "$actual"
  fi
}

# Record one jq assertion against a JSON value.
assert_jq() {
  local description="$1"
  local json="$2"
  local filter="$3"

  if jq -e "$filter" >/dev/null 2>&1 <<< "$json"; then
    PASS_COUNT=$((PASS_COUNT + 1))
    printf 'ok   %s\n' "$description"
  else
    FAIL_COUNT=$((FAIL_COUNT + 1))
    printf 'FAIL %s\n' "$description"
  fi
}

# Capture a Notion request and return an empty successful query result.
notion_post() {
  CAPTURED_PAYLOAD="$2"
  NOTION_RESPONSE_BODY='{"results":[],"has_more":false}'
  return 0
}

bug_title="BUG-8 - testing ci/cd"
assert_equal "parses dashed Bug ID" "8" "$(bug_code_from_text "$bug_title")"
assert_equal "parses DEFECT prefix" "3" "$(bug_code_from_text "DEFECT-3 - Android test CI/CD")"
assert_equal "parses bracketed DEFECT prefix" "3" "$(bug_code_from_text "[DEFECT-3] Android test CI/CD")"
assert_equal "parses lowercase defect prefix" "12" "$(bug_code_from_text "defect-12: fix issue")"

query_bug_data_source "3" || true
assert_jq "queries Bug ID as a numeric unique_id" "$CAPTURED_PAYLOAD" \
  '.filter.property == "Bug ID" and
   .filter.unique_id.equals == 3 and
   (.filter | has("formula") | not)'

query_data_source "task-source" "Known feature" || true
assert_jq "keeps task lookup on the title property" "$CAPTURED_PAYLOAD" \
  '.filter.property == "title" and
   .filter.title.contains == "Known feature" and
   (.filter | has("formula") | not)'

ASSIGNEE_PROPERTY_NAMES_JSON_INPUT='["Assignee", "Assignee to"]'
normalize_assignee_property_names
assert_jq "includes Assigned to in assignee property candidates" "$ASSIGNEE_PROPERTY_NAMES_JSON" \
  '. | index("Assigned to") != null'

printf '%s passed, %s failed\n' "$PASS_COUNT" "$FAIL_COUNT"
((FAIL_COUNT == 0))
