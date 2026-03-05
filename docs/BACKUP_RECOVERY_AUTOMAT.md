# Backup & Recovery automat

Acest ghid descrie implementarea și utilizarea backup-ului automat și a restaurării bazei SQLite.

## Ce este implementat

- backup manual din UI (`Setări` -> `Backup Acum`)
- backup automat periodic (scheduler Spring)
- frecvență configurabilă (`Zilnic` / `Săptămânal` / `Lunar`)
- validare integritate backup (`PRAGMA integrity_check`)
- restore din UI (`Setări` -> `Restore DB`)
- backup pre-restore automat (`*.pre-restore-*.bak`)

## Configurare

Fișierul `config.properties` controlează comportamentul operațional:

- `auto.backup=true|false`
- `backup.frequency=Zilnic|Săptămânal|Lunar`
- `backup.location=<director>`

Config la nivel aplicație:

- `backup.scheduler.fixed-delay-ms` (default 60000)
- `app.settings.file` (default `config.properties`)

## Cum funcționează backup-ul periodic

Un task programat rulează la intervalul `backup.scheduler.fixed-delay-ms` și:

1. citește setările backup,
2. verifică dacă backup automat este activ,
3. decide dacă backup-ul este due,
4. creează backup și îl validează,
5. actualizează metadata în `backup-meta.properties`.

## Recovery (restore)

### Din aplicație (recomandat)

1. deschide `Setări` -> tab `Backup`
2. apasă `Restore DB`
3. selectează fișierul `.db`/`.bak`
4. aplicația validează backup-ul
5. restore-ul este executat și se creează backup pre-restore

### Din script PowerShell

```powershell
./scripts/db/restore-sqlite-backup.ps1 -BackupFile "C:/path/bakery_backup_20260305_120000.db" -SQLitePath "bakery.db"
```

## RTO/RPO (operational targets)

- **RPO**: determinat de frecvența setată (zilnic/săptămânal/lunar)
- **RTO**: restore local de fișier (tipic în câteva secunde), urmat de restart aplicație

## Validare implementare

- teste unitare pentru backup/recovery: `BackupRecoveryServiceTest`
- test restaurare verifică și backup-ul pre-restore
- build Maven verde după integrare
