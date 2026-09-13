package org.hoods.pocket.domain.model

enum class TransactionCategory(
    val displayName: String,
    val applicableType: TransactionType? = null
) {
    UNCATEGORIZED("Uncategorized", null),

    // Expense categories
    GROCERIES("Groceries", TransactionType.EXPENSE),
    DINING("Dining", TransactionType.EXPENSE),
    TRANSPORT("Transport", TransactionType.EXPENSE),
    SHOPPING("Shopping", TransactionType.EXPENSE),
    BILLS("Bills", TransactionType.EXPENSE),
    ENTERTAINMENT("Entertainment", TransactionType.EXPENSE),

    // Income categories
    SALARY("Salary", TransactionType.INCOME),
    FREELANCE("Freelance", TransactionType.INCOME),
    INVESTMENT("Investment", TransactionType.INCOME),
    GIFT("Gift", TransactionType.INCOME),

    // Shared / Other
    OTHER("Other", null);

    fun isValidFor(type: TransactionType): Boolean {
        return applicableType == null || applicableType == type
    }

    companion object {
        fun categoriesFor(type: TransactionType): List<TransactionCategory> {
            return entries.filter { it.isValidFor(type) }
        }

        fun fromName(name: String?): TransactionCategory {
            if (name.isNullOrBlank()) return UNCATEGORIZED
            return entries.firstOrNull {
                it.name.equals(name, ignoreCase = true) || it.displayName.equals(name, ignoreCase = true)
            } ?: UNCATEGORIZED
        }
    }
}
