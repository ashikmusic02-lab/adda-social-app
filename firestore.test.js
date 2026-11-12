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
const ALICE_EMAIL = "alice@example.com";
const BOB_UID = "bob_456";
const BOB_EMAIL = "bob@example.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function getAliceDb() {
  return testEnv
    .authenticatedContext(ALICE_UID, { email: ALICE_EMAIL })
    .firestore();
}

function getBobDb() {
  return testEnv
    .authenticatedContext(BOB_UID, { email: BOB_EMAIL })
    .firestore();
}

async function seedUserWithUsername(db, uid, email, username, displayName = "User") {
  const now = new Date();
  const batch = db.batch();
  batch.set(db.collection("usernames").doc(username), {
    uid,
    createdAt: now,
  });
  batch.set(db.collection("users").doc(uid), {
    uid,
    displayName,
    username,
    email,
    language: "bn",
    themeMode: "system",
    onboardingCompleted: true,
    createdAt: now,
    updatedAt: now,
  });
  await assertSucceeds(batch.commit());
}

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

test("2. Atomic Batch Claim: Authenticated user can claim username and create users/{uid} in same batch (get allowed, list denied)", async () => {
  const aliceDb = getAliceDb();
  await seedUserWithUsername(aliceDb, ALICE_UID, ALICE_EMAIL, "rahim_bd", "রহিম উদ্দিন");

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  // allow list is removed on users collection
  await assertFails(aliceDb.collection("users").get());
});

test("3. অন্যের UID দিয়ে ইউজারনেম দাবি করা ব্যর্থ (Claiming username with another user's UID fails)", async () => {
  const aliceDb = getAliceDb();
  const now = new Date();
  const batch = aliceDb.batch();

  batch.set(aliceDb.collection("usernames").doc("spoof_handle"), {
    uid: BOB_UID,
    createdAt: now,
  });
  batch.set(aliceDb.collection("users").doc(ALICE_UID), {
    uid: ALICE_UID,
    displayName: "Alice",
    username: "spoof_handle",
    email: ALICE_EMAIL,
    language: "bn",
    themeMode: "system",
    onboardingCompleted: true,
    createdAt: now,
    updatedAt: now,
  });

  await assertFails(batch.commit());
});

test("4. আগের নেওয়া নাম ওভাররাইট ব্যর্থ (Overwriting an already taken username fails)", async () => {
  const aliceDb = getAliceDb();
  const bobDb = getBobDb();

  await seedUserWithUsername(aliceDb, ALICE_UID, ALICE_EMAIL, "dhaka_star", "Alice");

  // Bob attempts to overwrite 'dhaka_star' with his own batch
  const bobBatch = bobDb.batch();
  bobBatch.set(bobDb.collection("usernames").doc("dhaka_star"), {
    uid: BOB_UID,
    createdAt: new Date(),
  });
  bobBatch.set(bobDb.collection("users").doc(BOB_UID), {
    uid: BOB_UID,
    displayName: "Bob",
    username: "dhaka_star",
    email: BOB_EMAIL,
    language: "en",
    themeMode: "dark",
    onboardingCompleted: true,
    createdAt: new Date(),
    updatedAt: new Date(),
  });
  await assertFails(bobBatch.commit());
});

test("5. অন্যের users ডকুমেন্ট পড়া ব্যর্থ ও সিস্টেম ফিল্ড (followersCount, isBanned, role) লেখা ব্যর্থ", async () => {
  const aliceDb = getAliceDb();
  const bobDb = getBobDb();

  await seedUserWithUsername(bobDb, BOB_UID, BOB_EMAIL, "bob_user", "Bob");

  // Alice cannot read or update Bob's profile
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).update({ displayName: "Hacked" })
  );

  // Alice cannot inject followersCount, isBanned, or role during create
  const now = new Date();
  for (const forbiddenField of [
    { followersCount: 500 },
    { isBanned: false },
    { role: "admin" },
  ]) {
    const b = aliceDb.batch();
    b.set(aliceDb.collection("usernames").doc("alice_sys"), {
      uid: ALICE_UID,
      createdAt: now,
    });
    b.set(aliceDb.collection("users").doc(ALICE_UID), {
      uid: ALICE_UID,
      displayName: "Alice",
      username: "alice_sys",
      email: ALICE_EMAIL,
      language: "bn",
      themeMode: "light",
      onboardingCompleted: true,
      createdAt: now,
      updatedAt: now,
      ...forbiddenField,
    });
    await assertFails(b.commit());
  }
});

test("6. (ক) অন্যের ক্লেইম করা নাম প্রোফাইলে বসানো ব্যর্থ (Setting someone else's claimed username in profile fails on create and update)", async () => {
  const aliceDb = getAliceDb();
  const bobDb = getBobDb();
  const now = new Date();

  // Alice claims 'alice_official_1'
  await seedUserWithUsername(aliceDb, ALICE_UID, ALICE_EMAIL, "alice_official_1", "Alice");

  // Case A: Bob tries to create his users/{BOB_UID} doc pointing to Alice's claimed username 'alice_official_1' without owning it
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      uid: BOB_UID,
      displayName: "Bob",
      username: "alice_official_1",
      email: BOB_EMAIL,
      language: "bn",
      themeMode: "system",
      onboardingCompleted: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Case B: Bob creates a valid profile with 'bob_initial', then tries to update his username to Alice's 'alice_official_1'
  await seedUserWithUsername(bobDb, BOB_UID, BOB_EMAIL, "bob_initial", "Bob");
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).update({
      username: "alice_official_1",
      updatedAt: new Date(),
    })
  );
});

test("7. (খ) ক্লেইম ছাড়া নাম বদলানো ব্যর্থ (Creating or changing username in users without creating usernames claim in same batch fails)", async () => {
  const aliceDb = getAliceDb();
  const now = new Date();

  // Creating users doc alone without usernames/{username} claim in the same batch fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice",
      username: "unclaimed_name",
      email: ALICE_EMAIL,
      language: "bn",
      themeMode: "system",
      onboardingCompleted: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Seed valid user with 'alice_old'
  await seedUserWithUsername(aliceDb, ALICE_UID, ALICE_EMAIL, "alice_old", "Alice");

  // Updating users/{ALICE_UID}.username to 'alice_new' without creating usernames/alice_new claim fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      username: "alice_new",
      updatedAt: new Date(),
    })
  );

  // Even if old claim is deleted in the batch, changing username without creating new claim fails
  const batchWithoutNewClaim = aliceDb.batch();
  batchWithoutNewClaim.delete(aliceDb.collection("usernames").doc("alice_old"));
  batchWithoutNewClaim.update(aliceDb.collection("users").doc(ALICE_UID), {
    username: "alice_new",
    updatedAt: new Date(),
  });
  await assertFails(batchWithoutNewClaim.commit());
});

test("8. (গ) পুরোনো ক্লেইম না মুছে নাম বদলানো ব্যর্থ (Changing username without deleting old claim in same batch fails)", async () => {
  const aliceDb = getAliceDb();

  // Seed valid user with 'alice_first'
  await seedUserWithUsername(aliceDb, ALICE_UID, ALICE_EMAIL, "alice_first", "Alice");

  // Alice tries to claim 'alice_second' and update users/{ALICE_UID}.username to 'alice_second'
  // WITHOUT deleting 'usernames/alice_first' in the same batch (trying to hoard two usernames)
  const hoardBatch = aliceDb.batch();
  hoardBatch.set(aliceDb.collection("usernames").doc("alice_second"), {
    uid: ALICE_UID,
    createdAt: new Date(),
  });
  hoardBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    username: "alice_second",
    updatedAt: new Date(),
  });

  await assertFails(hoardBatch.commit());
});

test("9. (ঘ) প্রোফাইলে নাম রেখে ক্লেইম মোছা ব্যর্থ (Deleting username claim while keeping that username in users profile fails)", async () => {
  const aliceDb = getAliceDb();

  // Seed valid user with 'alice_keep'
  await seedUserWithUsername(aliceDb, ALICE_UID, ALICE_EMAIL, "alice_keep", "Alice");

  // 1. Standalone delete of usernames/alice_keep while users/{ALICE_UID}.username is still 'alice_keep' fails
  await assertFails(
    aliceDb.collection("usernames").doc("alice_keep").delete()
  );

  // 2. Batch that deletes usernames/alice_keep and updates displayName (keeping username == 'alice_keep') also fails
  const fakeBatch = aliceDb.batch();
  fakeBatch.delete(aliceDb.collection("usernames").doc("alice_keep"));
  fakeBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    displayName: "Alice Updated",
    updatedAt: new Date(),
  });
  await assertFails(fakeBatch.commit());
});

test("10. (ঙ) 'admin' ও অন্যান্য সংরক্ষিত নাম ক্লেইম ব্যর্থ (Claiming reserved usernames like 'admin' fails)", async () => {
  const aliceDb = getAliceDb();
  const reservedList = [
    "admin",
    "administrator",
    "support",
    "moderator",
    "mod",
    "adda",
    "official",
    "staff",
    "help",
    "system",
    "root",
    "null",
    "undefined",
  ];

  for (const reserved of reservedList) {
    const now = new Date();
    const batch = aliceDb.batch();
    batch.set(aliceDb.collection("usernames").doc(reserved), {
      uid: ALICE_UID,
      createdAt: now,
    });
    batch.set(aliceDb.collection("users").doc(ALICE_UID), {
      uid: ALICE_UID,
      displayName: "Alice",
      username: reserved,
      email: ALICE_EMAIL,
      language: "bn",
      themeMode: "system",
      onboardingCompleted: true,
      createdAt: now,
      updatedAt: now,
    });
    await assertFails(batch.commit());
  }
});

test("11. (চ) সঠিক নাম বদলানো সফল (Valid atomic username change in a single batch succeeds, and non-username profile update succeeds)", async () => {
  const aliceDb = getAliceDb();

  // 1. Initial onboarding with 'alice_v1'
  await seedUserWithUsername(aliceDb, ALICE_UID, ALICE_EMAIL, "alice_v1", "Alice");

  // 2. Updating displayName/bio without changing username succeeds without extra batch operations
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Updated",
      bio: "বাংলায় আড্ডা",
      updatedAt: new Date(),
    })
  );

  // 3. Atomic username change: create 'usernames/alice_v2', delete 'usernames/alice_v1', update 'users/{ALICE_UID}.username'
  const swapBatch = aliceDb.batch();
  swapBatch.set(aliceDb.collection("usernames").doc("alice_v2"), {
    uid: ALICE_UID,
    createdAt: new Date(),
  });
  swapBatch.delete(aliceDb.collection("usernames").doc("alice_v1"));
  swapBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    username: "alice_v2",
    updatedAt: new Date(),
  });
  await assertSucceeds(swapBatch.commit());

  // Verify old username is released and new username is active
  const oldSnap = await aliceDb.collection("usernames").doc("alice_v1").get();
  const newSnap = await aliceDb.collection("usernames").doc("alice_v2").get();
  const userSnap = await aliceDb.collection("users").doc(ALICE_UID).get();
  if (oldSnap.exists) throw new Error("Expected old username claim to be deleted");
  if (!newSnap.exists || newSnap.data().uid !== ALICE_UID) {
    throw new Error("Expected new username claim to exist for Alice");
  }
  if (userSnap.data().username !== "alice_v2") {
    throw new Error("Expected user profile username to be updated to alice_v2");
  }
});

test("12. (ছ) ভুয়া email ব্যর্থ (Creating or updating user profile with email != request.auth.token.email fails)", async () => {
  const aliceDb = getAliceDb();
  const now = new Date();

  // 1. Create with fake email ("fake_spoof@example.com" != "alice@example.com") fails
  const spoofEmailBatch = aliceDb.batch();
  spoofEmailBatch.set(aliceDb.collection("usernames").doc("alice_spoof_mail"), {
    uid: ALICE_UID,
    createdAt: now,
  });
  spoofEmailBatch.set(aliceDb.collection("users").doc(ALICE_UID), {
    uid: ALICE_UID,
    displayName: "Alice",
    username: "alice_spoof_mail",
    email: "fake_spoof@example.com",
    language: "bn",
    themeMode: "system",
    onboardingCompleted: true,
    createdAt: now,
    updatedAt: now,
  });
  await assertFails(spoofEmailBatch.commit());

  // 2. Seed valid user with real email, then attempt to update email to another address -> fails
  await seedUserWithUsername(aliceDb, ALICE_UID, ALICE_EMAIL, "alice_real_mail", "Alice");
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      email: "changed_email@example.com",
      updatedAt: new Date(),
    })
  );
});
