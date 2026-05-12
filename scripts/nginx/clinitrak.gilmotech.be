# CliniTrak — Configuration Nginx
# Fichier source : scripts/nginx/clinitrak.gilmotech.be
# Déploiement    : /etc/nginx/sites-available/clinitrak.gilmotech.be
#
# Activer avec :
#   sudo ln -s /etc/nginx/sites-available/clinitrak.gilmotech.be \
#              /etc/nginx/sites-enabled/clinitrak.gilmotech.be
#   sudo nginx -t && sudo systemctl reload nginx

# ── Redirect HTTP → HTTPS ──────────────────────────────────────────────────
server {
    listen 80;
    listen [::]:80;
    server_name clinitrak.gilmotech.be;
    return 301 https://$host$request_uri;
}

# ── HTTPS ──────────────────────────────────────────────────────────────────
server {
    listen 443 ssl;
    listen [::]:443 ssl;
    server_name clinitrak.gilmotech.be;

    # SSL — certificat Let's Encrypt (certbot --nginx)
    ssl_certificate     /etc/letsencrypt/live/clinitrak.gilmotech.be/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/clinitrak.gilmotech.be/privkey.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    # Frontend Angular — SPA statique
    root /home/claude-worker/clinitrak/clinitrak-frontend/dist/clinitrak-frontend/browser;
    index index.html;

    # ── API Gateway (Spring Cloud Gateway, port 8080) ──────────────────────
    location /api/ {
        proxy_pass         http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;

        # SSE (Server-Sent Events) — désactiver le buffering
        proxy_buffering            off;
        proxy_cache                off;
        chunked_transfer_encoding  on;
    }

    # ── Swagger UI (documentation API) ────────────────────────────────────
    # Commenter en production si non souhaité publiquement
    location /swagger-ui/ {
        proxy_pass       http://localhost:8080/swagger-ui/;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /v3/api-docs {
        proxy_pass       http://localhost:8080/v3/api-docs;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # ── MinIO Console (port 9001) ──────────────────────────────────────────
    # Restreindre par IP en production (décommentez allow/deny)
    location /minio/ {
        # allow 10.0.0.0/8;   # VPN interne uniquement
        # deny  all;
        proxy_pass              http://localhost:9001/;
        proxy_set_header        Host $host;
        proxy_http_version      1.1;
        proxy_set_header        Upgrade $http_upgrade;
        proxy_set_header        Connection "upgrade";
    }

    # ── SPA fallback — toutes les routes Angular ───────────────────────────
    location / {
        try_files $uri $uri/ /index.html;
    }

    # ── Compression Gzip ───────────────────────────────────────────────────
    gzip            on;
    gzip_vary       on;
    gzip_min_length 1024;
    gzip_types      text/plain text/css text/xml
                    application/json application/javascript
                    application/xml+rss application/atom+xml
                    image/svg+xml;

    # ── Headers de sécurité ────────────────────────────────────────────────
    add_header X-Frame-Options        "SAMEORIGIN"  always;
    add_header X-Content-Type-Options "nosniff"     always;
    add_header X-XSS-Protection       "1; mode=block" always;
    add_header Referrer-Policy        "strict-origin-when-cross-origin" always;
}
