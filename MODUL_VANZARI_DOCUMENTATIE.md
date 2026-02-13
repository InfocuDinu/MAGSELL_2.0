# Modul Vânzări (POS) - Documentație Completă

## Descriere Generală

Modulul de vânzări (Point of Sale - POS) permite înregistrarea vânzărilor de produse finite și scăderea automată a stocurilor. Produsele pot proveni din:
1. **Producție** (produse fabricate folosind rețete)
2. **Achiziții** (marfă cumpărată prin NIR/facturi)

## Arhitectură

### Fluxul Complet de la Achiziție/Producție la Vânzare

```
┌──────────────────────────────────────────────────────────────┐
│                    APROVIZIONARE                              │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────┐         ┌──────────────┐                   │
│  │ Import SPV  │   sau   │ Factură      │                   │
│  │   (XML)     │         │  Manuală     │                   │
│  └──────┬──────┘         └───────┬──────┘                   │
│         │                        │                            │
│         └────────────┬───────────┘                            │
│                      ↓                                        │
│              ┌───────────────┐                                │
│              │ InvoiceLine   │                                │
│              │ (ProductType: │                                │
│              │ MATERIE_PRIMA)│                                │
│              └───────┬───────┘                                │
│                      ↓                                        │
│              ┌───────────────┐                                │
│              │  Ingredient   │                                │
│              │ (stoc actualizat)                              │
│              └───────────────┘                                │
└──────────────────────────────────────────────────────────────┘
                      ↓
┌──────────────────────────────────────────────────────────────┐
│                      PRODUCȚIE                                │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────┐         ┌──────────────┐                   │
│  │   Product   │────────>│  RecipeItem  │                   │
│  │  (defineşte)│         │ (rețetă cu   │                   │
│  └─────────────┘         │ ingrediente) │                   │
│                          └──────┬───────┘                    │
│                                 │                             │
│                                 ↓                             │
│                         ┌──────────────┐                     │
│                         │ Ingredient   │                     │
│                         │ (necesar     │                     │
│                         │  producție)  │                     │
│                         └──────────────┘                     │
│                                                               │
│  Execuție Producție:                                         │
│  1. Verificare stocuri ingrediente                           │
│  2. Scădere stoc ingrediente                                 │
│  3. Creştere stoc produs finit                               │
│  4. Salvare ProductionReport                                 │
│                                                               │
└──────────────────────────────────────────────────────────────┘
                      ↓
┌──────────────────────────────────────────────────────────────┐
│                      VÂNZĂRI (POS)                            │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  Metode de Adăugare în Coș:                                 │
│                                                               │
│  ┌──────────────────┐        ┌──────────────────┐           │
│  │ 1. Buton Produs  │   sau  │ 2. Introducere   │           │
│  │   (click rapid)  │        │     Manuală      │           │
│  └────────┬─────────┘        └─────────┬────────┘           │
│           │                            │                     │
│           │      ┌─────────────────────┘                     │
│           │      │                                           │
│           ↓      ↓                                           │
│       ┌─────────────────┐                                    │
│       │  Coș Cumpărături│                                    │
│       │  (CartItem list)│                                    │
│       └────────┬────────┘                                    │
│                ↓                                              │
│       ┌─────────────────┐                                    │
│       │ Procesare Plată │                                    │
│       └────────┬────────┘                                    │
│                ↓                                              │
│       ┌─────────────────┐                                    │
│       │  SaleService    │                                    │
│       │  .createSale()  │                                    │
│       └────────┬────────┘                                    │
│                ↓                                              │
│  1. Salvare Sale (header)                                    │
│  2. Creare SaleItem pentru fiecare produs                    │
│  3. SCĂDERE AUTOMATĂ STOC PRODUS                             │
│  4. Salvare în baza de date                                  │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

## Funcționalități Implementate

### 1. Introducere Manuală Produse (NOU) ✅

**Buton:** "➕ Introduceți Manual" în interfața POS

**Caracteristici:**
- Dialog interactiv pentru căutare produse
- Căutare după nume sau cod bare
- Filtrare în timp real
- Auto-completare când un singur rezultat
- Validare stoc înainte de adăugare
- Introducere cantitate personalizată
- Previzualizare preț total

**Utilizare:**
```
1. Click pe "➕ Introduceți Manual"
2. Introduceți nume produs sau cod bare în câmpul de căutare
3. Selectați produsul din listă (sau auto-selectare la un rezultat)
4. Introduceți cantitatea dorită
5. Verificați stocul disponibil și prețul total
6. Click "Adaugă în Coș"
```

**Validări:**
- ✅ Cantitate > 0
- ✅ Cantitate <= Stoc disponibil
- ✅ Produs selectat

### 2. Selecție Rapidă cu Butoane ✅

**Caracteristici:**
- Grilă de butoane pentru produse frecvente
- Adăugare rapidă cu un click (cantitate = 1)
- Butoane dezactivate când stoc epuizat
- Filtrare cu câmp de căutare

### 3. Coș de Cumpărături ✅

**Funcționalități:**
- Vizualizare produse adăugate
- Cantitate, preț unitar, total per produs
- Buton ștergere pentru fiecare produs
- Sumar total coș
- Golire completă coș

### 4. Procesare Plată ✅

**Metode de plată suportate:**
- Numerar
- Card Bancar
- Tichete Masă
- Altele

**Proces:**
1. Selectare metodă plată
2. Introducere sumă primită (pentru numerar)
3. Calculare automată rest
4. Click "Încasează"
5. **Scădere automată stoc produse**
6. Salvare vânzare în baza de date
7. Generare bon fiscal

## Scăderea Automată a Stocurilor

### Mecanism Implementat

Fișier: `SaleService.java` (liniile 38-105)

```java
@Transactional
public Sale createSale(List<CartItem> cartItems, ...) {
    // ... creare vânzare ...
    
    for (CartItem cartItem : cartItems) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Produsul nu există"));
        
        // VERIFICARE STOC
        if (product.getCurrentStock().compareTo(cartItem.getQuantity()) < 0) {
            throw new IllegalArgumentException("Stoc insuficient pentru: " + product.getName());
        }
        
        // SCĂDERE STOC AUTOMAT (Linia 69)
        product.setCurrentStock(product.getCurrentStock().subtract(cartItem.getQuantity()));
        productsToUpdate.add(product);
        
        // Creare SaleItem pentru istoric
        SaleItem saleItem = new SaleItem();
        saleItem.setProduct(product);
        saleItem.setQuantity(cartItem.getQuantity());
        // ...
    }
    
    // Salvare batch (atomică)
    productRepository.saveAll(productsToUpdate);
    // ...
}
```

### Caracteristici Importante

1. **Tranzacțional (@Transactional)**
   - Toate operațiunile sunt atomice
   - Dacă o operație eșuează, totul se anulează (rollback)
   - Consistență garantată

2. **Verificare Stoc**
   - Se verifică stocul ÎNAINTE de scădere
   - Eroare dacă stoc insuficient
   - Previne vânzări fără stoc

3. **Batch Update**
   - Toate produsele se actualizează simultan
   - Performanță optimizată
   - Evită inconsistențe

4. **Istoric Complet**
   - Fiecare vânzare se salvează în `sales`
   - Fiecare produs vândut în `sale_items`
   - Traceability completă

## Entități Implicate

### Sale (Vânzare)
```java
@Entity
@Table(name = "sales")
public class Sale {
    private Long id;
    private LocalDateTime saleDate;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private BigDecimal cashReceived;
    private BigDecimal changeAmount;
    private String invoiceNumber;
    private List<SaleItem> saleItems;
}
```

### SaleItem (Produs Vândut)
```java
@Entity
@Table(name = "sale_items")
public class SaleItem {
    private Long id;
    private Sale sale;
    private Product product;         // Referință la produs
    private String productName;      // Nume salvat (pentru istoric)
    private BigDecimal quantity;     // Cantitate vândută
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
```

### Product (Produs Finit)
```java
@Entity
@Table(name = "products")
public class Product {
    private Long id;
    private String name;
    private BigDecimal salePrice;
    private BigDecimal physicalStock;  // STOC ACTUALIZAT LA VÂNZARE
    private BigDecimal minimumStock;
    private Boolean isActive;
}
```

## Exemplu Complet de Utilizare

### Scenario: Vânzare Pâine și Cozonac

1. **Stocuri Inițiale**
   ```
   Product: Pâine Albă
   - Stoc: 50 buc
   - Preț: 5.00 lei
   
   Product: Cozonac
   - Stoc: 20 buc
   - Preț: 15.00 lei
   ```

2. **Adăugare în Coș - Metoda 1 (Butoane)**
   ```
   - Click pe butonul "Pâine Albă" → adaugă 1 buc
   - Click pe butonul "Pâine Albă" → adaugă încă 1 buc (total 2)
   ```

3. **Adăugare în Coș - Metoda 2 (Manual)**
   ```
   - Click "➕ Introduceți Manual"
   - Căutare: "cozonac"
   - Selectare: "Cozonac - 15.00 lei (Stoc: 20)"
   - Cantitate: 3
   - Previzualizare: "Total: 45.00 lei"
   - Click "Adaugă în Coș"
   ```

4. **Coș Rezultat**
   ```
   Pâine Albă    x 2  @ 5.00  = 10.00 lei
   Cozonac       x 3  @ 15.00 = 45.00 lei
   ─────────────────────────────────────
   TOTAL:                       55.00 lei
   ```

5. **Procesare Plată**
   ```
   - Metodă: Numerar
   - Sumă primită: 100.00 lei
   - Rest calculat: 45.00 lei
   - Click "Încasează"
   ```

6. **Rezultat în Baza de Date**
   
   **Tabel `sales`:**
   ```
   ID: 123
   sale_date: 2026-02-11 10:00:00
   total_amount: 55.00
   payment_method: Numerar
   cash_received: 100.00
   change_amount: 45.00
   invoice_number: INV-1707650400000
   ```
   
   **Tabel `sale_items`:**
   ```
   ID: 456, sale_id: 123, product_id: 1, product_name: "Pâine Albă", 
        quantity: 2, unit_price: 5.00, total_price: 10.00
   
   ID: 457, sale_id: 123, product_id: 2, product_name: "Cozonac",
        quantity: 3, unit_price: 15.00, total_price: 45.00
   ```
   
   **Tabel `products` (ACTUALIZAT):**
   ```
   Product ID: 1, name: "Pâine Albă"
   - Stoc ÎNAINTE: 50 buc
   - Stoc DUPĂ:    48 buc  (50 - 2)
   
   Product ID: 2, name: "Cozonac"
   - Stoc ÎNAINTE: 20 buc
   - Stoc DUPĂ:    17 buc  (20 - 3)
   ```

## Interfața Utilizator

### Layout POS

```
┌─────────────────────────────────────────────────────────────┐
│ Punct de Vânzare (POS)                       🕐 12:34:56   │
├─────────────────────────────────────────────────────────────┤
│ [Căutare...] [Categorii▼] [🔄 Reîncarcă] [➕ Manual]       │
├───────────────────────────────┬─────────────────────────────┤
│  PRODUSE (70%)                │  COŞ CUMPĂRĂTURI (30%)      │
│                               │                              │
│  ┌─────┐ ┌─────┐ ┌─────┐    │  Product    Cant  Preț Total│
│  │Pâine│ │Corn │ │Cov  │    │  ────────────────────────── │
│  │5 lei│ │3 lei│ │15lei│    │  Pâine      2     5.00 10.00│
│  └─────┘ └─────┘ └─────┘    │  Cozonac    3    15.00 45.00│
│                               │  [Șterge] [Șterge]          │
│  ┌─────┐ ┌─────┐ ┌─────┐    │                              │
│  │Bagel│ │Tort │ │Prăj │    │  Produse: 2                  │
│  │4 lei│ │50lei│ │2 lei│    │  TOTAL: 55.00 lei            │
│  └─────┘ └─────┘ └─────┘    │                              │
│                               │  ─────────────────────────  │
│                               │  PLATĂ                       │
│                               │  Metodă: [Numerar▼]         │
│                               │  Primită: [100.00]          │
│                               │  Rest: 45.00 lei            │
│                               │                              │
│                               │  [💳 Încasează] [🧾 Bon]    │
├───────────────────────────────┴─────────────────────────────┤
│ Status POS: Gata         Vânzări azi: 1,234.56 lei [📊]    │
└─────────────────────────────────────────────────────────────┘
```

## Verificare Conformitate Cerințe

### Cerință: "Introducere manuală produse vândute"
✅ **IMPLEMENTAT** - Dialog "➕ Introduceți Manual" cu:
- Căutare după nume/cod bare
- Selectare produs
- Introducere cantitate
- Validare stoc

### Cerință: "Scădere din total produse fabricate sau marfă achiziționată"
✅ **IMPLEMENTAT AUTOMAT** - SaleService.createSale():
- Linia 69: `product.setCurrentStock(product.getCurrentStock().subtract(quantity))`
- Funcționează pentru TOATE produsele, indiferent de origine:
  - Produse fabricate (din producție)
  - Marfă achiziționată (din NIR/facturi)
- Mecanism unic, transparent

## Statistici și Rapoarte

### Disponibile în SaleService

```java
// Rapoarte disponibile
getTodaySales()              // Vânzări din ziua curentă
getTodayTotalSales()         // Valoare totală azi
getSalesByDateRange(start, end)  // Vânzări pe interval
getTotalSalesByDateRange()   // Valoare pe interval
getTopSellingProducts()      // Top produse vândute
```

### Utilizare în UI

- Buton "📊 Raport Zilnic" în POS
- Label "Vânzări azi" în footer
- Acces la istoric complet prin modul Rapoarte

## Securitate și Validări

### Verificări Implementate

1. **Stoc Suficient**
   - Verificare înainte de adăugare în coș
   - Verificare înainte de salvare vânzare
   - Eroare clară dacă insuficient

2. **Cantitate Validă**
   - Trebuie > 0
   - Trebuie să fie număr valid
   - Trebuie <= stoc disponibil

3. **Produs Valid**
   - Trebuie să existe în baza de date
   - Trebuie să fie activ
   - Trebuie să aibă preț

4. **Tranzacționalitate**
   - @Transactional pe toate operațiunile
   - Rollback automat la eroare
   - Consistență garantată

## Concluzie

Modulul de vânzări (POS) al aplicației MAGSELL 2.0 este **COMPLET FUNCȚIONAL** și respectă arhitectura cerută:

✅ **Introducere manuală produse** - Dialog interactiv implementat  
✅ **Scădere automată stoc** - Mecanism transparent și sigur  
✅ **Suport producție + achiziții** - Funcționează pentru ambele surse  
✅ **Interfață intuitivă** - Două metode de adăugare (butoane + manual)  
✅ **Validări complete** - Stoc, cantitate, produs  
✅ **Tranzacțional** - Consistență garantată  
✅ **Istoric complet** - Toate vânzările salvate  

Sistemul permite fluxul complet: **Achiziție/Producție → Stoc → Vânzare → Scădere Stoc → Raportare**
