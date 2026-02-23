# MAGSELL 2.0 – BakeryManager Pro

**BakeryManager Pro** este un sistem ERP desktop complet pentru gestionarea unei patiserii, dezvoltat în Java 21 cu JavaFX pentru interfața grafică și Spring Boot pentru logica de business.

---

## Cuprins

1. [Stack Tehnologic](#stack-tehnologic)
2. [Structura Proiectului](#structura-proiectului)
3. [Funcționalități](#funcționalități)
   - [Autentificare și Roluri](#1-autentificare-și-roluri)
   - [Dashboard](#2-dashboard)
   - [Modul Vânzări (POS)](#3-modul-vânzări-pos)
   - [Modul Gestiune Stocuri](#4-modul-gestiune-stocuri)
   - [Modul Producție](#5-modul-producție)
   - [Modul Facturi & Recepție](#6-modul-facturi--recepție)
   - [Modul Clienți & CRM](#7-modul-clienți--crm)
   - [Modul Comenzi Personalizate](#8-modul-comenzi-personalizate)
   - [Modul Rapoarte](#9-modul-rapoarte)
   - [Modul Pierderi (Waste Tracking)](#10-modul-pierderi-waste-tracking)
   - [Modul Administrare & Setări](#11-modul-administrare--setări)
   - [Integrare Case de Marcat Fiscale](#12-integrare-case-de-marcat-fiscale)
4. [Structura Bazei de Date](#structura-bazei-de-date)
5. [Instalare și Rulare](#instalare-și-rulare)
6. [Flux de Lucru Zilnic](#flux-de-lucru-zilnic)
7. [Troubleshooting](#troubleshooting)
8. [Contribuții](#contribuții)

---

## Stack Tehnologic

| Componentă | Tehnologie |
|---|---|
| Limbaj | Java 17/21 (LTS) |
| Interfață grafică | JavaFX 21 (Modular) |
| Backend | Spring Boot 3.2+ (Dependency Injection, JPA) |
| Baza de date | SQLite (embedded, fără instalare separată) |
| ORM | Spring Data JPA / Hibernate 6.4.1 |
| Build | Maven 3.6+ |
| Parsare XML | Jackson Dataformat XML (facturi SPV / UBL 2.1) |
| Componente UI | ControlsFX |
| Generare PDF | OpenPDF 1.3.30 |
| Utilitare | Lombok |

---

## Structura Proiectului

```
src/main/java/com/bakerymanager/
├── BakeryApplication.java          # Punctul de intrare (extends JavaFX Application)
├── config/
│   └── SpringFXMLLoader.java       # Integrare Spring + JavaFX
├── controller/                     # Controllere JavaFX (logică UI)
│   ├── MainController.java
│   ├── LoginController.java
│   ├── DashboardController.java
│   ├── POSController.java
│   ├── InventoryController.java
│   ├── ProductionController.java
│   ├── InvoicesController.java
│   └── ReportsController.java
├── entity/                         # Entități JPA (tabele bază de date)
├── repository/                     # Interfețe Spring Data JPA
├── service/                        # Logica de business
├── dto/                            # Data Transfer Objects (import SPV)
├── smartbill/                      # Arhitectură hexagonală (Ports & Adapters)
└── utils/                          # Utilitare și parsere XML

src/main/resources/
├── application.properties          # Configurare aplicație
├── fxml/                           # Fișiere de interfață (FXML)
└── css/
    └── style.css                   # Stilizare unificată
```

---

## Funcționalități

### 1. Autentificare și Roluri

- **Ecran de login** cu validare utilizator și parolă
- **Roluri disponibile:**
  - `ADMIN` – acces complet la toate modulele
  - `MANAGER` – rapoarte, producție, gestiune
  - `CASHIER` – vânzări (POS)
  - `PRODUCTION` – personal producție
- **Conturi implicite:** `admin / admin123`, `casier / casier123`
- Parole stocate cu hash SHA-256
- Tracking dată/oră ultimul login
- Activare / dezactivare conturi utilizator

---

### 2. Dashboard

- Statistici în timp real: vânzări zilnice, stocuri, producție
- Alerte pentru:
  - Produse și ingrediente cu stoc scăzut
  - Produse aproape de expirare (3 zile)
  - Ingrediente aproape de expirare (7 zile)
  - Comenzi personalizate cu termen apropiat
- Acces rapid la toate modulele aplicației

---

### 3. Modul Vânzări (POS)

Interfață optimizată pentru touchscreen, destinată casierilor.

#### Adăugare produse în coș
- **Butoane rapide** – grilă vizuală cu produsele active; adăugare cu un singur click (cantitate = 1)
- **Introducere manuală** – dialog de căutare după nume sau cod bare, cu:
  - Filtrare în timp real
  - Auto-selectare când există un singur rezultat
  - Introducere cantitate personalizată
  - Previzualizare preț total
  - Validare stoc înainte de adăugare
- Butoane dezactivate automat când stocul unui produs este epuizat

#### Coș de cumpărături
- Afișare cantitate, preț unitar și total per produs
- Ștergere individuală a articolelor
- Golire completă a coșului

#### Procesare plată
- Metode de plată suportate: **Numerar**, **Card Bancar**, **Tichete Masă**, **Altele**
- Calculare automată rest la plata cu numerar
- Scădere automată a stocului la finalizarea vânzării (mecanism tranzacțional)
- Generare bon fiscal și salvare vânzare în baza de date
- Asociere vânzare cu client (pentru puncte de loialitate)

#### Rapoarte rapide POS
- Total vânzări din ziua curentă afișat în footer
- Buton raport zilnic direct din interfața POS

---

### 4. Modul Gestiune Stocuri

#### Gestiune ingrediente (materii prime)
- Adăugare, editare, ștergere ingrediente
- Unitate de măsură configurabilă
- Stoc curent, stoc minim, preț ultima achiziție
- **Dată expirare** și alerte automate pentru ingrediente care expiră în 7 zile
- **Număr lot** și dată lot (trasabilitate furnizor → producție)
- Suport FIFO / FEFO (First Expired, First Out)

#### Gestiune produse finite
- Adăugare, editare, ștergere produse
- Preț de vânzare, stoc fizic, stoc minim
- **Dată expirare** cu alertă la 3 zile
- **Număr lot producție** și dată fabricație
- Cod bare (barcode) pentru identificare rapidă

#### Mișcări de stoc
- Istoric complet al tuturor intrărilor și ieșirilor de stoc
- Audit trail: tip operație, cantitate, dată, sursă

---

### 5. Modul Producție

#### Editor rețete
- Definire rețete (Bill of Materials) pentru fiecare produs finit
- Specificare cantitate de ingredient necesară per unitate de produs
- Vizualizare și editare interactivă

#### Ordine de producție
- Creare ordin de producție cu una sau mai multe linii de produs
- **Verificare automată stocuri** înainte de execuție
- **Execuție producție:**
  1. Scădere stoc ingrediente consumate
  2. Creștere stoc produse finite fabricate
  3. Salvare raport de producție
- Calculare cost de producție
- Tracking eficiență producție

---

### 6. Modul Facturi & Recepție

#### Recepție manuală (NIR – Notă de Intrare-Recepție)
- Formular pentru introducerea manuală a facturilor de achiziție
- Adăugare linii factură cu produs, cantitate, preț unitar
- Actualizare automată stoc ingrediente la confirmare
- Generare PDF pentru NIR

#### Import SPV (e-Factura)
- Parsare fișiere XML în format **UBL 2.1** (standard ANAF/SPV)
- Mapare automată a articolelor din factură cu ingredientele din baza de date
- Actualizare automată prețuri medii de achiziție
- Tracking furnizor

---

### 7. Modul Clienți & CRM

- Înregistrare clienți: nume, telefon, email
- **Sistem de puncte de loialitate:** 1 punct la fiecare 10 LEI cheltuiți
- Acordare automată de puncte la fiecare vânzare finalizată
- Redeemire puncte de loialitate
- Tracking total cumpărături și data ultimei achiziții
- Căutare client după telefon
- Top clienți după valoarea totală a achizițiilor
- Identificare clienți VIP (praguri configurabile de puncte)

---

### 8. Modul Comenzi Personalizate

Destinat gestionării comenzilor speciale (ex: torturi personalizate).

- Creare comandă asociată unui client
- Specificare produs, cantitate, preț, termen de livrare
- Câmp de personalizare (ex: „La mulți ani Maria! Tort cu frișcă.")
- Înregistrare avans plătit și calcul rest de plată
- **Statusuri comandă:**
  - `PENDING` – În așteptare
  - `CONFIRMED` – Confirmată
  - `IN_PROGRESS` – În producție
  - `READY` – Gata de livrare
  - `DELIVERED` – Livrată
  - `CANCELLED` – Anulată
- Alertă automată pentru comenzi cu termen apropiat
- Detectare comenzi întârziate

---

### 9. Modul Rapoarte

- **Raport Vânzări:** Vânzări pe intervale de timp, top produse vândute
- **Raport Stocuri:** Stocuri curente, alerte stoc minim, produse expirate
- **Raport Producție:** Istoric ordine de producție, eficiență, costuri
- **Raport Costuri & Profitabilitate:** Analiza marjelor per produs
- **Raport Pierderi (Waste):** Total pierderi pe perioadă și pe motive
- Export PDF pentru toate tipurile de rapoarte

---

### 10. Modul Pierderi (Waste Tracking)

Înregistrarea și analiza pierderilor de produse și ingrediente.

#### Tipuri de pierderi
- `EXPIRED` – Expirat
- `DAMAGED` – Deteriorat
- `BURNT` – Ars
- `DROPPED` – Căzut
- `QUALITY_ISSUE` – Probleme de calitate
- `OVERPRODUCTION` – Supraproducție
- `CONTAMINATION` – Contaminare
- `OTHER` – Altele

#### Funcționalități
- Înregistrare waste pentru produse finite sau ingrediente
- Calculare automată cost estimat al pierderilor
- Rapoarte waste zilnic / săptămânal / lunar
- Rapoarte detaliate pe motive (identificare probleme recurente)
- Conformitate cu cerințele de siguranță alimentară

---

### 11. Modul Administrare & Setări

- Configurare informații companie (nume, adresă, CIF, monedă, TVA)
- Gestionare utilizatori: creare, editare, activare/dezactivare
- **Backup automat** al bazei de date SQLite
- Configurare logging și nivel de detaliu

---

### 12. Integrare Case de Marcat Fiscale

- Interfață `FiscalPrinterService` pentru integrare cu case de marcat reale
- Implementare mock (`MockFiscalPrinterService`) pentru development/testare
- Format bon fiscal conform cerințelor legale:
  ```
  ========================================
         MAGSELL 2.0 - PATISERIE
  ========================================
  BON FISCAL
  Nr: INV-12345
  Data: 11.02.2026 10:22:00
  ----------------------------------------
  Cozonac               2 x 25.00 =  50.00
  Tort Aniversare       1 x 85.00 =  85.00
  ----------------------------------------
  TOTAL:                          135.00 LEI
  Plată: Numerar
  Primit:                         150.00 LEI
  Rest:                            15.00 LEI
  ========================================
  ```
- Degradare gracioasă: vânzarea se salvează chiar dacă printarea eșuează
- Compatibil cu: DATECS, TREMOL, Custom, NCR (necesită driver certificat)

---

## Structura Bazei de Date

| Tabel | Descriere |
|---|---|
| `users` | Utilizatori și roluri |
| `products` | Produse finite (stoc, preț, lot, expirare) |
| `ingredients` | Materii prime (stoc, lot, expirare) |
| `recipe_items` | Rețete – legătură produs ↔ ingredient |
| `sales` | Tranzacții de vânzare |
| `sale_items` | Linii de vânzare (produs, cantitate, preț) |
| `invoices` | Facturi de achiziție (header) |
| `invoice_lines` | Linii factură |
| `reception_notes` | Note de intrare-recepție (NIR) |
| `reception_note_lines` | Linii NIR |
| `production_orders` | Ordine de producție |
| `production_order_lines` | Linii ordin de producție |
| `stock_movements` | Audit trail mișcări de stoc |
| `customers` | Clienți și puncte de loialitate |
| `custom_orders` | Comenzi personalizate |
| `waste_tracking` | Înregistrare pierderi |

Baza de date SQLite (`bakery.db`) este creată automat la prima rulare prin Hibernate (`ddl-auto=update`).

---

## Instalare și Rulare

### Cerințe de sistem

- **Java 21 JDK** (Eclipse Adoptium recomandat)
- **Maven 3.6+**
- **Windows 10/11** (testată pe Windows; Linux/macOS suportate teoretic)
- Fără bază de date externă – SQLite este embedded

### Pași de instalare

1. **Clonează repository-ul:**
   ```bash
   git clone <repository-url>
   cd MAGSELL_2.0
   ```

2. **Compilează aplicația:**
   ```bash
   mvn clean compile
   ```

3. **Rulează aplicația:**

   **Opțiunea 1 – Maven (recomandat pentru development):**
   ```bash
   mvn javafx:run
   ```

   **Opțiunea 2 – Script PowerShell (Windows):**
   ```powershell
   .\run-app.ps1
   ```

   **Opțiunea 3 – Script Batch (Windows):**
   ```cmd
   run-app.bat
   ```

   **Opțiunea 4 – JAR executabil:**
   ```bash
   java -jar target/bakery-manager-pro-1.0.0-jar-with-dependencies.jar
   ```

### Prima rulare

- Baza de date `bakery.db` se creează automat în directorul rădăcină
- Toate tabelele sunt generate automat de Hibernate
- Autentificare implicită: `admin / admin123`

---

## Flux de Lucru Zilnic

1. **Login** – Autentificare cu contul propriu
2. **Dashboard** – Verificare alerte stoc scăzut / produse expirate
3. **Recepție Marfă** – Import facturi SPV (XML) sau adăugare manuală NIR
4. **Producție** – Creare și execuție ordine de producție conform rețetelor
5. **Vânzări** – Utilizare modul POS pentru înregistrarea vânzărilor
6. **Rapoarte** – Generare rapoarte zilnice la sfârșitul programului
7. **Pierderi** – Înregistrare waste dacă este cazul

---

## Troubleshooting

| Problemă | Soluție |
|---|---|
| `JavaFX runtime components are missing` | Rulați cu scriptul PowerShell `.\run-app.ps1` |
| `Module javafx.controls not found` | Verificați că `JAVA_HOME` indică spre JDK 21 |
| Baza de date nu se creează | Verificați permisiunile de scriere în directorul proiectului |
| Import SPV eșuat | Verificați că fișierul XML respectă formatul UBL 2.1 |
| Aplicația nu pornește | Rulați `java -version` și `mvn -version` pentru a verifica instalarea |

### Logging

Fișierul de log se află la `logs/bakery-manager.log`. Nivelul de logging se configurează în `src/main/resources/application.properties`:
```properties
logging.level.com.bakerymanager=INFO
```

---

## Contribuții

1. Creați un branch pentru funcționalitatea nouă
2. Respectați structura pe 4 straturi: `entity` → `repository` → `service` → `controller`
3. Adăugați teste unitare pentru serviciile noi
4. Documentați modificările în acest README

---

## Licență

Acest proiect este dezvoltat pentru uz intern și nu are o licență publică specifică.

---

**MAGSELL 2.0 – BakeryManager Pro** | Soluția completă pentru managementul patiseriei tale!
