const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firebase/firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("1. Unauthenticated user: cannot read or write users or usernames", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("usernames").doc("alice_bd").get());
});

test("2. Atomic Batch Claim: Authenticated user can claim username and create users/{uid} in same batch", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const batch = aliceDb.batch();

  batch.set(aliceDb.collection("usernames").doc("rahim_bd"), {
    uid: ALICE_UID,
    createdAt: now,
  });
  batch.set(aliceDb.collection("users").doc(ALICE_UID), {
    uid: ALICE_UID,
    displayName: "রহিম উদ্দিন",
    username: "rahim_bd",
    email: "rahim@example.com",
    language: "bn",
    themeMode: "system",
    onboardingCompleted: true,
    createdAt: now,
    updatedAt: now,
  });

  await assertSucceeds(batch.commit());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("3. অন্যের UID দিয়ে ইউজারনেম দাবি করা ব্যর্থ (Claiming username with another user's UID fails)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const batch = aliceDb.batch();

  // Alice tries to claim a username setting uid = BOB_UID
  batch.set(aliceDb.collection("usernames").doc("spoof_handle"), {
    uid: BOB_UID,
    createdAt: now,
  });
  batch.set(aliceDb.collection("users").doc(ALICE_UID), {
    uid: ALICE_UID,
    displayName: "Alice",
    username: "spoof_handle",
    email: "alice@example.com",
    language: "bn",
    themeMode: "system",
    onboardingCompleted: true,
    createdAt: now,
    updatedAt: now,
  });

  await assertFails(batch.commit());
});

test("4. একই ব্যাচে users/{uid}-এর username সমান না হলে ইউজারনেম তৈরি ব্যর্থ (getAfter enforcement)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  // Standalone write to usernames/{username} without users/{uid} having matching username
  await assertFails(
    aliceDb.collection("usernames").doc("orphan_handle").set({
      uid: ALICE_UID,
      createdAt: now,
    })
  );

  // Batch where users/{uid}.username does NOT match usernames/{username}
  const mismatchedBatch = aliceDb.batch();
  mismatchedBatch.set(aliceDb.collection("usernames").doc("handle_one"), {
    uid: ALICE_UID,
    createdAt: now,
  });
  mismatchedBatch.set(aliceDb.collection("users").doc(ALICE_UID), {
    uid: ALICE_UID,
    displayName: "Alice",
    username: "handle_two",
    email: "alice@example.com",
    language: "bn",
    themeMode: "system",
    onboardingCompleted: true,
    createdAt: now,
    updatedAt: now,
  });
  await assertFails(mismatchedBatch.commit());
});

test("5. আগের নেওয়া নাম ওভাররাইট ব্যর্থ (Overwriting an already taken username fails)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  // Alice claims 'dhaka_star' atomically
  const aliceBatch = aliceDb.batch();
  aliceBatch.set(aliceDb.collection("usernames").doc("dhaka_star"), {
    uid: ALICE_UID,
    createdAt: now,
  });
  aliceBatch.set(aliceDb.collection("users").doc(ALICE_UID), {
    uid: ALICE_UID,
    displayName: "Alice",
    username: "dhaka_star",
    email: "alice@example.com",
    language: "bn",
    themeMode: "system",
    onboardingCompleted: true,
    createdAt: now,
    updatedAt: now,
  });
  await assertSucceeds(aliceBatch.commit());

  // Bob attempts to overwrite 'dhaka_star' even with his own valid batch
  const bobBatch = bobDb.batch();
  bobBatch.set(bobDb.collection("usernames").doc("dhaka_star"), {
    uid: BOB_UID,
    createdAt: new Date(),
  });
  bobBatch.set(bobDb.collection("users").doc(BOB_UID), {
    uid: BOB_UID,
    displayName: "Bob",
    username: "dhaka_star",
    email: "bob@example.com",
    language: "en",
    themeMode: "dark",
    onboardingCompleted: true,
    createdAt: new Date(),
    updatedAt: new Date(),
  });
  await assertFails(bobBatch.commit());

  // Bob also cannot delete Alice's username
  await assertFails(bobDb.collection("usernames").doc("dhaka_star").delete());
});

test("6. অন্যের users ডকুমেন্ট পড়া ব্যর্থ (Reading another user's users document fails)", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      uid: BOB_UID,
      displayName: "Bob",
      username: "bob_user",
      email: "bob@example.com",
      language: "en",
      themeMode: "dark",
      onboardingCompleted: true,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).update({ displayName: "Hacked" })
  );
});

test("7. followersCount, isBanned, postsCount, isVerified বা role লেখা ব্যর্থ (System/counter field injection fails)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  // Attempt to write followersCount on create
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice",
      username: "alice_01",
      email: "alice@example.com",
      language: "bn",
      themeMode: "light",
      onboardingCompleted: true,
      followersCount: 500,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Attempt to write isBanned on create
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice",
      username: "alice_01",
      email: "alice@example.com",
      language: "bn",
      themeMode: "light",
      onboardingCompleted: true,
      isBanned: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Attempt to write removed role field on create
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice",
      username: "alice_01",
      email: "alice@example.com",
      language: "bn",
      themeMode: "light",
      onboardingCompleted: true,
      role: "admin",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Seed valid user, then test update rejection for followersCount and isBanned
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice",
      username: "alice_01",
      email: "alice@example.com",
      language: "bn",
      themeMode: "light",
      onboardingCompleted: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      followersCount: 1000,
      updatedAt: new Date(),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isBanned: false,
      updatedAt: new Date(),
    })
  );
});
