# Implementation Contract: [Feature / Slice Title]

## 1. User Outcome
<!-- What can the user accomplish or experience after this change? (1-2 sentences) -->

## 2. In-Scope Behavior
<!-- Specific observable capabilities and functional requirements implemented in this slice -->
- 

## 3. Explicit Exclusions
<!-- Deliberately out-of-scope capabilities, components, or refactors -->
- 

## 4. Per-Screen Design-Alignment Matrix
<!-- Define upfront design references, required layout anchors, approved contract-driven omissions, and expected evidence -->

| Screen / Journey | Mobile Reference (`pocket-budget-mobile-design/`) | Desktop Reference (`pocket-budget-desktop-design/`) | Required Layout Anchors | Approved Contract-Driven Omissions | Evidence Paths | Verdict (`Aligned` / `Approved deviation` / `Correction required`) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `[Screen Name]` | `[mockup.png]` | `[mockup.png]` | `[Structural anchors: e.g. Desktop card container, two-column form row, nav anchors]` | `[Explicitly excluded elements: e.g. Categories, budgets]` | `docs/evidence/screenshots/[mobile-*.png]`<br>`docs/evidence/screenshots/[desktop-*.png]` | `[Target Verdict]` |

## 5. Acceptance Criteria
<!-- Verifiable and testable conditions required to declare the slice done -->
- [ ] 
- [ ] 

## 6. Verification Plan
<!-- Targeted automated commands, full regression tasks, and manual checks -->
- **Focused Unit / Component Tests:**
  ```bash
  ./gradlew :shared:jvmTest --tests "<TestClass>"
  ```
- **Wider Multiplatform Verification:**
  ```bash
  ./gradlew :shared:jvmTest
  ./gradlew :shared:testAndroidHostTest
  ```
- **Manual / Visual Verification (if applicable):**
  - 

## 7. Constraints & Rules
<!-- Architecture, multiplatform boundaries, data types, and coding conventions -->
- **Source Set Scoping:** Keep cross-platform domain and state logic in `shared/src/commonMain/`.
- **Data Types:** Monetary values must use `Long` integer cents (no `Double`/`Float`). Timestamps use `kotlin.time.Instant`.
- **Dependencies:** Do not introduce external libraries or Gradle dependencies without approval.
- **Development Cycle:** Follow the test-first workflow (`.agents/workflows/test-first.md`) for observable behavior changes.

## 8. Authority Boundaries
<!-- Operational permissions for Git, code modifications, and external actions -->
- **File Modifications:** Limited strictly to in-scope files.
- **Git Commits:** Create atomic commits only when explicitly requested/approved.
- **Git Push & Remotes:** Do not push to remote repositories or alter remote branch configurations.
- **External Services:** No external network requests, database migrations, or cloud configurations without explicit sign-off.
