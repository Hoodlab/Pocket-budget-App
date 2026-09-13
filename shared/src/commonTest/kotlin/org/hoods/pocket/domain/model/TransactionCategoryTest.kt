package org.hoods.pocket.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransactionCategoryTest {

    @Test
    fun `categoriesFor EXPENSE returns only expense and shared categories`() {
        val expenseCategories = TransactionCategory.categoriesFor(TransactionType.EXPENSE)

        assertTrue(expenseCategories.contains(TransactionCategory.GROCERIES))
        assertTrue(expenseCategories.contains(TransactionCategory.DINING))
        assertTrue(expenseCategories.contains(TransactionCategory.TRANSPORT))
        assertTrue(expenseCategories.contains(TransactionCategory.SHOPPING))
        assertTrue(expenseCategories.contains(TransactionCategory.BILLS))
        assertTrue(expenseCategories.contains(TransactionCategory.ENTERTAINMENT))
        assertTrue(expenseCategories.contains(TransactionCategory.OTHER))
        assertFalse(expenseCategories.contains(TransactionCategory.SALARY))
        assertFalse(expenseCategories.contains(TransactionCategory.FREELANCE))
        assertFalse(expenseCategories.contains(TransactionCategory.INVESTMENT))
        assertFalse(expenseCategories.contains(TransactionCategory.GIFT))
    }

    @Test
    fun `categoriesFor INCOME returns only income and shared categories`() {
        val incomeCategories = TransactionCategory.categoriesFor(TransactionType.INCOME)

        assertTrue(incomeCategories.contains(TransactionCategory.SALARY))
        assertTrue(incomeCategories.contains(TransactionCategory.FREELANCE))
        assertTrue(incomeCategories.contains(TransactionCategory.INVESTMENT))
        assertTrue(incomeCategories.contains(TransactionCategory.GIFT))
        assertTrue(incomeCategories.contains(TransactionCategory.OTHER))
        assertFalse(incomeCategories.contains(TransactionCategory.GROCERIES))
        assertFalse(incomeCategories.contains(TransactionCategory.DINING))
        assertFalse(incomeCategories.contains(TransactionCategory.TRANSPORT))
        assertFalse(incomeCategories.contains(TransactionCategory.SHOPPING))
        assertFalse(incomeCategories.contains(TransactionCategory.BILLS))
        assertFalse(incomeCategories.contains(TransactionCategory.ENTERTAINMENT))
    }

    @Test
    fun `UNCATEGORIZED and OTHER are valid for both EXPENSE and INCOME`() {
        assertTrue(TransactionCategory.UNCATEGORIZED.isValidFor(TransactionType.EXPENSE))
        assertTrue(TransactionCategory.UNCATEGORIZED.isValidFor(TransactionType.INCOME))
        assertTrue(TransactionCategory.OTHER.isValidFor(TransactionType.EXPENSE))
        assertTrue(TransactionCategory.OTHER.isValidFor(TransactionType.INCOME))
    }

    @Test
    fun `fromName maps valid names and displayNames case-insensitively`() {
        assertEquals(TransactionCategory.GROCERIES, TransactionCategory.fromName("GROCERIES"))
        assertEquals(TransactionCategory.GROCERIES, TransactionCategory.fromName("Groceries"))
        assertEquals(TransactionCategory.GROCERIES, TransactionCategory.fromName("groceries"))
        assertEquals(TransactionCategory.SALARY, TransactionCategory.fromName("SALARY"))
        assertEquals(TransactionCategory.SALARY, TransactionCategory.fromName("Salary"))
    }

    @Test
    fun `fromName defaults to UNCATEGORIZED for unknown or blank names`() {
        assertEquals(TransactionCategory.UNCATEGORIZED, TransactionCategory.fromName(null))
        assertEquals(TransactionCategory.UNCATEGORIZED, TransactionCategory.fromName(""))
        assertEquals(TransactionCategory.UNCATEGORIZED, TransactionCategory.fromName("   "))
        assertEquals(TransactionCategory.UNCATEGORIZED, TransactionCategory.fromName("UNKNOWN_CATEGORY_XYZ"))
    }
}
