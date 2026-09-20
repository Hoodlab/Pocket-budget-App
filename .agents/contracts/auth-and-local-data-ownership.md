# Implementation Contract: Account & Local Data Ownership

## 1. User Outcome

- A user can use Pocket Budget completely offline without creating an account.
- A user can sign up and sign in using email and password via Supabase Auth.
- On first sign-in on a device with existing data, the user is presented with a clear choice:
  1. **Claim for Account:** Atomically associate existing device data with the signed-in account.
  2. **Keep on Device:** Leave existing data unassigned and local to this device.
- A user can sign out, returning the app to unauthenticated mode where account records are hidden and unclaimed device records are accessible.
- No account can ever see another account's claimed local records.
- Remote configuration is explicit and does not silently fall back to `localhost`.
- Email confirmation is handled truthfully without assuming immediate session creation.

## 2. In-Scope Behavior

- **Multiplatform Dependencies:** Add `supabase-bom`, `supabase-auth`, `ktor-client-core`, `ktor-client-android` (Android), `ktor-client-cio` (JVM Desktop), and `ktor-client-darwin` (iOS).
- **Explicit Configuration:** `SupabaseConfig` and `SetupConfigScreen` handling missing credentials without silent localhost fallback.
- **Authentication:** Email & password sign-up, sign-in, and sign-out against Supabase GoTrue.
- **Truthful Confirmation:** Distinct handling for immediate session vs confirmation-required responses without mutating Supabase Auth settings.
- **Auth State:** Observable `Flow<AuthState>` with `Unauthenticated` and `Authenticated(user)` states.
- **Local Data Tagging:** Nullable `userId` column in Room entities (`transactions`, `monthly_budgets`, `category_budgets`) via `MIGRATION_4_5`.
- **Local Isolation & Query Push-Down:** Room queries filter by active user ID (`WHERE userId = :activeUserId` when signed in; `WHERE userId IS NULL` when signed out).
- **Claim Prompt & Flow:** Screen/dialog triggered upon sign-in if unassigned records exist (`userId IS NULL`).
- **Atomic Data Association:** `ClaimLocalDataUseCase` executes an atomic Room transaction associating unassigned records with the signed-in `userId`.
- **Navigation:** Type-safe routes `Screen.Auth`, `Screen.Account`, `Screen.ClaimData`, and `Screen.SetupConfig`.
- **Automated Tests:** Room `MIGRATION_4_5` migration test, multi-account visibility DAO tests, Auth ViewModels unit tests, and Compose UI journey tests.

## 3. Explicit Exclusions

- Automatic remote database synchronisation (sync engine, outbox, conflict handling) — deferred to sync contract.
- Social OAuth logins (Google, Apple, GitHub).
- Changing hosted or local Supabase Auth settings or email templates.
- Password reset and email recovery flows.
- Multi-device cross-network syncing.

## 4. Acceptance Criteria

- [ ] Gradle version catalog and build files include Supabase Auth and target-specific Ktor client engines (`android`, `cio`, `darwin`).
- [ ] Runtime configuration does not silently fall back to `localhost`; displays `SetupConfigScreen` when unconfigured.
- [ ] Room version is bumped to 5 with `MIGRATION_4_5` adding `userId TEXT DEFAULT NULL` to all three tables without data loss.
- [ ] An unauthenticated user can create, view, edit, and delete transactions and budgets offline.
- [ ] When unauthenticated, only records with `userId IS NULL` are visible in the app.
- [ ] On first sign-in with existing local records, the user is prompted to claim or keep local data.
- [ ] Choosing "Claim for account" atomically updates all local tables with the signed-in `userId`.
- [ ] Choosing "Keep on Device" leaves records with `userId = NULL`.
- [ ] A signed-in user sees only records matching their `userId`. Other accounts' records are strictly hidden.
- [ ] Signing out returns the app to unauthenticated mode; claimed account records are hidden and unclaimed device records reappear.
- [ ] Email confirmation-required responses are reported truthfully without establishing a signed-in session.
- [ ] Automated tests cover Room migration, multi-account visibility isolation, auth view models, and claim flows.
- [ ] Verification gate passes cleanly.

## 5. Verification Plan

- **Room Migration & Scoping Tests:**
  ```bash
  ./gradlew :shared:jvmTest --tests "org.hoods.pocket.data.local.database.RoomMigrationTest"
  ```
- **Local Multi-Account Isolation Tests:**
  ```bash
  ./gradlew :shared:jvmTest --tests "org.hoods.pocket.data.RoomDataOwnershipTest"
  ```
- **Auth & Claim ViewModel Tests:**
  ```bash
  ./gradlew :shared:jvmTest --tests "org.hoods.pocket.presentation.auth.*"
  ```
- **Full Verification Gate:**
  ```bash
  ./scripts/verify.sh
  ```

## 6. Constraints & Rules

- **Dependencies:** Use `supabase-auth:3.1.4`, `ktor:3.1.1` (`ktor-client-android` on Android, `ktor-client-cio` on Desktop JVM, `ktor-client-darwin` on iOS).
- **Architecture Boundaries:** Domain models and use cases must not depend on Room, Compose, or Supabase SDK classes. ViewModels depend solely on use cases.
- **Screen Quad Decomposition:** Each new screen implements `<Feature>ViewModel`, `<Feature>UiState`, `<Feature>Screen`, and `<Feature>Content`.
- **Zero Secrets:** Never hardcode passwords, tokens, or service-role keys in source files or Git state.
- **Auth Settings:** Do not alter local or hosted Supabase Auth settings.

## 7. Authority Boundaries

- **File Modifications:** Limited to `gradle/libs.versions.toml`, `shared/build.gradle.kts`, `shared/src/`, and `.agents/`.
- **Git Commits:** Create atomic checkpoint commit only after explicit user approval.
- **External Services:** Do not mutate remote tables or hosted settings.
