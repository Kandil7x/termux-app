# Termux Neon

نسخة معدلة من Termux بهوية Cyber Neon ولوحة أدوات اسمها **Neon Deck**.
الطرفية نفسها ما اتغيرتش: اللوحة بتكتب نص في الجلسة الحالية زي ما تكتبه بإيدك، وهي مخفية افتراضياً.

## اللي اتغير

| الملف | التعديل |
|---|---|
| `terminal-emulator/.../TerminalColorScheme.java` | ألوان الطرفية الافتراضية (نيون). أي `colors.properties` عندك بيتغلب عليها. |
| `app/src/main/res/values*/themes.xml`, `colors.xml`, `styles.xml` | خلفية داكنة، لون أزرار لوحة المفاتيح الإضافية، حوار النيون |
| `app/src/main/res/layout/activity_termux.xml` | شريط Neon Deck فوق أزرار الـ extra keys + زر في القائمة الجانبية |
| `app/src/main/java/com/termux/app/neon/*` | `NeonDeck` (اللوحة) و`NeonCatalog` (كتالوج الحزم) و`NeonAction` و`NeonIo` |
| `app/src/main/assets/neon/deck.json` | أزرار اللوحة الافتراضية |
| `app/src/main/assets/neon/catalog.json` | 44 حزمة بوصف عربي وإنجليزي |
| `TermuxActivity.java`, `TermuxTerminalViewClient.java` | ربط اللوحة (3 أسطر + اختصار Volume Up + G) |
| `.github/workflows/neon_build.yml` | بناء APK على GitHub |

## البناء (على GitHub، من غير ما تحتاج كمبيوتر)

1. اعمل Fork لـ `termux/termux-app` على حسابك.
2. على الكمبيوتر أو من Termux: `git clone <fork-url> && cd termux-app && git checkout -b neon`
3. `git apply neon.patch && git add -A && git commit -m "Neon" && git push -u origin neon`
4. من تبويب Actions على GitHub، شغّل **Neon build** (لو Actions متوقفة على الـ fork فعّلها الأول).
5. بعد ما يخلص، نزّل `termux-neon-debug` من Artifacts. خد ملف `universal` أو `arm64-v8a`.

البناء المحلي: `./gradlew assembleDebug` (محتاج JDK 17 و Android SDK و NDK).

## التثبيت (مهم، اقرأه كله)

- الـ APK موقّع بمفتاح مختلف عن نسخة F-Droid، فلازم **تمسح Termux الرسمي الأول**، والمسح بيمسح كل بياناتك.
- اعمل نسخة احتياطية قبل المسح، من Termux الرسمي:
  ```
  termux-setup-storage
  tar -zcf ~/storage/shared/termux-backup.tar.gz -C /data/data/com.termux/files ./home ./usr
  ```
  وتأكد إن الملف موجود وانسخه بعيد عن الموبايل.
- الاسترجاع في النسخة الجديدة (بعد `termux-setup-storage`):
  ```
  tar -zxf ~/storage/shared/termux-backup.tar.gz -C /data/data/com.termux/files --recursive-unlink --preserve-permissions
  ```
- التطبيق بيستخدم `sharedUserId`، يعني إضافات Termux (API وBoot وWidget وغيرها) لازم تكون موقعة بنفس المفتاح. إضافات F-Droid مش هتشتغل معاه. لازم تثبّت نسخ الإضافات الموقعة بنفس المفتاح (راجع صفحة Releases لكل إضافة).
- اسم الحزمة فضل `com.termux` عن قصد: حزم Termux الجاهزة (bootstrap و`pkg`) مبنية على المسار `/data/data/com.termux`. لو غيرت الاسم هتحتاج تبني الـ bootstrap والحزم كلها من الأول.

## الاستخدام

- افتح القائمة الجانبية (اسحب من الشمال) واضغط **[ NEON DECK ]**، أو اضغط Volume Up + G.
- الزر بيكتب الأمر في الجلسة الحالية. لو الأمر عليه `newSession` بيفتح جلسة جديدة الأول.
- **+ Install**: كتالوج الحزم. علّم اللي عايزه واضغط Install selected، فيتنفذ `pkg install -y ...`.
- **Edit**: يفتح `~/.termux/neon/deck.json` في nano. بعد التعديل اضغط **Reload**.

شكل `deck.json`:

```json
{
  "version": 1,
  "actions": [
    { "label": "git status", "command": "git status" },
    { "label": "update", "command": "pkg update -y && pkg upgrade -y", "newSession": true },
    { "label": "^C", "send": "\u0003" }
  ]
}
```

- `command`: يتكتب ومعاه Enter.
- `send`: نص خام من غير Enter (للمفاتيح الخاصة).
- أي عنصر من غير `label` أو من غير `command`/`send` بيتجاهل.
- لو الملف فيه خطأ، اللوحة بترجع للافتراضي وبتظهر رسالة.

## تخصيص الشكل

- ألوان الواجهة: `app/src/main/res/values/colors.xml` (`neon_*`).
- ألوان الطرفية: `TerminalColorScheme.java`، أو `~/.termux/colors.properties` على الموبايل من غير إعادة بناء.
- الخط: `~/.termux/font.ttf` (JetBrains Mono Nerd Font مناسب جداً).
- حزم جديدة في الكتالوج: أضف عنصر في `catalog.json` (`name` و`category` و`description` و`description_ar`).

## حدود معروفة

- التعديلات اتكتبت على كود Termux بتاريخ `8629e63` (سبتمبر 2026) واتفحصت صياغتها ومواردها، لكن **ما اتبنيتش ولا اتجربت على جهاز** قبل التسليم. أول بناء ممكن يطلّع خطأ صغير في الترجمة، وده متوقع ويتصلح بسرعة.
- أسماء الحزم في الكتالوج ما اتأكدتش منها واحدة واحدة. لو حزمة مش موجودة `pkg` هيقولك في الطرفية.
- بناء الـ debug بيستخدم مفتاح الاختبار الموجود في المستودع، مش مفتاح إصدار خاص بيك.

## الرخصة

Termux تحت GPLv3. أي نسخة معدلة توزعها لازم تبقى GPLv3 ومعاها الكود. وفكّر في اسم مختلف عن "Termux" لو هتنشرها للناس.
