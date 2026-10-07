#!/usr/bin/env bash
# Adds the vehicle & fuel workflow to the shared "helpdesk" Keycloak realm. Safe to re-run.
#
#   scripts/keycloak-setup.sh [container]          container defaults to logbook-keycloak-1 (local dev)
#
# Env overrides:
#   KEYCLOAK_REALM     realm to use (default helpdesk)
#   FE_ORIGINS         comma-separated frontend origins allowed to log in (default http://localhost:5173)
#   ADMIN_USERNAME     existing realm user to make the first vehicle SYSTEM_ADMIN (optional)
#   SECRET_FILE        where to put the admin client secret instead of printing it: a deployment .env
#                      (fills KEYCLOAK_ADMIN_CLIENT_SECRET=) or, locally, application-local.yaml
#
# Creates, if missing:
#   vehicle-workflow        public client the frontend logs in with (PKCE), holding the app's roles
#   vehicle-workflow-admin  confidential client the backend uses to create accounts (service account)
# and prints the admin client's secret for KEYCLOAK_ADMIN_CLIENT_SECRET.
set -euo pipefail

CONTAINER=${1:-logbook-keycloak-1}
REALM=${KEYCLOAK_REALM:-helpdesk}
FE_ORIGINS=${FE_ORIGINS:-http://localhost:5173}
ROLES=(STATION_ADMIN DRIVER HOD HR_ADMIN_MANAGER VEHICLE_ADMIN SYSTEM_ADMIN)

# MSYS_NO_PATHCONV stops Git Bash on Windows rewriting /opt/... into a Windows path
kc() { MSYS_NO_PATHCONV=1 docker exec "$CONTAINER" /opt/keycloak/bin/kcadm.sh "$@"; }
client_id() { kc get clients -r "$REALM" -q clientId="$1" --fields id --format csv --noquotes | tr -d '\r'; }

# log in with the container's own admin credentials; if the admin password was changed since the
# container was created, ask for it (read silently, passed to kcadm on stdin, never echoed or stored here)
if ! MSYS_NO_PATHCONV=1 docker exec "$CONTAINER" sh -c \
  '/opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master --user "$KEYCLOAK_ADMIN" --password "$KEYCLOAK_ADMIN_PASSWORD"' >/dev/null 2>&1; then
  read -rp "Keycloak admin username [admin]: " admin_user
  read -rsp "Keycloak admin password: " admin_pass; echo
  printf '%s\n' "$admin_pass" | MSYS_NO_PATHCONV=1 docker exec -i "$CONTAINER" \
    /opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master --user "${admin_user:-admin}" >/dev/null
  unset admin_pass
fi

redirects=$(echo "$FE_ORIGINS" | tr ',' '\n' | sed 's|.*|"&/*"|' | paste -sd, -)
origins=$(echo "$FE_ORIGINS" | tr ',' '\n' | sed 's|.*|"&"|' | paste -sd, -)

app_id=$(client_id vehicle-workflow)
if [ -z "$app_id" ]; then
  kc create clients -r "$REALM" \
    -s clientId=vehicle-workflow -s name="Vehicle & Fuel Workflow" \
    -s publicClient=true -s standardFlowEnabled=true -s directAccessGrantsEnabled=false \
    -s "redirectUris=[$redirects]" -s "webOrigins=[$origins]" \
    -s 'attributes."pkce.code.challenge.method"=S256' -s 'attributes."post.logout.redirect.uris"=+' >/dev/null
  app_id=$(client_id vehicle-workflow)
  echo "created client vehicle-workflow"
else
  kc update "clients/$app_id" -r "$REALM" -s "redirectUris=[$redirects]" -s "webOrigins=[$origins]"
  echo "client vehicle-workflow exists; redirect URIs set to $FE_ORIGINS"
fi

for role in "${ROLES[@]}"; do
  kc create "clients/$app_id/roles" -r "$REALM" -s name="$role" >/dev/null 2>&1 && echo "created role $role" || true
done

# The helpdesk realm's shared "roles" scope has an unconfigured client-roles mapper, so tokens carry no
# client roles. Map ours on this client instead of touching the shared scope the help desk relies on.
if ! kc get "clients/$app_id/protocol-mappers/models" -r "$REALM" --fields name | grep -q '"vehicle roles"'; then
  kc create "clients/$app_id/protocol-mappers/models" -r "$REALM" \
    -s name="vehicle roles" -s protocol=openid-connect -s protocolMapper=oidc-usermodel-client-role-mapper \
    -s 'config."usermodel.clientRoleMapping.clientId"=vehicle-workflow' \
    -s 'config."claim.name"=resource_access.${client_id}.roles' \
    -s 'config."access.token.claim"=true' -s 'config."id.token.claim"=false' -s 'config."userinfo.token.claim"=false' \
    -s 'config.multivalued=true' -s 'config."jsonType.label"=String' >/dev/null
  echo "added the vehicle roles token mapper"
fi

admin_id=$(client_id vehicle-workflow-admin)
if [ -z "$admin_id" ]; then
  kc create clients -r "$REALM" \
    -s clientId=vehicle-workflow-admin -s publicClient=false -s serviceAccountsEnabled=true \
    -s standardFlowEnabled=false -s directAccessGrantsEnabled=false >/dev/null
  admin_id=$(client_id vehicle-workflow-admin)
  echo "created client vehicle-workflow-admin"
fi
# enough to create users, look up the vehicle-workflow client and assign its roles
kc add-roles -r "$REALM" --uusername service-account-vehicle-workflow-admin --cclientid realm-management \
  --rolename manage-users --rolename view-users --rolename query-users --rolename view-clients --rolename query-clients

if [ -n "${ADMIN_USERNAME:-}" ]; then
  kc add-roles -r "$REALM" --uusername "$ADMIN_USERNAME" --cclientid vehicle-workflow --rolename SYSTEM_ADMIN
  echo "gave $ADMIN_USERNAME the SYSTEM_ADMIN role"
fi

secret=$(kc get "clients/$admin_id/client-secret" -r "$REALM" --fields value --format csv --noquotes | tr -d '\r')
if [ -n "${SECRET_FILE:-}" ] && [ "${SECRET_FILE##*/}" = ".env" ]; then
  # server: fill in the KEYCLOAK_ADMIN_CLIENT_SECRET= line of the deployment .env
  sed -i "s|^KEYCLOAK_ADMIN_CLIENT_SECRET=.*|KEYCLOAK_ADMIN_CLIENT_SECRET=$secret|" "$SECRET_FILE"
  echo "admin client secret written to $SECRET_FILE"
elif [ -n "${SECRET_FILE:-}" ]; then
  # local dev: append to a git-ignored Spring config file instead of printing the secret
  printf '\nworkflow:\n  keycloak:\n    admin-client-secret: %s\n' "$secret" >> "$SECRET_FILE"
  echo "admin client secret written to $SECRET_FILE"
else
  echo "KEYCLOAK_ADMIN_CLIENT_SECRET=$secret"
fi
