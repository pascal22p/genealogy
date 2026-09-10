# Genealogy

A web-based genealogy management platform built with Scala 3 and the Play Framework. It enables users to explore, manage, and visualize family trees, individual records, family relations, media, and source citations across multiple genealogy databases.

---

### Features

- **Multi-Tree Management**: Host and manage multiple independent genealogy databases (`baseId`).
- **Rich Genealogical Data**: Track individuals, families, events, attributes, notes, and addresses.
- **Tree Visualizations & Exports**:
  - Interactive ancestor and descendant tree views.
  - Export genealogical trees in multiple formats: **SVG**, **PNG**, **PDF** (via Apache FOP), and **Graphviz DOT**.
- **GEDCOM Support**: Import standard GEDCOM files to populate genealogical databases.
- **Source & Citation Management**: Manage repositories, source records, and citations to preserve genealogical evidence.
- **Media Management**: Upload, link, and display photographs, documents, and media attachments.
- **Internationalization (i18n)**: Bilingual interface supporting **English** (`en`) and **French** (`fr`).
- **User Authentication**: Secure session-based authentication with password hashing via Password4j.
- **SEO & Discovery**: Automated dynamic sitemaps for individuals, families, events, surnames, and firstnames.
- **Observability**: Built-in OpenTelemetry instrumentation and optional Pyroscope continuous profiling support.

---

### Tech Stack

- **Language**: [Scala 3](https://www.scala-lang.org/) (3.9.x)
- **Framework**: [Play Framework 3.0](https://www.playframework.com/) (Twirl templates, Guice DI)
- **UI Components**: HMRC Play Frontend / GOV.UK Design System
- **Database Access**: [Anorm](https://playframework.github.io/anorm/) with [MariaDB Connector/J](https://mariadb.com/kb/en/about-mariadb-connector-j/)
- **Rendering Engines**: [Graphviz](https://graphviz.org/) & [Apache FOP](https://xmlgraphics.apache.org/fop/)
- **Build Tool**: [sbt](https://www.scala-sbt.org/)
- **Containerization**: Docker & Docker Compose (Eclipse Temurin JDK base image)

---

### Prerequisites

To run this application locally, ensure you have installed:

- **Java JDK**: 21 or later (Eclipse Temurin recommended)
- **sbt**: 1.9+
- **MariaDB** or **MySQL**: 10.5+ / 8.0+
- **Graphviz**: Required for generating tree graphs (`dot` executable must be in your `PATH`)

---

### Configuration

The application is configured via `conf/application.conf` and can be overridden using environment variables:

| Environment Variable | Description | Default / Example |
|---|---|---|
| `APP_SECRET` | Play application secret key for cryptographic signing | auto-generated / random string |
| `DB_URL` | JDBC connection URL for MariaDB | `jdbc:mariadb://localhost:3306/genealogie?createDatabaseIfNotExist=true...` |
| `DB_USER` | Database username | `root` |
| `DB_PASSWORD` | Database password | `example` |
| `PLAY_FILTERS_HOSTS` | Allowed HTTP host filter | `localhost:9123` |
| `PROTOCOL` | Application protocol (`http://` or `https://`) | `http://` |
| `APP_NAME` | Application display name | `Localhost` |
| `MEDIA_PATH` | Path where media files are stored on disk | `./Medias/` |
| `UPLOAD_PATH` | Temporary upload directory | `./Upload/` |
| `EXTERNAL_ASSETS_PATH` | Path to external public assets | `./external-public/` |
| `IS_SESSION_SECURE` | Enforce secure cookies for sessions | `false` |
| `CUSTOM_JS_FILE` | Optional path to custom client JavaScript | `""` |
| `PYROSCOPE_AGENT_ENABLED` | Enable Pyroscope profiling | `false` |
| `PYROSCOPE_SERVER_ADDRESS`| Pyroscope server endpoint | `http://localhost:4040` |

---

### Quick Start

#### 1. Database Setup

Initialize your MariaDB/MySQL database using the schema provided in `doc/tables.sql`:

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS genealogie CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p genealogie < doc/tables.sql
```

#### 2. Run Locally with sbt

```bash
# Start the development server (default port: 9123)
sbt run
```

Open your browser and navigate to `http://localhost:9123`.

#### 3. Run with Docker Compose

A `docker-compose.yml` file is provided for containerized deployments:

```bash
# Copy and configure environment variables
export APP_SECRET="your-secure-app-secret"
export DB_URL="jdbc:mariadb://db_host:3306/genealogie"
export DB_USER="db_user"
export DB_PASSWORD="db_password"

# Launch the container
docker compose up -d
```

---

### Development & Testing

#### Run Tests

```bash
# Run the test suite
sbt test

# Run tests with code coverage
sbt clean coverage test coverageReport
```

#### Code Formatting & Linters

```bash
# Check code formatting with Scalafmt
sbt scalafmtCheck

# Reformat code automatically
sbt scalafmt
```

#### Build Docker Image

```bash
# Build local Docker image
sbt docker:publishLocal
```

---

### License

This project is licensed under the terms defined in the [LICENSE](LICENSE) file.
