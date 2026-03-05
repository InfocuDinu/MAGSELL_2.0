# Audit Trail complet

Implementarea audit trail oferă trasabilitate operațională și evidență pentru modificări critice.

## Ce este auditat

- **AUTH**: permisiuni allow/deny (RBAC)
- **BUSINESS**: evenimente operaționale (backup, restore, transfer intern)
- **DATA_CHANGE**: modificări critice cu stare `before` și `after`

## Date stocate

Tabela: `access_audit_log`

Câmpuri cheie:
- `username`, `full_name`, `role_name` (cine)
- `event_type`, `action`, `resource`, `outcome` (ce)
- `created_at` (când)
- `details` (context)
- `before_state`, `after_state` (evidență schimbare)

## Fluxuri acoperite

- autorizări RBAC (navigare/operațiuni sensibile)
- salvare/restaurare setări
- backup manual și restore DB
- operațiuni stoc (recepție/consum/ajustare/return/waste/transfer) la nivel de lot/batch

## Query-uri utile

```sql
-- Ultimele evenimente audit
SELECT id, created_at, username, event_type, action, resource, outcome
FROM access_audit_log
ORDER BY created_at DESC
LIMIT 100;

-- Modificări critice (with before/after)
SELECT created_at, username, action, resource, before_state, after_state
FROM access_audit_log
WHERE event_type = 'DATA_CHANGE'
ORDER BY created_at DESC;
```
