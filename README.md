# AniHoyo

AniHoyo monorepo containing the Spring Boot backend, React/Vite frontend, and
the deployment configuration for the complete stack.

## Repository layout

- `AniHoyo-backend-server/`: Spring Boot API and Maven wrapper.
- `AniHoyo-frontend-src/`: React/Vite application and npm lockfile.
- `deploy/`: Nginx configuration, database migrations, and storage tooling.
- `docker-compose.yml`: full application stack.
- `.env.example`: configuration template; copy it to `.env` and supply local secrets.
- `data/uploads/`: local runtime files; only `.gitkeep` is versioned.

See [DEPLOY.md](DEPLOY.md) for configuration and deployment instructions.

## Git workflow

Run Git commands from this directory. Backend, frontend, and deployment changes
are committed together in this repository; the application directories are
ordinary folders, not submodules or independent repositories.

The consolidation commit retains both original repositories' commit histories
as parents, including commits that had not yet been pushed to GitHub. Existing
uncommitted source changes are included in its snapshot. Earlier commits use
the original repositories' directory layouts.

Local `.env` files, dependencies, build output, and runtime uploads are ignored.
