# PLAN UNIFICAT – IMPLEMENTĂRI RĂMASE (după Faza 3)

**Scop:** Acest document înlocuiește informațiile operaționale din cele 4 documente analizate, astfel încât ele pot fi arhivate/șterse.

**Surse analizate:**
1. `CHECKLIST_IMPLEMENTARE_FINAL.md`
2. `FAZA_3_RAPORTARE_AVANSATA_REZUMAT.md`
3. `IMPLEMENTARE_SCHEDULER_AVANSA.md`
4. `POINT_9_FINANCIAL_REPORTS_IMPLEMENTATION.md`

**Data consolidare:** 25 februarie 2026

---

## 1) Ce este deja considerat finalizat (din documente)

- Faza 1: 5/5 ✅
- Faza 2: 3/3 ✅
- Faza 3: 5/5 ✅
  - Raport Producție (costuri cumulative)
  - Raport Vânzări (profit/marjă)
  - Raport Stocuri FEFO
  - Raport Discrepanțe inventar
  - Dashboard KPI

---

## 2) Implementări rămase – backlog consolidat

## A. FAZA 4 – Integrări și optimizări (prioritate ridicată)

- [ ] **SmartBill API v2**
  - Extindere export NIR
  - Mapping complet documente + erori API + retry logic
  - Validare de business înainte de trimitere

- [ ] **Payment Processing POS**
  - Fluxuri complete pentru metode de plată
  - Validări + reconciliere tranzacții
  - Status-uri tranzacție și audit

- [ ] **Barcode Scanning (inventory intake/outtake)**
  - Flux dedicat recepție/consum stoc
  - Mapping cod bare → articol/lot
  - Traseu complet în mișcări de stoc

- [ ] **Multi-location (depozite multiple)**
  - Model date locații
  - Stocuri separate per locație
  - Transfer între locații + raportare

- [ ] **RBAC (permisiuni utilizator)**
  - Roluri: Admin / Manager / Operator
  - Restricții UI + service + endpoint level
  - Audit acces operațiuni sensibile

---

## B. FAZA 5 – Producție/Operaționalizare (prioritate ridicată)

- [ ] **Migrare DB H2 → SQLite persistent**
  - Scripturi migrare + validări date
  - Fallback/rollback plan

- [ ] **Backup & Recovery automat**
  - Programare backup periodic
  - Restaurare testată (RTO/RPO minim)

- [ ] **Audit Trail complet**
  - Evenimente cheie (cine/ce/când)
  - Evidență modificări critice

- [ ] **Security hardening**
  - Criptare date sensibile
  - Management chei/API keys
  - Politici acces / rotație chei

- [ ] **Performance tuning**
  - Caching selectiv
  - Query optimization + indexare
  - Monitoring latențe/throughput

---

## C. Restanțe specifice din documentele de raportare financiară (Point 9)

### C1. Enhancements calcul costuri (tehnic)
- [ ] FEFO complet pentru costuri (nu doar fallback simplificat)
- [ ] Integrare cost manoperă din scheduling (Point 8)
- [ ] Recursivitate completă costuri pe rețete multi-level
- [ ] VAT deductibil din facturi de achiziție

### C2. Rapoarte financiare avansate (analitice)
- [ ] Cost variance analysis (standard vs actual)
- [ ] Profit margin by category
- [ ] Seasonal trend analysis
- [ ] Cost allocation refinements

### C3. UI/UX & operațional
- [ ] Testare manuală completă pe rapoartele noi (UAT)
- [ ] Stabilizare flux export PDF pentru toate tipurile financiare
- [ ] Verificare consistență TextArea vs TableView în pagina rapoarte

---

## D. Restanțe/next steps din Scheduler (Point 8)

- [ ] Integrare completă în navigația principală (menu/navbar routing)
- [ ] Binding final FXML + action handlers pentru toate fluxurile de scheduling
- [ ] Auto-scheduling engine (alocare optimă)
- [ ] Resource leveling (echilibrare încărcare resurse)
- [ ] Alerts & notifications (over-capacity, deadlines, indisponibilitate)
- [ ] Integrare execuție producție în timp real vs plan
- [ ] Raportare avansată scheduler (Gantt/heatmap/bottleneck)

---

## 3) Ordine recomandată de implementare (pragmatic)

### Sprint 1 (stabilizare operațională)
1. Migrare SQLite
2. Backup & Recovery
3. Audit Trail
4. Security minim viabil

### Sprint 2 (integrare business)
1. SmartBill v2
2. Payment Processing POS
3. Barcode inventory flow

### Sprint 3 (scalare și control)
1. Multi-location
2. RBAC
3. Performance tuning

### Sprint 4 (analitică avansată)
1. FEFO complet + labor cost integration
2. VAT deductibil + cost variance
3. Scheduler advanced analytics

---

## 4) Criterii de acceptanță globale (Done Definition)

Un item se consideră finalizat doar dacă are:
- cod implementat + build verde,
- testare minimă (unit/integration unde relevant),
- validare funcțională (UAT pe fluxul principal),
- documentație de utilizare/operare scurtă,
- fără regresii în modulele existente.

---

## 5) Notă de consolidare

Acest document este destinat să înlocuiască central informația operațională din cele 4 documente sursă, pentru mentenanță mai simplă.
