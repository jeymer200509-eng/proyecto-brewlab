#!/bin/sh
set -eu
config_dir=/usr/local/tomcat/webapps/ROOT/WEB-INF/config
mkdir -p "$config_dir"
# Preserve credentials across container recreation. Never overwrite existing config.
if [ ! -f "$config_dir/brewlab.properties" ]; then
  umask 077
  secret=$(head -c 48 /dev/urandom | base64 | tr -d '\n')
  {
    printf 'jwt.secret=%s\n' "$secret"
    printf 'jwt.ttlMinutes=720\napp.timezone=America/Bogota\n'
    # Existing local SQL fallback; no external MySQL is required for first launch.
    printf 'db.url=jdbc:mysql://127.0.0.1:3306/brewlab?connectTimeout=1000\n'
  } > "$config_dir/brewlab.properties"
fi
exec catalina.sh run
