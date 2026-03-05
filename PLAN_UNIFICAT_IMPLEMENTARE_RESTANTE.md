# PLAN UNIFICAT – DOAR CE MAI ESTE DE FĂCUT

**Actualizat:** 5 martie 2026

---

## 1) Backlog rămas (activ)

## A. Restanțe raportare financiară (Point 9)

### A1. Enhancements calcul costuri (tehnic)
- [ ] Integrare cost manoperă din scheduling (Point 8)
- [ ] Recursivitate completă costuri pe rețete multi-level
- [ ] VAT deductibil din facturi de achiziție

### A2. Rapoarte financiare avansate (analitice)
- [ ] Cost variance analysis (standard vs actual)
- [ ] Profit margin by category
- [ ] Seasonal trend analysis
- [ ] Cost allocation refinements

### A3. UI/UX & operațional
- [ ] Testare manuală completă pe rapoartele noi (UAT)
- [ ] Stabilizare flux export PDF pentru toate tipurile financiare
- [ ] Verificare consistență TextArea vs TableView în pagina rapoarte

---

## 2) Ordine recomandată pentru închidere

1. Integrare cost manoperă + VAT deductibil
2. Cost variance + category margin + seasonal trend
3. Cost allocation refinements
4. UAT complet + stabilizare export PDF + validare consistență UI

---

## 3) Done Definition (pentru fiecare item rămas)

- cod implementat + build verde
- testare minimă (unit/integration unde relevant)
- validare funcțională (UAT pe fluxul principal)
- documentație de utilizare/operare scurtă
- fără regresii în modulele existente
