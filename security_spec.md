# Security Specification (Phase 0: Payload-First Security TDD)

## 1. Data Invariants
1. **Strict User Ownership & Isolation**: Every document in `/users/{userId}/notes/{noteId}` and `/users/{userId}/categories/{categoryId}` belongs exclusively to `userId`. Only an authenticated user where `request.auth.uid == userId` may read (`get`, `list`), `create`, `update`, or `delete` documents in their scope.
2. **Path & Document ID Hardening**: Both `userId`, `noteId`, and `categoryId` must match `^[a-zA-Z0-9_\-]+$` and have `.size() <= 128` on single-document operations (`get`, `create`, `update`, `delete`).
3. **Immutable Metadata**: On `update`, `userId` and `createdAt` must remain strictly equal to `resource.data.userId` and `resource.data.createdAt`.
4. **Temporal Integrity**: `createdAt` and `updatedAt` must be server timestamps (`is timestamp && <= request.time`).
5. **Bounded Fields & Lists**: All strings (`title`, `content`, `category`, `colorHex`, `passwordHash`, `encryptedPayload`, `reminderLabel`) have explicit `.size()` caps, and `tags` is a bounded list (`size() <= 10`) where empty lists are permitted or first element is a string.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read/Write**: `auth = null` attempting `get` or `create` on `/users/alice/notes/note1` -> REJECTED.
2. **Cross-User Read/Write**: `auth.uid = "bob"` attempting `get`, `list`, `update`, or `delete` on `/users/alice/notes/note1` -> REJECTED.
3. **Identity Spoofing in Body**: `auth.uid = "alice"` creating `/users/alice/notes/note1` with `userId: "bob"` -> REJECTED.
4. **Shadow Field Injection on Create**: Creating a note with an undeclared field `isAdmin: true` -> REJECTED by `keys().hasOnly(...)`.
5. **Shadow Field Injection on Update**: Updating a note with an extra field `hacked: "yes"` -> REJECTED by `affectedKeys().hasOnly(...)`.
6. **Immutable `createdAt` Tampering**: Updating a note with a modified `createdAt` timestamp -> REJECTED by `incoming().createdAt == existing().createdAt`.
7. **Immutable `userId` Transfer**: Updating a note's `userId` to another user -> REJECTED by `incoming().userId == existing().userId`.
8. **Future Timestamp Injection**: Creating or updating a note with `updatedAt` in the future (`> request.time`) -> REJECTED.
9. **Oversized Title / Content (Denial of Wallet)**: Creating a note with `title.size() > 200` or `content.size() > 20000` -> REJECTED.
10. **Unbounded Tags Array**: Creating or updating a note with 15 tags (`tags.size() > 10`) -> REJECTED.
11. **Type Poisoning on Update**: Updating `isPinned` with a string `"true"` or `reminderEpochMillis` with a string -> REJECTED by `isValidNote(incoming())`.
12. **Malformed Document ID**: Creating a note with an invalid ID or oversized ID (> 128 chars) -> REJECTED by `isValidId(noteId)`.
