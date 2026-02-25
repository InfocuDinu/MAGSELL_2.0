# Verificare implementare față de checklist-ul tău

Mai jos este statusul real pe fiecare punct, pe baza codului actual:

## A) Gestiune materii prime & auxiliare
- ✅ Loturi + expirare + FEFO
- ⚠️ Intrare în stoc pe lot cu expirare + preț (da)
- ❌ Furnizor pe lot (nu este stocat)
- ✅ Unități de măsură + conversii (kg↔g, l↔ml, bax↔buc)
- ✅ Documente mișcare stoc:
  - ✅ intrare (NIR), ✅ consum FEFO, ✅ jurnal mișcări
  - ✅ transfer intern / ajustare / retur furnizor / pierderi-casare
- ✅ Jurnal auditabil pe utilizator + motiv + timestamp
- ✅ Inventariere asistată (zone/depozite, diferențe scriptic/faptic, propuneri ajustare)
- ✅ Reaprovizionare inteligentă (stoc siguranță + consum mediu/zi + listă “de comandat azi” + export CSV)

## B) Producție
- ✅ Fișă tehnologică completă
  - ✅ gramaje rețetă
  - ✅ randament, timp preparare/coacere, pierderi tehnologice
- ❌ Planificator producție pe schimb/zi + capacități
- ✅ Ordine de producție (planificat/în lucru/finalizat/anulat)
- ✅ Consum real vs standard (raport devieri)
- ✅ Produse semifinite / rețete multi-nivel (subproduse consumate în alte rețete)
- ✅ Wastage management (rebuturi, pierderi, expirat, donații) + impact cost/profit

## C) POS
- ⚠️ POS rapid (căutare + UI ok)
- ❌ shortcut-uri tastatură, favorites, scanner coduri
- ❌ Politici comerciale (promoții, discounturi, happy hour)
- ⚠️ Loyalty/clienți
  - ✅ puncte + raport
  - ❌ cupoane, segmentare, istoric avansat
- ✅ Precomenzi (model + raport)
- ❌ flux complet UI (creare/urmărire)
- ❌ Offline resilience (sync ulterior)

## D) Financiar/Fiscal/Compliance
- ⚠️ Integrare casă fiscală
  - ✅ mock + stub real
  - ❌ driver certificat / Z / reprint etc.
- ⚠️ e-Factura/SPV
  - ✅ import XML
  - ❌ validare avansată + reconciliere automată
- ⚠️ Rapoarte contabile
  - ✅ profitabilitate produs/categorie
  - ❌ TVA breakdown, profit/zi/lună, cost producție complet
- ⚠️ Export contabil
  - ✅ CSV simplu
  - ❌ SAF‑T / jurnale TVA / ERP standard

## E) Raportare & decizie
- ⚠️ Dashboard executiv real-time
  - ✅ alerte, stoc scăzut
  - ❌ vânzări/oră, top produse, marjă zilnică, waste%
- ⚠️ Forecast cerere
  - ✅ basic (medie + uplift)
  - ❌ sezon, meteo, sărbători
- ⚠️ Analiză rentabilitate
  - ✅ produs/categorie
  - ❌ client/canal/proces
- ✅ Alerting (stoc critic, expirare, deviații consum, marjă negativă)

## 3) Interfață modernă / UX
- ⚠️ Design system unificat (parțial CSS)
- ❌ Fluxuri “task-first” / wizard-uri
- ⚠️ Feedback vizual (basic)
- ❌ Loading/skeleton pentru tabele mari
- ❌ Accesibilitate completă
- ⚠️ Dark mode real (nu)
- ❌ Micro-interacțiuni (undo, toast-uri)

## 4) Arhitectură
- ⚠️ Use-case layer dedicat (nu)
- ❌ DTO-uri UI/API
- ⚠️ Validation centralizat (parțial)
- ❌ Observabilitate (correlation IDs)
- ❌ Test strategy completă

## ✅ Concluzie
Am implementat core-ul funcțional (loturi/FEFO, mișcări, producție, consum standard vs real, profitabilitate, alertare, forecast simplu, export contabil CSV, onboarding), dar multe puncte din checklist rămân neimplementate (în special inventariere, promoții POS, offline, SAF‑T/TVA, waste, rețete multi‑nivel, use-case layer).

---

# Plan de continuare (pentru tot ce a rămas de implementat)

## Faza 1 (prioritate foarte mare – conformitate + operațional)
1. **✅ Conversii unități de măsură (implementat)**
  - tabel conversii (kg↔g, l↔ml, bax↔buc)
  - folosire în stoc, rețete și consum
2. **Documente mișcare stoc complete**
   - transfer intern, ajustare, retur furnizor, pierderi/casare
   - jurnal cu motiv + utilizator + timestamp
3. **✅ Wastage management (implementat)**
   - tipuri pierderi (rebut, expirat, donație)
   - impact direct în cost/profit
4. **✅ Inventariere asistată (implementat)**
   - zone/depozite
   - diferențe scriptic/faptic
   - propuneri ajustare
5. **✅ Reaprovizionare inteligentă (implementată)**
   - stoc de siguranță + consum mediu/zi
  - listă automată “de comandat azi” (+ export CSV)

## Faza 2 (profitabilitate + proces)
6. **✅ Fișă tehnologică completă (implementată)**
   - randament, timp preparare/coacere, pierderi tehnologice
7. **✅ Produse semifinite / rețete multi‑nivel (implementat)**
   - subproduse (cremă, glazură)
   - consum în alte rețete
8. **Planificator producție avansat**
   - schimburi/ore
   - capacități (cuptoare, personal, linii)
9. **Rapoarte contabile avansate**
   - TVA breakdown
   - profit zi/lună
   - cost producție complet

## Faza 3 (POS & client)
10. **POS ultra‑rapid**
    - shortcut-uri tastatură
    - butoane favorite
    - scanare coduri
11. **Politici comerciale**
    - promoții (2+1, happy hour)
    - discount pe categorie
12. **Loyalty avansat**
    - cupoane
    - segmentare clienți
    - istoric cumpărări
13. **Precomenzi UI complet**
    - creare/urmărire/status
    - avans & livrare
14. **Offline resilience**
    - mod local + sincronizare

## Faza 4 (compliance + scalare)
15. **Integrare fiscală reală**
    - driver certificat + reprint + raport Z
16. **e-Factura/SPV complet**
    - validare XML + reconciliere automată
17. **Exporturi contabile standard**
    - SAF‑T
    - jurnale TVA
    - export ERP

## Faza 5 (UX & arhitectură)
18. **UX modern complet**
    - wizard-uri task-first
    - loading/skeleton
    - accesibilitate
    - dark mode real
    - micro‑interacțiuni (toast/undo)
19. **Use-case layer + DTO-uri**
    - controllers fără entități JPA
20. **Observabilitate & audit**
    - logging structurat + correlation IDs
21. **Test strategy**
    - unit + integration + e2e pe fluxuri critice
