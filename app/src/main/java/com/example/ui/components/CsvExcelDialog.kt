package com.example.ui.components

import androidx.compose.runtime.Composable

/**
 * CsvExcelDialog delega directamente a ExcelCsvImporter.
 */
@Composable
fun CsvExcelDialog(
    initialExportMode: Boolean = false,
    onDismiss: () -> Unit,
    onImportText: (String) -> Unit,
    onExportRequest: suspend () -> String
) {
    ExcelCsvImporter(
        initialExportMode = initialExportMode,
        onDismiss = onDismiss,
        onImportText = onImportText,
        onExportRequest = onExportRequest
    )
}
