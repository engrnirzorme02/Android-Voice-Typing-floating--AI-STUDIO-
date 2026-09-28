# 🎙️ Voice Bubble - Floating Voice Typing for Android

**Voice Bubble** একটি অত্যাধুনিক Android অ্যাপ্লিকেশন যা স্ক্রিনের উপর ফ্লোটিং বাবলের মাধ্যমে দ্রুত ভয়েস টাইপিং সুবিধা প্রদান করে। এটি আপনার কণ্ঠস্বর রেকর্ড করে, Google Gemini 2.5 Flash AI-এর সাহায্যে ব্যাকরণ ও ফিলার শব্দ পরিষ্কার করে স্বয়ংক্রিয়ভাবে ক্লিপবোর্ডে কপি করে দেয়, যাতে SwiftKey বা যেকোনো কীবোর্ডে মাত্র ১-ট্যাপে পেস্ট করা যায়।

---

## 🌟 মূল বৈশিষ্ট্য (Key Features)

### 🎯 ভয়েস রেকর্ডিং ও স্পিচ সার্ভিস
- **রিয়েল-টাইম ফ্লোটিং বাবল**: স্ক্রিনের যেকোনো অ্যাপের উপর ভাসমান বাবলে ট্যাপ করে তৎক্ষণাৎ কথা বলা শুরু করুন।
- **মাল্টি-ল্যাঙ্গুয়েজ সাপোর্ট**: বাংলা (বাংলাদেশ `bn-BD`, ভারত `bn-IN`), ইংরেজি (`en-US`, `en-GB`), হিন্দি, আরবি ইত্যাদি ভাষা সমর্থন।
- **অফলাইন মোড (পরিকল্পিত / Planned)**: সম্পূর্ণ অফলাইন স্পিচ রিকগনিশন ভবিষ্যতে যোগ করা হবে; বর্তমানে সিস্টেম অফলাইন স্পিচ মডেল সাপোর্ট করে।

### ✨ AI-চালিত টেক্সট পলিশিং (Gemini 2.5 Flash)
- **Android Keystore AES-GCM সিকিউর স্টোরেজ**: আপনার ব্যক্তিগত Gemini API Key সম্পূর্ণ এনক্রিপ্টেড অবস্থায় ডিভাইসের নিরাপদ কি-স্টোরে সংরক্ষিত থাকে।
- **ব্যাকরণ ও ফিলার দূরীকরণ**: কথ্য বাংলার অপ্রয়োজনীয় শব্দ দূর করে পরিচ্ছন্ন লিখিত বাংলায় রূপান্তর।
- **সরাসরি HTTP কল**: কোনো থার্ড পার্টি SDK বা অপ্রয়োজনীয় নির্ভরতা ছাড়া উচ্চগতির সুরক্ষিত API কল।

### 📋 ক্লিপবোর্ড ও প্রসেস টেক্সট
- **স্বয়ংক্রিয় কপি**: ট্রান্সক্রিপশন সম্পন্ন হওয়ার সাথে সাথে ক্লিপবোর্ডে কপি হয়।
- **Android Process Text (ACTION_PROCESS_TEXT)**: যেকোনো টেক্সট সিলেক্ট করে কনটেক্সট মেনু থেকে "Polish with AI" বা "Dictate replacement" সুবিধা।
- **Quick Settings Tile**: নোটিফিকেশন শেডের কুইক সেটিংস টাইল থেকে ১-ট্যাপে বাবল চালু/বন্ধ।

### 🔄 স্বয়ংক্রিয় সেলফ-আপডেটার (GitHub Releases)
- অ্যাপের ভেতর থেকেই নতুন আপডেট চেক ও ইনস্টলেশন।
- সাইজ ও SHA-256 চেকসাম ভেরিফিকেশন।
- রিলিজ ফ্লো: Git tag `vX.Y.Z` -> GitHub Actions বিল্ড -> GitHub Releases -> ইন-অ্যাপ আপডেট ডিটেকশন ও ইনস্টল।

---

## 📋 সিস্টেম প্রয়োজনীয়তা (Requirements)

- **ন্যূনতম SDK**: API 24 (Android 7.0 Nougat)
- **টার্গেট SDK**: API 36 (Android 16)
- **অনুমতিসমূহ**:
  - `SYSTEM_ALERT_WINDOW` (ফ্লোটিং বাবলের জন্য)
  - `RECORD_AUDIO` (ভয়েস রেকর্ডিংয়ের জন্য)
  - `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MICROPHONE` (অ্যান্ড্রয়েড ১৪+ ব্যাকগ্রাউন্ড মাইক্রোফোন সার্ভিস)
  - `POST_NOTIFICATIONS` (অ্যান্ড্রয়েড ১৩+)
  - `REQUEST_INSTALL_PACKAGES` (ইন-অ্যাপ আপডেটের জন্য)

---

## 🚀 সেটআপ ও বিল্ড গাইড (Build & Setup)

### বিল্ড করার নিয়ম:
```bash
git clone https://github.com/engrnirzorme02/Android-Voice-Typing-floating--AI-STUDIO-.git
cd Android-Voice-Typing-floating--AI-STUDIO-
./gradlew assembleDebug
```

### রিলিজ বিল্ড (`assembleRelease`):
```bash
export KEYSTORE_PATH="/path/to/keystore.jks"
export KEYSTORE_PASSWORD="password"
export KEY_ALIAS="alias"
export KEY_PASSWORD="password"
./gradlew assembleRelease
```
*দ্রষ্টব্য: যদি রিলিজ কি-স্টোর এনভায়রনমেন্ট ভেরিয়েবল না থাকে, তবে লোকাল বিল্ডের জন্য ডিবাগ কি-স্টোর ব্যবহার করে বিল্ড সফল হবে।*

### রিলিজ ও আপডেট ওয়ার্কফ্লো:
1. নতুন রিলিজের জন্য গিট ট্যাগ তৈরি করুন: `git tag v1.0.1` এবং পুশ করুন: `git push origin v1.0.1`
2. GitHub Actions স্বয়ংক্রিয়ভাবে `app-release.apk` বিল্ড করে `voicebubble.apk` ও `voicebubble.apk.sha256` তৈরি করবে এবং GitHub Release এ আপলোড করবে।
3. ব্যবহারকারীরা সরাসরি অ্যাপের Settings স্ক্রিন থেকে "Check for updates" বাটনে ট্যাপ করে নতুন সংস্করণ ইনস্টল করতে পারবেন।

---

## ⚠️ পরিকল্পিত বৈশিষ্ট্য (Planned Features)
- 🔄 সম্পূর্ণ অফলাইন অন-ডিভাইস AI মডেল সাপোর্ট (Planned)
- 🔄 ক্লাউড ব্যাকআপ ও ফায়ারস্টোর সিঙ্ক (Planned)
- 🔄 ক্রস-প্ল্যাটফর্ম / iOS ক্লায়েন্ট (Planned)

---

## 👤 লেখক
**Engr Nir Zorme** ([@engrnirzorme02](https://github.com/engrnirzorme02))
