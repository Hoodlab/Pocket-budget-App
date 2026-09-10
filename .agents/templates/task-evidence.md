# Task Evidence: [Task / Feature Slice Name]

## 1. Outcome
<!-- Summary of user outcome and capability delivered (1-2 sentences) -->

## 2. Scope
### In-Scope Deliverables
<!-- Key components, use cases, domain logic, and UI elements completed -->
- 

### Explicit Exclusions
<!-- Related features, fields, or abstractions intentionally omitted per contract/rules -->
- 

## 3. Test-First Evidence (RED & GREEN)
### RED Phase (Failing Test)
- **Failing Test**: `[Package.TestClassName.testMethodName]`
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest --tests "[Package.TestClassName]"
  ```
- **Observed Failure Output**:
  ```text
  <!-- Paste failure output (AssertionError or missing reference before implementation) -->
  ```

### GREEN Phase (Passing Test)
- **Implementation File**: `[path/to/ImplementationFile.kt]`
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest --tests "[Package.TestClassName]"
  ```
- **Observed Passing Output**:
  ```text
  <!-- Paste passing test output -->
  BUILD SUCCESSFUL
  ```

## 4. Automated UI-Flow Test Coverage

| Scenario | Test Class & Method | Result | Description |
| :--- | :--- | :--- | :--- |
| **Valid Add** | `[AddTransactionUiTest.validAdd_...()]` | PASS | Valid input saves to repository and triggers completion. |
| **Invalid Input** | `[AddTransactionUiTest.invalidInput_...()]` | PASS | Blank/zero inputs show validation errors without saving. |
| **Edit Without Duplication** | `[EditTransactionUiTest.editWithoutDuplication_...()]` | PASS | Editing existing record updates in place without inserting duplicates. |
| **Cancel Deletion** | `[EditTransactionUiTest.cancelDeletion_...()]` | PASS | Cancelling delete confirmation leaves record intact in repository. |
| **Confirmed Deletion** | `[EditTransactionUiTest.confirmedDeletion_...()]` | PASS | Confirming delete permanently removes record from repository. |
| **Cross-Screen Journey** | `[TransactionJourneyUiTest.fullJourney_...()]` | PASS | End-to-end multi-screen flow across List -> Add -> Edit -> Delete. |

### UI-Flow Execution Commands & Logs
```bash
./gradlew :shared:jvmTest --tests "org.hoods.pocket.presentation.transaction.add.AddTransactionUiTest"
./gradlew :shared:jvmTest --tests "org.hoods.pocket.presentation.transaction.edit.EditTransactionUiTest"
./gradlew :shared:jvmTest --tests "org.hoods.pocket.presentation.transaction.TransactionJourneyUiTest"
```

## 5. Visual-Validation Evidence

<!-- When both mobile and desktop references exist, inspect both before implementation and capture, review, and record evidence for both supported viewport sizes. If one cannot run, record it as NOT RUN with the reason. -->

### Captured Artifacts (from Build Output to Durable Evidence)
- **Build Output Directory**: `build/reports/visual-validation/screenshots/`
- **Approved Durable Evidence**: `docs/evidence/screenshots/`
  - Mobile:
    - `[screen-mobile-1.png]`
  - Desktop:
    - `[screen-desktop-1.png]`

### Per-Screen Design-Alignment Matrix
<!-- Do not describe a screen as aligned only because its colours and tokens match. Real alignment requires structural composition, responsive layout anchors, and component hierarchy to match both references. -->

| Screen / Journey | Mobile Reference (`pocket-budget-mobile-design/`) | Desktop Reference (`pocket-budget-desktop-design/`) | Required Layout Anchors | Approved Contract-Driven Omissions | Evidence Paths | Verdict (`Aligned` / `Approved deviation` / `Correction required`) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `[Screen Name]` | `[mockup.png]` | `[mockup.png]` | `[e.g. Left nav rail, card container, two-column row, header actions]` | `[e.g. Category grid, budgets]` | `docs/evidence/screenshots/[mobile-*.png]`<br>`docs/evidence/screenshots/[desktop-*.png]` | `[Aligned / Approved deviation / Correction required]` |

### Visual Comparison Against Design References

#### Mobile Viewport (e.g. 390x844 dp)
| Captured Screen | Reference Mockup (`docs/design/reference/pocket-budget-mobile-design/`) | Visual Review Summary |
| :--- | :--- | :--- |
| `[screen-mobile-1.png]` | `[pocket-budget-mobile-design/...png]` | Token adherence, typography scale, mobile card layout, and spacing verified. |

#### Desktop Viewport (e.g. 1024x768 dp)
| Captured Screen | Reference Mockup (`docs/design/reference/pocket-budget-desktop-design/`) | Visual Review Summary |
| :--- | :--- | :--- |
| `[screen-desktop-1.png]` | `[pocket-budget-desktop-design/...png]` | Token adherence, widescreen layout arrangement, hierarchy, and desktop responsiveness verified. |

*(If a viewport cannot be captured or tested, record it as `NOT RUN` with the specific reason)*

### Design Token Alignment
- **Colors**: `PocketColors.Primary` (`#005136`), `Surface` (`#EEFDF2`), `Container` (`#FFFFFF`), `Income` (`#059669`), `Expense` (`#BA1A1A`).
- **Typography**: Display/Headline semi-bold, body regular, hero numeric displays matching Work Sans scales.
- **Shapes**: `16.dp` rounded cards/inputs, capsule full-radius buttons and segmented toggles.
- **Spacing**: 8pt grid, 72dp item heights with 40dp badges.

### Contract-Driven Deviations
- **[Component Name]**: [e.g. Category selector visible in mockup omitted per contract Section 3].
- **[Component Name]**: [e.g. Date picker visible in mockup omitted per contract Section 3; read-only timestamp indicator used].

## 6. Verification Results
- **Verification Gate Command**:
  ```bash
  ./scripts/verify.sh
  ```
- **Gate Summary**:
  ```text
  Git diff check (whitespace/formatting) : PASS
  Shared JVM Unit Tests (:shared:jvmTest) : PASS
  Android Host Unit Tests (:shared:testAndroidHostTest) : PASS
  Desktop App Compilation (:desktopApp:compileKotlin) : PASS
  Android App Packaging (:androidApp:assembleDebug) : PASS
  iOS Simulator Tests (:shared:iosSimulatorArm64Test) : NOT RUN (Target requires ARM64, host is macOS x86_64)
  ```

## 7. Execution Command Log
<!-- Complete log of every command actually run during verification -->
- `git diff --check`: `PASS`
- `./gradlew :shared:jvmTest`: `PASS`
- `./gradlew :shared:testAndroidHostTest`: `PASS`
- `./gradlew :desktopApp:compileKotlin`: `PASS`
- `./gradlew :androidApp:assembleDebug`: `PASS`
- `./gradlew :shared:iosSimulatorArm64Test`: `NOT RUN` (host platform is macOS x86_64, whereas the iOS simulator target requires ARM64).

## 8. Limitations & Unverified Targets
<!-- Truthful statement of environment limitations, disabled targets, or manual test constraints -->
- **iOS Simulator Tests**: `NOT RUN` (host platform is macOS x86_64, whereas the iOS simulator target requires ARM64).

## 9. Final Checkpoint
- **Commit Hash**: `[e.g. 7ff5008]`
- **Commit Message**:
  ```text
  [type(scope): summary]

  - bullet point detail
  ```
- **Final Git Status**:
  ```text
  On branch main
  nothing to commit, working tree clean
  ```
