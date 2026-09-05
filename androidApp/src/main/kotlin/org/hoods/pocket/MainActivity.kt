package org.hoods.pocket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.emptyFlow
import org.hoods.pocket.data.local.database.createTransactionRepository
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.repository.TransactionRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val repository = createTransactionRepository(applicationContext)

        setContent {
            App(repository = repository)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    val previewRepo = remember {
        object : TransactionRepository {
            override fun getTransactions() = emptyFlow<List<Transaction>>()
            override suspend fun insertTransaction(transaction: Transaction) {}
        }
    }
    App(repository = previewRepo)
}