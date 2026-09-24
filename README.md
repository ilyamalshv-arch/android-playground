# android-playground

Android-приложения, которые пишутся с телефона через Claude Code, без ПК.

## Как это работает

1. Claude Code правит код и пушит в `main`.
2. GitHub Actions (`.github/workflows/build.yml`) собирает APK. Это занимает около 5 минут.
3. Готовый APK появляется в **Releases**. Самый свежий лежит тут:
   https://github.com/ilyamalshv-arch/android-playground/releases/latest
4. Откройте ссылку на телефоне, скачайте `.apk` и установите.
   В первый раз Android попросит разрешить установку из этого источника (браузера).

Все сборки подписаны одним ключом (`keystore/debug.keystore`), поэтому новая версия
ставится поверх старой, и данные приложения сохраняются.
Этот ключ публичный и подходит только для личных экспериментов, не для Google Play.

## Стек

Kotlin, Jetpack Compose, Material 3, minSdk 26 (Android 8.0+).
