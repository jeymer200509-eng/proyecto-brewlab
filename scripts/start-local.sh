#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
docker compose up --build -d
printf 'BrewLab: http://localhost:8080\nVer arranque: docker compose logs -f brewlab\n'
