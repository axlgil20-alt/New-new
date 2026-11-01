# QUETZI — Informe técnico de Fase 2 (Quincenas)

## Alcance respetado

Se trabajó sobre el proyecto existente extraído de `QUETZI-Fase2(1).zip`. El ZIP no contiene metadatos Git; para reconocer el estado recibido y limitar los cambios, se compararon los archivos de código editados directamente con el contenido original del ZIP. Solo se modificaron dos archivos de código del proyecto; se creó este informe como documentación aparte.

No se implementaron las correcciones de contraste ni el espaciado de Registros: quedan pendientes, como se indicó. No se inició la Fase 3. No se generó APK ni ZIP intermedio o final.

## Archivos de código modificados

- `app/src/main/java/com/example/ui/screens/PlannedVsRealScreen.kt`
- `app/src/test/java/com/example/ExcelImportAndQuincenaFiltersTest.kt`

## Cambios realizados

1. **Tarjetas/resúmenes duplicados:** la pestaña Mes ahora presenta únicamente el resumen mensual consolidado; la pestaña de la primera quincena presenta solo su desglose detallado; y la pestaña de la segunda quincena presenta solo su desglose detallado. Se conservaron las tarjetas de movimientos, su lista y la navegación existentes.
2. **Motor financiero:** la inspección de `QuincenaFilterHelper` y la pantalla confirma que ya estaban implementadas las reglas previstas: ingreso de fin de mes asignado a Q1 del mes siguiente; ingreso del día 15 asignado a Q2 del mismo mes; traspaso calculado como saldo de apertura, no como ingreso nuevo; gastos reales descontados una vez; y gastos pendientes reservados para la proyección. El resumen mensual cierra con el disponible de Q2, que incorpora el arrastre de Q1. Se añadió una aserción de regresión matemática que compara ese cierre contra saldo inicial + ingresos del mes − gastos reales.
3. **Estado vacío:** se conservó la fotografía existente y se redujo condicionalmente la opacidad de la capa clara cuando la lista del ciclo seleccionado está vacía (de `0xB3` a `0x7A`). En ciclos con movimientos se preserva el tono original.

## Verificación

- Comparación del código contra el ZIP de origen: confirmada; los únicos cambios en el código del proyecto fueron los dos archivos enumerados arriba.
- Inspección del lector Excel y las pruebas existentes: contemplan la exclusión de “Saldo anterior” y resúmenes; las pruebas fuente registran 53 movimientos contables (8 ingresos y 45 gastos), Q26,022 de ingresos y Q23,665 de gastos. Estos son resultados afirmados por las pruebas existentes, **no una ejecución realizada en esta sesión**.
- Prueba añadida con resultado esperado: ingreso Q1 Q1,000; gasto real Q1 Q200; ingreso Q2 Q500; gasto real Q2 Q300; saldo de apertura Q2 Q800; disponible real al cierre mensual Q1,000; pendientes Q100; proyección Q900.
- **Pruebas Gradle: no ejecutadas.** `./gradlew testDebugUnitTest --console=plain` falló antes de compilar porque no encontró la ubicación del Android SDK. Al intentar instalar Android API 36, `sdkmanager` solicitó aceptar el acuerdo de licencia vinculante del SDK de Google. La instalación fue cancelada sin aceptar el acuerdo; por tanto, el SDK API 36 no quedó instalado y no es posible confirmar compilación ni ejecución de pruebas en este entorno.
- No se realizó validación visual en un Samsung Galaxy A17 ni bajo luz exterior.

## Pendientes y limitaciones

- Ejecutar pruebas unitarias y compilación cuando haya un Android SDK API 36 ya instalado y licenciado en el entorno.
- Realizar validación visual del estado vacío y de las tres pestañas en dispositivo.
- Correcciones de contraste/espaciado de Registros: pendientes y no implementadas.
- Fase 3: no iniciada.
- APK/ZIP: no generados, según la instrucción de no producir paquetes intermedios y de no empezar Fase 3.
- Consumo de créditos: el entorno no expone un contador verificable; no se puede informar un consumo real sin inventarlo.
