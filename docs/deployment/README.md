# Deployment guide

## Local Docker Compose

```powershell
docker compose up --build
docker compose ps
```

Verify:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
Invoke-RestMethod http://localhost:8080/api/v1/simulations
```

## Ubuntu VM target

1. Install Docker Engine and the Compose plugin from Docker's official repository.
2. Clone the repository at the exact release commit.
3. Create `.env` from `.env.example` and replace all development credentials.
4. Configure DNS and TLS termination. The checked-in Nginx config is an HTTP development baseline.
5. Run `docker compose pull` and `docker compose up -d --build`.
6. Capture `docker compose ps`, health output, migration history, and the exact commit for report evidence.
7. Configure automated PostgreSQL backups and test restoration before calling the deployment production-ready.

Never commit the VM `.env`, TLS private keys, provider credentials, database backups, or student exports.
