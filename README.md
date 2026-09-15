# Backend Taller Semillas

API REST del Sistema de Gestión Académica (SGA) para el jardín Taller Semillas. Se construyó a partir del frontend existente y de la especificación funcional: página pública, cuentas y roles, entrevistas, aspirantes, preinscripción, matrícula, alumnos y acudientes.

Está implementada con Java 25, Spring Boot 4.1.1, Maven, PostgreSQL, Flyway y Spring Security.

## Estado del proyecto

La primera integración funcional está lista para desarrollo local. La API `/api/v1` cubre los módulos que usa el frontend: autenticación, agenda de entrevistas, formularios configurables, aspirantes, alumnos, acudientes, historial médico básico, inscripciones y matrícula. También incorpora el flujo de admisión solicitado: entrevista, resultado, enlace de preinscripción de un solo uso con vencimiento, creación de inscripción y formalización de la matrícula anual.

Las pruebas automatizadas pasaron el 15 de septiembre de 2026: 17 pruebas, 0 errores. Incluyen el contexto Spring, reglas de dominio, autenticación, endpoints compatibles con el frontend y migraciones contra H2 en modo PostgreSQL. También se comprobó que el frontend compila para producción. Falta una prueba de integración contra una instancia PostgreSQL real antes del despliegue.

No están finalizados para uso productivo la carga de fotografías y documentos, las notificaciones por correo, las excepciones al horario y las comunicaciones directas. El módulo de historial médico se conserva porque el frontend ya lo declara, aunque no está expuesto en su menú actual.

## Arquitectura hexagonal

```text
src/main/java/com/tallersemillas/backend/
  domain/                         reglas y tipos de negocio
  application/port/               puertos de entrada y salida
  application/service/            casos de uso originales
  infrastructure/adapter/         adaptadores JPA, HTTP y seguridad originales
  school/domain/                  reglas del SGA integradas
  school/application/             casos de uso de gestión y admisiones
  school/application/port/        puertos del núcleo del SGA
  school/infrastructure/          JDBC, cifrado, controladores y seguridad
  configuration/                  configuración de Spring
```

El núcleo `school/domain` y sus puertos no dependen de Spring ni de JDBC. Los controladores REST, la persistencia PostgreSQL, la seguridad Bearer y el cifrado son adaptadores de infraestructura.

## Ejecutar localmente

Requiere JDK 25 y PostgreSQL 16 o superior. Crea una base `tallersemillas` y un usuario con permisos sobre ella. No se incluye una contraseña ni una base de datos de demostración.

Genera una clave de cifrado de 32 bytes y guárdala de forma segura. La misma clave es necesaria para leer los datos cifrados posteriormente.

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

En PowerShell, desde esta carpeta:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/tallersemillas'
$env:DB_USER = 'tallersemillas'
$env:DB_PASSWORD = 'una-clave-local-segura'
$env:DATA_ENCRYPTION_KEY = 'la-clave-base64-generada-arriba'
$env:FRONTEND_ORIGINS = 'http://localhost:5173'
$env:SPRING_PROFILES_ACTIVE = 'local'
.\mvnw.cmd -Peclipse-compiler spring-boot:run
```

Flyway aplica las migraciones `V1` a `V4` al iniciar. Haz respaldo de la base antes de actualizar una instalación que ya contenga datos. No pierdas ni cambies `DATA_ENCRYPTION_KEY` sin un procedimiento de rotación: los campos protegidos ya guardados no se podrán descifrar con otra clave.

## Ejecutar con Docker

Docker Compose crea dos contenedores en la misma red: `postgres` para la base de datos y `backend` para la API. El backend se conecta a PostgreSQL mediante el nombre interno `postgres`; no uses `localhost` como host de base de datos dentro del contenedor.

Primero crea un archivo `.env` a partir de `.env.example` y asigna una contraseña de PostgreSQL y una clave de cifrado Base64 de 32 bytes. Puedes generarla en PowerShell con:

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Después, desde la carpeta del backend:

```powershell
Copy-Item .env.example .env
# Edita .env y reemplaza DB_PASSWORD y DATA_ENCRYPTION_KEY.
docker compose up --build -d
docker compose ps
docker compose logs -f backend
```

La API queda disponible en `http://localhost:8080` y Flyway crea el esquema al arrancar. Para detener los contenedores sin borrar la base:

```powershell
docker compose down
```

Para eliminar también los datos locales de PostgreSQL, usa `docker compose down -v`.

Si quieres crear solo la imagen de la API, sin iniciar la base:

```powershell
docker build -t backendtallersemillas:local .
```

Esa imagen necesita una instancia PostgreSQL accesible y las variables `DB_URL`, `DB_USER`, `DB_PASSWORD` y `DATA_ENCRYPTION_KEY` para poder ejecutarse.

### Conectar el frontend en Docker

Aunque el servidor web del frontend se ejecuta en un contenedor, su JavaScript se ejecuta en el navegador de Windows. Por eso debe usar `VITE_API_URL=http://localhost:8080/api/v1`. Reconstrúyelo después de iniciar el backend:

```powershell
cd C:\Users\santi\Documents\GitHub\frontendtallersemillas\proy-semillas
docker build --build-arg VITE_API_URL=http://localhost:8080/api/v1 -t proysemillas .
docker rm -f proysemillas
docker run -d --name proysemillas -p 5173:80 proysemillas
```

## Verificar

```powershell
.\mvnw.cmd -Peclipse-compiler test
.\mvnw.cmd -Peclipse-compiler package
```

El perfil `eclipse-compiler` permite compilar Java 25 en el entorno Windows usado para esta entrega. En equipos donde el compilador estándar funcione se puede usar `./mvnw.cmd test`.

## API usada por el frontend

Configura `VITE_API_URL=http://localhost:8080/api/v1` en el frontend. Las rutas administrativas usan `Authorization: Bearer <token>`.

| Flujo | Rutas principales |
|---|---|
| Cuenta y sesión | `POST /auth/register`, `POST /auth/login`, `GET /auth/me`, `POST /auth/logout` |
| Entrevistas | `GET /time_slot/available_time_slots`, `POST /interview/create`, `PATCH /interview/{id}/result` |
| Preinscripción | `POST /interview/{id}/invitation`, `GET/POST /preinscription/{token}` |
| Matrícula | `POST /inscription/{id}/enrollments`, `GET /students/{id}/enrollments` |
| Gestión | `/student`, `/guardian`, `/medical_history`, `/form_schema`, `/time_slot`, `/interview`, `/inscription` |

El enlace de preinscripción vence a los siete días y queda inutilizable tras su envío. Una inscripción aprobada aún no convierte al estudiante en activo; la formalización anual de matrícula realiza ese cambio.

## Roles y primer usuario de gestión

Las cuentas públicas se crean con el rol `USUARIO`. Los roles `ADMINISTRADOR` y `DIRECTIVO` pueden usar los módulos de gestión ya implementados. Como los correos están cifrados, asigna inicialmente el rol mediante el identificador que responde el registro, después de verificar la identidad de la persona:

```sql
UPDATE accounts SET role = 'ADMINISTRADOR' WHERE id = 'uuid-verificado-de-la-cuenta';
```

Cierra e inicia sesión después de cambiar el rol. No hay usuarios, contraseñas ni administradores predeterminados.

## Protección de datos

La información sensible de alumnos, acudientes, entrevistas, inscripciones y expedientes se guarda cifrada con AES-GCM. Las búsquedas necesarias se apoyan en valores HMAC, y las escrituras generan eventos de auditoría. El límite de envíos públicos es de 30 solicitudes por IP cada 15 minutos como protección inicial.

Antes de un despliegue real aún se deben definir y operar HTTPS, copias de seguridad con restauración verificada, monitoreo, una política de retención, recuperación de contraseña, verificación de correo y una estrategia de almacenamiento de archivos. Nunca subas `DB_PASSWORD`, `DATA_ENCRYPTION_KEY` ni datos reales al repositorio.
