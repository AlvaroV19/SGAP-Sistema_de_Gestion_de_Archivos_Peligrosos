# SGAP - Sistema de Gestión de Archivos Peligrosos

SGAP es una plataforma desarrollada para la gestión segura de archivos potencialmente peligrosos, permitiendo el análisis, almacenamiento, cuarentena y administración de documentos mediante controles de seguridad, procesamiento asíncrono y control de acceso basado en roles.

El sistema implementa mecanismos de autenticación JWT, análisis de archivos con Apache Tika, almacenamiento distribuido con MinIO y persistencia de datos con PostgreSQL.

---

# Arquitectura General

El proyecto está compuesto por:

- **Frontend:** Aplicación desarrollada con Vue.js + Vite
- **Backend:** API REST desarrollada con Spring Boot
- **Base de Datos:** PostgreSQL
- **Almacenamiento de Archivos:** MinIO
- **Procesamiento de Archivos:** Apache Tika
- **Autenticación:** JWT + Spring Security

---

# Tecnologías Utilizadas

## Backend
- Java 17
- Spring Boot 4
- Spring Security
- Spring Data JPA
- JWT (jjwt)
- PostgreSQL
- MinIO SDK
- Apache Tika
- OpenPDF
- Lombok
- Maven

## Frontend
- Vue.js
- Vite
- JavaScript
- Node.js
- npm

## Infraestructura
- Docker
- Docker Compose

---

# Estructura del Proyecto

```txt
SGAP/
│
├── archivos_de_prueba/
│   ├── HIGH_payload.com
│   ├── LOW_documento.txt
│   └── MEDIUM_factura.pdf.txt
│
├── frontend-sgap/
│   ├── public/
│   ├── router/
│   ├── src/
│   ├── package.json
│   └── vite.config.js
│
├── sgap-security/
│   ├── src/
│   ├── storage/
│   ├── docker-compose.yml
│   ├── pom.xml
│   └── target/
│
└── README.md
```

---

# Requisitos Previos

Antes de ejecutar el proyecto, asegúrate de tener instalado:

- Java JDK 17 o superior
- Maven
- Node.js y npm
- Docker Desktop
- Git

---

# Configuración del Backend

## Tecnologías del Backend

El backend utiliza las siguientes dependencias principales:

- Spring Boot 4
- Spring Security
- JWT Authentication
- PostgreSQL
- MinIO
- Apache Tika
- OpenPDF

---

# Configuración de Variables

El proyecto utiliza configuración mediante `application.properties`.

## Base de Datos

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/sgap_db
spring.datasource.username=postgres
spring.datasource.password=12345
```

---

## Configuración MinIO

```properties
minio.url=http://localhost:9000
minio.access-key=minioadmin
minio.secret-key=minioadmin
minio.bucket=sgap
```

---

## Configuración JWT

```properties
jwt.secret=sgap-super-secret-key-change-in-production-32chars!!
jwt.expiration-ms=86400000
```

---

# Ejecución del Proyecto

# 1. Clonar el repositorio

```bash
git clone https://github.com/AlvaroV19/SGAP-Sistema_de_Gestion_de_Archivos_Peligrosos.git
cd SGAP
```

---

# 2. Ejecutar Docker Desktop

Asegúrate de tener Docker Desktop en ejecución antes de iniciar el backend.

---

# 3. Levantar PostgreSQL y MinIO

Ubícate en la carpeta:

```bash
cd sgap-security
```

Ejecuta:

```bash
docker compose up
```

Esto iniciará:

- PostgreSQL
- MinIO
- Servicios necesarios del backend

---

# 4. Ejecutar el Backend

Desde la carpeta `sgap-security`, ejecutar:

## Desde IntelliJ IDEA

Ejecutar la clase:

```txt
SgapApplication
```

## Desde terminal

```bash
./mvnw spring-boot:run
```

o en Windows:

```bash
mvn spring-boot:run
```

---

# 5. Ejecutar el Frontend

Abrir otra terminal y ubicarse en:

```bash
cd frontend-sgap
```

## Instalar dependencias

```bash
npm install
```

## Ejecutar aplicación

```bash
npm run dev
```

---

# Accesos del Sistema

## Frontend

```txt
http://localhost:5173/
```

---

## Consola MinIO

```txt
http://localhost:9001/
```

---

# Credenciales del Sistema

## Usuarios del Sistema

| Usuario | Contraseña | Rol |
|----------|------------|------|
| admin | Admin@2025! | ROLE_ADMIN |
| analyst | Analyst@2025! | ROLE_ANALYST |
| user | User@2025! | ROLE_USER |

---

## Credenciales MinIO

| Usuario | Contraseña |
|----------|------------|
| minioadmin | minioadmin |

---

# Roles del Sistema

## ROLE_ADMIN
Permisos completos del sistema:

- Gestión de usuarios
- Administración de archivos
- Control total del sistema
- Acceso administrativo

---

## ROLE_ANALYST
Funciones de análisis y revisión:

- Análisis de archivos
- Revisión de amenazas
- Gestión operativa limitada

---

## ROLE_USER
Acceso básico al sistema:

- Carga de archivos
- Consulta de información permitida
- Interacción básica con la plataforma

---

# Funcionalidades Principales

- Subida segura de archivos
- Procesamiento asíncrono
- Análisis de contenido con Apache Tika
- Gestión de cuarentena
- Almacenamiento distribuido con MinIO
- Autenticación JWT
- Control de acceso por roles
- Gestión de amenazas
- Persistencia con PostgreSQL
- Generación de reportes PDF

---

# Configuración de Seguridad

El sistema implementa:

- JWT Authentication
- Spring Security
- Control de roles
- CORS Configuration
- Procesamiento asíncrono seguro
- Cuarentena de archivos sospechosos

---

# Archivos de Prueba

La carpeta:

```txt
archivos_de_prueba/
```

contiene ejemplos de archivos para validar el funcionamiento del análisis de seguridad:

- HIGH → Riesgo alto
- MEDIUM → Riesgo medio
- LOW → Riesgo bajo

---

# Notas Importantes

- Verificar que Docker Desktop esté activo antes de iniciar el backend.
- Asegurarse de que los puertos `5173`, `5432`, `9000` y `9001` estén disponibles.
- No subir la carpeta `target/` al repositorio.
- No subir `node_modules/`.
- Utilizar `.gitignore` correctamente para evitar archivos compilados.

---

# Recomendación de .gitignore

```gitignore
# Backend
target/
*.class

# Frontend
node_modules/
dist/

# IDEs
.idea/
.vscode/

# Logs
*.log
```

---

# Autor

Proyecto académico desarrollado para la gestión segura de archivos peligrosos utilizando arquitectura cliente-servidor, procesamiento asíncrono y almacenamiento distribuido.
