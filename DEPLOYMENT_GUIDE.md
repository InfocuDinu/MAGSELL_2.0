# Ghid de Deployment - MAGSELL 2.0 Bakery Manager Pro

## 📋 Cerințe de Sistem

### Minim Necesar
- **Sistem de Operare:** Windows 10/11 sau Linux
- **Java:** OpenJDK 17 sau superior
- **RAM:** 2GB minim
- **Spațiu Disk:** 10GB disponibil
- **Rezoluție:** 1280x720 minim

### Recomandat
- **Sistem de Operare:** Windows 11 Professional
- **Java:** OpenJDK 21 (recomandat)
- **RAM:** 4GB
- **Spațiu Disk:** 20GB disponibil
- **Rezoluție:** 1920x1080

## 🚀 Instalare Rapidă

### Pasul 1: Verificare Java

```bash
# Verificați versiunea Java
java -version

# Ar trebui să vedeți ceva similar cu:
# openjdk version "17.0.x" sau "21.0.x"
```

Dacă Java nu este instalat:
- **Windows:** Descărcați de la https://adoptium.net/
- **Linux:** `sudo apt install openjdk-21-jdk` (Ubuntu/Debian)

### Pasul 2: Descărcare Aplicație

1. Descărcați `bakery-manager-1.0.0.jar` din releases
2. Creați un folder dedicat: `C:\BakeryManager` (Windows) sau `~/bakery-manager` (Linux)
3. Copiați JAR-ul în acest folder

### Pasul 3: Pornire Inițială

**Windows:**
```batch
cd C:\BakeryManager
java -jar bakery-manager-1.0.0.jar
```

**Linux:**
```bash
cd ~/bakery-manager
java -jar bakery-manager-1.0.0.jar
```

### Pasul 4: Configurare Inițială

La prima pornire:
1. Aplicația va crea automat fișierul `bakery.db`
2. Navigați la **Settings** (Setări)
3. Completați informațiile companiei:
   - Nume companie
   - CUI
   - Adresă
   - Telefon
   - Email

## ⚙️ Configurare Avansată

### Configurare Production Mode

Pentru mediul de producție, creați fișier `application-local.properties`:

```properties
# Database location
spring.datasource.url=jdbc:sqlite:C:/BakeryManager/data/bakery.db

# Logging
logging.file.name=C:/BakeryManager/logs/bakery.log
logging.level.com.bakerymanager=INFO

# Backup location
backup.default.location=C:/BakeryManager/backups
```

Pornire cu profil production:
```bash
java -jar -Dspring.profiles.active=prod bakery-manager-1.0.0.jar
```

### Configurare Backup Automat

1. Deschideți aplicația
2. Mergeți la **Settings → Backup**
3. Activați "Backup Automat"
4. Selectați frecvența: Zilnic (recomandat)
5. Alegeți locația: folder separat de aplicație

**Recomandare:** Configurați backup pe un drive diferit sau cloud storage.

### Configurare Logging

Fișierele de log se creează automat în `logs/bakery-manager.log`.

Niveluri de logging disponibile:
- **ERROR:** Doar erori critice
- **WARN:** Avertismente și erori  
- **INFO:** Informații generale (recomandat producție)
- **DEBUG:** Informații detaliate (doar pentru debug)

Pentru a schimba nivelul, editați în `application-local.properties`:
```properties
logging.level.com.bakerymanager=INFO
```

## 🔄 Proceduri Operaționale

### Pornire Zilnică

**Opțiune 1: Manual**
```bash
java -jar bakery-manager-1.0.0.jar
```

**Opțiune 2: Script (Windows)**
Creați `start-bakery.bat`:
```batch
@echo off
cd C:\BakeryManager
start javaw -jar bakery-manager-1.0.0.jar
```

**Opțiune 3: Shortcut (Windows)**
1. Click dreapta pe desktop → New → Shortcut
2. Location: `javaw -jar "C:\BakeryManager\bakery-manager-1.0.0.jar"`
3. Nume: "Bakery Manager"

### Oprire Aplicație

**Recomandat:**
- Utilizați butonul "Exit" din aplicație
- Asigurați-vă că toate vânzările sunt salvate

**Forțat (doar în caz de urgență):**
- Windows: Ctrl+Alt+Del → Task Manager → End Process
- Linux: `pkill -f bakery-manager`

### Backup Manual

**Înainte de update sau modificări importante:**

```batch
# Windows
copy bakery.db backups\bakery_%date:~-4,4%%date:~-7,2%%date:~-10,2%.db

# Linux
cp bakery.db backups/bakery_$(date +%Y%m%d).db
```

### Restore din Backup

1. Opriți aplicația complet
2. Redenumițbaza de date curentă:
   ```
   ren bakery.db bakery_old.db
   ```
3. Copiați backup-ul:
   ```
   copy backups\bakery_20260210.db bakery.db
   ```
4. Porniți aplicația

## 🔧 Troubleshooting

### Problema: "Java not found"
**Soluție:**
```bash
# Verificați PATH
echo %PATH%  (Windows)
echo $PATH   (Linux)

# Reinstalați Java și asigurați-vă că se adaugă la PATH
```

### Problema: "Database is locked"
**Cauze posibile:**
- Aplicația rulează deja (verificați Task Manager)
- Crash anterior (fișier .db-journal rămas)

**Soluție:**
```bash
# Opriți toate instanțele
# Ștergeți bakery.db-journal dacă există
del bakery.db-journal
```

### Problema: "OutOfMemoryError"
**Soluție:** Creșteți memoria alocată:
```bash
java -Xmx2G -jar bakery-manager-1.0.0.jar
```

### Problema: Aplicația pornește dar nu se vede fereastra
**Soluție:** Verificați multiple monitoare:
```bash
# Porniți cu reset window position
java -Djavafx.platform=win -jar bakery-manager-1.0.0.jar
```

### Problema: Import SPV eșuează
**Verificări:**
1. Fișierul XML este valid (deschideți în browser)
2. Dimensiune < 10MB
3. Verificați logs: `logs/bakery-manager.log`
4. Format UBL 2.1 (RO e-Factura)

### Problema: PDF Export nu funcționează
**Verificări:**
1. Verificați permisiuni folder destinație
2. Spațiu disk disponibil
3. Check logs pentru erori specifice

## 📊 Monitorizare și Mentenanță

### Daily Checks
- [ ] Verificare aplicație pornește corect
- [ ] Check backup automat executat (în logs)
- [ ] Verificare spațiu disk disponibil

### Weekly Checks
- [ ] Review logs pentru ERROR sau WARN
  ```bash
  findstr "ERROR" logs\bakery-manager.log
  ```
- [ ] Verificare dimensiune bază de date
- [ ] Test restore backup

### Monthly Checks
- [ ] Curățare log files vechi (>30 zile)
- [ ] Arhivare backups vechi
- [ ] Review performance (timp încărcare)
- [ ] Update Java dacă disponibil

### Yearly Checks
- [ ] Full database backup extern
- [ ] Review configurații security
- [ ] Update aplicație la versiune nouă

## 🔐 Securitate

### Best Practices

1. **Baza de Date:**
   - Nu partajați `bakery.db` prin email/USB
   - Permisiuni restrictive pe folder
   - Backup encriptat pentru cloud

2. **Configurare:**
   - Păstrați `config.properties` privat
   - Nu commitați în git
   - Backup separat pentru configurare

3. **Accesoperator:**
   - Parole unice pentru fiecare operator
   - Schimbare parolă periodic
   - Log all modifications

4. **Backups:**
   - Minim 2 copii (local + extern)
   - Test restore lunar
   - Encriptare pentru cloud storage

### Permisiuni Recomandate (Windows)

```
C:\BakeryManager\
  ├── bakery-manager-1.0.0.jar  (Read-only pentru users)
  ├── bakery.db                  (Read-write pentru app user)
  ├── logs\                      (Read-write pentru app user)
  └── backups\                   (Read-write pentru app user)
```

## 📈 Update la Versiune Nouă

### Procedură Update

1. **Pre-Update:**
   ```bash
   # Backup complet
   xcopy /E /I BakeryManager BakeryManager_backup_%date%
   ```

2. **Download:**
   - Descărcați noua versiune JAR
   - Citiți CHANGELOG pentru breaking changes

3. **Test (Recomandat):**
   ```bash
   # Creați folder test
   mkdir BakeryManager_test
   copy BakeryManager\bakery.db BakeryManager_test\
   cd BakeryManager_test
   java -jar bakery-manager-NEW.jar
   ```

4. **Production Update:**
   ```bash
   # Opriți aplicația
   # Backup database
   copy bakery.db backups\bakery_before_update.db
   
   # Replace JAR
   copy bakery-manager-NEW.jar bakery-manager-1.0.0.jar
   
   # Pornire
   java -jar bakery-manager-1.0.0.jar
   ```

5. **Post-Update:**
   - Verificați toate modulele
   - Test o vânzare completă
   - Test import SPV
   - Test generare rapoarte
   - Verificați logs pentru erori

## 📞 Support

### Self-Help Resources
1. **Logs:** `logs/bakery-manager.log` - prima sursă de informații
2. **Documentație:** `README.md`, `PRODUCTION_READINESS.md`
3. **FAQ:** Verificați issues închise pe GitHub

### Informații Necesare pentru Support
Când raportați o problemă, includeți:
- Versiune aplicație (din About)
- Versiune Java (`java -version`)
- Sistem operare
- Ultimele 50 linii din log:
  ```bash
  type logs\bakery-manager.log | more /E +1000
  ```
- Pași pentru reproducere problemă

## ✅ Checklist Post-Instalare

- [ ] Java instalat și verificat
- [ ] Aplicație pornită cu succes
- [ ] Configurate setări companie
- [ ] Testat adăugare produs
- [ ] Testat creare vânzare
- [ ] Configurat backup automat
- [ ] Verificat generare rapoarte
- [ ] Creat shortcut desktop
- [ ] Documentat proceduri operaționale
- [ ] Training operators completat

---

**Pentru asistență tehnică:** Consultați documentația sau contactați echipa de suport.

**Versiune Ghid:** 1.0
**Data:** 2026-02-10
**Aplicație:** MAGSELL 2.0 Bakery Manager Pro v1.0.0
