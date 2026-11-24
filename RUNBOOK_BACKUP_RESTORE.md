# OPERATIONAL RUNBOOK: POSTGRESQL BACKUP & DISASTER RECOVERY

**System:** Athenaeum LMS (Multi-Tenant Engine)  
**Scope:** Automated backups, retention rotation, point-in-time recovery, and verified test drills.

---

## 1. BACKUP ARCHITECTURE & STRATEGY

* **Engine:** PostgreSQL 15+ / 16+
* **Format:** Custom Archive (`-Fc`), enabling parallel restores, compression, and selective table recovery.
* **Integrity:** SHA-256 checksum generated and stored alongside every archive.
* **Retention Schedule:**
  - Daily snapshots: Retained for 7 days.
  - Weekly snapshots: Retained for 4 weeks.
  - Monthly archives: Retained for 12 months.

---

## 2. AUTOMATED BACKUP SCRIPT

Save as `/opt/athenaeum/scripts/backup.sh` (or `backup.ps1` on Windows):

```bash
#!/usr/bin/env bash
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/athenaeum}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/athenaeum_${TIMESTAMP}.dump"
CHECKSUM_FILE="${BACKUP_FILE}.sha256"

mkdir -p "${BACKUP_DIR}"

echo "[$(date -Iseconds)] Initiating Athenaeum PostgreSQL backup..."

# Execute pg_dump in custom compressed format
PGPASSWORD="${PGPASSWORD}" pg_dump \
  -h "${PGHOST:-localhost}" \
  -p "${PGPORT:-5432}" \
  -U "${PGUSER:-lms_user}" \
  -d "${PGDATABASE:-library_saas}" \
  -Fc \
  -b \
  -v \
  -f "${BACKUP_FILE}"

# Generate SHA-256 checksum for data integrity verification
sha256sum "${BACKUP_FILE}" > "${CHECKSUM_FILE}"

echo "[$(date -Iseconds)] Backup complete: ${BACKUP_FILE}"
echo "[$(date -Iseconds)] Checksum verified: $(cat ${CHECKSUM_FILE})"

# Apply retention policy: Remove daily archives older than 7 days
find "${BACKUP_DIR}" -name "athenaeum_*.dump" -type f -mtime +7 -delete
find "${BACKUP_DIR}" -name "athenaeum_*.sha256" -type f -mtime +7 -delete

echo "[$(date -Iseconds)] Retention cleanup finished."
```

### Automated Cron Job Setup
Add to `/etc/cron.d/athenaeum-backup` (runs daily at 02:00 AM):
```cron
0 2 * * * root /opt/athenaeum/scripts/backup.sh >> /var/log/athenaeum_backup.log 2>&1
```

---

## 3. POINT-IN-TIME DISASTER RECOVERY & RESTORE DRILL

### Step 1: Checksum Verification
Before restoring any archive, verify the file has not suffered bit rot or corruption:
```bash
sha256sum -c athenaeum_20261124_020000.dump.sha256
# Expected output: athenaeum_20261124_020000.dump: OK
```

### Step 2: Restore to Clean Database
To prevent overwriting live data during an audit or drill, restore into a dedicated drill database:

```bash
# 1. Create target recovery database
createdb -h localhost -U postgres athenaeum_recovery_test

# 2. Execute pg_restore
PGPASSWORD="${PGPASSWORD}" pg_restore \
  -h "${PGHOST:-localhost}" \
  -p "${PGPORT:-5432}" \
  -U "${PGUSER:-postgres}" \
  -d athenaeum_recovery_test \
  --clean \
  --if-exists \
  --no-owner \
  --no-privileges \
  -v \
  athenaeum_20261124_020000.dump
```

### Step 3: Post-Restore Data Integrity Verification
Run the verification queries to validate multi-tenant integrity and record consistency:

```sql
-- Connect to recovered database
\c athenaeum_recovery_test

-- 1. Verify Flyway migrations were preserved
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;

-- 2. Verify Tenant counts
SELECT count(*) AS total_tenants FROM tenants;

-- 3. Verify Patron counts
SELECT count(*) AS total_users FROM users;

-- 4. Verify Catalog & Physical Copies
SELECT count(*) AS total_books FROM books;
SELECT count(*) AS total_copies FROM book_copies;

-- 5. Check for orphaned copies (must be 0)
SELECT count(*) AS orphaned_copies 
FROM book_copies c 
LEFT JOIN books b ON c.book_id = b.id 
WHERE b.id IS NULL;

-- 6. Check for orphaned transactions (must be 0)
SELECT count(*) AS orphaned_loans 
FROM transactions t 
LEFT JOIN users u ON t.user_id = u.id 
WHERE u.id IS NULL;
```

---

## 4. EMERGENCY DRILL CHECKLIST

| Step | Action | Expected Result | Sign-off |
|---|---|---|---|
| **1** | Pull latest archive from offsite storage | File downloaded within 5 minutes | [ ] |
| **2** | Execute SHA-256 validation | `OK` hash match | [ ] |
| **3** | Execute `pg_restore` on drill instance | 0 fatal exit codes | [ ] |
| **4** | Boot staging app pointing to recovered DB | `ddl-auto=validate` passes | [ ] |
| **5** | Authenticate sample admin and student | Dashboard loads with data | [ ] |
