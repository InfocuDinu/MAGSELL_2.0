# BakeryManager Pro - Rulare Aplicație

## 📦 JAR Executabil Creat

JAR-ul executabil cu toate dependențele a fost creat cu succes:
```
target/bakery-manager-pro-1.0.0-jar-with-dependencies.jar
```

## 🚀 Metode de Rulare

### Metoda 1: Script PowerShell (Recomandat)
```powershell
.\run-app.ps1
```

### Metoda 2: Script Batch
```cmd
run-app.bat
```

### Metoda 3: Manual (necesită JavaFX)
```cmd
java --module-path "C:\Users\%USERNAME%\.m2\repository\org\openjfx\javafx-controls\21.0.1\javafx-controls-21.0.1.jar;C:\Users\%USERNAME%\.m2\repository\org\openjfx\javafx-fxml\21.0.1\javafx-fxml-21.0.1.jar;C:\Users\%USERNAME%\.m2\repository\org\openjfx\javafx-graphics\21.0.1\javafx-graphics-21.0.1.jar;C:\Users\%USERNAME%\.m2\repository\org\openjfx\javafx-base\21.0.1\javafx-base-21.0.1.jar" --add-modules javafx.controls,javafx.fxml -cp "target\bakery-manager-pro-1.0.0-jar-with-dependencies.jar" com.bakerymanager.BakeryApplication
```

### Metoda 4: Folosind Maven (dacă JavaFX module path funcționează)
```cmd
mvnw.cmd javafx:run
```

## 🔧 Cerințe de Sistem

- **Java 21+** (Eclipse Adoptium JDK 21.0.9.10-hotspot recomandat)
- **JavaFX 21.0.1** dependențe descărcate automat de Maven
- **Windows 10/11** (testat pe Windows)

## 📋 Funcționalități Implementate

✅ **Dashboard** - Statistici și vizualizări în timp real  
✅ **POS** - Vânzări cu istoric complet și salvare automată  
✅ **Inventory** - Gestiune completă stocuri și ingrediente  
✅ **Production** - Management producție și rețete  
✅ **Invoices** - Import SPV îmbunătățit cu căutare inteligentă  
✅ **Reports** - Rapoarte vânzări și analize  
✅ **Settings** - Configurare sistem  

## 🗄️ Bază de Date

Aplicația folosește **SQLite** cu fișierul `bakery.db` în directorul rădăcină.
Toate entitățile sunt create automat la prima rulare.

## 🐛 Depanare

### Dacă aplicația nu pornește:
1. Verificați dacă Java 21 este instalat: `java -version`
2. Verificați dacă JAVA_HOME este setat corect
3. Asigurați-vă că dependențele JavaFX sunt descărcate: `mvnw.cmd dependency:resolve`
4. Încercați scriptul PowerShell pentru erori detaliate

### Erori comune:
- **"JavaFX runtime components are missing"** -> Rulați cu scriptul PowerShell
- **"Module javafx.controls not found"** -> Verificați path-ul către JavaFX modules
- **"Connection refused"** -> Baza de date SQLite se creează automat

## 📞 Suport

Pentru probleme tehnice, verificați log-urile din consolă sau contactați echipa de dezvoltare.

---
**BakeryManager Pro v1.0.0** - Sistem complet de management pentru patiserii
