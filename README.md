# AR RACING

Android 3D racing game built with Java, libGDX and gdx-gltf PBR rendering.

## Build APK with GitHub Actions

1. Push to `main` (or `master`), or open **Actions** and run **Build AR RACING APK** manually.
2. Open the completed workflow run.
3. Download the `AR-RACING-debug-apk` artifact ZIP and extract `app-debug.apk`.

## Controls

- Touch the lower-right half of the screen to accelerate.
- Touch the lower-left half to brake.
- Touch the upper-left / upper-right halves to steer.

## 3D models

The game runs with built-in procedural placeholder track and cars. To use custom GLB models, add these files under `android/src/main/assets/models/`:

- `track.glb`
- `blue_car.glb`
- `yellow_car.glb`
- `red_car.glb`

If a GLB file is missing or cannot be loaded, the game uses its built-in fallback model.

## Technology

- Java 17
- Android Gradle Plugin 8.10.1
- libGDX 1.14.2
- gdx-gltf 2.3.0 (JitPack)
