#!/usr/bin/env bash
# =============================================================================
# Twobbie (PairPulse) - Build, rename, and upload a production Android App Bundle
# =============================================================================
# Usage (--version-code is accepted as an alias of --version):
#   ./scripts/build_and_upload.sh [--version <number>]
#
# Credentials (never committed):
#   GDRIVE_CLIENT_ID / GDRIVE_CLIENT_SECRET from the process environment, or
#   from scripts/.env (KEY=VALUE, gitignored). The OAuth client type must be
#   "TVs and Limited Input devices" to use the Device Authorization Flow.
#
# Token storage:
#   scripts/.gdrive_token.json is created after the first login (gitignored)
#   and refreshed automatically afterwards.
#
# The destination Google Drive folder is fixed (see GDRIVE_FOLDER_ID below).
# =============================================================================

set -Eeuo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
readonly GRADLE_FILE="$PROJECT_ROOT/app/build.gradle.kts"
readonly SOURCE_AAB="$PROJECT_ROOT/app/build/outputs/bundle/productionRelease/app-production-release.aab"
readonly ENV_FILE="$SCRIPT_DIR/.env"
readonly TOKEN_FILE="$SCRIPT_DIR/.gdrive_token.json"
readonly GDRIVE_FOLDER_ID="1pxnjByM0TcoBOVel464vW_xysYloLMfF"
readonly OAUTH_DEVICE_CODE_URL="https://oauth2.googleapis.com/device/code"
readonly OAUTH_TOKEN_URL="https://oauth2.googleapis.com/token"
readonly DRIVE_API_BASE="https://www.googleapis.com/drive/v3"
readonly DRIVE_UPLOAD_BASE="https://www.googleapis.com/upload/drive/v3"
# drive.file is the broadest scope the Device Flow supports.
readonly OAUTH_SCOPE="https://www.googleapis.com/auth/drive.file"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

GDRIVE_CLIENT_ID="${GDRIVE_CLIENT_ID:-}"
GDRIVE_CLIENT_SECRET="${GDRIVE_CLIENT_SECRET:-}"
ACCESS_TOKEN=""
VERSION_NAME=""
VERSION_CODE=""
RENAMED_AAB=""
UPLOADED_FILE_ID=""
UPLOADED_WEB_LINK=""
REQUESTED_VERSION_CODE=""

# Prints an informational message to stderr.
log_info() { printf "${CYAN}[INFO]${RESET}  %s\n" "$*" >&2; }

# Prints a success message to stderr.
log_success() { printf "${GREEN}[OK]${RESET}    %s\n" "$*" >&2; }

# Prints a warning message to stderr.
log_warn() { printf "${YELLOW}[WARN]${RESET}  %s\n" "$*" >&2; }

# Prints an error message to stderr.
log_error() { printf "${RED}[ERROR]${RESET} %s\n" "$*" >&2; }

# Prints a section heading to stderr.
log_step() { printf "\n${BOLD}--- %s ---${RESET}\n" "$*" >&2; }

# Prints usage information.
print_usage() {
  printf 'Usage: %s [--version <number>]\n' "$(basename "$0")"
  printf '\n'
  printf 'Credentials (environment or scripts/.env):\n'
  printf '  GDRIVE_CLIENT_ID\n'
  printf '  GDRIVE_CLIENT_SECRET\n'
}

# Parses and validates command-line arguments.
parse_arguments() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --version|--version-code)
        [[ $# -ge 2 ]] || {
          log_error "$1 requires a value."
          exit 2
        }
        REQUESTED_VERSION_CODE="$2"
        shift 2
        ;;
      --help|-h)
        print_usage
        exit 0
        ;;
      *)
        log_error "Unknown argument: $1"
        print_usage >&2
        exit 2
        ;;
    esac
  done

  if [[ -n "$REQUESTED_VERSION_CODE" && ! "$REQUESTED_VERSION_CODE" =~ ^[1-9][0-9]*$ ]]; then
    log_error "versionCode must be a positive integer."
    exit 2
  fi
}

# Ensures local build and upload dependencies are available.
check_dependencies() {
  log_step "Checking dependencies"
  local command_name
  local missing=0

  for command_name in curl jq java; do
    if command -v "$command_name" >/dev/null 2>&1; then
      log_success "$command_name found"
    else
      log_error "$command_name is required but not installed."
      missing=1
    fi
  done

  if [[ ! -x "$PROJECT_ROOT/gradlew" ]]; then
    log_error "Gradle wrapper is missing or not executable: $PROJECT_ROOT/gradlew"
    missing=1
  fi

  [[ "$missing" -eq 0 ]] || exit 1
}

# Loads GDRIVE_* credentials from the environment or scripts/.env (KEY=VALUE).
# Values already present in the process environment take priority.
load_credentials() {
  log_step "Loading Google OAuth credentials"

  if [[ -f "$ENV_FILE" ]]; then
    local line key value
    while IFS= read -r line || [[ -n "$line" ]]; do
      line="${line%$'\r'}"
      [[ "$line" =~ ^[[:space:]]*(export[[:space:]]+)?(GDRIVE_[A-Z_]+)=(.*)$ ]] || continue
      key="${BASH_REMATCH[2]}"
      value="${BASH_REMATCH[3]}"
      value="${value%\"}"; value="${value#\"}"
      value="${value%\'}"; value="${value#\'}"
      case "$key" in
        GDRIVE_CLIENT_ID) [[ -n "$GDRIVE_CLIENT_ID" ]] || GDRIVE_CLIENT_ID="$value" ;;
        GDRIVE_CLIENT_SECRET) [[ -n "$GDRIVE_CLIENT_SECRET" ]] || GDRIVE_CLIENT_SECRET="$value" ;;
      esac
    done < "$ENV_FILE"
  fi

  if [[ -z "$GDRIVE_CLIENT_ID" || -z "$GDRIVE_CLIENT_SECRET" ]]; then
    log_error "GDRIVE_CLIENT_ID and GDRIVE_CLIENT_SECRET are required (environment or scripts/.env)."
    log_error "Create an OAuth client: https://console.cloud.google.com/apis/credentials"
    exit 1
  fi

  log_success "Google OAuth client credentials loaded."
}

# Persists a token response, keeping the existing refresh token when the
# response does not include a new one.
save_token() {
  local response="$1"
  local previous_refresh_token=""
  [[ ! -f "$TOKEN_FILE" ]] || previous_refresh_token="$(jq -r '.refresh_token // empty' "$TOKEN_FILE")"

  local expires_in expiry_ts
  expires_in="$(jq -r '.expires_in // 3600' <<<"$response")"
  expiry_ts=$(( $(date +%s) + expires_in - 60 ))

  ACCESS_TOKEN="$(jq -r '.access_token' <<<"$response")"

  (
    umask 077
    jq -n \
      --arg access_token "$ACCESS_TOKEN" \
      --arg refresh_token "$(jq -r --arg prev "$previous_refresh_token" '.refresh_token // $prev' <<<"$response")" \
      --argjson expiry "$expiry_ts" \
      '{access_token: $access_token, refresh_token: $refresh_token, expiry_timestamp: $expiry}' \
      > "${TOKEN_FILE}.tmp"
  )
  mv "${TOKEN_FILE}.tmp" "$TOKEN_FILE"
  chmod 600 "$TOKEN_FILE"
}

# Refreshes the access token with the stored refresh token. Returns 1 on failure.
refresh_access_token() {
  local refresh_token="$1"
  log_info "Refreshing access token..."

  local response
  response="$(curl --silent \
    --data-urlencode "client_id=$GDRIVE_CLIENT_ID" \
    --data-urlencode "client_secret=$GDRIVE_CLIENT_SECRET" \
    --data-urlencode "refresh_token=$refresh_token" \
    --data-urlencode 'grant_type=refresh_token' \
    "$OAUTH_TOKEN_URL")"

  if [[ -z "$(jq -r '.access_token // empty' <<<"$response")" ]]; then
    log_warn "Token refresh failed ($(jq -r '.error // "unknown"' <<<"$response")). Re-authenticating."
    return 1
  fi

  save_token "$response"
  log_success "Access token refreshed."
}

# Loads a valid token from the token file, refreshing it when expired.
load_token() {
  [[ -f "$TOKEN_FILE" ]] || return 1

  local access_token refresh_token expiry_ts
  access_token="$(jq -r '.access_token // empty' "$TOKEN_FILE")"
  refresh_token="$(jq -r '.refresh_token // empty' "$TOKEN_FILE")"
  expiry_ts="$(jq -r '.expiry_timestamp // 0' "$TOKEN_FILE")"

  [[ -n "$access_token" ]] || return 1

  if [[ "$(date +%s)" -lt "$expiry_ts" ]]; then
    ACCESS_TOKEN="$access_token"
    log_success "Loaded a valid token from $(basename "$TOKEN_FILE")"
    return 0
  fi

  [[ -z "$refresh_token" ]] || refresh_access_token "$refresh_token"
}

# Runs the OAuth 2.0 Device Authorization Grant flow and stores the token.
google_auth() {
  log_step "Google OAuth - Device Authorization Flow"

  local raw_response http_code device_response
  raw_response="$(curl --silent --write-out '\n%{http_code}' \
    --data-urlencode "client_id=$GDRIVE_CLIENT_ID" \
    --data-urlencode "scope=$OAUTH_SCOPE" \
    "$OAUTH_DEVICE_CODE_URL")"
  http_code="$(tail -n 1 <<<"$raw_response")"
  device_response="$(sed '$d' <<<"$raw_response")"

  if [[ "$http_code" != "200" ]]; then
    log_error "Google returned HTTP $http_code: $(jq -r '.error // "unknown_error"' <<<"$device_response")"
    log_error "$(jq -r '.error_description // ""' <<<"$device_response")"
    log_error "The OAuth client type must be \"TVs and Limited Input devices\" (not Desktop app),"
    log_error "and the Google Drive API must be enabled in the Cloud project."
    exit 1
  fi

  local device_code user_code verification_url interval expires_in
  device_code="$(jq -r '.device_code // empty' <<<"$device_response")"
  user_code="$(jq -r '.user_code // empty' <<<"$device_response")"
  verification_url="$(jq -r '.verification_url // empty' <<<"$device_response")"
  interval="$(jq -r '.interval // 5' <<<"$device_response")"
  expires_in="$(jq -r '.expires_in // 300' <<<"$device_response")"

  if [[ -z "$device_code" || -z "$user_code" ]]; then
    log_error "Unexpected device-code response from Google."
    exit 1
  fi

  printf '\n  1. Open: %b%s%b\n' "$CYAN" "$verification_url" "$RESET" >&2
  printf '  2. Enter the code: %b%s%b\n' "${BOLD}${YELLOW}" "$user_code" "$RESET" >&2
  printf '  (the code expires in %ss)\n\n' "$expires_in" >&2

  local deadline=$(( $(date +%s) + expires_in ))
  local token_response error_code

  log_info "Waiting for authentication..."
  while [[ "$(date +%s)" -lt "$deadline" ]]; do
    sleep "$interval"

    token_response="$(curl --silent \
      --data-urlencode "client_id=$GDRIVE_CLIENT_ID" \
      --data-urlencode "client_secret=$GDRIVE_CLIENT_SECRET" \
      --data-urlencode "device_code=$device_code" \
      --data-urlencode 'grant_type=urn:ietf:params:oauth:grant-type:device_code' \
      "$OAUTH_TOKEN_URL")"
    error_code="$(jq -r '.error // empty' <<<"$token_response")"

    case "$error_code" in
      "")
        save_token "$token_response"
        log_success "Authentication successful. Token saved to $(basename "$TOKEN_FILE")"
        return 0
        ;;
      authorization_pending) ;;
      slow_down) interval=$(( interval + 5 )) ;;
      *)
        log_error "Authentication failed: $error_code"
        exit 1
        ;;
    esac
  done

  log_error "Authentication timed out. Run the script again."
  exit 1
}

# Ensures a usable access token exists, logging in when needed.
ensure_authenticated() {
  log_step "Checking authentication"
  load_token || google_auth
}

# Reads production version metadata from the Android Gradle configuration.
# versionCode accepts both `versionCode = 5` and `versionCode = ... ?: 5`.
read_version_metadata() {
  log_step "Reading Android version metadata"

  VERSION_NAME="$(sed -nE 's/^[[:space:]]*versionName[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' "$GRADLE_FILE" | head -1)"

  if [[ -n "$REQUESTED_VERSION_CODE" ]]; then
    VERSION_CODE="$REQUESTED_VERSION_CODE"
  else
    VERSION_CODE="$(sed -nE 's/^[[:space:]]*versionCode[[:space:]]*=[^0-9]*([0-9]+)[[:space:]]*$/\1/p' "$GRADLE_FILE" | head -1)"
  fi

  if [[ -z "$VERSION_NAME" || ! "$VERSION_NAME" =~ ^[A-Za-z0-9._-]+$ ]]; then
    log_error "Could not read a filename-safe versionName from $GRADLE_FILE"
    exit 1
  fi

  if [[ ! "$VERSION_CODE" =~ ^[1-9][0-9]*$ ]]; then
    log_error "Could not read a positive versionCode from $GRADLE_FILE"
    exit 1
  fi

  log_success "versionName=$VERSION_NAME, versionCode=$VERSION_CODE"
}

# Builds the production AAB and renames it to the Twobbie release convention.
build_and_rename_aab() {
  log_step "Building production Android App Bundle"
  (
    cd "$PROJECT_ROOT"
    ./gradlew :app:bundleProductionRelease -PreleaseVersionCode="$VERSION_CODE"
  )

  if [[ ! -f "$SOURCE_AAB" ]]; then
    log_error "AAB was not found at the expected path: $SOURCE_AAB"
    exit 1
  fi

  RENAMED_AAB="$(dirname "$SOURCE_AAB")/Twobbie_${VERSION_NAME}+${VERSION_CODE}.aab"
  mv -f "$SOURCE_AAB" "$RENAMED_AAB"

  local size
  size="$(du -sh "$RENAMED_AAB" | awk '{print $1}')"
  log_success "AAB ready: $RENAMED_AAB ($size)"
}

# Verifies that the access token is accepted by Google Drive, refreshing once on 401.
verify_drive_connection() {
  log_step "Verifying Google Drive connection"
  local response_file http_code refresh_token
  response_file="$(mktemp)"

  http_code="$(curl --silent --show-error \
    --output "$response_file" \
    --write-out '%{http_code}' \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    "$DRIVE_API_BASE/about?fields=user(emailAddress)")"

  if [[ "$http_code" == "401" ]]; then
    refresh_token="$(jq -r '.refresh_token // empty' "$TOKEN_FILE" 2>/dev/null || true)"
    if [[ -n "$refresh_token" ]] && refresh_access_token "$refresh_token"; then
      http_code="$(curl --silent --show-error \
        --output "$response_file" \
        --write-out '%{http_code}' \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        "$DRIVE_API_BASE/about?fields=user(emailAddress)")"
    fi
  fi

  if [[ "$http_code" != "200" ]]; then
    rm -f "$response_file"
    log_error "Google Drive rejected the access token (HTTP $http_code)."
    log_error "Delete $TOKEN_FILE and run the script again to re-authenticate."
    exit 1
  fi

  local account_email
  account_email="$(jq -r '.user.emailAddress // "unknown"' "$response_file")"
  rm -f "$response_file"
  log_success "Google Drive connected as $account_email"
}

# Uploads the renamed AAB through a single resumable Google Drive session.
upload_to_drive() {
  log_step "Uploading Android App Bundle to Google Drive"
  local file_name
  local file_size
  local metadata
  local headers_file
  local response_file
  local http_code
  local upload_uri

  file_name="$(basename "$RENAMED_AAB")"
  file_size="$(wc -c < "$RENAMED_AAB" | tr -d ' ')"
  metadata="$(jq -n \
    --arg name "$file_name" \
    --arg parent "$GDRIVE_FOLDER_ID" \
    '{name: $name, parents: [$parent]}')"
  headers_file="$(mktemp)"
  response_file="$(mktemp)"

  log_info "Starting resumable upload for $file_name..."
  http_code="$(curl --silent --show-error \
    --output "$response_file" \
    --dump-header "$headers_file" \
    --write-out '%{http_code}' \
    -X POST \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H 'Content-Type: application/json; charset=UTF-8' \
    -H 'X-Upload-Content-Type: application/octet-stream' \
    -H "X-Upload-Content-Length: $file_size" \
    --data "$metadata" \
    "$DRIVE_UPLOAD_BASE/files?uploadType=resumable&supportsAllDrives=true&fields=id,name,webViewLink")"

  upload_uri="$(awk 'tolower($1) == "location:" {gsub("\\r", "", $2); print $2}' "$headers_file" | tail -1)"
  rm -f "$headers_file"

  if [[ "$http_code" != "200" || -z "$upload_uri" ]]; then
    local drive_error
    drive_error="$(jq -r '.error.message // "unknown error"' "$response_file" 2>/dev/null || printf 'invalid response')"
    rm -f "$response_file"
    log_error "Could not start the upload (HTTP $http_code): $drive_error"
    exit 1
  fi

  http_code="$(curl --silent --show-error \
    --output "$response_file" \
    --write-out '%{http_code}' \
    -X PUT \
    -H 'Content-Type: application/octet-stream' \
    -H "Content-Length: $file_size" \
    --data-binary "@$RENAMED_AAB" \
    "$upload_uri")"

  if [[ "$http_code" != "200" && "$http_code" != "201" ]]; then
    local upload_error
    upload_error="$(jq -r '.error.message // "unknown error"' "$response_file" 2>/dev/null || printf 'invalid response')"
    rm -f "$response_file"
    log_error "Google Drive upload failed (HTTP $http_code): $upload_error"
    exit 1
  fi

  UPLOADED_FILE_ID="$(jq -r '.id // empty' "$response_file")"
  UPLOADED_WEB_LINK="$(jq -r '.webViewLink // empty' "$response_file")"
  rm -f "$response_file"
  log_success "Upload completed."
}

# Prints the final local and remote artifact details.
print_result() {
  local folder_url="https://drive.google.com/drive/folders/$GDRIVE_FOLDER_ID"

  printf '\n'
  printf "${GREEN}${BOLD}Build and upload completed.${RESET}\n"
  printf '  File:      %s\n' "$RENAMED_AAB"
  printf '  File ID:   %s\n' "$UPLOADED_FILE_ID"
  [[ -z "$UPLOADED_WEB_LINK" ]] || printf '  Drive URL: %s\n' "$UPLOADED_WEB_LINK"
  printf '  Folder:    %s\n' "$folder_url"
}

# Reports the failing source line without exposing environment contents.
on_error() {
  local exit_code=$?
  local line_number="${BASH_LINENO[0]:-unknown}"
  log_error "Script failed at line $line_number with exit code $exit_code."
  exit "$exit_code"
}

trap on_error ERR

# Runs the complete authenticate, build, rename, and upload workflow.
main() {
  parse_arguments "$@"
  check_dependencies
  load_credentials
  ensure_authenticated
  verify_drive_connection
  read_version_metadata
  build_and_rename_aab
  upload_to_drive
  print_result
}

main "$@"
