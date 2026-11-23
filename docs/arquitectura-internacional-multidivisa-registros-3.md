# KETZI — Arquitectura técnica internacional y multidivisa integrada con Registros 3.0

**Estado:** diseño técnico, no implementación  
**Repositorio de referencia:** `axlgil20-alt/New-new`  
**HEAD analizado:** `ac4529c96ef18602c90d53bdeb6dfa875de58892`  
**Room analizado:** versión 14  
**Fecha del diseño:** 23 de noviembre de 2026  

## 1. Alcance y límites de esta entrega

Este documento define una arquitectura concreta para incorporar país configurable, moneda principal, monedas por cuenta y movimientos en moneda extranjera, tipos de cambio históricos, transferencias multicurrency y consolidación patrimonial en KETZI. La integración cubre Registros 3.0, HOY, Quincenas 2.0, Pagos, Smart Cart, Metas, tarjetas, cuotas y reportes.

No se modificó el repositorio de trabajo, no se cambió Room, no se creó ninguna migración, no se compiló la aplicación y no se creó commit ni push. Las entidades, tablas y campos que no aparecen en la sección de “existente” son propuestas nuevas y quedan **por confirmar** antes de implementarse.

La regla central del diseño es la siguiente:

> Cada importe se guarda en la moneda en la que ocurrió. La conversión a moneda principal ocurre únicamente en una frontera explícita de lectura/consolidación. Nunca se convierte un importe ya convertido ni se reemplaza el importe original.

## 2. Resumen ejecutivo de la solución

La solución conserva la arquitectura Android nativa Kotlin + Jetpack Compose + Room + Coroutines + offline-first. Se añaden cuatro capacidades de dominio:

1. **Catálogo y metadatos de moneda:** códigos ISO 4217, escala decimal, símbolo y nombre local.
2. **Preferencias internacionales:** país de residencia/configuración, moneda principal y zona horaria financiera.
3. **Historial de tipos de cambio:** tasa fechada, fuente, par de monedas, fecha de obtención, precisión y estado de procedencia.
4. **Operación multicurrency:** detalle persistido de conversión y comisiones para transferencias, pagos y otros movimientos compuestos.

`FinancialAccountEntity.currencyCode` ya existe y se conserva. Sin embargo, el HEAD analizado todavía no tiene un modelo completo de catálogo de monedas, una tabla de tipos de cambio, una tabla de preferencias internacionales ni un detalle de transferencia con moneda de origen y destino.

Para no romper la lógica vigente, `amountMinor: Long` continúa siendo la fuente primaria del importe operativo de una cuenta. Se propone añadir a `TransactionEntity` columnas explícitas para moneda original, escala, importe original, importe consolidado y referencia de tasa. Los campos heredados `amount: Double` y `amountMinor: Long` se conservan por compatibilidad; en nuevas escrituras se derivan desde el importe nativo de la cuenta y no se usan para aritmética interna.

## 3. Inventario verificable del HEAD

### 3.1 Estructuras existentes

| Área | Estructura existente | Evidencia del HEAD | Tratamiento |
|---|---|---|---|
| Cuentas | `FinancialAccountEntity` / tabla `accounts` | `AccountModels.kt` | Conservar; `currencyCode` ya existe con valor por defecto `GTQ`. |
| Movimientos | `TransactionEntity` / tabla `transactions` | `Models.kt` | Conservar campos actuales y ampliar de forma aditiva. |
| Precisión | `MoneyMinorUnits` | `MoneyMinorUnits.kt` | Conservar como adaptador heredado; generalizar con escala por moneda por confirmar. |
| Tipos de asiento | `TransactionEntryKind` | `AccountModels.kt` | Conservar `STANDARD`, transferencias, pagos, cuotas y ajustes. |
| Transferencias | `transferGroupId`, `counterpartAccountId`, `AccountsRepository.transferBetweenAccounts` | `AccountsRepository.kt` | Reusar como agrupador; añadir detalle multicurrency. |
| Libro mayor | `AccountLedgerDao` | `AccountDao.kt` | Añadir consultas por moneda, grupo y referencia de tasa. |
| Presupuesto | `BudgetLimitEntity`, `BudgetRepository` | `Models.kt`, `BudgetRepository.kt` | Mantener reglas; definir la moneda del presupuesto explícitamente. |
| Metas | `SavingsGoalEntity` | `Models.kt` | Añadir moneda de meta y valores consolidados solo si se confirma el alcance. |
| Pagos | `PaymentCommitmentEntity` | `Models.kt` | Conservar importe original; añadir moneda/cuenta y conversión cuando corresponda. |
| Cuotas | `InstallmentPlanEntity`, `InstallmentScheduleItemEntity` | `AccountModels.kt` | Cada plan pertenece a una cuenta y por tanto a su moneda; no mezclar monedas. |
| Ciclos de tarjeta | `CreditCardCycleCalculator` | servicio existente | Calcular cada tarjeta en su moneda nativa; consolidar después. |
| Saldos | `AccountBalanceCalculator` | servicio existente | Calcular primero por cuenta/moneda; consolidar en una capa separada. |
| Registros | `BudgetViewModel`, filtros, estados de `Double` | `BudgetViewModel.kt` | Mantener API visual provisional y añadir modelos de presentación con moneda. |
| Mis Cuentas | `AccountsViewModel` | `AccountsViewModel.kt` | Mostrar moneda de cuenta y total consolidado separado. |
| Quincenas | `QuincenaFilterHelper` | helper existente | No redefinir; solo cambiar la suma a un agregador monetario con conversión posterior. |
| Room | `AppDatabase` versión 14 | `AppDatabase.kt` | Próxima versión será una migración aditiva, pendiente de aprobación. |

### 3.2 Límites actuales que deben corregirse en una fase posterior

- `MoneyMinorUnits` está fijado a escala 2 y describe centavos de GTQ/USD; no cubre monedas con escala 0, 3 o reglas especiales.
- `TransactionEntity.amount` sigue siendo `Double`; se debe evitar usarlo para nuevos cálculos.
- `BudgetLimitEntity`, `SavingsGoalEntity`, `RecurringExpenseEntity`, `PaymentCommitmentEntity`, `SmartCartItemEntity` y plantillas usan `Double` sin moneda propia.
- `BudgetViewModel` expone símbolos y totales como `Double`; su contrato de UI deberá evolucionar sin cambiar las reglas contables.
- `AccountsRepository.transferBetweenAccounts` acepta un solo `amountMinor`, por lo que actualmente modela transferencias de una sola moneda.
- `AccountBalanceCalculator` suma todas las cuentas como si compartieran moneda; eso no es válido cuando existen cuentas distintas de la moneda principal.

## 4. Modelo de moneda y precisión

### 4.1 Moneda ISO

Se propone un catálogo nuevo, **por confirmar**, basado en códigos ISO 4217 de tres letras. No se debe aceptar texto libre como identidad monetaria.

```kotlin
@Entity(
    tableName = "currency_catalog",
    indices = [Index(value = ["isoCode"], unique = true)]
)
data class CurrencyCatalogEntity(
    @PrimaryKey val isoCode: String,
    val numericCode: String?,
    val displayName: String,
    val symbol: String,
    val minorUnitScale: Int,
    val isActive: Boolean = true
)
```

La clase anterior es una propuesta de diseño, no existe en el HEAD. Debe validarse si se usará un catálogo estático compilado, una tabla Room sembrada o ambas opciones. Para el funcionamiento offline-first, el catálogo mínimo debe estar incluido localmente y poder actualizarse sin bloquear la app.

### 4.2 Precisión

La precisión se determina por moneda, no por usuario ni por pantalla:

- `minorUnitScale = 0`: monedas sin subunidad operativa.
- `minorUnitScale = 2`: caso habitual de GTQ, USD y muchas monedas.
- `minorUnitScale = 3`: monedas que operan con milésimas.
- La escala debe ser configurable por catálogo, pero no editable libremente por el usuario después de crear movimientos.

Se propone generalizar `MoneyMinorUnits` con funciones parametrizadas por escala:

```kotlin
object CurrencyAmountMath {
    fun toBigDecimal(minor: Long, scale: Int): BigDecimal
    fun fromBigDecimal(value: BigDecimal, scale: Int, rounding: RoundingMode): Long
    fun fromDecimalText(value: String, scale: Int, rounding: RoundingMode): Long
}
```

Esto es **nuevo y por confirmar**. `MoneyMinorUnits` debe permanecer como adaptador compatible para los caminos actuales de escala 2 durante la migración.

Regla propuesta: redondeo monetario final `HALF_UP`, salvo que una regla regulatoria o de proveedor confirme otro modo. Las divisiones de tasa se calculan con `BigDecimal` y `DOMAIN_MATH_CONTEXT`; el resultado almacenado se redondea únicamente al guardar el importe de destino.

## 5. Preferencias internacionales

Se propone una tabla nueva, **por confirmar**, para separar preferencias de usuario de `FinancialAccountEntity`:

| Tabla propuesta | Campo | Tipo | Regla |
|---|---|---|---|
| `international_preferences` | `id` | `Int` PK fija | Una sola fila local, id `1`. |
|  | `countryCode` | `String` nullable | ISO 3166-1 alpha-2; por confirmar si se requiere país fiscal distinto al de residencia. |
|  | `primaryCurrencyCode` | `String` | ISO 4217; inicial `GTQ`. |
|  | `financialZoneId` | `String` | Inicial `America/Guatemala`; no sustituir automáticamente `QuincenaFilterHelper.FINANCIAL_ZONE`. |
|  | `localeTag` | `String` | Preferencia de formato, por ejemplo `es-GT`. |
|  | `updatedAtMillis` | `Long` | Auditoría local. |

La configuración de país no debe cambiar la moneda de movimientos existentes ni alterar fechas históricas. Cambiar la moneda principal solo cambia la presentación consolidada y los nuevos presupuestos creados después del cambio, sujeto a las reglas de la sección 11.

## 6. Cambios de entidades y tablas

### 6.1 `FinancialAccountEntity` / `accounts`

`currencyCode` ya existe y debe conservarse como identidad monetaria de la cuenta.

| Campo actual relevante | Campo nuevo/modificado propuesto | Tipo | Justificación | Módulos dependientes |
|---|---|---|---|---|
| `currencyCode = "GTQ"` | Validarlo contra `currency_catalog.isoCode` | `String` | Evita códigos inválidos. | Accounts, UI, importación, reportes |
| `openingBalanceMinor` | `openingBalanceScale` | `Int` nullable/default 2 | Permite interpretar correctamente el `Long` histórico cuando la cuenta no usa escala 2. | `AccountBalanceCalculator`, AccountsRepository |
| `creditLimitMinor` | `creditLimitScale` | `Int` nullable/default 2 | El límite debe usar la escala de la moneda de la tarjeta. | Tarjetas, cuotas |
| existente | `currencyDisplayOverride` | `String?`, por confirmar | Solo si se permite personalizar símbolo; no debe cambiar ISO. | UI |

Recomendación: no duplicar `currencyCode` en cada cuenta mediante una nueva columna paralela. La cuenta es la fuente de moneda para el saldo nativo; el movimiento conserva además su moneda original para auditoría.

### 6.2 `TransactionEntity` / `transactions`

Se conservan `amount`, `amountMinor`, `accountId`, `entryKind`, `transferGroupId`, `counterpartAccountId`, importación y conciliación. Los nuevos campos son aditivos y deben ser nullable durante la migración.

| Campo actual | Campo nuevo propuesto | Tipo Room | Justificación técnica | Dependencias |
|---|---|---|---|---|
| `amount: Double` | Conservar sin usar para aritmética nueva | `REAL` | Compatibilidad con UI, tests y datos heredados. | Todos los consumidores heredados |
| `amountMinor: Long` | Conservar como importe operativo en moneda de la cuenta | `INTEGER` | Mantiene el libro mayor actual. | Saldos, cuotas, tarjetas |
| — | `originalAmountMinor` | `INTEGER NULL` | Importe en moneda original del evento. | Registros, importación, detalle |
| — | `originalCurrencyCode` | `TEXT NULL` | Moneda ISO del importe original. | Registros, reportes |
| — | `originalAmountScale` | `INTEGER NULL` | Interpretación exacta del `Long`. | Formateo y migración |
| — | `accountAmountMinor` | `INTEGER NULL` | Importe en moneda nativa de la cuenta. Para operaciones normales puede igualar `originalAmountMinor`. | Saldos por cuenta |
| — | `accountCurrencyCode` | `TEXT NULL` | Snapshot de moneda de cuenta al registrar. Protege el histórico ante cambios administrativos futuros. | Auditoría |
| — | `accountAmountScale` | `INTEGER NULL` | Escala del importe de la cuenta. | Saldos |
| — | `primaryAmountMinor` | `INTEGER NULL` | Importe convertido a la moneda principal en la frontera de consolidación. | Reportes, patrimonio, HOY |
| — | `primaryCurrencyCode` | `TEXT NULL` | Moneda principal usada para esa conversión. | Reportes |
| — | `conversionRateId` | `INTEGER NULL` | FK lógica hacia el registro de tasa aplicado. | FX, auditoría |
| — | `conversionRateNumerator` | `TEXT NULL` | Numerador decimal serializado, por ejemplo `7.8125`. | Reproducibilidad offline |
| — | `conversionRateDenominator` | `TEXT NULL` | Denominador decimal serializado, por ejemplo `1`. | Reproducibilidad offline |
| — | `conversionRateAsOfMillis` | `INTEGER NULL` | Fecha efectiva de la tasa, distinta de fecha de captura. | Reportes históricos |
| — | `conversionSource` | `TEXT NULL` | `MANUAL`, `ECB`, `BCR`, `IMPORT`, `SYSTEM`, etc.; catálogo por confirmar. | Diagnóstico |
| — | `conversionStatus` | `TEXT NOT NULL DEFAULT 'NOT_REQUIRED'` | Evita interpretar null como conversión pendiente. | UI, validaciones |
| — | `conversionVersion` | `INTEGER NOT NULL DEFAULT 1` | Permite invalidar/recalcular conversiones sin reemplazar el original. | Cambio de moneda principal |

**Decisión propuesta:** para un movimiento normal, `originalAmountMinor == accountAmountMinor` y `originalCurrencyCode == accountCurrencyCode`. Si una compra se registra en USD en una cuenta USD, el libro de la cuenta usa USD. La conversión a GTQ se guarda como snapshot solo cuando se ejecuta la consolidación o cuando la operación requiere mostrar explícitamente el equivalente.

No se debe sobrescribir `amountMinor` con `primaryAmountMinor`. Si se hace, se produce doble conversión o se altera el saldo de la cuenta.

### 6.3 `PaymentCommitmentEntity` / `payment_commitments`

Se propone el mismo patrón mínimo:

| Campo nuevo | Tipo | Regla |
|---|---|---|
| `amountCurrencyCode` | `TEXT NULL` | Moneda del compromiso. En migración se inicializa `GTQ`. |
| `amountScale` | `INTEGER NULL` | Escala de esa moneda. |
| `primaryAmountMinor` | `INTEGER NULL` | Equivalente para vista consolidada, nunca reemplaza `amountMinor`. |
| `primaryCurrencyCode` | `TEXT NULL` | Moneda principal al convertir. |
| `conversionRateId` | `INTEGER NULL` | Referencia a la tasa aplicada. |

`creditCardAccountId` y `paymentSourceAccountId` se conservan. El compromiso de estado de cuenta debe usar la moneda de la tarjeta; si la cuenta fuente tiene otra moneda, el pago es una operación multicurrency separada.

### 6.4 Presupuestos, metas, recurrentes y Smart Cart

Estas estructuras usan actualmente `Double` y no contienen moneda explícita. La migración debe ser gradual:

| Entidad existente | Campo nuevo propuesto | Tipo | Tratamiento |
|---|---|---|---|
| `BudgetLimitEntity` | `currencyCode` | `TEXT NOT NULL DEFAULT 'GTQ'` | Un presupuesto se define en una moneda; no sumar directamente presupuestos de monedas distintas. |
| `SavingsGoalEntity` | `currencyCode` | `TEXT NOT NULL DEFAULT 'GTQ'` | Meta en moneda objetivo; aportes desde otra moneda requieren conversión explícita. |
| `RecurringExpenseEntity` | `currencyCode` | `TEXT NOT NULL DEFAULT 'GTQ'` | Su generación crea un movimiento en la moneda indicada o exige cuenta compatible. |
| `SmartCartItemEntity` | `currencyCode` | `TEXT NOT NULL DEFAULT 'GTQ'` | Precios estimados se muestran en la moneda de la lista; por confirmar si el carrito tendrá moneda propia o la principal. |
| `ShoppingTemplateEntity` | `currencyCode` | `TEXT NOT NULL DEFAULT 'GTQ'` | Evita mezclar plantillas de distintas monedas. |

Estos campos son **propuestos**, no existentes. Mientras no se aprueben, la UI debe seguir funcionando con GTQ y no ofrecer mezcla silenciosa de monedas.

## 7. Tablas nuevas para FX y operaciones compuestas

### 7.1 Historial de tipos de cambio

Entidad nueva, **por confirmar**:

```kotlin
@Entity(
    tableName = "exchange_rates",
    indices = [
        Index(value = ["baseCurrencyCode", "quoteCurrencyCode", "effectiveAtMillis"]),
        Index(value = ["source", "sourceReference"], unique = true)
    ]
)
data class ExchangeRateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val baseCurrencyCode: String,
    val quoteCurrencyCode: String,
    val rateNumerator: String,
    val rateDenominator: String = "1",
    val effectiveAtMillis: Long,
    val observedAtMillis: Long,
    val source: String,
    val sourceReference: String? = null,
    val isManual: Boolean = false,
    val isEstimated: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)
```

La tasa debe significar inequívocamente: `1 unidad de base = rateNumerator / rateDenominator unidades de quote`. Guardar como texto decimal evita que `Double` altere la tasa. La tabla debe conservar tasas históricas; no se actualiza destructivamente.

Fuentes propuestas: `MANUAL`, `IMPORTED`, `OFFLINE_CACHED`, y una fuente externa por confirmar. El diseño no inicia Firebase ni presupone un proveedor remoto. En modo offline se usa la última tasa disponible solo si la política lo permite y se marca `isEstimated = true` o `conversionStatus = RATE_STALE`.

### 7.2 Detalle de operación multicurrency

Entidad nueva, **por confirmar**, para transferencias y pagos compuestos:

```kotlin
@Entity(
    tableName = "currency_operation_details",
    indices = [
        Index(value = ["operationGroupId"], unique = true),
        Index(value = ["rateId"])
    ]
)
data class CurrencyOperationDetailEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val operationGroupId: String,
    val operationType: String,
    val sourceAccountId: Long,
    val destinationAccountId: Long,
    val sourceAmountMinor: Long,
    val sourceCurrencyCode: String,
    val destinationAmountMinor: Long,
    val destinationCurrencyCode: String,
    val feeAmountMinor: Long = 0L,
    val feeCurrencyCode: String? = null,
    val rateId: Long? = null,
    val rateNumerator: String,
    val rateDenominator: String = "1",
    val rateAsOfMillis: Long,
    val rateSource: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)
```

El detalle es la fuente de verdad de la relación entre origen, destino, tasa y comisión. Los dos asientos de `transactions` siguen siendo necesarios para el libro mayor: uno `TRANSFER_OUT` y otro `TRANSFER_IN`, ambos con el mismo `transferGroupId`. La comisión debe ser un tercer asiento `STANDARD` o un nuevo `FEE` por confirmar, asociado al mismo grupo pero excluido de la suma principal de la transferencia.

## 8. Flujo de datos y frontera de conversión

### 8.1 Registro de movimiento normal

1. La UI solicita monto decimal como texto, moneda y cuenta.
2. El ViewModel valida que la cuenta exista y que la moneda elegida sea compatible o inicia una operación de conversión confirmada.
3. El repositorio convierte el texto a `Long` usando la escala de la moneda, nunca `Double`.
4. `TransactionEntity` guarda el original y el importe operativo de la cuenta.
5. El saldo de la cuenta se calcula solo con `accountAmountMinor`/`amountMinor` en la moneda de esa cuenta.
6. La consolidación a moneda principal se realiza al construir un snapshot de presentación o al persistir un snapshot versionado.

### 8.2 Transferencia entre monedas distintas

La nueva operación debe recibir explícitamente:

```text
sourceAccountId
sourceAmountMinor
sourceCurrencyCode
 destinationAccountId
destinationCurrencyCode
rateId o tasa manual
feeAmountMinor opcional
feeCurrencyCode opcional
operationKey/idempotencyKey
```

Secuencia atómica en `AppDatabase.withTransaction`:

1. Cargar ambas cuentas y verificar que no estén archivadas.
2. Verificar que los códigos de moneda guardados en cuentas coincidan con los solicitados.
3. Resolver la tasa elegida; si no existe, no inventarla silenciosamente.
4. Calcular `destinationAmount` con `BigDecimal` y redondear solo a la escala destino.
5. Crear `CurrencyOperationDetailEntity`.
6. Crear `TRANSFER_OUT` por el importe de origen.
7. Crear `TRANSFER_IN` por el importe de destino.
8. Crear la comisión separada, si existe, en la moneda y cuenta correspondientes.
9. Confirmar que la operación no exceda saldo o regla de tarjeta aplicable.
10. Insertar todo dentro de una sola transacción y devolver un resultado con ambos importes.

La transferencia se rechaza si falta la tasa, si el resultado no puede representarse en la escala destino o si la misma `operationKey` ya existe con importes diferentes. El reintento con la misma clave devuelve la operación existente, no crea otra.

### 8.3 Prevención de conversiones dobles

Se implementará una única frontera conceptual:

```text
Importe original -> importe de cuenta -> snapshot consolidado a moneda principal
```

Reglas obligatorias:

- Nunca usar `primaryAmountMinor` como entrada para recalcular `primaryAmountMinor`.
- Cada snapshot consolidado porta `primaryCurrencyCode`, `conversionVersion` y `conversionRateId`.
- Un movimiento que ya tiene una conversión válida para la misma moneda principal y versión se reutiliza.
- Si cambia la moneda principal, se conserva la conversión histórica anterior y se genera una nueva versión desde el importe original o el importe de cuenta, nunca desde la conversión anterior.
- La UI no debe llamar a una función de conversión sobre un monto que ya esté etiquetado como `PRIMARY_CONVERTED`.

## 9. Consolidación patrimonial, presupuestos y reportes

### 9.1 Saldos y patrimonio

`AccountBalanceCalculator` debe dividirse conceptualmente en dos pasos:

1. `calculateAccountSnapshot`: cálculo exacto por cuenta en la moneda de la cuenta. Mantiene `Long`, `BigDecimal`, tarjetas, cuotas, límites y asientos existentes.
2. `calculateConsolidatedPortfolio`: recibe snapshots por cuenta y convierte cada snapshot a la moneda principal usando una política de tasa explícita.

El patrimonio consolidado no debe sumar `GTQ 100` + `USD 100` directamente. Si una cuenta no tiene tasa válida:

- La cuenta sigue apareciendo con saldo nativo.
- El total consolidado se marca como incompleto o usa una política aprobada de tasa anterior/estimada.
- Nunca se presenta un cero como si fuera un saldo real.

### 9.2 Presupuestos y Quincenas

Los presupuestos se calculan en la moneda del presupuesto. Para un registro en otra moneda, el agregado debe convertirlo una sola vez usando la tasa fechada según política:

- Preferencia recomendada: tasa efectiva del movimiento si fue registrada.
- Si falta, tasa histórica más cercana anterior a la fecha del movimiento, marcada como estimada.
- Si no existe, el gasto queda excluido del total consolidado y se muestra como “pendiente de conversión”, no se convierte a tasa actual sin aviso.

`QuincenaFilterHelper` no se modifica en sus reglas temporales:

- Zona `America/Guatemala`.
- Día 15 inicia Q2.
- Último día del mes inicia Q1 del mes siguiente.
- Q1: último día del mes anterior a día 14.
- Q2: día 15 a penúltimo día.
- El tipo de movimiento no cambia el ciclo.

La modificación futura debe estar fuera del helper: después de filtrar por Quincena, un `QuincenaMoneyAggregator` convierte y suma. Así se preserva el calendario aprobado y solo cambia la representación monetaria.

### 9.3 Pagos, tarjetas y cuotas

- Una tarjeta tiene una moneda nativa fija por cuenta.
- Compras y cargos se registran en la moneda de la tarjeta.
- `CreditCardCycleCalculator` calcula corte, deuda y pagos exclusivamente en esa moneda.
- Un pago desde una cuenta de moneda distinta es una operación multicurrency: salida en la moneda fuente, abono en la moneda de la tarjeta y detalle de tasa.
- El compromiso del estado de cuenta permanece en la moneda de la tarjeta.
- Los intereses y comisiones se guardan separados del principal cuando provienen de otra operación; no se esconden dentro de una tasa de conversión.
- Los planes de cuotas se crean en la moneda de la tarjeta y todas sus cuotas mantienen esa moneda. No se permite cambiar la moneda de un plan existente.

### 9.4 HOY, Registros 3.0, Smart Cart y Metas

**Registros 3.0:** cada fila muestra importe original y, opcionalmente, equivalente en moneda principal. El filtro por moneda debe distinguir “todas”, moneda original y moneda principal. Los movimientos de transferencia muestran origen/destino y no afectan gasto presupuestario según `TransactionEntryKind`.

**HOY:** las tarjetas resumidas muestran una sola moneda principal para totales globales, con indicador de cuentas o gastos sin tasa. El detalle puede mostrar moneda nativa.

**Smart Cart:** cada lista debe operar en una moneda definida. Si el usuario compra desde una cuenta de otra moneda, el checkout debe invocar una conversión explícita o registrar el gasto en la moneda de la cuenta seleccionada. No debe convertir precios estimados cada vez que se recompone la pantalla.

**Metas:** una meta tiene moneda objetivo. Una contribución en otra moneda crea una operación de conversión registrada, conserva el importe aportado y actualiza el progreso en la moneda de la meta.

**Reportes y diagnóstico:** `FinancialDiagnosticService` y reportes deben exponer subtotales nativos y total consolidado, junto con cobertura de tasas, tasa estimada y fecha de la tasa. Un total consolidado incompleto debe ser visible.

## 10. Responsabilidades por capa

| Capa | Responsabilidad propuesta |
|---|---|
| Entidades | Persistir importes, monedas, escalas, tasas y estados sin lógica de UI. |
| `CurrencyAmountMath` | Parsear, redondear y convertir usando `BigDecimal`; nunca `Double` para cálculo. |
| `ExchangeRateRepository` nuevo, por confirmar | Consultar, insertar y seleccionar tasas locales; no depende de red para leer. |
| `CurrencyConversionService` nuevo, por confirmar | Resolver tasa, calcular destino, crear snapshot y evitar conversiones repetidas. |
| `AccountBalanceCalculator` | Calcular cada cuenta y tarjeta en su moneda nativa. |
| `ConsolidatedPortfolioCalculator` nuevo | Convertir snapshots a la moneda principal y devolver cobertura/errores. |
| `AccountsRepository` | Orquestar transferencias multicurrency, pagos de tarjeta y comisiones en una transacción Room. |
| `BudgetRepository` | Mantener normalización de transacciones; delegar la conversión al servicio y preservar grupos. |
| `AccountLedgerDao` | Consultas por cuenta, grupo, moneda, estado de conversión y tasa. |
| `TransactionDao` | CRUD heredado y consultas de Registros; nunca recalcular importes convertidos en SQL. |
| `QuincenaFilterHelper` | Mantener calendario y clasificación actual. No calcular tipos de cambio. |
| `BudgetViewModel` | Exponer valores de presentación con código/símbolo y estado de conversión. |
| `AccountsViewModel` | Exponer saldos nativos, patrimonio consolidado y cuentas sin tasa. |
| `FinancialDiagnosticService` | Detectar mezcla de monedas sin tasa, escala inválida, conversión obsoleta y grupos incompletos. |
| UI Compose | Pedir moneda y tasa de forma explícita; no ocultar estimaciones ni cambiar datos persistidos por formatear. |

## 11. Migración segura de datos actuales en GTQ

### 11.1 Principios

1. Todos los registros existentes se interpretan como GTQ.
2. No se recalculan históricos con una tasa externa.
3. `FinancialAccountEntity.currencyCode` vacío o inválido se normaliza a `GTQ` solo dentro de una migración validada.
4. `amount` y `amountMinor` actuales conservan su valor lógico; si hay discrepancia se aplica la misma reconciliación de paridad ya existente.
5. Las columnas nuevas de `transactions` se agregan nullable o con defaults compatibles.
6. La migración es aditiva y reversible por respaldo previo, no destructiva.

### 11.2 Secuencia Room propuesta, no ejecutada

La siguiente numeración es una propuesta; debe confirmarse contra el código final antes de implementarse.

- **14 → 15:** añadir tablas `currency_catalog`, `international_preferences` y, si se aprueba, `exchange_rates`. Sembrar catálogo mínimo y una fila de preferencias con `countryCode = "GT"`, `primaryCurrencyCode = "GTQ"`, `financialZoneId = "America/Guatemala"`.
- **15 → 16:** añadir columnas monetarias aditivas a `transactions` y `payment_commitments`; rellenar históricos con `GTQ`, escala 2, y equivalencia original = importe actual. No crear `primaryAmountMinor` mediante una tasa; para GTQ puede copiarse el importe solo porque la moneda principal inicial es GTQ, dejando referencia `conversionStatus = NOT_REQUIRED` o `IDENTITY` según el enum aprobado.
- **16 → 17:** añadir moneda a presupuestos, metas, recurrentes, Smart Cart y plantillas, todos con `GTQ` para filas existentes.
- **17 → 18:** añadir `currency_operation_details` e índices, después de validar que los grupos existentes `transferGroupId` no tengan inconsistencias.

No se debe escribir esta secuencia en `AppDatabase` hasta que las entidades, DAOs y pruebas estén aprobadas.

### 11.3 Verificaciones antes y después de migrar

Antes:

- Copia/exportación de la base local.
- Conteo por tabla y suma de `amountMinor`/`amount`.
- Detección de `amountMinor` desincronizado.
- Conteo de grupos de transferencia, cuotas y compromisos enlazados.

Después:

- Conteo de filas idéntico por tabla existente.
- Sumas GTQ idénticas por `accountId`, `entryKind`, mes y Quincena.
- Ningún `originalCurrencyCode` nulo en registros heredados migrados.
- Ningún histórico recalculado por una tasa externa.
- Room `validateMigration` y pruebas de apertura desde una base v14 real.

## 12. Cambio de moneda principal

Cambiar la moneda principal no cambia cuentas, movimientos, presupuestos ni Quincenas. Solo cambia la moneda de consolidación futura y la representación global.

Política propuesta:

- Mantener `primaryCurrencyCode` actual en cada snapshot persistido.
- Incrementar `conversionVersion` al cambiar moneda principal.
- Recalcular desde `accountAmountMinor`/`originalAmountMinor` y una nueva tasa, nunca desde `primaryAmountMinor` anterior.
- Los presupuestos existentes conservan su moneda; el usuario debe confirmar si desea crear un presupuesto nuevo en la moneda principal.
- Mostrar una advertencia si faltan tasas históricas.
- No cambiar automáticamente `currencyCode` de ninguna cuenta.

Esta política requiere confirmación funcional porque afecta la interpretación de reportes históricos.

## 13. Rendimiento y compatibilidad con Android 10 / Galaxy A01

El diseño debe evitar cálculos costosos en Compose y consultas que carguen toda la base en cada recomposición.

Medidas concretas:

- Índices en `(accountId, dateMillis)`, `(originalCurrencyCode, dateMillis)`, `(conversionStatus, dateMillis)`, `(transferGroupId)` y pares de moneda/fecha en `exchange_rates`.
- Conversión por lote en `Dispatchers.Default` o `Dispatchers.IO`, fuera del hilo principal.
- Cachear tasas seleccionadas por par, fecha y versión.
- No guardar ni recalcular `BigDecimal` para cada recomposición; construir un snapshot inmutable.
- Usar `Flow` de Room con consultas acotadas por rango de fecha cuando sea posible.
- Evitar sincronización remota obligatoria; ninguna pantalla debe esperar red para mostrar datos nativos.
- Limitar el catálogo inicial de monedas a las activas y permitir ampliación local.
- Persistir resultados de reportes grandes solo si una medición en dispositivo demuestra beneficio.
- Mantener `WRITE_AHEAD_LOGGING` y operaciones atómicas existentes.
- No iniciar Firebase como parte de esta arquitectura.

Objetivo de aceptación por confirmar: abrir Registros y Mis Cuentas sin bloqueo visible con una base local de tamaño representativo del usuario, y completar la consolidación en segundo plano sin exceder memoria disponible del A01.

## 14. Riesgos y decisiones pendientes

| Riesgo | Impacto | Mitigación | Requiere confirmación |
|---|---|---|---|
| Escalas distintas por moneda | Importes mal redondeados | Catálogo ISO y `Long` con escala explícita | Sí |
| Falta de tasa histórica | Totales incompletos o engañosos | Estado `RATE_MISSING` y no convertir silenciosamente | Sí |
| Tasa estimada offline | Reporte aparentemente exacto | Marcar fuente, fecha y estimación | Sí |
| Cambio de moneda principal | Reinterpretación de históricos | Versionar snapshots y recalcular desde original | Sí |
| Tarjeta en moneda extranjera | Deuda y pagos mal comparados | Ciclo nativo por tarjeta; pago multicurrency compuesto | Sí |
| Cuotas e intereses | Mezcla de capital, interés y FX | Plan en una moneda; comisión/interés separado | Sí |
| Reconciliación bancaria | Duplicados o monedas incorrectas | Conservar identidad bancaria y validar moneda de cuenta | Sí |
| Transferencia parcial | Salida y entrada no cuadran | Operación atómica e idempotente | No, es requisito técnico |
| Comisiones | Inflar o esconder gasto | Tercer asiento separado y visible | Sí: decidir `FEE` o `STANDARD` |
| Presupuestos de varias monedas | Límites comparados incorrectamente | Presupuesto con moneda y agregación convertida | Sí |
| Rendimiento A01 | Pantallas lentas | Índices, snapshots y cálculo fuera de UI | No, es requisito |
| API o proveedor de tasas | Dependencia externa y costo | Adaptador opcional; manual/offline primero | Sí |
| Datos heredados con `Double` | Pérdida de centavos | Conservar `amountMinor` y reconciliar paridad | No, es requisito |

## 15. Orden de implementación recomendado

1. **Contrato funcional:** confirmar país inicial, moneda principal, fuentes de tasas, política de tasa faltante/estimada, comisiones, cambio de moneda principal y alcance de Smart Cart/Metas.
2. **Modelo puro:** implementar y probar `CurrencyAmountMath`, códigos de estado y selección de tasa sin tocar Room.
3. **Catálogo y preferencias:** añadir entidades/DAOs y pruebas de migración aisladas.
4. **Migración de GTQ:** ejecutar en una rama de implementación con copia, pruebas de conteos y paridad.
5. **Campos de transacción:** añadir original/cuenta/principal y adaptar `BudgetRepository` para conservar la fuente primaria.
6. **Servicio FX:** implementar selección de tasa, cache local, auditoría y prevención de doble conversión.
7. **Saldos por cuenta:** adaptar `AccountBalanceCalculator` y `CreditCardCycleCalculator` sin cambiar reglas de `TransactionEntryKind`.
8. **Transferencias multicurrency:** extender `AccountsRepository`, `AccountLedgerDao` y `AccountsViewModel` con operación atómica e idempotente.
9. **Registros 3.0 y HOY:** introducir modelos de UI con importe original/equivalente y estados visibles.
10. **Presupuestos y Quincenas:** mantener `QuincenaFilterHelper` intacto; añadir agregador monetario posterior al filtro.
11. **Pagos, cuotas, Smart Cart y Metas:** migrar uno por uno con moneda explícita y pruebas de no duplicación.
12. **Reportes y diagnóstico:** indicadores de cobertura de tasas, saldos nativos y consolidados.
13. **Optimización en dispositivo:** medir en Galaxy A01/Android 10 o equivalente antes de activar cálculos avanzados por defecto.

## 16. Plan de pruebas

### 16.1 Unitarias de dinero y FX

- Parseo de `0`, valores negativos permitidos por dominio y valores inválidos.
- Escalas 0, 2 y 3.
- Redondeo `HALF_UP` en fronteras exactas y casos `.005`.
- Tasa `1:1`, tasa fraccionaria, tasa grande y tasa con muchos decimales.
- Conversión de origen a destino con suma exacta de residuos.
- Comisión en moneda origen, destino y tercera moneda si se aprueba.
- Rechazo de doble conversión por estado/versionado.
- Reproducción exacta usando numerador/denominador persistidos.

### 16.2 Unitarias de reglas existentes

- `TransactionEntryKind.TRANSFER_OUT`, `TRANSFER_IN` y `CARD_PAYMENT` no afectan presupuesto.
- `STANDARD` e `INSTALLMENT_CHARGE` sí afectan presupuesto como antes.
- `BALANCE_ADJUSTMENT` no infla presupuesto.
- `MoneyMinorUnits` mantiene paridad heredada de `amount`/`amountMinor`.
- `QuincenaFilterHelper` conserva zona, día 15, último día y arrastre.
- Cuotas distribuyen exactamente el total en la moneda de la cuenta.
- Tarjeta calcula ciclos y deuda en su moneda nativa.

### 16.3 Integración Room

- Apertura de una base v14 y migración aditiva a cada versión propuesta.
- Verificación de esquema y columnas default.
- Conteos y sumas antes/después.
- Transferencia multicurrency atómica: cero filas si falla la tasa o una validación.
- Reintento idempotente no duplica asientos ni comisión.
- Eliminación de un grupo elimina sus asientos y conserva auditoría según política aprobada.
- Índices usados por consultas de cuenta, grupo, tasa y fecha.

### 16.4 Integración de módulos

- Registros: mostrar original y equivalente sin alterar el original.
- HOY: total consolidado correcto y señalización de tasa faltante.
- Quincenas: mismos movimientos en Q1/Q2 que antes de multidivisa.
- Pagos: abono en moneda de tarjeta desde cuenta extranjera conserva ambos importes.
- Smart Cart: checkout no convierte repetidamente.
- Metas: aportes en moneda distinta actualizan la meta una sola vez.
- Importación bancaria: conserva `externalId`, `importHash`, `bankReference`, `importSource` y conciliación.
- Diagnóstico: identifica grupos incompletos, escalas inválidas y tasas vencidas.

### 16.5 Regresión de rendimiento

- Base pequeña, mediana y grande.
- Apertura en frío y caliente.
- Cambio de filtro de mes/Quincena/moneda.
- Rotación de pantalla durante consolidación.
- Memoria y tiempo en Android 10 / Galaxy A01 o dispositivo equivalente.
- Sin red: lectura, registro, transferencia con tasa manual y reportes nativos deben funcionar.

## 17. Criterios de aceptación antes de implementar

La implementación solo debe aprobarse cuando se confirme que:

1. `GTQ` es la moneda inicial y todos los históricos actuales se conservan sin recalcular.
2. La moneda de cada cuenta es fija para su libro mayor y no se modifica al cambiar la moneda principal.
3. Se acepta la política de tasas faltantes, vencidas y estimadas.
4. Se define si las comisiones son asientos `STANDARD` o un nuevo `TransactionEntryKind.FEE`.
5. Se define el comportamiento de presupuestos y metas con moneda distinta.
6. Se acepta el versionado de conversiones al cambiar la moneda principal.
7. Se aprueba la fuente de tasas o se decide operar inicialmente solo con tasas manuales/importadas.
8. Se validan las migraciones con una copia real de una base Room v14.
9. Se acuerda que Quincenas 2.0 no cambia sus reglas temporales.
10. Se completan las pruebas de no pérdida, no doble conversión, atomicidad e idempotencia.

## 18. Conclusión

La ruta segura para KETZI no es reemplazar el modelo financiero actual, sino extenderlo alrededor de sus invariantes ya existentes: `amountMinor` como verdad operativa, `TransactionEntryKind` para clasificación, `transferGroupId` para operaciones compuestas, Room offline-first, `AccountsRepository` con transacciones e idempotencia, y `QuincenaFilterHelper` como autoridad temporal de Guatemala.

La multidivisa debe añadir contexto y equivalentes auditables sin destruir el importe original. Cada cuenta calcula primero en su propia moneda; la moneda principal solo aparece en una frontera de consolidación explícita. Con esta separación, Registros 3.0, HOY, patrimonio, presupuestos, Pagos, tarjetas, cuotas, Smart Cart, Metas y reportes pueden evolucionar sin cambiar las reglas financieras aprobadas ni introducir conversiones dobles.

**Siguiente acción autorizable:** revisar y aprobar las decisiones marcadas “por confirmar”. Hasta esa aprobación no se debe modificar Room, crear migraciones ni iniciar implementación.
