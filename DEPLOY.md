# Subir RapiTrip (base + backend) a la nube

El SQL viejo (`BD_Principal.sql` y los `SP_*.sql` sueltos) **no alcanza** para la app actual.
Usa solo `sql/cloud/`:

1. `01_schema.sql` — tablas (incluye `google_id`, chat, push, pago VIP)
2. `02_procedures.sql` — stored procedures (incluye login Google e historial)
3. `03_seed.sql` — tarifas **Economico (id 1)** y **Comfort (id 2)** (VIP no se crea)
4. `04_migraciones.sql` — cambios futuros de columnas

## Primera vez (Railway, recomendado)

1. Crea cuenta en [railway.app](https://railway.app) con GitHub.
2. New Project → **GitHub repo** `taxi-backend` (sube esta carpeta a GitHub).
3. New → **Database** → **MySQL**.
4. En el servicio MySQL abre Query / consola y pega, en orden:
   - `01_schema.sql`
   - `02_procedures.sql`
   - `03_seed.sql`
5. En el servicio del backend (el del Dockerfile) agrega variables:

```
SPRING_DATASOURCE_URL=jdbc:mysql://HOST:PORT/MYSQLDATABASE
SPRING_DATASOURCE_USERNAME=MYSQLUSER
SPRING_DATASOURCE_PASSWORD=MYSQLPASSWORD
JWT_SECRET=una-clave-larga-nueva
GOOGLE_OAUTH_CLIENT_ID=667307810414-8tsvkpqdtaoc59oea7umg2ffeddflhbo.apps.googleusercontent.com
GOOGLE_MAPS_API_KEY=tu-key
APP_SECURITY_BETA_OPEN=true
PORT=8080
```

HOST/PORT/USER/PASSWORD salen de las variables del plugin MySQL de Railway (`MYSQLHOST`, `MYSQLPORT`, `MYSQLUSER`, `MYSQLPASSWORD`, `MYSQLDATABASE`).

6. Generate Domain. Prueba `https://tu-app.up.railway.app/health` → `{"app":"RapiTrip","status":"ok"}`.
7. En la app Android, `ApiConfig.kt`, pon esa URL (https y wss).

## Actualizar código (backend)

Cada `git push` a GitHub vuelve a construir el Dockerfile. No toca MySQL.

## Actualizar la base

Si cambias tablas:

1. Escribe el `ALTER` en `sql/cloud/04_migraciones.sql`
2. Ejecútalo en la consola MySQL de Railway (o `scripts/aplicar-sql.ps1 -SoloMigraciones`)
3. Luego push del backend si el Java también cambió

## Probar en tu PC con Docker

```
cd taxi-backend
docker compose up --build
```

MySQL queda en `localhost:3306` (se ejecutan solos 01, 02 y 03 la primera vez).
Backend en `http://localhost:8080/health`.

## Local sin Docker

Sigue usando `localhost` + usuario `root`. Las variables de entorno tienen esos valores por defecto.
