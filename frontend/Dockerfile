# --- Build stage: install deps and produce the static production build ---
FROM node:24-alpine AS build
WORKDIR /build

# Copy only the manifest/lockfile first so `npm ci` is cached independently
# of source changes.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Vite bakes VITE_-prefixed vars into the static bundle at BUILD time, not
# runtime - once main.js is generated, the value can't be changed by an
# environment variable at container start. This must be the URL the user's
# BROWSER will call directly (the backend's host-mapped address), not the
# internal Docker service name, since the browser never joins the compose
# network. Passed in via docker-compose's build.args.
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

RUN npm run build

# --- Runtime stage: slim Nginx serving the static build ---
FROM nginx:1.27-alpine AS runtime

# Grant the nginx binary the specific capability needed to bind port 80
# directly, so the process can run as the existing non-root "nginx" user
# instead of needing a root master process (the usual reason Nginx images
# default to root). Docker's default container capability set already
# includes CAP_NET_BIND_SERVICE in the bounding set, so a non-root exec can
# pick it up via this file capability.
RUN apk add --no-cache libcap \
    && setcap 'cap_net_bind_service=+ep' /usr/sbin/nginx \
    && apk del libcap

COPY nginx.conf /etc/nginx/conf.d/default.conf

# Re-point the pid file under /tmp (writable by a non-root user, unlike
# /run) and drop the "user nginx;" directive - it's only meaningful for a
# root master dropping privileges for its workers, which no longer applies
# since the whole process now starts as "nginx" already.
RUN mkdir -p /tmp/nginx \
    && sed -i -e '/^user  *nginx;/d' -e 's#pid\s*/run/nginx.pid;#pid /tmp/nginx/nginx.pid;#' /etc/nginx/nginx.conf \
    && chown -R nginx:nginx /var/cache/nginx /var/log/nginx /tmp/nginx /usr/share/nginx/html

COPY --from=build --chown=nginx:nginx /build/dist /usr/share/nginx/html

USER nginx

EXPOSE 80

ENTRYPOINT ["nginx", "-g", "daemon off;"]
