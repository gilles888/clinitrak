# =============================================================================
# Vhost Nginx — clinitrak.gilmotech.be
# Géré via le repo : scripts/nginx/clinitrak.gilmotech.be
# SSL : géré par Certbot (Let's Encrypt — auto-renew via systemd timer)
# =============================================================================

server {
    server_name clinitrak.gilmotech.be;

    # Frontend Angular — SPA statique
    root /home/claude-worker/clinitrak/web;
    index index.html;

    server_tokens off;

    # ── Compression Gzip ──────────────────────────────────────────────────────
    gzip            on;
    gzip_vary       on;
    gzip_proxied    any;
    gzip_comp_level 6;
    gzip_min_length 1024;
    gzip_types
        text/plain text/css text/xml
        application/json application/javascript
        application/xml+rss application/atom+xml
        image/svg+xml font/woff font/woff2;

    # ── En-têtes de sécurité ──────────────────────────────────────────────────
    add_header X-Frame-Options           "SAMEORIGIN"                          always;
    add_header X-Content-Type-Options    "nosniff"                             always;
    add_header X-XSS-Protection          "1; mode=block"                       always;
    add_header Referrer-Policy           "strict-origin-when-cross-origin"     always;
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # ── Assets statiques (JS/CSS/fonts/images) — cache long ──────────────────
    location ~* \.(js|css|woff|woff2|ttf|eot|ico|png|jpg|jpeg|gif|svg|webp)$ {
        expires 1y;
        add_header Cache-Control          "public, immutable";
        add_header X-Frame-Options        "SAMEORIGIN"  always;
        add_header X-Content-Type-Options "nosniff"     always;
        try_files $uri =404;
    }

    # ── API Gateway (Spring Cloud Gateway, port 8080) ─────────────────────────
    location /api/ {
        proxy_pass         http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_set_header   Connection        "";
        proxy_connect_timeout  30s;
        proxy_send_timeout     60s;
        proxy_read_timeout    300s;

        # SSE (Server-Sent Events) — désactiver le buffering
        proxy_buffering           off;
        proxy_cache               off;
        chunked_transfer_encoding on;
    }

    # ── Swagger UI ────────────────────────────────────────────────────────────
    location /swagger-ui/ {
        proxy_pass       http://localhost:8080/swagger-ui/;
        proxy_set_header Host $host;
    }

    location /v3/api-docs {
        proxy_pass       http://localhost:8080/v3/api-docs;
        proxy_set_header Host $host;
    }

    # ── Certbot ACME challenge ────────────────────────────────────────────────
    location /.well-known/acme-challenge/ {
        root /var/www/html;
    }

    # ── SPA Angular — fallback index.html pour le routing côté client ─────────
    location / {
        try_files $uri $uri/ /index.html;
    }

    location = /favicon.ico { log_not_found off; access_log off; }
    location = /robots.txt  { log_not_found off; access_log off; }

    # ── TLS — géré par Certbot ────────────────────────────────────────────────
    listen [::]:443 ssl;
    listen 443 ssl;
    ssl_certificate     /etc/letsencrypt/live/clinitrak.gilmotech.be/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/clinitrak.gilmotech.be/privkey.pem;
    include             /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam         /etc/letsencrypt/ssl-dhparams.pem;
}

# ── Redirection HTTP → HTTPS ──────────────────────────────────────────────────
server {
    server_name clinitrak.gilmotech.be;
    listen 80;
    listen [::]:80;

    location /.well-known/acme-challenge/ {
        root /var/www/html;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}
