# সম্পূর্ণ Firestore ডাটাবেজ স্কিমা ও খরচ অপ্টিমাইজেশন পরিকল্পনা (`docs/FIRESTORE_SCHEMA.md`)

> **বাস্তবায়ন নীতি:** এই নথিতে সম্পূর্ণ প্রোজেক্টের ১৩টি কালেকশন (`users`, `usernames`, `posts`, `comments`, `likes`, `follows`, `bookmarks`, `notifications`, `reports`, `blocks`, `categories`, `auditLogs`, `appSettings`) এবং Denormalization ও Counter পরিকল্পনা দেওয়া হলো।
> **ধাপ ১-এ শুধুমাত্র `users` এবং `usernames` বাস্তবায়িত ও খোলা হয়েছে।** বাকি ১১টি কালেকশন ধাপ ২–৬ এর জন্য পরিকল্পিত এবং ধাপ ১-এর `firestore.rules`-এ সম্পূর্ণ বন্ধ (`deny all`)।

---

## ১. `users/{userId}` — **[ধাপ ১-এ বাস্তবায়িত]**
ব্যবহারকারীর প্রোফাইল ও পছন্দসমূহ। ডকুমেন্ট আইডি = Firebase Auth `uid`।
- `uid` (`string`, আবশ্যিক): ব্যবহারকারীর Firebase Auth UID।
- `displayName` (`string`, আবশ্যিক, ১–৬০ অক্ষর): ব্যবহারকারীর পূর্ণ নাম।
- `username` (`string`, আবশ্যিক, ৩–২৪ অক্ষর, `^[a-z0-9_]+$`): ইউনিক হ্যান্ডেল।
- `email` (`string`, আবশ্যিক, সর্বোচ্চ ১২০ অক্ষর): ব্যবহারকারীর ইমেইল।
- `photoUrl` (`string`, ঐচ্ছিক, সর্বোচ্চ ৫০০ অক্ষর): প্রোফাইল ছবির URL।
- `bio` (`string`, ঐচ্ছিক, সর্বোচ্চ ১৬০ অক্ষর): সংক্ষিপ্ত পরিচিতি।
- `language` (`string`: `'bn'` | `'en'`): অ্যাপের ভাষা।
- `themeMode` (`string`: `'system'` | `'light'` | `'dark'`): থিম পছন্দ।
- `onboardingCompleted` (`boolean`): নাম ও ইউজারনেম নির্ধারণ সম্পন্ন হয়েছে কি না।
- `followersCount` (`number`, সিস্টেম ফিল্ড — ক্লায়েন্ট থেকে লেখা নিষিদ্ধ, ডিফল্ট `0`)।
- `followingCount` (`number`, সিস্টেম ফিল্ড — ক্লায়েন্ট থেকে লেখা নিষিদ্ধ, ডিফল্ট `0`)।
- `postsCount` (`number`, সিস্টেম ফিল্ড — ক্লায়েন্ট থেকে লেখা নিষিদ্ধ, ডিফল্ট `0`)।
- `isVerified` (`boolean`, সিস্টেম ফিল্ড — ক্লায়েন্ট থেকে লেখা নিষিদ্ধ)।
- `isBanned` (`boolean`, সিস্টেম ফিল্ড — ক্লায়েন্ট থেকে লেখা নিষিদ্ধ)।
- `createdAt` (`timestamp`, আবশ্যিক, অপরিবর্তনীয়): তৈরি হওয়ার সার্ভার সময়।
- `updatedAt` (`timestamp`, আবশ্যিক): সর্বশেষ পরিবর্তনের সার্ভার সময়।
- **নোট (`role` ফিল্ড নেই):** `users` ডকুমেন্টে কোনো `role` ফিল্ড রাখা হয়নি। অ্যাডমিন বা মডারেটর রোল পরবর্তীতে (ধাপ ৬) **Firebase Auth Custom Claims** দিয়ে নিয়ন্ত্রিত হবে।

## ২. `usernames/{username}` — **[ধাপ ১-এ বাস্তবায়িত]**
ইউজারনেম ইউনিক রাখার রেজিস্ট্রি। ডকুমেন্ট আইডি = ছোট হাতের ইউজারনেম (`^[a-z0-9_]{3,24}$`)।
- `uid` (`string`, আবশ্যিক, অবশ্যই `request.auth.uid`-এর সমান)।
- `createdAt` (`timestamp`, আবশ্যিক): ইউজারনেম নেওয়ার সার্ভার সময়।
- **অ্যাটমিক নিয়ম (`getAfter`):** `usernames/{username}` তৈরি করার সময় একই ব্যাচে বা ট্রানজ্যাকশনে `users/{request.auth.uid}` ডকুমেন্টের `username` ফিল্ডটি এই `username`-এর সমান হতে হবে। `update` সম্পূর্ণ নিষিদ্ধ এবং `delete` শুধুমাত্র মালিক করতে পারবে।

---

## ৩–১৩. পরবর্তী ধাপের পরিকল্পিত কালেকশনসমূহ (ধাপ ১-এ বন্ধ)
- **৩. `posts/{postId}` (ধাপ ২):** `authorUid`, `authorUsername`, `authorDisplayName`, `authorPhotoUrl`, `content`, `imageUrls`, `categoryId`, `likesCount`, `commentsCount`, `bookmarksCount`, `isHidden`, `createdAt`, `updatedAt`।
- **৪. `comments/{commentId}` (ধাপ ৩):** `postId`, `authorUid`, `authorUsername`, `authorDisplayName`, `authorPhotoUrl`, `text`, `createdAt`।
- **৫. `likes/{postId}_{uid}` (ধাপ ২):** `postId`, `uid`, `createdAt`।
- **৬. `follows/{followerUid}_{followingUid}` (ধাপ ৩):** `followerUid`, `followingUid`, `createdAt`।
- **৭. `bookmarks/{uid}_{postId}` (ধাপ ৩):** `uid`, `postId`, `createdAt`।
- **৮. `notifications/{notificationId}` (ধাপ ৪):** `recipientUid`, `actorUid`, `actorUsername`, `actorDisplayName`, `type`, `targetId`, `isRead`, `createdAt`।
- **৯. `reports/{reportId}` (ধাপ ৫):** `reporterUid`, `targetType`, `targetId`, `reason`, `details`, `status`, `createdAt`।
- **১০. `blocks/{blockerUid}_{blockedUid}` (ধাপ ৫):** `blockerUid`, `blockedUid`, `createdAt`।
- **১১. `categories/{categoryId}` (ধাপ ৩/৬):** `slug`, `nameBn`, `nameEn`, `postsCount`, `isActive`।
- **১২. `auditLogs/{logId}` (ধাপ ৬):** `adminUid`, `action`, `targetType`, `targetId`, `reason`, `createdAt`।
- **১৩. `appSettings/global` (ধাপ ৬):** `maintenanceMode`, `allowNewRegistrations`, `minSupportedVersion`, `updatedAt`।
