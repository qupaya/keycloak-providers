#!/usr/bin/env bash
# Starts a throwaway Keycloak with the built provider jar and checks the sha1 provider end to end.
# Usage: ./gradlew jar && scripts/sha1-keycloak-check.sh [path/to/providers.jar]
# Needs docker, curl and jq. KEYCLOAK_PORT overrides the host port (default 18080).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="${1:-$(ls -t "$ROOT"/build/libs/keycloak-providers-*.jar 2>/dev/null | head -1)}"
[ -f "$JAR" ] || { echo "No provider jar found. Run ./gradlew jar first or pass its path."; exit 2; }
KEYCLOAK_VERSION="$(sed -nE 's/^val keycloakVersion = "(.+)"/\1/p' "$ROOT/build.gradle.kts")"
PORT="${KEYCLOAK_PORT:-18080}"
URL="http://localhost:$PORT"
NAME="keycloak-providers-sha1-check"
ADMIN_USER=check-admin
ADMIN_PASS="$(openssl rand -hex 16)"
REALM=sha1check
SALT=check-salt
FAILURES=0

PROVIDERS_DIR="$(mktemp -d)"
cp "$JAR" "$PROVIDERS_DIR/"
cleanup() { docker rm -f "$NAME" >/dev/null 2>&1 || true; rm -rf "$PROVIDERS_DIR"; }
trap cleanup EXIT

sha1hex() { printf '%s' "$1$SALT" | shasum -a 1 | awk '{print toupper($1)}'; }
admin_token() { curl -sf -d client_id=admin-cli -d grant_type=password -d "username=$ADMIN_USER" --data-urlencode "password=$ADMIN_PASS" "$URL/realms/master/protocol/openid-connect/token" | jq -r .access_token; }
admin() { local method=$1 path=$2; shift 2; curl -s -o /dev/null -w '%{http_code}' -X "$method" -H "Authorization: Bearer $(admin_token)" -H 'Content-Type: application/json' "$URL/admin/realms$path" "$@"; }
admin_get() { curl -sf -H "Authorization: Bearer $(admin_token)" "$URL/admin/realms$1"; }
user_id() { admin_get "/$REALM/users?username=$1&exact=true" | jq -r '.[0].id'; }
algorithm() { admin_get "/$REALM/users/$(user_id "$1")/credentials" | jq -r '.[] | select(.type=="password") | .credentialData | fromjson | .algorithm'; }
login() { curl -s -o /dev/null -w '%{http_code}' -d client_id=admin-cli -d grant_type=password -d "username=$1" --data-urlencode "password=$2" "$URL/realms/$REALM/protocol/openid-connect/token"; }
reset_password() { admin PUT "/$REALM/users/$(user_id "$1")/reset-password" -d "{\"type\":\"password\",\"value\":\"$2\",\"temporary\":false}"; }
create_sha1_user() {
  local secret credential
  secret="$(jq -nc --arg v "$(sha1hex "$2")" --arg s "$(printf '%s' "$SALT" | base64)" '{value:$v, salt:$s, additionalParameters:{}}')"
  credential='{"hashIterations":1,"algorithm":"sha1","additionalParameters":{}}'
  admin POST "/$REALM/users" -d "$(jq -nc --arg u "$1" --arg secret "$secret" --arg cred "$credential" \
    '{username:$u, enabled:true, email:($u+"@example.test"), firstName:$u, lastName:"check", emailVerified:true,
      credentials:[{type:"password", secretData:$secret, credentialData:$cred}]}')"
}
expect() {
  if [ "$2" = "$3" ]; then echo "PASS  $1"; else echo "FAIL  $1: expected $3, got $2"; FAILURES=$((FAILURES + 1)); fi
}

echo "Starting Keycloak $KEYCLOAK_VERSION with $(basename "$JAR") on $URL"
docker rm -f "$NAME" >/dev/null 2>&1 || true
docker run -d --name "$NAME" -p "$PORT:8080" \
  -e KC_BOOTSTRAP_ADMIN_USERNAME="$ADMIN_USER" -e KC_BOOTSTRAP_ADMIN_PASSWORD="$ADMIN_PASS" \
  -v "$PROVIDERS_DIR:/opt/keycloak/providers:ro" \
  "quay.io/keycloak/keycloak:$KEYCLOAK_VERSION" start-dev >/dev/null
for _ in $(seq 1 90); do curl -sf "$URL/realms/master" >/dev/null 2>&1 && break; sleep 2; done
expect "Keycloak serves requests with the provider jar loaded" "$(curl -s -o /dev/null -w '%{http_code}' "$URL/realms/master")" 200
[ "$FAILURES" -eq 0 ] || { docker logs --tail 30 "$NAME"; exit 1; }

expect "create realm with password history 3" "$(admin POST "" -d "{\"realm\":\"$REALM\",\"enabled\":true,\"passwordPolicy\":\"passwordHistory(3)\"}")" 201
expect "import user with a sha1 password that is then changed" "$(create_sha1_user changed Changed-Old-1)" 201
expect "import user with a sha1 password that is never changed" "$(create_sha1_user unchanged Unchanged-Old-1)" 201

expect "first reset moves the sha1 hash into the password history" "$(reset_password changed Changed-New-2)" 204
expect "reset with a sha1 entry in the password history" "$(reset_password changed Changed-Newer-3)" 204
expect "history rejects the password stored as sha1" "$(reset_password changed Changed-Old-1)" 400

expect "login with the right sha1 password" "$(login unchanged Unchanged-Old-1)" 200
rehashed=sha1
for _ in $(seq 1 10); do rehashed="$(algorithm unchanged)"; [ "$rehashed" != sha1 ] && break; sleep 1; done
expect "login re-hashes the sha1 password with the realm algorithm" "$([ "$rehashed" != sha1 ] && echo rehashed || echo "$rehashed")" rehashed
expect "login with a wrong password" "$(login unchanged Wrong-Pass-9)" 400

keycloak_log="$(docker logs "$NAME" 2>&1)"
expect "no null hash provider in the Keycloak log" "$(grep -q 'because "hash" is null' <<<"$keycloak_log" && echo found || echo absent)" absent

echo
[ "$FAILURES" -eq 0 ] && echo "All checks passed." || { echo "$FAILURES check(s) failed."; exit 1; }
