# Face Swap

An Android app that swaps the face in one photo onto another — entirely **on-device**.
No photo is ever uploaded anywhere; all detection and compositing runs locally on the phone.

## How it works (v1 - geometric pipeline)

1. **Detect** — [Google ML Kit Face Detection](https://developers.google.com/ml-kit/vision/face-detection) finds faces and landmarks (eyes, mouth, contour) on-device.
2. **Align** — an affine transform maps the source face's eyes/mouth onto the target face's.
3. **Blend** — the warped source face is composited through a feathered mask (soft edges, no hard seam) with a simple color match to the target's skin tone.

This is the "fast path" described in [`FACE_SWAP_APP_GUIDE.txt`](FACE_SWAP_APP_GUIDE.txt) — pure Android
graphics APIs (`Matrix`, `Canvas`, `BlurMaskFilter`), no native dependencies, no model download.
Quality is decent for similar face angles/lighting; a learned swap model (see Roadmap) is the
planned upgrade for harder cases.

## Tech stack

- Kotlin + Jetpack Compose (Material 3)
- ML Kit Face Detection (on-device, free)
- Coroutines for background processing
- Coil for image loading
- Min SDK 26 (Android 8.0), target SDK 35

## Building

```bash
./gradlew assembleDebug
```

The debug APK lands at `app/build/outputs/apk/debug/app-debug.apk` and can be sideloaded directly
(enable "Install unknown apps" for your file manager/browser on the device).

Prebuilt APKs are also published under [Releases](../../releases).

## Roadmap

- [ ] Swap among multiple detected faces (currently uses the largest face in each photo)
- [ ] Upgrade blend quality with Delaunay-triangle warping instead of a single global affine transform
- [ ] Optional AI-model mode (e.g. a mobile-converted [MobileFSGAN](https://github.com/HoiM/MobileFSGAN) or
      [inswapper](https://github.com/deepinsight/insightface) model via ONNX Runtime Mobile) for higher fidelity
- [ ] Camera capture as an image source, in addition to the gallery picker
- [ ] Signed release build + Play Store listing

## Responsible use

Face-swap output can be misused. This app is for personal, consensual use (e.g. swapping
your own face into a photo, or with people who've agreed). Don't use it to impersonate
someone without their consent or to create misleading media of real people.

## License

MIT — see [LICENSE](LICENSE). ML Kit, AndroidX, and Coil are used under their own licenses.
