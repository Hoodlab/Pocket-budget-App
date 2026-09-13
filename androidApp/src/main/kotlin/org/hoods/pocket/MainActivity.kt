package org.hoods.pocket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.emptyFlow
import org.hoods.pocket.data.local.database.createBudgetRepository
import org.hoods.pocket.data.local.database.createCategoryBudgetRepository
import org.hoods.pocket.data.local.database.createTransactionRepository
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.repository.TransactionRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val repository = createTransactionRepository(applicationContext)
        val budgetRepository = createBudgetRepository(applicationContext)
        val categoryBudgetRepository = createCategoryBudgetRepository(applicationContext)

        setContent {
            App(
                repository = repository,
                budgetRepository = budgetRepository,
                categoryBudgetRepository = categoryBudgetRepository
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    val previewRepo = remember {
        object : TransactionRepository {
            override fun getTransactions() = emptyFlow<List<Transaction>>()
            override fun getTransactionsByDateRange(startEpochMillis: Long, endEpochMillis: Long) = emptyFlow<List<Transaction>>()
            override fun getExpenseTotalByDateRange(startEpochMillis: Long, endEpochMillis: Long) = kotlinx.coroutines.flow.flowOf(0L)
            override suspend fun insertTransaction(transaction: Transaction) {}
            override suspend fun updateTransaction(transaction: Transaction) {}
            override suspend fun deleteTransaction(id: String) {}
            override suspend fun getTransactionById(id: String): Transaction? = null
        }
    }
    App(repository = previewRepo)
}