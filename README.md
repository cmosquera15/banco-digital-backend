[![CI Pipeline - Banco Digital Backend](https://github.com/FabricaEscuela2026-EAP13/banco-digital-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/FabricaEscuela2026-EAP13/banco-digital-backend/actions/workflows/ci.yml)[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=FabricaEscuela2026-EAP13_banco-digital-backend&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=FabricaEscuela2026-EAP13_banco-digital-backend)[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=FabricaEscuela2026-EAP13_banco-digital-backend&metric=bugs)](https://sonarcloud.io/summary/new_code?id=FabricaEscuela2026-EAP13_banco-digital-backend)[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=FabricaEscuela2026-EAP13_banco-digital-backend&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=FabricaEscuela2026-EAP13_banco-digital-backend)[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=FabricaEscuela2026-EAP13_banco-digital-backend&metric=coverage)](https://sonarcloud.io/summary/new_code?id=FabricaEscuela2026-EAP13_banco-digital-backend)

# Banco Digital Backend

Backend REST para el proyecto Banco Digital (CodeFactory UdeA), construido con Spring Boot, PostgreSQL y seguridad JWT.

## Estado Actual

Este repositorio ya incluye:

- Autenticacion JWT stateless.
- Registro de usuarios cliente.
- Login con emision de token.
- HU3 implementada: actualizacion de datos personales del cliente autenticado.
- Manejo global de errores con formato estandar.
- Auditoria JPA (`created_at`, `updated_at`).
- Versionamiento de BD con Flyway y migracion inicial de indices.
- Swagger/OpenAPI con `Authorize` para probar endpoints protegidos.

## Stack Tecnico

- Java 21
- Spring Boot 3.4.4
- Spring Web, Spring Data JPA, Spring Security, Spring HATEOAS
- JWT (jjwt)
- PostgreSQL (Neon)
- Flyway
- Springdoc OpenAPI (Swagger UI)
- Actuator + Prometheus
- Maven Wrapper
- Arquitectura en capas

## Estructura Principal

```text
src/main/java/co/edu/udea/bancodigital
	config/
	controllers/
	dtos/
	exception/
	models/
	repositories/
	services/

src/main/resources
	application.properties
	db/migration/
```

## Requisitos Previos

- JDK 21
- Maven (opcional, se recomienda usar `mvnw`)
- PostgreSQL accesible (actualmente configurado para Neon)

## Variables y Configuracion

La aplicacion requiere al menos esta variable de entorno:

- `JWT_SECRET_KEY`: clave Base64 usada para firmar JWT.

Ejemplo en PowerShell:

```powershell
$env:JWT_SECRET_KEY="TU_CLAVE_BASE64_AQUI"
```

Notas:

- La conexion a BD esta definida en `application.properties` para entorno de desarrollo.
- Para despliegue real, se recomienda sobreescribir credenciales por variables de entorno (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`).

## Variables de entorno requeridas

- `SPRING_DATASOURCE_URL`
- `DATASOURCE_USERNAME`
- `DATASOURCE_PASSWORD`
- `APP_JWT_SECRET`

## Ejecutar Proyecto

### Windows (PowerShell)

```powershell
$env:JWT_SECRET_KEY="TU_CLAVE_BASE64_AQUI"
.\mvnw.cmd spring-boot:run
```

### Linux/macOS

```bash
export JWT_SECRET_KEY="TU_CLAVE_BASE64_AQUI"
./mvnw spring-boot:run
```

La API queda disponible en:

- Base URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Produccion:

- Base URL: `https://banco-digital-backend-u3ts.onrender.com`
- Swagger UI: `https://banco-digital-backend-u3ts.onrender.com/swagger-ui.html`

## Migraciones con Flyway

Flyway esta habilitado y se ejecuta en arranque.

- Carpeta de migraciones: `src/main/resources/db/migration`
- Migracion actual: `V12__drop_obsolete_transaction_date_indexes.sql`
- Tabla de control en BD: `flyway_schema_history`
- Las primeras migraciones documentan y ajustan un esquema que ya existia en Neon. No deben editarse si ya fueron aplicadas, porque Flyway valida checksums.
- Para una base completamente vacia, se debe crear primero el esquema base o definir una migracion baseline consolidada antes de ejecutar las migraciones incrementales.

Consulta util para validar estado:

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

## Seguridad

- Endpoints publicos:
	- `POST /api/v1/auth/login`
	- `POST /api/v1/usuarios/registro`
	- Rutas de Swagger/OpenAPI
- Endpoints protegidos:
	- `PUT /api/v1/usuarios/me`
	- Cualquier otra ruta no marcada como publica
- Roles:
	- Soporte para RBAC basado en catalogo `roles`.

Para endpoints protegidos usar header:

```http
Authorization: Bearer <token>
```

## Endpoints disponibles

| Metodo | Ruta | Acceso | Descripcion |
|--------|------|--------|-------------|
| POST | /api/v1/auth/login | Publico | Iniciar sesion, retorna JWT Bearer |
| POST | /api/v1/usuarios/registro | Publico | Registrar nuevo cliente |
| PUT | /api/v1/usuarios/me | CLIENTE | Actualizar datos personales |
| POST | /api/v1/cuentas | CLIENTE | Crear cuenta bancaria |
| GET | /api/v1/cuentas/me | CLIENTE | Listar mis cuentas |
| GET | /api/v1/cuentas/{idCuenta}/saldo | CLIENTE | Consultar saldo de una cuenta |
| GET | /api/v1/admin/clientes | ADMIN | Listar todos los clientes |
| GET | /api/v1/admin/cuentas | ADMIN | Listar todas las cuentas del sistema |

Los endpoints protegidos requieren header: Authorization: Bearer <token>

## Autenticacion

El sistema usa JWT. El token se obtiene en `POST /api/v1/auth/login` y debe enviarse como Bearer token en el header `Authorization` de cada request protegido.

## Base de datos

- Motor: PostgreSQL en Neon (serverless)
- Migraciones gestionadas con Flyway
- Tablas: usuarios, cuentas, transacciones, roles, tipos_cuenta, estados_cuenta, tipos_documento, tipos_transaccion

## CI/CD

- Pipeline en GitHub Actions (.github/workflows/ci.yml)
- Trigger: push y PR hacia develop y main
- Ejecuta mvn verify con Java 21 Temurin
- 8 tests automatizados

## Endpoints Implementados

### 1) Registro de usuario

`POST /api/v1/usuarios/registro`

Request ejemplo:

```json
{
	"idTipoDoc": 1,
	"numeroDocumento": "1032456789",
	"nombre": "Camilo",
	"primerApellido": "Mosquera",
	"segundoApellido": "Lopez",
	"direccion": "Calle 10 #20-30",
	"telefono": "3001234567",
	"correo": "camilo@example.com",
	"contrasena": "ClaveSegura1!"
}
```

### 2) Login

`POST /api/v1/auth/login`

Request ejemplo:

```json
{
	"correo": "camilo@example.com",
	"contrasena": "ClaveSegura1!"
}
```

Response ejemplo:

```json
{
	"token": "<jwt>",
	"tipo": "Bearer",
	"nombre": "Camilo",
	"correo": "camilo@example.com",
	"idRol": 2,
	"rol": "CLIENTE"
}
```

### 3) HU3 - Actualizar datos cliente

`PUT /api/v1/usuarios/me` (requiere JWT)

Request ejemplo:

```json
{
	"nombre": "Camilo Andres",
	"primerApellido": "Mosquera",
	"segundoApellido": "Lopez",
	"direccion": "Cra 45 #50-20",
	"telefono": "3009876543",
	"correo": "camilo.andres@example.com"
}
```

Comportamiento implementado:

- Actualiza solo los campos personales permitidos.
- Valida formatos (correo, telefono, nombres).
- Impide correo duplicado.
- Mantiene datos sensibles fuera de la operacion.
- Retorna `updatedAt` para trazabilidad.

## HU3 - Criterios funcionales cubiertos

### Escenario exitoso

- Cliente autenticado.
- Modifica datos personales validos.
- El sistema persiste cambios y responde exito.

### Escenario no exitoso

- Cliente autenticado.
- Envia datos invalidos.
- El sistema rechaza con `400 Bad Request` y detalle de validacion.

## Formato de Errores

Las respuestas de error siguen un formato estandar:

```json
{
	"errorCode": "VALIDATION_ERROR",
	"message": "Error de validacion en la solicitud",
	"details": "telefono: El telefono debe ser celular colombiano: 10 digitos iniciando en 3",
	"traceId": "uuid",
	"timestamp": "2026-04-05T13:20:00"
}
```

## Comandos Utiles

```bash
# Compilar y validar
./mvnw -DskipTests validate

# Ejecutar pruebas
./mvnw test
```

En Windows reemplazar `./mvnw` por `.\\mvnw.cmd`.

## Proximos pasos

- Agregar migracion Flyway V2 para procedimiento almacenado.
- Aumentar cobertura automatizada para HU3 (unitaria e integracion).
- Externalizar secretos y credenciales en todos los ambientes.
