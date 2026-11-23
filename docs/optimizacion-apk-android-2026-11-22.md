# KETZI — optimización de APK Android

**Fecha:** 22 de noviembre de 2026
**Base:** commit `177c304` (`fix: harden account balance and payment consistency`)
**Rama de trabajo:** `manus/ketzi-stability-20261122`

## Cambios aplicados

- Release activa R8 (`isMinifyEnabled`) y la reducción de recursos (`isShrinkResources`). Se eliminó la referencia a `app/proguard-rules.pro`, archivo inexistente; se conservaron las reglas optimizadas predeterminadas de Android.
- Release genera APK universal y APK separados para `arm64-v8a`, `armeabi-v7a`, `x86` y `x86_64`. La división también produce variantes Debug con esos ABI.
- Se comprobaron referencias estáticas en Kotlin/XML y no se encontró uso de `Resources.getIdentifier`. Se trasladaron 10 imágenes sin referencias a `design-reference/unused-resources/`, fuera del source set Android; se preservaron sus bytes originales.
- Se reencodificaron nueve fotos usadas como WebP Q90, sin cambiar dimensiones ni encuadre, y se movieron a `drawable-nodpi` para evitar el escalado por densidad en fondos y tarjetas con restricciones explícitas. Las fotos WebP medidas dieron PSNR entre 37.80 y 40.55 dB. Dos JPEG de `drawable-nodpi` se conservaron porque el WebP Q90 resultaba mayor.
- La mascota Quetzi se reencodificó como WebP lossless: 1,801,645 → 1,224,522 bytes (−32.0 %), con igualdad píxel a píxel verificada.
- Los recursos de Tikal y el código de `SplashScreen.kt` no se modificaron.

El conjunto de imágenes en `app/src/main/res` pasó de 25,623,562 bytes en el commit base a 8,049,426 bytes: −17,574,136 bytes (−68.6 %). Los recursos retirados del empaquetado quedan preservados en el repositorio fuera de `res`.

## Tamaños medidos

### Release

| Artefacto | ABI | Bytes | MiB |
|---|---|---:|---:|
| `app-universal-release.apk` | universal (4 ABI) | 52,426,836 | 50.00 |
| `app-arm64-v8a-release.apk` | arm64-v8a | 22,389,913 | 21.35 |
| `app-armeabi-v7a-release.apk` | armeabi-v7a | 18,107,313 | 17.27 |
| `app-x86-release.apk` | x86 | 22,886,405 | 21.83 |
| `app-x86_64-release.apk` | x86_64 | 22,951,491 | 21.89 |

La referencia Release previa, construida sin R8, se reportó como **aprox. 78 MiB**; el APK universal actual es 50.00 MiB. La cifra de referencia anterior era aproximada, por lo que no se atribuye una comparación byte a byte.

### Debug

| Artefacto | ABI | Bytes | MiB |
|---|---|---:|---:|
| `app-universal-debug.apk` | universal (4 ABI) | 72,147,880 | 68.81 |
| `app-arm64-v8a-debug.apk` | arm64-v8a | 42,110,957 | 40.16 |
| `app-armeabi-v7a-debug.apk` | armeabi-v7a | 37,828,357 | 36.08 |
| `app-x86-debug.apk` | x86 | 42,607,449 | 40.63 |
| `app-x86_64-debug.apk` | x86_64 | 42,672,535 | 40.70 |

El APK Debug universal anterior medía 89,725,058 bytes; el actual pesa 72,147,880 bytes, una reducción de 17,577,178 bytes (19.6 %).

## Verificación

- `testDebugUnitTest`: **132 pruebas, 0 fallos, 0 errores, 0 omitidas**. Incluye las pruebas financieras y la prueba Robolectric de migración Room 12→13.
- `assembleDebug` y `assembleRelease`: completados. R8, reducción de recursos y `lintVitalRelease` terminaron sin errores.
- Los cinco APK Release pasan `apksigner verify` y `zipalign -c -p 4`.
- El `applicationId` continúa en `com.aistudio.presupuestofamiliar.wsrjwe`, `minSdk=24` y `targetSdk=36`.
- No se modificó código de cuentas, la base Room, sus esquemas ni migraciones.

## Limitaciones conocidas y pendientes

- El APK Release actual está firmado con la clave local **Debug** (`CN=Android Debug`); es un artefacto de validación, **no es apto para producción ni publicación**. Falta configurar y proteger una clave de firma de producción.
- No había dispositivo Android/emulador conectado: no se hizo instalación ni prueba visual/performance en un Galaxy A01. Los tamaños y la verificación criptográfica no sustituyen esa prueba en hardware.
- Gradle emitió una advertencia no bloqueante: las herramientas SDK leen XML hasta versión 3 y encontraron un archivo SDK XML versión 4. La compilación terminó correctamente.
- Próxima etapa: validación instalada en un teléfono modesto y continuación del trabajo funcional en Mis Cuentas e integración entre módulos, sin alterar el Splash aprobado ni las migraciones.
