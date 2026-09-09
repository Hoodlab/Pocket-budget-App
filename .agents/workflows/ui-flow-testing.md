---
description: Use when building, testing, or modifying user interfaces in Pocket Budget to enforce automated Compose UI-flow coverage and visual validation.
---

# UI-Flow Testing and Visual Validation

Use this workflow for UI screens and user interaction flows in Pocket Budget.

## 1. Inspect Scope & Design References

Before writing tests or modifying UI:

- Read the active implementation contract to confirm which interactive behaviors are in-scope and which are explicitly excluded.
- Inspect the design system tokens in [`docs/design/reference/pocket-budget-design-system.md`](../../docs/design/reference/pocket-budget-design-system.md).
- When both mobile and desktop references exist, inspect both before implementation in [`docs/design/reference/pocket-budget-mobile-design/`](../../docs/design/reference/pocket-budget-mobile-design/) and [`docs/design/reference/pocket-budget-desktop-design/`](../../docs/design/reference/pocket-budget-desktop-design/) using `view_file`.

## 2. Apply Stable Test Tags

- Add semantic test tags (`Modifier.testTag(...)`) only where automated tests require targeted interaction or assertions.
- Do not add tags for decorative or non-interactive elements unless testing specific state changes.
- Use clean, descriptive, lower_snake_case tag names (e.g. `add_transaction_fab`, `amount_input`, `description_input`, `save_transaction_button`, `delete_transaction_button`, `confirm_delete_button`, `cancel_delete_button`, `error_message`).

## 3. Implement Automated UI-Flow Coverage

Cover all essential user scenarios with automated tests using `runComposeUiTest`:

- **Valid Submission**: User enters valid input, submits, and observes successful completion/navigation.
- **Validation & Error Handling**: Invalid or blank inputs show validation errors without navigating away or saving corrupt data.
- **Update / Edit Semantics**: Editing updates existing records in place without duplicating items.
- **Cancel Flow**: Confirmations (such as deletion dialogs) allow safe cancellation without altering data.
- **Confirmed Action**: Confirming performs the destructive or final action and leaves state clean.
- **Cross-Screen Navigation Journey**: Full end-to-end multi-screen flow across navigation destinations.

## 4. Run Automated UI Tests

Execute focused UI tests:
```bash
./gradlew :shared:jvmTest --tests "<Package>.<TestClass>"
```

Confirm all assertions pass cleanly on the JVM host before proceeding.

## 5. Visual Validation & Design Comparison

Capture visual evidence and review against design references:

1. **Dual-Viewport Screenshot Capture to Build Directory**:
   - When both mobile and desktop references exist, capture, review, and record evidence for both supported viewport sizes (e.g., mobile at 390x844 dp and desktop at 1024x768 dp). If one cannot run, record it as NOT RUN with the reason.
   - Automated screenshot capture must output images to `build/reports/visual-validation/screenshots/` first so normal test runs do not dirty tracked files.
2. **Side-by-Side Visual Review**:
   - Compare captured screens against the corresponding PNG mockups in `docs/design/reference/` across both mobile and desktop viewports.
   - Verify design token adherence (colors, typography, shapes, spacing).
   - Document any intentional contract-driven deviations (e.g. excluded date pickers or category selectors).
3. **Durable Evidence Selection**:
   - After visual review, copy only the approved screenshots to durable evidence (e.g. `docs/evidence/screenshots/`).

## 6. Verification Gate & Truthful Reporting

Run the full project verification gate:
```bash
./scripts/verify.sh
```

Record every command executed in the task evidence document. Mark unsupported targets as `NOT RUN` with the exact architectural or environmental reason (e.g. `iosSimulatorArm64Test` on x86_64 host). Do not commit without explicit approval.
