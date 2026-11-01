# QUETZI — Informe técnico de Fase 3

**Proyecto de trabajo:** `QUETZI-Fase2` (fuente Android existente)
**Estado de compilación:** **NO EJECUTADA**
**APK:** no generado.
**Pruebas:** se añadieron pruebas unitarias focalizadas, pero no se ejecutaron.

## 1. Resultado por alcance

### HOY / Inicio

- **IMPLEMENTADO — búsqueda de movimientos:** busca localmente por nombre/concepto, categoría y monto sobre `filteredTransactions`; conserva filtros y período existentes, se actualiza al escribir y muestra un estado sin coincidencias. No modifica ni duplica movimientos.
- **IMPLEMENTADO — legibilidad:** el saldo neto tiene una fila propia de ancho completo; se evita el recorte del rótulo de estado «Alerta»; el indicador «Supuestos ON/OFF» queda separado verticalmente del texto del saldo.
- **IMPLEMENTADO — paleta y tema nocturno:** fondo cálido `#FFF9E6` y superficie principal oscura `#1E261D`; ingresos, gastos/alertas y progreso tienen tonos separados y contrastantes. Se adaptaron las tarjetas ejecutiva y financiera, la tarjeta del consejo de Quetzi y los movimientos. La identidad gráfica, imagen y animaciones de la mascota se conservaron.
- **IMPLEMENTADO — protección del FAB:** la lista termina con 80dp de espacio desplazable. No se alteró el FAB global ni la navegación.
- **VERIFICADO PARCIALMENTE:** revisión estática de las llamadas, filtros, ramas de resultados y espacio de cierre. La adaptación real en el Samsung Galaxy A17 5G no se probó en dispositivo.

### Límites

- **IMPLEMENTADO — alta, consulta, edición y eliminación:** se reutilizan la entidad Room, DAO, repositorio y diálogo existentes. El acceso de edición quedó conectado mediante la pantalla y `MainActivity`. En edición, la categoría se conserva (es la clave primaria existente); se pueden cambiar monto, motivo y consejo.
- **IMPLEMENTADO — gasto, restante y porcentaje:** gasto real por categoría del mes seleccionado; restante limitado a cero y excedente por separado; porcentaje sin capar y barra limitada a 100% visualmente. Los importes de este módulo se muestran en quetzales (`Q`) con privacidad discreta respetada.
- **IMPLEMENTADO — umbral de aviso autorizado:** aviso preventivo desde **80%** exacto y comparación de excedido en la tarjeta. El excedido real también se conserva en las advertencias ya existentes de Inicio. El aviso preventivo es contextual dentro de Límites; no se añadió notificación del sistema operativo.
- **IMPLEMENTADO — reglas de datos:** el cálculo recorre las transacciones existentes, filtra por mes y categoría, aplica `isGastoReal`, excluye montos inválidos/negativos y filas explícitamente marcadas como duplicado sospechoso. Pendientes/supuestos, descartados e ingresos no reducen el presupuesto. No se escriben ni fabrican transacciones.
- **VERIFICADO PARCIALMENTE:** el estado de datos se actualiza por el flujo existente de transacciones y el cálculo usa la lista completa de Room, no el subconjunto de la búsqueda actual.
- **PENDIENTE / límite conocido:** la pantalla de edición conserva la categoría para proteger la clave primaria `category`; cambiar categoría requiere un flujo explícito de actualización en Room. Se excluyen duplicados marcados (`isSuspectedDuplicate`), pero no se suprimen automáticamente pares inferidos por coincidencia de importe/fecha/concepto: pueden ser compras legítimas idénticas y la heurística existente no permite resolverlo sin riesgo de subcontarlas.

## 2. Reglas financieras y precisión

`BudgetLimitCalculator.monthlySpend` utiliza inicio de mes inclusivo y comienzo del mes siguiente exclusivo, en el huso horario local del dispositivo. Normaliza categoría (espacios y mayúsculas), suma cada fila elegible una vez y redondea cada movimiento a centavos con `BigDecimal` y `HALF_UP`. El límite también se normaliza a centavos. El porcentaje mostrado se redondea a dos decimales; el umbral preventivo compara gasto y límite exactos en centavos, no el porcentaje de presentación.

El modelo actual conserva `Double` y la clave primaria `category`. No se cambió el esquema Room, no se creó migración y no se modificaron transacciones existentes. Se reutilizan `BudgetLimitEntity`, `BudgetDao.setBudget/deleteBudget`, `BudgetRepository` y `BudgetViewModel`.

## 3. Archivos de código modificados

- `app/src/main/java/com/example/ui/screens/InicioScreen.kt` — búsqueda local, tema, rótulo de período, modo oscuro de tarjetas y espacio seguro del FAB.
- `app/src/main/java/com/example/ui/components/MonthlyExecutiveSummaryCard.kt` — presentación completa de Neto, legibilidad del estado y superficies/indicadores por tema.
- `app/src/main/java/com/example/ui/components/FinancialSummaryCard.kt` — separación del toggle y tratamiento claro/oscuro de superficies e indicadores.
- `app/src/main/java/com/example/ui/components/TransactionItemCard.kt` — parámetro oscuro opcional; solo Inicio lo activa, las otras llamadas conservan el valor predeterminado.
- `app/src/main/java/com/example/ui/components/QuetziMascotView.kt` — solo superficies/chip/botón por tema; ilustración, identidad y animación no cambiaron.
- `app/src/main/java/com/example/ui/screens/BudgetsScreen.kt` — gasto real, saldo, porcentaje, alertas del 80%/excedido, Q, edición y contraste.
- `app/src/main/java/com/example/ui/screens/LimitsScreen.kt` — propagación del callback de edición.
- `app/src/main/java/com/example/ui/components/BudgetLimitDialog.kt` — reutilización para edición y captura/validación de monto hasta centavos (admite coma decimal).
- `app/src/main/java/com/example/MainActivity.kt` — cableado del estado de edición y símbolo Q en el diálogo; navegación no modificada.
- `app/src/main/java/com/example/ui/viewmodel/BudgetViewModel.kt` — alertas de límite excedido calculadas con gasto real del mes seleccionado.
- `app/src/main/java/com/example/ui/util/BudgetLimitCalculator.kt` — cálculo mensual y monetario reutilizable.
- `app/src/test/java/com/example/BudgetLimitCalculatorTest.kt` — pruebas unitarias focalizadas.

## 4. Verificaciones, dependencias y pendientes

- **VERIFICADO PARCIALMENTE:** inspección estática de firmas/callsites de `BudgetsScreen`, `LimitsScreen` y `TransactionItemCard`; revisión de `isGastoReal`, del símbolo y formato discreto, del flujo Room existente y del cableado del diálogo. Se comprobó que el inventario de salida no contenía APK/AAB/ZIP antes de preparar el archivo final.
- **PENDIENTE:** ejecutar `BudgetLimitCalculatorTest` y la suite Gradle; validar renderizado, interacción, accesibilidad bajo luz exterior y comportamiento en Samsung Galaxy A17 5G.
- **NO VERIFICADO:** compilación Android. No se ejecutó compilación ni se aceptaron licencias/instalaron componentes SDK en esta fase.
- **PENDIENTE:** validación integral de Gradle/Android/dispositivo y eventual APK definitivo, para la etapa indicada por el propietario.
- **Control de créditos:** no hay telemetría de saldo/consumo de créditos accesible desde este entorno; por tanto, no se atribuye una cifra. No se invocaron servicios de generación de medios ni se creó APK.
- No se modificaron Splash, Registros, Quincenas, Smart Cart, Asesor, Ajustes, importador Excel ni navegación general.
