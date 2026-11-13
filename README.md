# AR RACING

A 3D Android racing game built with Java and libGDX.

## Build APK in GitHub Actions

1. Push changes to the `main` branch, or run the workflow manually from the Actions tab.
2. Open the **Build AR RACING APK** workflow run.
3. When it succeeds, download the `AR-RACING-debug-apk` artifact.
4. Extract the ZIP file to get `app-debug.apk`.

## Project modules

- `core`: shared libGDX game code.
- `android`: Android launcher and APK configuration.

The project uses libGDX 1.14.2 and Java 17 for the cloud build.
