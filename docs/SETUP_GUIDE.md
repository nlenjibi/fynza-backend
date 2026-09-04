# Enterprise CI/CD Setup Guide

## Prerequisites

### Local Development
- Docker & Docker Compose (v3.9+)
- Java 17 (for local backend development)
- Node.js 20 (for local frontend development)
- Git with SSH configured
- GitHub CLI (optional)

### GitHub Configuration
- Repository with main and dev branches
- Branch protection rules configured
- GitHub Secrets configured
- GitHub Actions enabled

### External Services
- Vercel account with project created
- Container registry access (GHCR)
- Optional: SonarQube instance
- Optional: Slack workspace

## Step 1: Repository Setup

### Clone Repository
```bash
git clone <repository-url>
cd <repository-name>
```

### Create Branches
```bash
# Create dev branch if not exists
git checkout -b dev
git push origin dev

# Create feature branch for development
git checkout -b feature/initial-setup
```

### Configure Git
```bash
# Set up Git user
git config user.name "Your Name"
git config user.email "your.email@example.com"

# Enable GPG signing (optional but recommended)
git config commit.gpgsign true
```

## Step 2: GitHub Configuration

### Branch Protection Rules

**For dev branch:**
1. Go to Settings → Branches
2. Click "Add rule"
3. Branch name pattern: `dev`
4. Enable:
   - Require a pull request before merging
   - Require 1 approval
   - Require status checks to pass
   - Require branches to be up to date
   - Dismiss stale pull request approvals

**For main branch:**
1. Branch name pattern: `main`
2. Enable:
   - Require a pull request before merging
   - Require 2 approvals
   - Require status checks to pass
   - Require branches to be up to date
   - Require signed commits
   - Dismiss stale pull request approvals

### GitHub Secrets

Go to Settings → Secrets and variables → Actions

Add these secrets:

```
VERCEL_TOKEN=<your-vercel-token>
VERCEL_ORG_ID=<your-vercel-org-id>
VERCEL_PROJECT_ID=<your-vercel-project-id>
SONAR_HOST_URL=<optional-sonarqube-url>
SONAR_TOKEN=<optional-sonarqube-token>
```

**How to get Vercel credentials:**
1. Go to https://vercel.com/account/tokens
2. Create new token
3. Copy token to VERCEL_TOKEN
4. Get ORG_ID from account settings
5. Get PROJECT_ID from project settings

## Step 3: Local Development Setup

### Environment Configuration

```bash
# Copy environment template
cp .env.example .env

# Edit .env with your values
nano .env
```

**Example .env:**
```
DB_USER=appuser
DB_PASSWORD=apppassword
DB_NAME=appdb
SPRING_PROFILE=dev
NODE_ENV=development
NEXT_PUBLIC_API_URL=http://localhost:8080
GRAFANA_PASSWORD=admin
```

### Start Services

```bash
# Build and start all services
docker-compose up --build

# Or run in background
docker-compose up -d --build

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f backend
```

### Verify Services

```bash
# Frontend
curl http://localhost:3000

# Backend
curl http://localhost:8080/actuator/health

# Database
psql -h localhost -U appuser -d appdb

# Redis
redis-cli -h localhost ping

# Prometheus
curl http://localhost:9090

# Grafana
curl http://localhost:3001

# Loki
curl http://localhost:3100/loki/api/v1/labels
```

## Step 4: Backend Setup

### Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/
│   │   │       ├── controller/
│   │   │       ├── service/
│   │   │       ├── repository/
│   │   │       ├── model/
│   │   │       └── Application.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/
│   └── test/
├── pom.xml
├── Dockerfile
└── .dockerignore
```

### Configure pom.xml

```bash
# Copy template
cp backend/pom.xml.template backend/pom.xml

# Edit with your project details
nano backend/pom.xml
```

### Create Application Properties

**application.yml:**
```yaml
spring:
  application:
    name: backend-api
  jpa:
    hibernate:
      ddl-auto: validate
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  redis:
    host: ${SPRING_REDIS_HOST}
    port: ${SPRING_REDIS_PORT}

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### Build Backend

```bash
cd backend

# Build
mvn clean package

# Run tests
mvn test

# Run locally
mvn spring-boot:run
```

## Step 5: Frontend Setup

### Project Structure

```
frontend/
├── src/
│   ├── pages/
│   ├── components/
│   ├── hooks/
│   ├── utils/
│   ├── styles/
│   └── types/
├── public/
├── package.json
├── next.config.js
├── tsconfig.json
├── .eslintrc.json
├── jest.config.js
├── Dockerfile
└── .dockerignore
```

### Configure package.json

```bash
# Copy template
cp frontend/package.json.template frontend/package.json

# Install dependencies
cd frontend
npm install
```

### Create Configuration Files

**.eslintrc.json:**
```json
{
  "extends": ["next/core-web-vitals"],
  "rules": {
    "react/react-in-jsx-scope": "off"
  }
}
```

**tsconfig.json:**
```json
{
  "compilerOptions": {
    "target": "ES2020",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "jsx": "preserve",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "strict": true,
    "noEmit": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "allowJs": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"]
    }
  },
  "include": ["next-env.d.ts", "**/*.ts", "**/*.tsx"],
  "exclude": ["node_modules"]
}
```

### Build Frontend

```bash
cd frontend

# Install dependencies
npm install

# Development
npm run dev

# Build
npm run build

# Run production build
npm start

# Lint
npm run lint

# Type check
npm run type-check

# Tests
npm test
```

## Step 6: Database Migrations

### Create Migration Files

```bash
# Create migration directory
mkdir -p backend/src/main/resources/db/migration

# Create initial migration
touch backend/src/main/resources/db/migration/V1__initial_schema.sql
```

### Write Migrations

**V1__initial_schema.sql:**
```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Test Migrations

```bash
# Reset database
docker-compose down -v

# Start services (migrations run automatically)
docker-compose up

# Verify schema
docker-compose exec postgres psql -U appuser -d appdb -c "\dt"
```

## Step 7: Monitoring Setup

### Prometheus Configuration

The `monitoring/prometheus.yml` is already configured to scrape:
- Backend metrics at `/actuator/prometheus`
- Redis metrics
- PostgreSQL metrics

### Grafana Setup

1. Access Grafana: http://localhost:3001
2. Login: admin / (password from .env)
3. Add Prometheus data source:
   - URL: http://prometheus:9090
   - Save & Test
4. Import dashboards:
   - Go to Dashboards → Import
   - Use dashboard IDs from Grafana.com

### Loki Setup

Logs are automatically collected from:
- Docker containers
- Application logs
- System logs

Access Loki: http://localhost:3100

## Step 8: API Documentation

### Enable Swagger UI

Backend automatically exposes Swagger UI at:
```
http://localhost:8080/swagger-ui.html
```

### Document Endpoints

Add OpenAPI annotations to controllers:

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @GetMapping
    @Operation(summary = "Get all products")
    public List<Product> getAll() {
        // Implementation
    }
    
    @PostMapping
    @Operation(summary = "Create product")
    public Product create(@RequestBody Product product) {
        // Implementation
    }
}
```

## Step 9: CI/CD Testing

### Test Workflow Locally

```bash
# Commit changes
git add .
git commit -m "feat: initial setup"

# Push to feature branch
git push origin feature/initial-setup

# Create PR on GitHub
# → Watch CI pipeline run
# → Verify all checks pass
```

### Monitor Pipeline

1. Go to GitHub → Actions
2. Click on workflow run
3. View logs for each job
4. Check for failures

### Common Issues

**Maven build fails:**
```bash
# Clear cache
mvn clean

# Rebuild
mvn package
```

**npm install fails:**
```bash
# Clear cache
npm cache clean --force

# Reinstall
npm install
```

**Docker build fails:**
```bash
# Clean Docker
docker system prune -a

# Rebuild
docker-compose up --build
```

## Step 10: Deployment

### Deploy to Development

```bash
# Merge to dev
git checkout dev
git merge feature/initial-setup
git push origin dev

# Watch CI pipeline
# → Verify all checks pass
# → Frontend deploys to Vercel dev
```

### Deploy to Production

```bash
# Create PR: dev → main
git checkout main
git pull origin main
git merge dev
git push origin main

# Get 2 approvals on GitHub
# → Merge PR
# → Watch CI pipeline
# → Frontend deploys to Vercel production
```

## Troubleshooting

### Services Won't Start

```bash
# Check port conflicts
lsof -i :3000
lsof -i :8080
lsof -i :5432

# Kill process
kill -9 <PID>

# Clean Docker
docker system prune -a
docker-compose up --build
```

### Database Connection Error

```bash
# Check database is running
docker-compose ps

# View logs
docker-compose logs postgres

# Reset database
docker-compose down -v
docker-compose up
```

### CI Pipeline Fails

1. Check GitHub Actions logs
2. View specific job output
3. Check for:
   - Missing environment variables
   - Dependency conflicts
   - Test failures
   - Build errors

### Deployment Issues

1. Check Vercel deployment logs
2. Verify environment variables
3. Check database migrations
4. Monitor error rates

## Next Steps

1. Create feature branches for development
2. Set up team access and permissions
3. Configure monitoring alerts
4. Set up backup strategy
5. Document API endpoints
6. Create runbooks for common issues
7. Schedule team training
8. Plan disaster recovery drills
