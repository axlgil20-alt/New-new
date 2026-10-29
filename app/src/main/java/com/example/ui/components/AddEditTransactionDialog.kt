package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionDialog(
    transaction: TransactionEntity? = null,
    currencySymbol: String = "Q ",
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TransactionInputForm(
                initialTransaction = transaction,
                currencySymbol = currencySymbol,
                onCancel = onDismiss,
                onSave = {
                    onSave(it)
                    onDismiss()
                }
            )
        }
    }
}
