# CliniTrak — Configuration Nginx (HTTP temporaire, en attente DNS + certbot)
# Fichier source : scripts/nginx/clinitrak.gilmotech.be
# Déploiement    : /etc/nginx/sites-available/clinitrak.gilmotech.be
#
# Une fois le DNS configuré et certbot exécuté, ce fichier sera remplacé
# automatiquement par la version HTTPS via certbot --nginx.

server {
    listen 80;
    listen [::]:80;
    server_name clinitrak.gilmotech.be _;

    # Frontend Angular — SPA statique
    root /home/claude-worker/clinitrak/web;
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
    location /swagger-ui/ {
        proxy_pass       http://localhost:8080/swagger-ui/;
        proxy_set_header Host $host;
    }

    location /v3/api-docs {
        proxy_pass       http://localhost:8080/v3/api-docs;
        proxy_set_header Host $host;
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
