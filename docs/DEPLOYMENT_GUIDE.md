# Deployment Guide

## Overview

This guide covers deployment strategies, procedures, and best practices for the enterprise CI/CD system.

## Deployment Environments

### Development (dev branch)
- **Trigger:** Push to dev branch
- **Frontend:** Vercel preview environment
- **Backend:** Container registry (optional)
- **Database:** Development database
- **Monitoring:** Enabled
- **Retention:** 7 days

### Production (main branch)
- **Trigger:** Push to main branch
- **Frontend:** Vercel production
- **Backend:** Container registry with version tag
- **Database:** Production database
- **Monitoring:** Full observability
- **Retention:** Indefinite

### Preview (Pull Requests)
- **Trigger:** PR creation
- **Frontend:** Vercel preview per PR
- **Backend:** Not deployed
- **Database:** Shared dev database
- **Monitoring:** Basic
- **Retention:** Until PR closes

## Deployment Procedures

### PR Preview Deployment

1. **Automatic Trigger**
   - PR created against dev or main
   - CI pipeline runs
   - Frontend builds successfully
   - Preview deployed to Vercel

2. **Reviewer Testing**
   - Click preview URL in PR comment
   - Test feature in isolation
   - Verify no regressions
   - Approve or request changes

3. **Cleanup**
   - PR closed → Preview destroyed
   - Automatic cleanup by Vercel
   - No manual intervention needed

### Development Deployment

1. **Merge to dev**
   ```bash
   git checkout dev
   git pull origin dev
   git merge feature/my-feature
   git push origin dev
   ```

2. **Automatic Deployment**
   - CI pipeline runs
   - All checks pass
   - Frontend deployed to Vercel dev
   - Database migrations run
   - Monitoring alerts enabled

3. **Verification**
   - Check Vercel deployment status
   - Verify database migrations
   - Monitor error rates
   - Test critical flows

### Production Deployment

1. **Create Release PR**
   ```bash
   git checkout main
   git pull origin main
   git merge dev
   git push origin main
   ```

2. **Code Review**
   - Minimum 2 approvals required
   - All CI checks must pass
   - Branch must be up-to-date
   - Signed commits required

3. **Automatic Deployment**
   - Semantic version generated
   - GitHub Release created
   - Frontend deployed to Vercel production
   - Database migrations run
   - Docker images tagged and pushed
   - Monitoring alerts enabled

4. **Post-Deployment**
   - Monitor error rates
   - Check performance metrics
   - Verify user transactions
   - Monitor infrastructure

## Deployment Strategies

### Blue-Green Deployment

**Setup:**
```yaml
# Vercel handles this automatically
# Two production environments:
# - Blue: Current production
# - Green: New version
```

**Process:**
1. Deploy new version to Green
2. Run smoke tests
3. Switch traffic to Green
4. Keep Blue as rollback

**Rollback:**
```bash
# Switch traffic back to Blue
vercel rollback
```

### Canary Deployment

**Setup:**
```yaml
# Route percentage of traffic to new version
# Monitor metrics before full rollout
```

**Process:**
1. Deploy to canary environment
2. Route 5% traffic to canary
3. Monitor error rates and latency
4. Gradually increase to 100%
5. Complete rollout or rollback

### Rolling Updates

**Setup:**
```yaml
# Update instances one at a time
# Maintain service availability
```

**Process:**
1. Update instance 1
2. Wait for health check
3. Update instance 2
4. Continue until all updated
5. Verify all instances healthy

## Rollback Procedures

### Automatic Rollback

**Triggers:**
- Critical error rate (>5%)
- High latency (>5s p99)
- Database migration failure
- Health check failure

**Process:**
```bash
# Automatic rollback to previous version
vercel rollback

# Or manual rollback
git revert <commit-hash>
git push origin main
```

### Manual Rollback

**Steps:**
1. Identify issue
2. Revert commit
3. Push to main
4. Monitor deployment
5. Verify fix

**Command:**
```bash
git revert <commit-hash>
git push origin main
```

## Database Migrations

### Migration Files

**Location:** `backend/src/main/resources/db/migration/`

**Naming Convention:** `V<version>__<description>.sql`

**Examples:**
- `V1__initial_schema.sql`
- `V2__add_users_table.sql`
- `V3__add_indexes.sql`

### Creating Migrations

1. **Create migration file:**
   ```bash
   touch backend/src/main/resources/db/migration/V4__add_products_table.sql
   ```

2. **Write SQL:**
   ```sql
   CREATE TABLE products (
       id SERIAL PRIMARY KEY,
       name VARCHAR(255) NOT NULL,
       price DECIMAL(10, 2) NOT NULL
   );
   ```

3. **Test locally:**
   ```bash
   docker-compose down -v
   docker-compose up
   ```

4. **Commit and push:**
   ```bash
   git add backend/src/main/resources/db/migration/
   git commit -m "feat: add products table"
   git push origin feature/products
   ```

### Migration Execution

**Development:**
- Automatic on `docker-compose up`
- Flyway runs migrations
- Schema updated

**Production:**
- Automatic on deployment
- Runs before application start
- Verified before traffic switch

### Rollback Migrations

**Undo last migration:**
```bash
# Create undo migration
touch backend/src/main/resources/db/migration/U4__add_products_table.sql
```

**Undo migration content:**
```sql
DROP TABLE IF EXISTS products;
```

## Monitoring Deployments

### Pre-Deployment Checks

```bash
# Verify CI pipeline
- All tests pass
- Code quality checks pass
- Security scans pass
- Docker builds succeed

# Verify database
- Migrations validated
- Backup created
- Restore tested
```

### Post-Deployment Checks

```bash
# Verify application
- Health checks pass
- Error rate < 1%
- Latency < 500ms p99
- Database connections healthy

# Verify infrastructure
- CPU usage < 80%
- Memory usage < 80%
- Disk usage < 80%
- Network latency < 100ms
```

### Monitoring Dashboards

**Grafana Dashboards:**
- Application metrics
- Infrastructure metrics
- Error rates and latency
- Database performance
- Cache hit rates

**Access:**
```
http://localhost:3001
Username: admin
Password: (from .env)
```

## Deployment Checklist

### Pre-Deployment
- [ ] All tests passing
- [ ] Code review approved
- [ ] Security scans passed
- [ ] Database backup created
- [ ] Rollback plan documented
- [ ] Team notified
- [ ] Monitoring alerts configured

### Deployment
- [ ] Merge to main
- [ ] CI pipeline running
- [ ] Semantic version generated
- [ ] GitHub Release created
- [ ] Docker images pushed
- [ ] Vercel deployment started

### Post-Deployment
- [ ] Health checks passing
- [ ] Error rate normal
- [ ] Latency normal
- [ ] Database migrations successful
- [ ] Monitoring alerts active
- [ ] User testing completed
- [ ] Team notified

## Troubleshooting

### Deployment Fails

**Check logs:**
```bash
# GitHub Actions logs
# → Actions tab → Workflow run → Failed job

# Vercel logs
# → Vercel dashboard → Deployments → Failed deployment

# Docker logs
docker-compose logs backend
docker-compose logs frontend
```

**Common issues:**
- Database migration failure
- Environment variable missing
- Docker build failure
- Dependency conflict

### Rollback Needed

**Steps:**
1. Identify issue
2. Revert commit
3. Push to main
4. Monitor deployment
5. Verify fix

**Command:**
```bash
git revert <commit-hash>
git push origin main
```

### Performance Issues

**Check metrics:**
```bash
# Prometheus
http://localhost:9090

# Grafana
http://localhost:3001

# Application metrics
http://localhost:8080/actuator/metrics
```

**Common issues:**
- Database query slow
- Memory leak
- Cache miss rate high
- External API slow

## Deployment Notifications

### Slack Integration (Optional)

**Setup:**
1. Create Slack webhook
2. Add to GitHub Secrets
3. Configure workflow notification

**Example:**
```yaml
- name: Notify Slack
  uses: slackapi/slack-github-action@v1
  with:
    webhook-url: ${{ secrets.SLACK_WEBHOOK }}
    payload: |
      {
        "text": "Deployment to production completed"
      }
```

### Email Notifications

**Setup:**
1. Configure GitHub notification settings
2. Enable deployment notifications
3. Add team email addresses

## Deployment Metrics

### Key Metrics

- **Deployment Frequency:** How often deployments occur
- **Lead Time:** Time from commit to production
- **Mean Time to Recovery:** Time to fix production issues
- **Change Failure Rate:** Percentage of deployments causing issues

### Targets

- Deployment Frequency: Daily
- Lead Time: < 1 hour
- MTTR: < 15 minutes
- Change Failure Rate: < 5%

## Best Practices

1. **Deploy frequently** - Small, incremental changes
2. **Automate everything** - Reduce manual errors
3. **Test thoroughly** - Catch issues early
4. **Monitor closely** - Detect issues quickly
5. **Document procedures** - Enable team knowledge
6. **Plan rollbacks** - Be prepared for failures
7. **Communicate clearly** - Keep team informed
8. **Review changes** - Catch issues before production
