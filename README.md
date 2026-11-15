# ELEKTRO I: YOU CAN (NOT) REDO

Videojuego de escritorio Java/Swing creado para Programación Orientada a Objetos.
Requiere **JDK 21** para desarrollar; el paquete nativo incluye su propio runtime.

## Clonar y abrir en IntelliJ IDEA

1. Instala un **JDK 21 completo**, por ejemplo Eclipse Temurin 21, y Git.
2. Clona el repositorio y selecciona la rama de esta modernización:
   ```sh
   git clone https://github.com/Aisack23/ELEKTRO_I.git
   cd ELEKTRO_I
   git switch feature/refactor-audio-exe
   ```
3. En IntelliJ IDEA, selecciona **File → Open** y abre el `pom.xml` de la raíz como proyecto Maven. No abras la antigua carpeta `Elektro` como proyecto independiente.
4. En **File → Project Structure → Project SDK**, selecciona JDK 21. Selecciona también JDK 21 para el importador y el runner de Maven en **Settings → Build, Execution, Deployment → Build Tools → Maven**. Usa el Maven Wrapper del proyecto.
5. Espera la importación de Maven. Ejecuta la clase `game.Main` desde `src/main/java`, o utiliza los comandos siguientes en la terminal de IntelliJ.

No hace falta instalar Maven globalmente: `mvnw` / `mvnw.cmd` descargan Maven **3.9.9** y verifican su SHA-256. Configura `JAVA_HOME` al directorio del JDK, no a `bin`, y añade `JAVA_HOME/bin` al `PATH`. La primera compilación requiere acceso HTTPS a Maven Central.

### Compilar, probar y ejecutar

Windows, desde PowerShell en la raíz del repositorio:

```powershell
.\mvnw.cmd clean verify
java -jar .\target\elektro-1.0.0.jar
```

Linux/macOS:

```sh
./mvnw clean verify
java -jar target/elektro-1.0.0.jar
```

Alternativa para desarrollar: `./mvnw exec:java` (Windows: `.\mvnw.cmd exec:java`), después de compilar. El antiguo `Elektro/build.sh` delega ahora en Maven. Los recursos se cargan desde el classpath, por lo que el JAR puede ejecutarse desde otro directorio.

Controles principales: WASD/flechas para mover o navegar, Enter para confirmar, Escape para opciones. Los combates se resuelven desde su ventana; cerrarla con la X está deshabilitado para evitar abandonar el estado de combate sin resolverlo.

## Estructura

```text
pom.xml                       Compilación, pruebas y manifiesto del JAR
.mvn/wrapper/ + mvnw*          Maven fijado y descarga verificada
src/main/java/audio/          Política de música y backend Java Sound
src/main/java/game/            Juego, mapas y niveles
src/main/java/battle/          Combates y su interfaz Swing
src/main/resources/           Imágenes, mapas, fuentes y audio
src/test/java/                Pruebas JUnit 5
scripts/package-windows.ps1    Ejecutable portable o instalador con jpackage
.github/workflows/build.yml   Verificación Linux/Windows y artefacto portable Windows
```

No se versionan `target`, `dist` ni configuraciones personales de IntelliJ. Maven es la fuente de configuración del proyecto.

## Añadir o reemplazar audio

Usa archivos **WAV PCM de 16 bits**, preferiblemente 44.1 kHz, mono o estéreo. No basta con cambiar la extensión de un MP3: hay que convertirlo. Respeta exactamente los nombres y las minúsculas; Linux distingue mayúsculas.

Para música, solo añade/reemplaza el archivo en la ruta indicada y vuelve a compilar; no tienes que editar clases Java:

| Contexto | Ruta dentro de `src/main/resources/` | Incluido actualmente |
| --- | --- | --- |
| Menú y selección de nivel | `audio/music/title.wav` | Sí, antigua BlueBoyAdventure |
| Casa | `audio/music/maps/house.wav` | Pendiente de añadir |
| Origen | `audio/music/maps/origen.wav` | Pendiente de añadir |
| Desierto | `audio/music/maps/desert.wav` | Pendiente de añadir |
| Centro comercial | `audio/music/maps/mall.wav` | Sí, antigua Merchant |
| Estacionamiento | `audio/music/maps/parkinglot.wav` | Pendiente de añadir |
| Bosque | `audio/music/maps/forest.wav` | Pendiente de añadir |
| Cueva | `audio/music/maps/cave.wav` | Sí, antigua Dungeon |
| Todos los combates, incluidos jefes | `audio/music/battle.wav` | Sí, antigua FinalBattle |

Cada mapa tiene una ruta exclusiva: añade una composición diferente en cada archivo para completar su banda sonora. No se han inventado pistas ni reutilizado la música de combate como música de mapa. Un archivo ausente produce silencio y un aviso; no interrumpe el juego. Para un mapa nuevo se utiliza automáticamente `audio/music/maps/<mapName>.wav`.

Los efectos existentes están en `audio/effects/`. Para disparar un efecto desde un evento del juego:

```java
gamePanel.getAudio().playEffect("/audio/effects/coin.wav");
```

Añadir el archivo no inventa el evento que debe activarlo: conecta esta llamada al evento correspondiente. La música y los efectos tienen volumen independiente de 0 a 5 en las opciones. La música se repite, los efectos pueden superponerse y su concurrencia se limita a 16 clips.

### Diseño del audio

- `MusicDirector` traduce estados y nombres de mapas a rutas. El combate tiene prioridad; al salir se selecciona de nuevo la música del mapa. Pausas, diálogos y opciones conservan la pista de ese contexto sin reiniciarla.
- `AudioManager` controla los canales, el volumen, la repetición y el ciclo de vida. Cierra música reemplazada, efectos terminados al reproducir el siguiente efecto y todos los clips al cerrar el juego.
- `AudioBackend` permite sustituir Java Sound o probar la política con un backend simulado.
- `JavaSoundBackend` carga WAV desde el JAR y libera asignaciones parciales si falla la apertura. Sin dispositivo, formato válido o archivo, el juego sigue en silencio y registra el diagnóstico. Los fallos de una misma ruta no generan avisos continuos.

## Generar el ejecutable `.exe` en Windows

**Ejecuta estos pasos en Windows** con un JDK 21 para Windows. `jpackage` no compila paquetes para otro sistema operativo. No se necesita Launch4j ni un Java instalado en el equipo de quien juega.

### Opción A: ejecutable portable, sin instalar WiX

Desde PowerShell en la raíz:

```powershell
powershell -NoProfile -File .\scripts\package-windows.ps1 -Type app-image
```

El script ejecuta las pruebas, crea el JAR y empaqueta un runtime reducido con `java.desktop` y `java.logging`. El resultado es:

```text
dist/windows-app-image/Elektro/Elektro.exe
```

Ejecuta ese `.exe`. Para distribuirlo, comprime y entrega **toda la carpeta `Elektro`**, incluidas `app` y `runtime`; el `.exe` por sí solo no contiene los recursos ni Java.

### Opción B: instalador `.exe`

1. Instala **WiX Toolset 3.x** (por ejemplo 3.14.1), necesario para los instaladores de `jpackage` en JDK 21. Comprueba que `candle.exe` y `light.exe` están en el `PATH`; WiX 4 usa una interfaz distinta y no sustituye este requisito.
2. Abre una nueva terminal PowerShell y ejecuta:

   ```powershell
   powershell -NoProfile -File .\scripts\package-windows.ps1 -Type exe
   ```

3. El instalador aparece en `dist/windows-exe/` con nombre `Elektro-1.0.0.exe`. Instala por usuario y permite elegir carpeta, crear acceso directo y entrada en el menú Inicio.

La versión se toma de `pom.xml`. Si ya existe el directorio de salida de ese tipo, mueve o elimina **esa salida generada** antes de repetir el empaquetado. El script no sobrescribe una distribución anterior. Los paquetes locales no están firmados digitalmente.

### Verificación de una distribución

En un equipo Windows sin Java instalado: ejecuta el portable o instala el instalador, abre un nivel, cambia de mapa, entra y sale de una batalla, comprueba los dos volúmenes y cierra el juego. Añade primero las cinco pistas de mapas pendientes si quieres verificar una banda sonora completa.

La acción de GitHub `Build and test` ejecuta las pruebas en Linux y Windows, y construye el portable en Windows. Una vez ejecutada en GitHub, su artefacto `Elektro-Windows-portable` estará disponible en Actions. El instalador con WiX se genera con el comando anterior.

## Cambios y validación

Esta refactorización corrige el fallo de arranque sin audio, el bucle infinito de ataques inválidos, la curación duplicada que modificaba los consumibles, la progresión basada en excepciones y fugas de gráficos/streams en los puntos modificados. La construcción de Swing, la actualización del juego, los mensajes temporales de batalla y el repintado usan el hilo de eventos; el reloj del juego espera entre frames en vez de consumir CPU continuamente.

`clean verify` ejecuta 9 pruebas: transiciones de música por contexto, separación de canales, límites y cierre de clips, ausencia de recursos/dispositivos, fallos de arranque de clips, ataques inválidos, curación y recorrido por los siete mapas. También se comprobó en Linux el arranque gráfico del JAR sin audio, la navegación del menú y la entrada al primer mapa, además de generar una imagen nativa con `jpackage`.

El `.exe` y el instalador no se han ejecutado en esta máquina Linux. La reproducción audible tampoco se ha comprobado aquí porque no hay dispositivo de sonido. Antes de una publicación final, valida esos puntos en Windows y recorre las cinemáticas y combates completos; estas comprobaciones no equivalen a una auditoría exhaustiva de todo el juego heredado.
