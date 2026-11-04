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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validNotePayload(userId = ALICE_UID, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    userId,
    title: "Weekly Sprint Notes",
    content: "Ship cloud sync and voice-to-text.",
    folderId: "folder_work",
    category: "Work",
    tags: ["sprint", "android"],
    colorHex: "#FDE68A",
    isPinned: true,
    isArchived: false,
    isTrashed: false,
    trashedAtEpochMillis: 0,
    isLocked: false,
    passwordHash: "",
    encryptedPayload: "",
    reminderEpochMillis: 0,
    reminderLabel: "",
    versionHistory: [],
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validFolderPayload(userId = ALICE_UID, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    userId,
    name: "Engineering Projects",
    parentFolderId: "",
    colorHex: "#F59E0B",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
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

test("1. Unauthenticated user cannot read or create notes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").set(validNotePayload())
  );
});

test("2. Authenticated user can create, archive, trash, version-track, and delete their own note", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const noteRef = aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1");

  await assertSucceeds(noteRef.set(validNotePayload(ALICE_UID)));
  await assertSucceeds(noteRef.get());
  await assertSucceeds(
    noteRef.update({
      title: "Updated Sprint Notes",
      folderId: "folder_archive",
      isPinned: false,
      isArchived: true,
      isTrashed: true,
      trashedAtEpochMillis: 1720000000000,
      versionHistory: ['{"savedAtMillis":1719999900000,"title":"Weekly Sprint Notes","content":"Old body","category":"Work"}'],
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(noteRef.delete());
});

test("3. Authenticated user cannot read, update, or delete another user's note", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("notes")
      .doc("bob_note")
      .set(validNotePayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobNoteRef = aliceDb.collection("users").doc(BOB_UID).collection("notes").doc("bob_note");

  await assertFails(bobNoteRef.get());
  await assertFails(bobNoteRef.update({ title: "Hacked" }));
  await assertFails(bobNoteRef.delete());
});

test("4. Shadow update with undeclared ghost field is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const noteRef = aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1");

  await assertFails(
    noteRef.set(validNotePayload(ALICE_UID, { isVerifiedAdmin: true }))
  );

  await assertSucceeds(noteRef.set(validNotePayload(ALICE_UID)));
  await assertFails(
    noteRef.update({
      title: "Valid Title",
      ghostField: "malicious",
    })
  );
});

test("5. Immutable createdAt and userId cannot be modified on update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const noteRef = aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1");
  await assertSucceeds(noteRef.set(validNotePayload(ALICE_UID)));

  await assertFails(
    noteRef.update({
      createdAt: new Date(),
    })
  );
  await assertFails(
    noteRef.update({
      userId: BOB_UID,
    })
  );
});

test("6. Client query alignment: list query with matching userId filter succeeds, without filter fails", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("notes")
      .doc("alice_note")
      .set(validNotePayload(ALICE_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("notes")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").get()
  );
});

test("7. Value poisoning (oversized strings or >10 tags or >10 versionHistory entries) is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const noteRef = aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_poison");

  await assertFails(
    noteRef.set(validNotePayload(ALICE_UID, { title: "A".repeat(250) }))
  );
  await assertFails(
    noteRef.set(
      validNotePayload(ALICE_UID, {
        tags: ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"],
      })
    )
  );
  await assertFails(
    noteRef.set(
      validNotePayload(ALICE_UID, {
        versionHistory: ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"],
      })
    )
  );
});

test("8. Folder CRUD, rename, move, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const folderRef = aliceDb.collection("users").doc(ALICE_UID).collection("folders").doc("folder_1");

  await assertSucceeds(folderRef.set(validFolderPayload(ALICE_UID)));
  await assertSucceeds(folderRef.get());
  await assertSucceeds(
    folderRef.update({
      name: "Renamed Engineering",
      parentFolderId: "parent_folder_root",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("folders").doc("folder_1").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("folders").doc("folder_1").delete()
  );

  await assertSucceeds(folderRef.delete());
});
