# BakeryManager Pro

## Descriere

BakeryManager Pro este o aplicație desktop ERP completă pentru gestionarea unei patiserii, dezvoltată în Java 21 cu JavaFX pentru interfața grafică și Spring Boot pentru backend logic.

## Arhitectură

### Stack Tehnologic
- **Limbaj:** Java 21 (LTS)
- **UI Framework:** JavaFX 21 (Modular)
- **Backend:** Spring Boot 3.2+ (Dependency Injection, JPA)
- **Baza de date:** SQLite (embedded, portabil)
- **ORM:** Spring Data JPA (Hibernate)
- **Build Tool:** Maven
- **XML Parsing:** Jackson Dataformat XML (pentru facturi SPV/UBL)
- **UI Components:** ControlsFX (dialog-uri moderne)

### Structura Proiectului

```
com.bakerymanager
├── config/                 # Configurare Spring + JavaFX Integration
├── controller/             # JavaFX Controllers (gestionează UI)
├── dto/                    # Data Transfer Objects (pentru import SPV)
├── entity/                 # Clasele mapate la Baza de Date (@Entity)
├── repository/             # Interfețe Spring Data JPA (@Repository)
├── service/                # Logica de business (@Service)
├── utils/                  # Utilitare și parsere
└── BakeryApplication.java  # Punctul de intrare (extends Application)
```

## Funcționalități

### 1. Modul Gestiune & SPV (Back-office)
- **Recepție Manuală (NIR):** Formular clasic pentru adăugarea stocurilor
- **Import SPV (e-Factura):** 
  - Parsează fișiere XML (standard UBL 2.1)
  - Mapare automată a produselor cu ingredientele din baza de date
  - Actualizare automată a prețurilor medii de achiziție

### 2. Modul Producție (Kitchen)
- **Editor de Rețete:** Interfață vizuală pentru definirea rețetelor
- **Fișa de Producție:**
  - Verificare stocuri disponibile
  - Execuție producție (scade ingrediente, crește stoc produse)
  - Validare stocuri înainte de producție

### 3. Modul Vânzări (POS - Front Office)
- **Interfață optimizată pentru Touchscreen:** Butoane mari, intuitive
- **Coș de cumpărături:** Managementul produselor selectate
- **Procesare plată:** Suport pentru multiple metode de plată
- **Bon fiscal:** Generare bonuri fiscale

### 4. Modul Rapoarte
- **Raport Stocuri:** Detaliere stocuri curente și alerte
- **Raport Vânzări:** Analiza vânzărilor pe perioade
- **Raport Producție:** Istoric și eficiență producție
- **Raport Costuri:** Analiza costurilor și profitabilitate

### 5. Modul Administrare
- **Setări Aplicație:** Configurare companie, monede, TVA
- **Backup Automat:** Protecție datelor cu backup programat
- **Dashboard:** Panou de control cu statistici în timp real

## Instalare și Rulare

### Prerechizite
- Java 21 JDK instalat
- Maven 3.6+ instalat

### Pași de Instalare

1. **Clonează repository-ul:**
   ```bash
   git clone <repository-url>
   cd bakery-manager-pro
   ```

2. **Compilează aplicația:**
   ```bash
   mvn clean compile
   ```

3. **Rulează aplicația:**
   ```bash
   mvn javafx:run
   ```
   
   Sau alternatively:
   ```bash
   mvn spring-boot:run
   ```

### Prima Rulare

La prima rulare, baza de date SQLite (`bakery.db`) va fi creată automat în directorul rădăcină al proiectului.

## Utilizare

### 1. Configurare Inițială
1. Accesează meniul **Setări** pentru a configura informațiile companiei
2. Adaugă ingrediente în modul **Gestiune Stocuri**
3. Definește produsele finite și rețetele în modul **Producție**

### 2. Flux de Lucru Zilnic
1. **Recepție Marfă:** Import facturi SPV sau adaugă manual stocuri
2. **Producție:** Execută producția conform rețetelor definite
3. **Vânzări:** Utilizează modul POS pentru vânzarea produselor
4. **Rapoarte:** Generează rapoarte la sfârșitul zilei

## Structura Bazei de Date

### Entități Principale

1. **Ingredient** (Materie primă)
   - id, nume, unitateMasura, stocCurent, pretUltimaAchizitie

2. **Product** (Produs finit)
   - id, nume, pretVanzare, stocFizic

3. **RecipeItem** (Rețetar)
   - Legătură many-to-many între Product și Ingredient
   - Cantitate necesară per unitate de produs

4. **Invoice & InvoiceLine** (Recepție/NIR)
   - Header factură și linii factură pentru achiziții

## Dezvoltare

### Adăugare de Noi Funcționalități

1. **Entități:** Adaugă în `entity/` cu adnotări JPA
2. **Repository:** Extinde `JpaRepository` în `repository/`
3. **Service:** Implementează logica de business în `service/`
4. **Controller:** Adaugă controller JavaFX în `controller/`
5. **FXML:** Creează fișierele de interfață în `resources/fxml/`

### Stilizare și UI

- Stilurile sunt definite în `resources/css/styles.css`
- Utilizează clase CSS predefinite pentru consistență vizuală
- ControlsFX pentru dialog-uri moderne

## Troubleshooting

### Probleme Comune

1. **Eroare JavaFX:** Asigură-te că JavaFX 21 este corect configurat
2. **Eroare Bază de Date:** Verifică permisiunile de scriere în directorul proiectului
3. **Import SPV Eșuat:** Verifică formatul fișierului XML (trebuie să fie UBL 2.1)

### Logging

Aplicația folosește SLF4J pentru logging. Nivelul de logging poate fi configurat în `application.properties`.

## Contribuții

Pentru contribuții, vă rugăm să:
1. Creați un branch pentru funcționalitatea nouă
2. Urmați standardele de cod existente
3. Adăugați teste unitare pentru funcționalitățile noi
4. Documentați modificările în README

## Licență

Acest proiect este dezvoltat pentru uz intern și nu are o licență specifică.

## Suport

Pentru suport tehnic sau întrebări, contactați echipa de dezvoltare.

---

**BakeryManager Pro** - Soluția completă pentru managementul patiseriei tale!
