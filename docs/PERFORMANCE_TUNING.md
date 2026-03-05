# Performance Tuning – BakeryManager Pro

## Ce s-a implementat

### 1) Caching selectiv

S-a activat cache-ul Spring cu provider **Caffeine**:

- Caches produse: `productsAll`, `productsActive`, `productsAvailable`, `productsLowStock`, `productsById`, `productsByBarcode`
- Caches ingrediente: `ingredientsAll`, `ingredientsAvailable`, `ingredientsLowStock`, `ingredientsById`, `ingredientsByBarcode`

Servicii optimizate:

- `ProductService` – metodele de read sunt cache-uite, iar mutațiile fac invalidare completă pe cache-urile relevante.
- `IngredientService` – același model (read cache + evict pe write/update/delete).

Config implicit (`application.properties`):

- `spring.cache.type=caffeine`
- `spring.cache.caffeine.spec=maximumSize=2000,expireAfterWrite=120s,recordStats`

Config producție (`application-prod.properties`):

- `maximumSize=5000`
- `expireAfterWrite=90s`

## 2) Query optimization + indexare

### Optimizare query-uri pe vânzări curente

Interogările pentru "today sales" au fost rescrise din forma bazată pe `DATE(column)` (care poate bloca utilizarea indexului) în formă bazată pe interval:

- `sale_date >= startOfDay`
- `sale_date < nextDayStart`

Afectează:

- `SaleRepository`
- `SaleService#getTodaySales`
- `SaleService#getTodayTotalSales`

### Indexuri adăugate în entități

- `sales`: `sale_date`, `operator`, `payment_method`
- `stock_movements`: `movement_date`, `ingredient_id`, `movement_type`, compus `ingredient_id,movement_type,movement_date`
- `ingredient_batches`: `ingredient_id`, compus FEFO `ingredient_id,expiry_date,received_date`, `batch_code`
- `ingredients`: `name`, `barcode`, `current_stock`, `minimum_stock`
- `products`: `name`, `barcode`, `is_active`, `physical_stock`
- `sale_items`: `sale_id`, `product_id`, `created_at`

> Notă: aplicația folosește `spring.jpa.hibernate.ddl-auto=update` în profile non-prod, astfel indexurile se sincronizează automat la pornire.

## 3) Monitoring latență/throughput

S-a adăugat `PerformanceMonitoringAspect`:

- instrumentează apelurile din `com.bakerymanager.service..*`
- loghează warning pentru apeluri lente peste prag
- publică periodic un sumar top metode după latența medie

Config:

- `app.performance.monitor.enabled` (default `true`)
- `app.performance.monitor.slow-threshold-ms` (default `250`)
- `app.performance.monitor.report-interval-ms` (default `300000`)

Variabile opționale de mediu:

- `APP_PERFORMANCE_MONITOR_ENABLED`
- `APP_PERFORMANCE_SLOW_THRESHOLD_MS`
- `APP_PERFORMANCE_REPORT_INTERVAL_MS`

## Recomandări operaționale

1. În producție, porniți cu prag `200ms` și ajustați după trafic real.
2. Verificați periodic logurile `[PERF]` pentru top metode lente.
3. Dacă sunt multe invaldări de cache, reduceți granularitatea evict-ului pe metodele de mutație cele mai frecvente.
4. Pentru volume mari de date, validați periodic existența indexurilor în SQLite (`PRAGMA index_list`).
