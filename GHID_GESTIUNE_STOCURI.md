# Ghid de Utilizare - Modul Gestiune Stocuri

## Prezentare Generală

Modulul de Gestiune Stocuri permite gestionarea completă a ingredientelor folosite în patiserie. Puteți adăuga, modifica, vizualiza și șterge ingrediente din baza de date.

## Funcționalități Principale

### 1. Vizualizare Ingrediente

**Tabel Ingrediente** (partea stângă a ecranului):
- ID - Identificator unic
- Nume - Numele ingredientului
- Unitate - Unitatea de măsură (KG, L, BUC, GRAM, ML)
- Stoc - Cantitatea curentă în stoc
- Stoc Minim - Pragul minim de alertă
- Preț - Prețul ultimei achiziții
- Cod Bare - Codul de bare (opțional)

**Buton "🔄 Reîncarcă"**: Reîmprospătează lista de ingrediente din baza de date.

### 2. Adăugare Ingredient Nou

**Pași:**
1. Click pe butonul "➕ Adaugă Ingredient" (sus, dreapta)
   - SAU click pe butonul "Salvează" când formularul este gol
2. Completați formularul din partea dreaptă:
   - **Nume*** (obligatoriu): Numele ingredientului
   - **Cantitate**: Cantitatea curentă în stoc
   - **Unitate*** (obligatoriu): Selectați unitatea de măsură
   - **Preț Achiziție**: Prețul ultimei achiziții
   - **Stoc Minim**: Pragul de alertă pentru stoc scăzut
   - **Cod Bare**: Codul de bare al produsului (opțional)
3. Click pe butonul "Salvează" (verde)
4. Ingredientul va fi adăugat în baza de date și va apărea în tabel

**Mesaj de confirmare**: "Ingredient adăugat cu succes!"

### 3. Modificare Ingredient Existent

**Pași:**
1. Selectați ingredientul din tabel (click pe linia dorită)
2. Formularul se va completa automat cu datele ingredientului
3. Modificați câmpurile dorite
4. Click pe butonul "Salvează" (verde)
5. Modificările vor fi salvate în baza de date

**Mesaj de confirmare**: "Ingredient actualizat cu succes!"

### 4. Ștergere Ingredient

**Pași:**
1. Selectați ingredientul din tabel (click pe linia dorită)
2. Click pe butonul "Șterge" (roșu)
3. Confirmați ștergerea în dialogul care apare
4. Ingredientul va fi șters din baza de date

**Mesaj de confirmare**: "Ingredient șters cu succes!"

**ATENȚIE**: Ștergerea este permanentă și nu poate fi anulată!

### 5. Anulare Editare

**Buton "Anulează"**: 
- Golește toate câmpurile formularului
- Deselectează ingredientul din tabel
- Pregătește formularul pentru o nouă înregistrare

## Validări și Reguli

### Câmpuri Obligatorii
- **Nume**: Trebuie completat, nu poate fi gol
- **Unitate de Măsură**: Trebuie selectată din listă

### Câmpuri Numerice
Următoarele câmpuri acceptă doar numere (cu zecimale):
- Cantitate
- Preț Achiziție
- Stoc Minim

**Format acceptat**: 
- Întregi: 10, 25, 100
- Zecimale: 2.5, 10.75, 0.5

### Unități de Măsură Disponibile
- **KG** - Kilogram
- **L** - Litru
- **BUC** - Bucată
- **GRAM** - Gram
- **ML** - Mililitru

## Mesaje de Eroare

### Erori de Validare
- "Numele ingredientului este obligatoriu!" - Când câmpul Nume este gol
- "Unitatea de măsură este obligatorie!" - Când nu este selectată o unitate
- "Cantitatea trebuie să fie un număr valid!" - Format numeric incorect
- "Prețul trebuie să fie un număr valid!" - Format numeric incorect
- "Stocul minim trebuie să fie un număr valid!" - Format numeric incorect

### Erori de Operare
- "Selectați un ingredient din tabel pentru a-l șterge!" - Când încercați să ștergeți fără selecție
- "Eroare la salvarea ingredientului: [detalii]" - Eroare de bază de date
- "Eroare la încărcarea ingredientelor: [detalii]" - Eroare la citirea datelor

## Bara de Status

În partea de jos a ecranului se afișează:
- **Mesaje de status**: Confirmări și informații despre operațiuni
- **Total ingrediente**: Numărul total de ingrediente din baza de date

**Exemple de mesaje**:
- "Sistem gata de utilizare"
- "Ingrediente încărcate: 15"
- "Selectat: Faina"
- "Ingredient adăugat cu succes!"
- "Formular golit"

## Fluxul de Lucru Recomandat

### Recepție Marfă
1. Click "Adaugă Ingredient"
2. Completați datele ingredientului nou recepționat:
   - Nume
   - Cantitate primită
   - Unitate de măsură
   - Preț de achiziție
   - Stoc minim (pentru alertă automată)
   - Cod de bare (dacă există)
3. Salvați

### Actualizare Stoc
1. Selectați ingredientul din tabel
2. Modificați cantitatea
3. Actualizați prețul (dacă s-a schimbat)
4. Salvați

### Inventariere
1. Click "Reîncarcă" pentru a vedea datele actuale
2. Verificați fiecare ingredient din tabel
3. Selectați și actualizați cantitățile după numărătoare
4. Salvați modificările

## Sfaturi și Trucuri

### Navigare Rapidă
- Folosiți mouse-ul pentru a selecta rapid din tabel
- Apăsați Tab pentru a naviga între câmpurile formularului
- Double-click pe un ingredient din tabel pentru selecție rapidă

### Gestionare Eficientă
- Setați întotdeauna un **Stoc Minim** pentru ingredientele critice
- Adăugați **Cod de bare** pentru a facilita scanarea
- Actualizați **Prețul** la fiecare recepție pentru calcule corecte
- Folosiți **Reîncarcă** pentru a sincroniza datele

### Prevenirea Erorilor
- Verificați unitatea de măsură înainte de a salva
- Nu lăsați câmpul Nume gol
- Folosiți punctul (.) pentru zecimale, nu virgula (,)
- Verificați de două ori înainte de a șterge un ingredient

## Depanare

### Problema: Nu se salvează ingredientul
**Soluții**:
1. Verificați că ați completat câmpurile obligatorii (Nume, Unitate)
2. Verificați formatul numerelor (folosiți punct pentru zecimale)
3. Verificați bara de status pentru mesaje de eroare
4. Încercați să reîmprospătați pagina (buton Reîncarcă)

### Problema: Tabelul este gol
**Soluții**:
1. Click pe butonul "Reîncarcă"
2. Verificați că există ingrediente în baza de date
3. Adăugați un ingredient nou pentru testare

### Problema: Formularul nu se golește
**Soluții**:
1. Click pe butonul "Anulează"
2. Click pe butonul "Adaugă Ingredient"

## Integrare cu Alte Module

### Modul Producție
- Ingredientele adăugate aici sunt folosite în rețete
- Stocul se reduce automat la execuția producției
- Alertele de stoc minim ajută la planificarea aprovizionării

### Modul Facturi (Import SPV)
- Importul de facturi actualizează automat stocurile
- Prețurile sunt actualizate cu media ponderată
- Ingredientele noi sunt adăugate automat

### Modul Rapoarte
- Rapoartele de stocuri folosesc datele din acest modul
- Statisticile de cost se bazează pe prețurile actualizate
- Alertele de stoc scăzut apar în dashboard

## Siguranța Datelor

- Toate modificările sunt salvate **imediat** în baza de date
- Nu există buton "Undo" - verificați datele înainte de a salva
- Ștergerea este **permanentă** - confirmați cu atenție
- Backup-ul bazei de date se face automat (vezi Modul Administrare)

## Suport

Pentru probleme sau întrebări:
1. Verificați acest ghid pentru soluții
2. Contactați administratorul de sistem
3. Verificați fișierul de log: `logs/bakery-manager.log`

---

**Versiune**: 1.0.0  
**Modul**: Gestiune Stocuri (Inventory Management)  
**Status**: Complet funcțional ✅
