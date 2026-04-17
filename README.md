<div align="center">

# zvonilka

**Blocks every call from a number that isn't in your contacts.**
Tap the button. That's it.

![minSdk](https://img.shields.io/badge/minSdk-29-26E5A8?style=for-the-badge)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![License](https://img.shields.io/badge/license-MIT-F1F3FF?style=for-the-badge)

</div>

---

## Что это

Минималистичное Android-приложение для жёсткой фильтрации входящих.
Один экран, одна кнопка, один принцип:

> **Если номер не в контактах — звонок не пройдёт.**

На каждый заблокированный вызов — push-уведомление с номером.

---

## Как это работает

Под капотом — системный [`CallScreeningService`](https://developer.android.com/reference/android/telecom/CallScreeningService)
(Android 10+). При входящем вызове Android передаёт детали сервису **до того, как телефон зазвонит**.
zvonilka смотрит номер, сверяет с `ContactsContract.PhoneLookup` и отвечает:

| Номер в контактах | Тумблер | Результат |
|---|---|---|
| ✓ | ON | пропускается как обычно |
| ✗ | ON | `setDisallowCall(true) + setRejectCall(true)` → звонок не доходит, пуш летит |
| — | OFF | ничего не делается |

Никаких серверов, никакой телеметрии, никаких списков «спам-номеров». Только локальная проверка контактов.

---

## Установка

1. Скачай APK из [**релизов**](../../releases/latest).
2. Перекинь на телефон, открой — разреши установку из неизвестных источников.
3. Запусти — нажми большую круглую кнопку.
4. Система спросит:
   - разрешение на контакты (читаем, чтобы отличить своих от чужих);
   - разрешение на уведомления (Android 13+);
   - назначить zvonilka приложением для фильтрации вызовов — **соглашайся, без этого Android не отдаст звонки сервису**.
5. Готово. Кнопка горит зелёным — защита активна.

---

## Сборка из исходников

```bash
git clone https://github.com/<you>/zvonilka.git
cd zvonilka
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties  # или путь к твоему Android SDK
./gradlew :app:assembleDebug
```

APK появится в `app/build/outputs/apk/debug/app-debug.apk`.

Требования: **JDK 17**, **Android SDK Platform 35**, **build-tools 35.0.0**.

---

## Стек

- **Kotlin** 2.1.20
- **AGP** 8.12.0 / Gradle 9.0.0
- **minSdk** 29 (Android 10) — нижняя граница, где доступен `RoleManager.ROLE_CALL_SCREENING`
- **targetSdk / compileSdk** 35 (Android 15)
- `androidx.core`, `androidx.appcompat`, `constraintlayout`, `material`

## Структура

```
app/src/main/
├── AndroidManifest.xml
├── java/com/zvoilka/blocker/
│   ├── MainActivity.kt          — экран с тумблером, запросы прав и роли
│   ├── CallBlockerService.kt    — CallScreeningService, собственно блокировка
│   └── BlockerPrefs.kt          — SharedPreferences-флаг on/off
└── res/
    ├── layout/activity_main.xml
    ├── drawable/                — кастомные фоны кнопки и векторные иконки
    └── mipmap-anydpi-v26/       — адаптивная иконка (трубка в шестиграннике с коралловым слэшем)
```

---

## FAQ

**А спам-базы? VoIP? Белые списки по регионам?**
Нет. Задача одна: отсечь всё, что не знаешь. Если нужен номер — добавь в контакты.

**Почему нужно «приложение для фильтрации вызовов по умолчанию»?**
Потому что Android не даёт произвольным приложениям решать судьбу входящих.
Фильтратор — одна роль на систему, назначается вручную. Это by design.

**Мой банк/курьер/врач не в контактах — их тоже заблокирует?**
Да. Либо сохрани номер, либо выключи защиту на время ожидания звонка.

**SMS блокирует?**
Нет. Только голосовые вызовы.

---

<div align="center">

made with ☕ and mild misanthropy

</div>
