# Migrare DB H2 → SQLite (persistent)

Acest document descrie procedura standardizată pentru migrarea datelor din H2 către SQLite, inclusiv validări și plan de fallback/rollback.

## Artefacte livrate

- Utilitar Java: `com.bakerymanager.tools.H2ToSqliteMigrationTool`
- Script migrare: `scripts/db/migrate-h2-to-sqlite.ps1`
- Script validare: `scripts/db/validate-h2-sqlite.ps1`
- Rapoarte migrare/validare: `logs/migration/*.txt`

## Prerechizite

- JDK 17+ instalat
- Maven wrapper funcțional (`mvnw.cmd`)
- Access la baza H2 sursă (`jdbc:h2:file:...` sau TCP URL)
- Permisiune de scriere în directorul target pentru `*.db`

## 1) Migrare

Exemplu:

- H2 sursă: `jdbc:h2:file:C:/data/bakery-h2`
- SQLite destinație: `C:/data/bakery.db`

Rulare:

```powershell
./scripts/db/migrate-h2-to-sqlite.ps1 -H2Url "jdbc:h2:file:C:/data/bakery-h2" -H2User "sa" -H2Password "" -SQLitePath "C:/data/bakery.db" -Replace $true -Backup $true -ValidateAfter $true
```

Ce face scriptul:

1. Build jar cu dependențe (`-DskipTests package`)
2. Creează backup al SQLite existent (dacă există)
3. Copiază tabele + date din H2 în SQLite
4. Rulează validare (număr de rânduri per tabel)
5. Salvează raport în `logs/migration/`

## 2) Validare separată (post-migrare)

```powershell
./scripts/db/validate-h2-sqlite.ps1 -H2Url "jdbc:h2:file:C:/data/bakery-h2" -H2User "sa" -H2Password "" -SQLitePath "C:/data/bakery.db"
```

Validarea verifică:

- existența tabelelor în SQLite
- egalitatea numărului de rânduri H2 vs SQLite pentru fiecare tabel

## 3) Fallback / Rollback plan

### Fallback imediat (runtime)

Dacă apar probleme după migrare:

1. Oprește aplicația.
2. Revino la fișierul SQLite anterior migrarei (backup `*.pre-migration-*.bak`).
3. Repornește aplicația cu `DATABASE_PATH` către backup/restaurat.

### Rollback operațional complet

1. Păstrează snapshot H2 original nemodificat (read-only backup).
2. Păstrează backup-ul SQLite pre-migrare generat automat.
3. Dacă validarea eșuează:
   - nu promova noul fișier SQLite în producție,
   - restaurează backup-ul SQLite anterior,
   - deschide incident de migrare cu raportul din `logs/migration/`.
4. Reexecută migrarea după corectarea cauzei (mapping tipuri/date, tabele lipsă, date corupte).

## 4) Criterii de acceptanță migrare

Migrarea este considerată OK dacă:

- utilitarul iese cu cod `0`,
- raportul de validare nu conține mismatch,
- aplicația pornește pe SQLite nou fără erori de schemă,
- smoke test minim pe fluxurile critice (Login, Inventory, POS, Reports).

## 5) Note tehnice

- Tipurile SQL sunt mapate la afinități SQLite (`INTEGER`, `REAL`, `TEXT`, `BLOB`).
- Coloanele `BOOLEAN` sunt persistate numeric (`0/1`).
- `DATE/TIME/TIMESTAMP` sunt serializate textual ISO-like.
- Pentru PK integer auto-increment, utilitarul folosește `INTEGER PRIMARY KEY AUTOINCREMENT`.
