# Vehicle & Fuel Workflow: server deployment (test.petrotrade.co.zw / 196.43.101.217)

Everything lives in this backend project:

```
docker-compose.yml       the four containers: vehicle-db, be, fe, vehicle-proxy
.env                     server settings and secrets (git-ignored; never commit or share)
.env.example             the same settings without secrets
docker/nginx/nginx.conf  HTTPS on port 8099
docker/nginx/certs/      tls.crt + tls.key, the 196.43.101.217 certificate the logbook proxy uses (git-ignored)
```

The images are built on your PC and pushed to Docker Hub (`tadiewa/vehicle-workflow-be` and `-fe`); the server only
pulls them. The server needs just these files, no source code:

```
docker-compose.yml  .env  docker/nginx/  scripts/keycloak-setup.sh
```

The app is served at **https://test.petrotrade.co.zw:8099**. It signs in through the shared help desk Keycloak
(https://196.43.101.217:8198, from the logbook stack), so the logbook stack must be running.

## Build and push (on your PC, every release)

Set `IMAGE_TAG` in `.env` (e.g. `uat-1`, then `uat-2` ...). `FRONTEND_DIR` must point at the frontend project.

```powershell
docker login
docker compose build
docker compose push
```

The Keycloak address is built into the web image, so build with this server's `.env`.

## First time on the server

1. Copy the files to the server (`<project-path>` e.g. `~/vehicle-workflow`):
   ```powershell
   ssh <user>@196.43.101.217 "mkdir -p <project-path>"
   scp -r docker-compose.yml .env docker scripts <user>@196.43.101.217:<project-path>/
   ```

2. Check the logbook stack's Keycloak container and network names (the defaults assume it was started from
   `~/helpdesk`):
   ```sh
   docker ps --format '{{.Names}}' | grep keycloak        # e.g. helpdesk-keycloak-1
   docker network ls | grep default                       # e.g. helpdesk_default
   ```
   If the network isn't `helpdesk_default`, set `KEYCLOAK_NETWORK` in `.env`.

3. Add the app to Keycloak. This creates the `vehicle-workflow` clients and roles, allows sign-in from the app's
   address, makes your help desk account a vehicle SYSTEM_ADMIN, and writes the backend's client secret into `.env`:
   ```sh
   cd <project-path>
   FE_ORIGINS=https://test.petrotrade.co.zw:8099,https://196.43.101.217:8099 ADMIN_USERNAME=<your help desk username> SECRET_FILE=.env \
     bash scripts/keycloak-setup.sh helpdesk-keycloak-1
   ```

4. Open port **8099** on the server firewall if needed, then start everything:
   ```sh
   docker compose pull
   docker compose up -d
   docker compose logs -f be        # wait for "Started ..."
   ```

5. Browse to https://test.petrotrade.co.zw:8099 and accept the certificate warning (self-signed, like the help desk).
   Sign in with your help desk account. Everyone else gets "awaiting access" until you give them a role on the
   Users page.

## Updating

1. On your PC: bump `IMAGE_TAG` in `.env`, then `docker compose build` and `docker compose push`.
2. On the server: set the same `IMAGE_TAG` in its `.env`, then `docker compose pull` and `docker compose up -d`.

The database lives in the `vehicle_db_data` volume and survives updates.

## Useful

```sh
docker compose ps
docker compose logs --tail 100 be
docker compose exec vehicle-db psql -U postgres vehiclefuelworkflow
docker compose exec vehicle-db pg_dump -U postgres vehiclefuelworkflow > backup-$(date +%F).sql
```
