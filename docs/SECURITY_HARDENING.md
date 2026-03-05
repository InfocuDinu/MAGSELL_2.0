# Security hardening

Acest document descrie măsurile implementate pentru securizare minim viabilă în producție.

## Implementări tehnice

### 1) Criptare/Protecție date sensibile

- parole utilizatori migrate la **BCrypt** (`spring-security-crypto`)
- hash-uri legacy `HASH_...` sunt migrate automat la login reușit
- policy parole:
  - minim 8 caractere
  - obligatoriu litere + cifre

### 2) Management chei / API keys

- token SmartBill este citit exclusiv din variabile de mediu
- validare startup pentru secrete:
  - token lipsă/placeholder -> warning sau fail (în funcție de policy)
  - cheie master lipsă/slabă -> warning sau fail

### 3) Politici acces / rotație chei

Configurări:

- `APP_SECURITY_REQUIRE_ENV_SECRETS` (prod recomandat: `true`)
- `APP_SECURITY_MASTER_KEY`
- `APP_SECURITY_MASTER_KEY_ID`
- `APP_SECURITY_MASTER_KEY_ROTATED_AT` (format `YYYY-MM-DD`)
- `APP_SECURITY_MAX_KEY_AGE_DAYS` (default `90`)

Comportament:

- dacă cheia e mai veche decât policy -> warning/fail (după `require-env-secrets`)
- în producție (`application-prod.properties`) policy este strictă (`true`)

## Recomandări operaționale

- rotește cheia master la max 90 zile
- schimbă imediat token-urile compromise
- nu comite chei reale în git
- folosește vault/secret manager pentru producție
