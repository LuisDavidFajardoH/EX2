# EX2 Lite

App mínima para la pantalla del **Geely EX2 (IHU629G, Android 9)**. Solo hace dos cosas:

1. **Sonido de bloqueo personalizado:** eliges un `.wav` y suena al cerrar el carro con la llave.
2. **Memoria de modo de manejo:** guarda Eco/Comfort/Sport y la regeneración Baja/Media/Alta, y los vuelve a poner cada vez que prendes o despiertas el carro.

APK de ~75 KB, sin internet, sin cuentas y sin nada en `/system`. Un reset de fábrica lo revierte todo.

> Proyecto personal y no oficial. Úsalo bajo tu propio riesgo; Geely no lo soporta.

## Requisitos

- La pantalla tiene que tener el **ADB abierto** (puerto 5555). Si ya instalaste Sapo con el pendrive, ya está abierto.
- Un computador con `adb` (Android platform-tools), conectado a la **misma red Wi-Fi de 2.4 GHz** que el carro.

## Instalar

```sh
adb connect IP_DEL_CARRO:5555
adb install -r app/ex2lite.apk
adb shell am start -n com.david.ex2lite/.MainActivity
```

La IP aparece arriba a la derecha en la pantalla del carro (ej. `192.168.x.x`).
La app queda en la lista de apps con el nombre **EX2 Lite**.

### ⚠️ Si también tienes Sapo

Apaga en Sapo **el cambio de sonido de bloqueo** y **el recordar modo de manejo/regeneración**.
Si los dos hacen lo mismo, se pelean y el resultado es impredecible.

## Usar

**Sonido de bloqueo**
1. Pon tus `.wav` (máximo 1 MB cada uno, WAV real, no un MP3 renombrado) en una USB,
   en la raíz o dentro de una carpeta `SonidosBloqueo`. También lee `/sdcard/SonidosBloqueo`
   y la carpeta `锁车音效` si ya la tenías.
2. Abre EX2 Lite → **Probar** para oírlo → **Usar**.
3. La app guarda una copia, así que después puedes sacar la USB.
4. **Volver al original** deja el sonido que tenía el carro.

**Modo de manejo**
- Toca el modo y la regeneración que quieras: se aplican al instante y quedan guardados.
- Con **Recordar al encender** prendido, se vuelven a poner solos durante los primeros ~20 s
  después de prender o despertar el carro, y solo si está en **P**. Si los cambias durante el
  viaje, la app no los toca hasta la próxima vez.

## Desinstalar

Primero toca **Volver al original** en la app. Luego:

```sh
adb uninstall com.david.ex2lite
```

## Compilar

Requisitos: **JDK 17**, **Android SDK build-tools 34.0.0** y la plataforma **android-28**.

```sh
export ANDROID_SDK="$HOME/Android/sdk"
cd app && ./build.sh        # genera app/ex2lite.apk
```

La app se firma con la *platform test-key* pública de AOSP (`app/keystore/`). El firmware del
EX2 confía en esa firma, así que la app corre como `android.uid.system` y puede usar el VHAL
y escribir `Settings.System`.

## Cómo funciona

| Archivo | Qué hace |
|---|---|
| `CarLink.java` | Lee y escribe modo de manejo (`570491136`) y regeneración (`537003264`) por `CarPropertyManager`; escucha suspender/despertar por `CarPowerManager`. |
| `LockSound.java` | Busca `.wav`, los valida (cabecera RIFF/WAVE, ≤ 1 MB), copia el elegido a la carpeta privada de la app y apunta `Settings.System lock_car_audio_path` a esa copia. |
| `KeeperService.java` | Servicio en primer plano que, tras arrancar o despertar, abre una ventana de 20 s y reescribe lo guardado si el carro volvió a sus valores de fábrica. |
| `BootReceiver.java` | Arranca el servicio al encender la pantalla o al actualizar la app. |
| `MainActivity.java` | La única pantalla. |

## Créditos

- **[assef/meu-ex2](https://github.com/assef/meu-ex2)** (MIT): base del proyecto, IDs del VHAL,
  script de compilación, *stubs* de `android.car` y llave de firma.
- **[SuspiciousRepair/drive_assist](https://github.com/SuspiciousRepair/drive_assist)**: documentó
  que el CarService reproduce el archivo de `lock_car_audio_path` y que el carro reinicia los modos
  al despertar. El código de aquí es propio; no se copió código de ese proyecto (GPL-3).
- **Ravi / [@appsdecarro](https://www.instagram.com/appsdecarro)** (Sapo): el método de desbloqueo
  de la pantalla.

Licencia MIT (ver `LICENSE`).

## Actualización en línea (OTA propio)

Después del primer install, la app se actualiza sola por internet. No se necesita USB ni computador para las siguientes versiones.

### Cómo funciona
- Subes a un repo de GitHub el `ex2lite.apk` (como *release* `latest`) y el `update/version.json`.
- La app, con internet en la pantalla, lee `version.json`. Si el `versionCode` es mayor que el instalado, descarga el APK, **verifica el SHA-256** y lo instala vía `pm install` por el adbd local (root). Si el hash no coincide, no instala nada.
- Botón **Buscar actualizaciones** en la pantalla de modo de manejo.

### Configurar (una vez)
1. Crea un repo en GitHub, por ejemplo `LuisDavidFajardoH/ex2`, y sube este proyecto.
2. En `app/src/com/david/ex2lite/Updater.java`, cambia `MANIFEST_URL` por la URL cruda de tu `version.json`:
   `https://raw.githubusercontent.com/LuisDavidFajardoH/ex2/main/update/version.json`
3. Vuelve a compilar e instala esa versión una vez (por ADB o USB).

### Publicar una versión nueva
```sh
export REPO=LuisDavidFajardoH/ex2
./release.sh 2 1.1 "Qué cambió"
git add -A && git commit -m "v1.1" && git push
gh release create v1.1 app/ex2lite.apk -t v1.1 -n "Qué cambió"
```
El `release.sh` sube el `versionCode`/`versionName`, recompila y recalcula el hash en `version.json` solo.

> El instalador vía adbd local necesita que el ADB de la pantalla esté abierto (ya lo está desde que pusiste Sapo). Un reset de fábrica lo cierra; ahí tocaría reabrirlo una vez.
