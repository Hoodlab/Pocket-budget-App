---
name: navigation
description: >-
  Guide for implementing type-safe Compose Navigation in Pocket Budget. Use to define
  type-safe routes, wire NavHost graphs, manage screen transitions, and maintain
  strict architectural boundaries across Multiplatform targets.
---

# Compose Multiplatform Navigation in Pocket Budget

This skill governs the structure, conventions, and implementation of type-safe navigation in Pocket Budget using Compose Multiplatform Navigation (`androidx.navigation.compose` / `org.jetbrains.androidx.navigation:navigation-compose`).

---

## 1. Principles & Architecture Boundaries

1. **Type Safety First**: All destinations are represented by `@Serializable` Kotlin objects or data classes. String-based route paths or URI templates are forbidden.
2. **Decoupled Composables**: Screen composables (e.g. `TransactionListScreen`, `AddTransactionForm`) must never receive `NavController` directly. Instead, expose explicit event callbacks:
   ```kotlin
   @Composable
   fun TransactionListScreen(
       transactions: List<Transaction>,
       onAddTransactionClick: () -> Unit,
       modifier: Modifier = Modifier
   )
   ```
3. **Centralized Graph Definition**: Navigation actions (`navigate`, `popBackStack`) belong in the top-level `NavHost` or dedicated navigation graph composable.
4. **Clean Layering**: Navigation is strictly a presentation/UI concern. Domain use cases, models, and repositories must remain completely agnostic of navigation components.

---

## 2. Defining Type-Safe Routes

Define routes using Kotlin Serialization:

```kotlin
package org.hoods.pocket.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object TransactionList : Screen

    @Serializable
    data object AddTransaction : Screen
}
```

- Use `data object` for destinations without arguments.
- Use `data class` with `@Serializable` for destinations requiring typed arguments (e.g., entity IDs).

```kotlin
sealed interface Screen {
    @Serializable
    data object TransactionList : Screen

    @Serializable
    data object AddTransaction : Screen

    @Serializable
    data class EditTransaction(val transactionId: String) : Screen
}
```

---

## 3. Dedicated Navigation Host Composable

Keep `App` focused on theme and app shell, keep repository state and use-case actions in a root composable, and let a dedicated navigation host own `rememberNavController()`, typed routes, and back-stack behavior:

```kotlin
@Composable
fun App(repository: TransactionRepository) {
    PocketTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PocketColors.Surface)
                .safeContentPadding()
        ) {
            PocketBudgetRoot(repository = repository)
        }
    }
}

@Composable
fun PocketBudgetRoot(
    repository: TransactionRepository,
    modifier: Modifier = Modifier
) {
    val addTransactionUseCase = remember(repository) { AddTransactionUseCase(repository) }
    val updateTransactionUseCase = remember(repository) { UpdateTransactionUseCase(repository) }
    val deleteTransactionUseCase = remember(repository) { DeleteTransactionUseCase(repository) }
    val getTransactionByIdUseCase = remember(repository) { GetTransactionByIdUseCase(repository) }
    val transactions by repository.getTransactions().collectAsState(initial = emptyList())

    PocketNavHost(
        transactions = transactions,
        onAddTransaction = { transaction -> addTransactionUseCase(transaction) },
        onUpdateTransaction = { updatedTransaction -> updateTransactionUseCase(updatedTransaction) },
        onDeleteTransaction = { transactionId -> deleteTransactionUseCase(transactionId) },
        onLoadTransaction = { transactionId -> getTransactionByIdUseCase(transactionId) },
        modifier = modifier
    )
}
```

In `PocketNavHost`, pass state and actions into screen composables and load entity data by ID rather than querying the rendered list:

```kotlin
@Composable
fun PocketNavHost(
    transactions: List<Transaction>,
    onAddTransaction: suspend (Transaction) -> Unit,
    onUpdateTransaction: suspend (Transaction) -> Unit,
    onDeleteTransaction: suspend (String) -> Unit,
    onLoadTransaction: suspend (String) -> Transaction?,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.TransactionList,
        modifier = modifier
    ) {
        composable<Screen.TransactionList> {
            TransactionListScreen(
                transactions = transactions,
                onAddTransactionClick = { navController.navigate(Screen.AddTransaction) },
                onTransactionClick = { transactionId -> navController.navigate(Screen.EditTransaction(transactionId)) }
            )
        }
        composable<Screen.AddTransaction> {
            AddTransactionForm(
                onSaveTransaction = { transaction ->
                    onAddTransaction(transaction)
                    navController.popBackStack()
                },
                onDismiss = { navController.popBackStack() }
            )
        }
        composable<Screen.EditTransaction> { backStackEntry ->
            val editRoute = backStackEntry.toRoute<Screen.EditTransaction>()
            var transaction by remember(editRoute.transactionId) { mutableStateOf<Transaction?>(null) }
            LaunchedEffect(editRoute.transactionId) {
                transaction = onLoadTransaction(editRoute.transactionId)
            }
            transaction?.let { currentTx ->
                EditTransactionScreen(
                    transaction = currentTx,
                    onSaveTransaction = { updated ->
                        onUpdateTransaction(updated)
                        navController.popBackStack()
                    },
                    onDeleteTransaction = { id ->
                        onDeleteTransaction(id)
                        navController.popBackStack()
                    },
                    onDismiss = { navController.popBackStack() }
                )
            }
        }
    }
}
```

---

## 4. Navigation Actions & Back Stack Handling

- **Navigating forward**: Use `navController.navigate(route)`.
- **Dismissing / Returning**: Use `navController.popBackStack()`.
- **Preventing duplicate destinations**: When navigating to top-level destinations, use `launchSingleTop = true` if appropriate.

---

## 5. Verification Checklist

Before creating a checkpoint for navigation changes:

- [ ] All destinations are `@Serializable` objects or classes.
- [ ] No screen composable directly depends on `NavController`.
- [ ] User flow remains identical (e.g. List -> Add -> Save/Dismiss -> List).
- [ ] `./scripts/verify.sh` passes across all active targets.
