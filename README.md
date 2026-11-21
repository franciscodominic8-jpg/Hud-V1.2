# Fold HUD (home screen launcher for Galaxy Z Fold 4)

Layout A "Command Center": clock, phone stats, network, weather, calendar, moon/sun, a draggable globe with news
callouts per country, weekly steps, news tabs (World / Myanmar), stocks, quick-open tiles you choose yourself (hold a tile or dock slot to change it), HUD icon packs, dock and app drawer.
Every widget opens its own app. Written with plain Android (no extra libraries) to keep memory and battery use low.

## Build the APK without installing anything (GitHub, free)
1. Make a free account at github.com and create a new repository (private is fine).
2. Upload the CONTENTS of this folder (all files and folders, including `app`, `gradle`, `settings.gradle`, `build.gradle`).
3. The build file must be at `.github/workflows/build-apk.yml`. If GitHub did not upload the hidden `.github` folder:
   click "Add file" > "Create new file", type the name `.github/workflows/build-apk.yml`, paste the text from `build-apk.yml.txt`, commit.
4. Open the "Actions" tab. The "Build APK" run starts by itself (or press "Run workflow"). Wait 5-10 minutes.
5. Open the finished run, scroll to "Artifacts", download `FoldHUD-apk` (a zip), unzip it: `app-release.apk`.
6. Copy the APK to the phone and tap it. Allow "Install unknown apps" for the app you opened it from.
7. Settings > Apps > Choose default apps > Home app > Fold HUD.
If a build fails, open the failed run, copy the red error text and send it to Claude.

## Build with Android Studio instead
Open this folder in Android Studio (Hedgehog or newer, JDK 17). Let it sync. Build > Select Build Variant > release.
Build > Build App Bundle(s) / APK(s) > Build APK(s).

## First start
Allow "Physical activity" (steps) and "Location" (approximate) when asked. Without location it guesses your city
from the internet connection. Tap the name "Julian's Fold" (or the ⋮ on the cover screen) for power mode and globe options.

## Power saving
- Saver (still picture), Balanced (default, about 6 frames a second while the home screen is visible), Full.
- Saver turns on by itself when Battery Saver is on or the battery is under 15%.
- Nothing runs when another app is on top or the screen is off. News/stocks/weather refresh only when old
  (a quarter as often in Battery Saver). Steps: one quick sensor read per hour, no background service.

## Settings you may want to change
`Config.java`: news feeds, stock symbols, steps goal, refresh times.

---------------------------------------------------------------------------------------------------

# Fold HUD (Myanmar)

## GitHub နဲ့ APK ဆောက်နည်း (ဘာမှ install မလုပ်ရ)
1. github.com မှာ အကောင့်အခမဲ့ဖွင့်ပြီး repository အသစ်တစ်ခု လုပ်ပါ။
2. ဒီ folder ထဲက ဖိုင်၊ folder အားလုံး (`app`, `gradle`, `settings.gradle`, `build.gradle` အပါအဝင်) ကို upload လုပ်ပါ။
3. build ဖိုင်က `.github/workflows/build-apk.yml` မှာ ရှိရမယ်။ မတင်သွားရင် "Add file" > "Create new file" နှိပ်ပြီး နာမည်ကို
   `.github/workflows/build-apk.yml` လို့ ရိုက်ကာ `build-apk.yml.txt` ထဲက စာကို paste လုပ်ပြီး commit လုပ်ပါ။
4. "Actions" tab ကိုဖွင့်ပါ။ "Build APK" အလိုအလျောက် စပါမယ် (ဒါမှမဟုတ် "Run workflow" နှိပ်ပါ)။ ၅ မိနစ်ကနေ ၁၀ မိနစ် စောင့်ပါ။
5. ပြီးသွားတဲ့ run ကိုဖွင့်ပြီး "Artifacts" မှာ `FoldHUD-apk` ကို ဒေါင်းလုဒ်လုပ်ပါ။ zip ဖြေရင် `app-release.apk` ပါပါမယ်။
6. APK ကို ဖုန်းထဲကူးပြီး နှိပ်ပါ။ "Install unknown apps" ကို ခွင့်ပြုပါ။
7. Settings > Apps > Choose default apps > Home app > Fold HUD ကို ရွေးပါ။
Build မအောင်ရင် မအောင်တဲ့ run ထဲက အနီရောင် error စာကို ကူးပြီး Claude ကို ပို့ပါ။

## ပထမဆုံး စတင်တဲ့အခါ
"Physical activity" (ခြေလှမ်းရေ) နဲ့ "Location" (ခန့်မှန်းတည်နေရာ) ကို ခွင့်ပြုပါ။ "Julian's Fold" ကို နှိပ်ရင် power mode နဲ့ ကမ္ဘာလုံး option တွေ ပေါ်ပါမယ်။

## ဘက်ထရီ သက်သာစေပုံ
Saver (ပုံတည်ငြိမ်)၊ Balanced (မူလ)၊ Full။ Battery Saver ဖွင့်ထားရင် ဒါမှမဟုတ် ဘက်ထရီ ၁၅% အောက်ရောက်ရင် Saver ကို အလိုအလျောက်သုံးပါတယ်။
တခြား app ပေါ်မှာရှိနေရင် ဒါမှမဟုတ် screen ပိတ်ထားရင် ဘာမှ မလည်ပါဘူး။
