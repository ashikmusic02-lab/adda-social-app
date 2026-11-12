# বাংলায় ধাপে ধাপে সেটআপ গাইড (শুধুমাত্র ফোন ব্যবহার করে) — ধাপ ১

নেটিভ Android (`app/`) আমাদের একমাত্র ইউজার অ্যাপ। কম্পিউটার ছাড়া শুধুমাত্র ফোন থেকে কীভাবে **স্থির `debug.keystore`-এর SHA-1 একবার Firebase-এ যোগ করবেন**, **GitHub Secrets সেট করবেন** এবং **APK বিল্ড করবেন** তার নির্দেশিকা নিচে দেওয়া হলো।

---

## ১. স্থির `debug.keystore` ও একবারই SHA-1 / SHA-256 যোগ করার নিয়ম

এই রিপোজিটরির রুটে একটি স্থির `debug.keystore` (এবং `debug.keystore.base64`) রাখা আছে এবং `app/build.gradle.kts`-এর `signingConfigs.debugConfig`-এ এটি যুক্ত করা আছে (`release` keystore কখনোই রিপোতে রাখা হয়নি)। এর ফলে আপনি যতবারই GitHub Actions দিয়ে APK বিল্ড করুন না কেন, প্রতি বিল্ডে **একই SHA-1 এবং SHA-256** ব্যবহৃত হবে।

**স্থির ডিবাগ ফিঙ্গারপ্রিন্ট (যা বিল্ড লগেও ছাপা হয়):**
- **SHA-1:** `13:EE:07:37:10:AE:FC:86:1A:59:F7:51:69:56:C6:64:A7:D9:62:6E`
- **SHA-256:** `D2:CA:D1:9E:AE:38:CB:8F:1C:D4:51:D5:FC:56:62:57:84:4D:5E:8C:0D:06:56:E1:81:63:27:6D:F5:B1:37:73`

**কীভাবে একবার Firebase-এ যোগ করবেন:**
১. ফোনের ব্রাউজার থেকে [Firebase Console](https://console.firebase.google.com/)-এ আপনার প্রজেক্টে যান।
২. **Project Settings (গিয়ার আইকন) -> Your apps -> Android app (`com.aistudio.socialhub.bnkxqz`)**-এ স্ক্রল করুন।
৩. **Add fingerprint** বাটনে ট্যাপ করে উপরের **SHA-1** এবং **SHA-256** মান দুটি পেস্ট করে Save করুন।
৪. **Build -> Authentication -> Sign-in method**-এ গিয়ে **Email/Password** এবং **Google** উভয় প্রোভাইডার **Enable** করুন।
৫. এরপর নতুন `google-services.json` ডাউনলোড করে নিন।

---

## ২. GitHub Secrets তালিকা (মাত্র ৩টি Secret প্রয়োজন)

যেহেতু `user-app/` (React) বাদ দিয়ে শুধুমাত্র নেটিভ Android (`app/`) রাখা হয়েছে, তাই কোনো `VITE_` Secret লাগবে না। ফোনের ব্রাউজারে **GitHub Repo -> Settings -> Secrets and variables -> Actions -> New repository secret**-এ গিয়ে শুধু নিচের ৩টি Secret যোগ করুন:

| Secret-এর নাম | কোন ওয়ার্কফ্লোতে লাগে | মান কীভাবে বানাবেন? |
| :--- | :--- | :--- |
| `GOOGLE_SERVICES_JSON_BASE64` | `build-user-debug-apk.yml` | ডাউনলোড করা `google-services.json` ফাইলটির পুরো টেক্সট কপি করে Base64-এ এনকোড করা স্ট্রিং। |
| `FIREBASE_PROJECT_ID` | `deploy-firebase-rules.yml` | আপনার Firebase Project ID (যেমন: `forhadbd`)। |
| `FIREBASE_SERVICE_ACCOUNT` | `deploy-firebase-rules.yml` | Firebase Console -> Project Settings -> **Service accounts** -> **Generate new private key** থেকে ডাউনলোড করা JSON ফাইলের সম্পূর্ণ টেক্সট। |
