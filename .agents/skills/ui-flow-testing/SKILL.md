---
name: ui-flow-testing
description: >-
  Guide for writing, executing, and visually validating Compose Multiplatform UI-flow
  tests in Pocket Budget. Use to implement automated UI journey tests, semantic test
  tags, headless JVM screenshot capture, and visual validation against design references.
---

# UI-Flow Testing and Visual Validation in Pocket Budget

This skill provides guidelines and operational patterns for writing and executing Compose Multiplatform UI-flow tests and capturing visual-validation evidence in Pocket Budget.

---

## 1. Architecture & Execution Context

Pocket Budget runs Compose Multiplatform UI tests natively on the JVM host:

- **Library**: JetBrains Compose Multiplatform testing API (`androidx.compose.ui.test.*`) with `compose.uiTest` in `commonTest` and `compose.desktop.currentOs` in `jvmTest`.
- **Test Entry Point**: `@OptIn(ExperimentalTestApi::class) runComposeUiTest { setContent { ... } }`.
- **Execution Task**:
  ```bash
  # Focused UI test execution
  ./gradlew :shared:jvmTest --tests "org.hoods.pocket.presentation.transaction.<TestClass>"
  ```
- **No Device/Emulator Required**: Tests execute headlessly on the JVM host, providing fast and deterministic UI verification.

---

## 2. Test Tags Strategy

Use `Modifier.testTag(tag)` sparingly and stably:

- **Targeted Elements**:
  - Interactive triggers: FABs, primary buttons, destructive actions, cancel buttons.
  - Form input fields: amount inputs, description inputs, search fields.
  - Interactive selectors: segmented toggles, tabs.
  - Dynamic status indicators: validation error messages.
  - List item rows: uniquely identifiable by ID (e.g. `transaction_item_${item.id}`).
- **Conventions**:
  - Use `lower_snake_case` naming (e.g. `add_transaction_fab`, `amount_input`, `confirm_delete_button`).
  - Never couple test tags to volatile layout attributes or internal styling.
  - Prefer text or content description matchers (`onNodeWithText`, `onNodeWithContentDescription`) when the text is user-facing and unchanging. Use `testTag` when disambiguation, empty fields, or icons require a stable semantic anchor.

---

## 3. Standard UI-Flow Test Scenarios

Every feature UI slice must provide automated test coverage for:

1. **Valid Submission**:
   - Fill all required inputs with valid data.
   - Click submit/save.
   - Assert repository receives expected data and completion outcome triggers.
2. **Invalid Input Handling**:
   - Provide blank or boundary-violating inputs.
   - Attempt submission.
   - Assert validation error is displayed and repository write is prevented.
3. **Edit Without Duplication**:
   - Seed existing record.
   - Open edit screen and modify fields.
   - Submit changes.
   - Assert existing record is updated in place, retaining original identifier and timestamps, with no extra records inserted.
4. **Cancel Confirmation**:
   - Trigger a destructive or sensitive operation (e.g. Delete).
   - Verify confirmation dialog appears with appropriate warning.
   - Click Cancel.
   - Verify dialog dismisses and underlying data remains unchanged.
5. **Confirmed Action**:
   - Trigger confirmation dialog.
   - Click Confirm.
   - Verify data is deleted/altered and navigation returns to previous destination.
6. **Cross-Screen Navigation Journey**:
   - Host the real navigation graph (`PocketNavHost` with real or fake repositories).
   - Test full user journey traversing list -> add -> list -> edit -> list.

---

## 4. Visual Validation & Screenshot Capture

To prevent visual regressions and ensure adherence to the Pocket Budget design system:

### Build Output Isolation
Automated screenshot capture during test runs must write to build output first:
```kotlin
val outputDir = File("build/reports/visual-validation/screenshots").apply { mkdirs() }
val outputFile = File(outputDir, "$screenshotName.png")
ImageIO.write(imageBitmap.toAwtImage(), "PNG", outputFile)
```
This ensures normal test runs do not pollute tracked Git working trees.

### Visual Review Sequence
1. Inspect the captured PNG files in `build/reports/visual-validation/screenshots/`.
2. Inspect the reference mockups in `docs/design/reference/` (`pocket-budget-desktop-design/` and `pocket-budget-mobile-design/`) using `view_file`.
3. Compare typography, colors, padding, button shapes, and interactive layouts against design tokens in [`pocket-budget-design-system.md`](../../docs/design/reference/pocket-budget-design-system.md).
4. Identify and document any contract-driven deviations (e.g. categories or date pickers excluded by contract).
5. Copy only verified, approved screenshots to durable evidence (e.g. `docs/evidence/screenshots/`).

---

## 5. Truthful Command & Status Reporting

- Record all commands actually executed with their exact outputs.
- Any platform or task that cannot run in the active host environment (e.g. `iosSimulatorArm64Test` on x86_64 Mac hosts) must be explicitly recorded as `NOT RUN` with the underlying technical reason.
