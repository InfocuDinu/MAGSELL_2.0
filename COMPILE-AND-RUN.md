# Cum să compilezi și să rulezi aplicația BakeryManager Pro

## Cerințe preliminare

- **Java 17 sau superior** - verifică cu `java -version`
- **Apache Maven 3.6+** - verifică cu `mvn --version`

## Compilare

### 1. Compilare simplă (doar clasele)
```bash
mvn clean compile
```

### 2. Creare pachet JAR (recomandat)
```bash
mvn clean package -DskipTests
```

Această comandă va crea:
- `target/bakery-manager-pro-1.0.0.jar` - JAR simplu
- `target/bakery-manager-pro-1.0.0-jar-with-dependencies.jar` - JAR cu toate dependențele (63MB)

## Rulare

### Metoda 1: Folosind scriptul de rulare (recomandat pentru Linux)
```bash
./run-app.sh
```

### Metoda 2: Folosind Maven
```bash
mvn javafx:run
```

### Metoda 3: Folosind JAR-ul compilat
```bash
# NOTĂ: Această metodă necesită configurare JavaFX module path
java -jar target/bakery-manager-pro-1.0.0-jar-with-dependencies.jar
```

### Pentru Windows
Există deja scripturi disponibile:
- `run-app.ps1` - Script PowerShell
- `run-app.bat` - Script Batch

## Rulare în medii headless (fără display)

Dacă rulezi aplicația pe un server sau în CI/CD, instalează xvfb:

```bash
# Ubuntu/Debian
sudo apt-get install xvfb

# Rulare cu xvfb
xvfb-run -a mvn javafx:run
```

## Verificare compilare

După compilare cu succes, vei vedea:
```
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

## Verificare rulare

După pornirea aplicației, vei vedea în consolă:
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::

...
Started application in X.XXX seconds
Main controller initialized successfully
```

## Baza de date

La prima rulare, fișierul `bakery.db` (SQLite) va fi creat automat în directorul rădăcină al proiectului.

## Probleme comune

### 1. "JavaFX runtime components are missing"
**Soluție:** Folosește `mvn javafx:run` în loc de `java -jar`

### 2. "Unable to open DISPLAY"
**Soluție:** Rulează cu xvfb: `xvfb-run -a mvn javafx:run`

### 3. Erori de compilare legate de Java 21
**Soluție:** Acest lucru a fost rezolvat - aplicația acum folosește Java 17

## Structura proiectului după compilare

```
MAGSELL_2.0/
├── pom.xml                 # Configurație Maven (actualizată pentru Java 17)
├── bakery.db              # Baza de date SQLite (creat la prima rulare)
├── run-app.sh             # Script de rulare pentru Linux
├── run-app.ps1            # Script de rulare pentru Windows (PowerShell)
├── run-app.bat            # Script de rulare pentru Windows (Batch)
├── src/                   # Codul sursă
└── target/                # Directorul cu fișierele compilate
    ├── classes/           # Clasele compilate
    └── *.jar             # Fișierele JAR create
```

## Informații suplimentare

Pentru mai multe detalii despre funcționalitățile aplicației, vezi `README.md` și `README-RUN.md`.
