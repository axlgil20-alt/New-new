# KETZI — traspaso para Gemini / Google AI Studio

## Estado del repositorio

- Repositorio: `axlgil20-alt/New-new`
- Rama: `feat/multidivisa-registros-3`
- Último commit publicado: `3ac7c5e fix: stabilize Excel import and Kotlin compilation`
- El arreglo anterior de navegación horizontal permanece en el commit `c0432be`.

## Cambios ya aplicados

1. `AccountsRepository.kt`
   - `makeSeparateCharge` ahora es `suspend` para permitir llamadas a Room dentro de la operación.
2. `CompactAccountsSummaryCard.kt`
   - Corregido el cierre de la `Row` de métricas.
3. `QuincenaFilterHelper.kt`
   - Corregido el uso de `effectiveAmountMinor` y la conversión con `MoneyMinorUnits.toBigDecimal(...)`.
4. `ExcelXlsxReader.kt`
   - Cierre explícito de streams.
   - Límite de entrada de 25 MB.
   - Límites de entradas ZIP y filas procesadas.
   - Rechazo claro de `.xls` binario; se aceptan `.xlsx`, `.csv` y `.tsv`.
5. `MainActivity.kt`
   - El `HorizontalPager` principal conserva `userScrollEnabled = false`.

## Validación realizada

La tarea `:app:compileDebugKotlin` terminó con `BUILD SUCCESSFUL`. Las pruebas unitarias específicas no llegaron a ejecutarse porque el sandbox ARM64 no podía ejecutar inicialmente el binario AAPT2 x86-64; posteriormente AAPT2 se ejecutó mediante QEMU.

La APK Debug universal fue construida y verificada con firma v2. También se generaron APKs `armeabi-v7a` y `arm64-v8a`.

## Problema pendiente en dispositivo real

En un Samsung Galaxy A01:

- La APK universal y ARM64 normalmente no llegan a instalarse; el instalador muestra mensajes genéricos como cancelado o no se pudo instalar.
- La APK ARM32 sí llegó a instalarse en algunas ocasiones.
- Al abrirla, en algunas pruebas sólo aparece una pantalla blanca y la interfaz nunca llega a mostrarse.
- No se dispone todavía de un `logcat` ni de una excepción concreta del dispositivo.

Esto debe investigarse como un problema de arranque/runtime, no asumir que es sólo lentitud de instalación. Las áreas prioritarias son:

1. `MainActivity` y la creación temprana de `BudgetViewModel`/`AccountsViewModel`.
2. Apertura/migración de `AppDatabase` en Android antiguo.
3. `SplashScreen`, especialmente la carga de imágenes y animación continua en un dispositivo de pocos recursos.
4. Compatibilidad de Compose/Material3 con la versión Android exacta del A01.
5. Carga de librerías nativas de ML Kit y selección de ABI.
6. Posibles errores antes de `setContent` o durante la primera composición.

## Metadatos actuales de APK

- `applicationId`: `com.aistudio.presupuestofamiliar.wsrjwe`
- `versionCode`: `1`
- `versionName`: `1.0`
- `minSdk`: `24`
- `targetSdk`: `36`
- ABIs configuradas: `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`

## Recomendación técnica para resolver la pantalla blanca

Preparar una variante de diagnóstico que:

- Muestre una pantalla inicial nativa simple, sin imágenes ni animaciones pesadas.
- Inicialice la base de datos y ViewModels de forma diferida después del primer frame.
- Capture y muestre un error visible si falla la inicialización.
- Registre la excepción de arranque en almacenamiento local para poder recuperarla sin ADB.
- Pruebe primero `minSdk 24`, `targetSdk 35` y una APK exclusivamente `armeabi-v7a`.
- No cambiar lógica financiera ni importación/exportación mientras se aísla el fallo de arranque.

## Comandos de compilación

En un entorno Android x86-64 estándar:

```bash
gradle --no-daemon :app:assembleDebug
```

En el sandbox ARM64 usado durante la validación, AAPT2 requiere emulación x86-64; Google AI Studio/Gemini no debería necesitar esa adaptación si usa un entorno Android/Linux x86-64.

## Información que falta para cerrar el diagnóstico

El dato decisivo es el error de Android Runtime justo al abrir la aplicación. Obtenerlo con:

```bash
adb logcat -c
adb shell monkey -p com.aistudio.presupuestofamiliar.wsrjwe 1
adb logcat -d -v time | grep -iE "AndroidRuntime|FATAL EXCEPTION|com.aistudio.presupuestofamiliar.wsrjwe"
```

No dar por resuelto el problema sólo porque Gradle genere una APK: la instalación y el primer arranque deben validarse en el Galaxy A01.
