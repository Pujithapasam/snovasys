#!/usr/bin/env bash
# ==========================================================
# BalanceVault - First-Admin Bootstrap Script (Linux/macOS)
# ==========================================================
set -e

BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-AdminSecure2026!}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@balancevault.internal}"
BOOTSTRAP_SECRET="${BOOTSTRAP_SECRET:-balancevault-bootstrap-secret-2026}"

echo "=========================================================="
echo "  BalanceVault - First-Admin Bootstrap Utility"
echo "=========================================================="
echo "Target URL:        $BACKEND_URL"
echo "Admin Username:    $ADMIN_USERNAME"
echo "Admin Email:       $ADMIN_EMAIL"
echo "=========================================================="

PAYLOAD=$(cat <<EOF
{
  "username": "$ADMIN_USERNAME",
  "password": "$ADMIN_PASSWORD",
  "email": "$ADMIN_EMAIL",
  "secret": "$BOOTSTRAP_SECRET"
}
EOF
)

echo "[i] Dispatching bootstrap request to /api/admin/bootstrap..."
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BACKEND_URL/api/admin/bootstrap" \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD")

HTTP_BODY=$(echo "$RESPONSE" | sed '$d')
HTTP_STATUS=$(echo "$RESPONSE" | tail -n 1)

if [ "$HTTP_STATUS" -eq 200 ]; then
  echo "✅ First-Admin bootstrap SUCCESSFUL!"
  echo "Response: $HTTP_BODY"
  echo "Login credentials: $ADMIN_USERNAME / $ADMIN_PASSWORD"
  exit 0
else
  echo "❌ Bootstrap failed with HTTP $HTTP_STATUS:"
  echo "$HTTP_BODY"
  exit 1
fi
