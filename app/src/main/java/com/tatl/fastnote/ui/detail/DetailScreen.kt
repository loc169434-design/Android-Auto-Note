package com.tatl.fastnote.ui.detail

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tatl.fastnote.util.DateUtils
import com.tatl.fastnote.util.FileExportHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit
) {
    val note by viewModel.note.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var editedTitle by remember(note) { mutableStateOf(note?.title ?: "") }
    var editedContent by remember(note) { mutableStateOf(note?.content ?: "") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isSaved) {
        if (isSaved) {
            snackbarHostState.showSnackbar("Đã lưu!")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết ghi chú") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    // Share button
                    IconButton(onClick = {
                        note?.let { n ->
                            try {
                                val shareIntent = FileExportHelper.createShareIntent(
                                    context = context,
                                    title = editedTitle,
                                    content = editedContent,
                                    createdAt = n.createdAt
                                )
                                val chooser = android.content.Intent.createChooser(shareIntent, "Chia sẻ qua")
                                chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(chooser)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Không thể chia sẻ. Vui lòng thử lại.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Chia sẻ")
                    }
                    // Save button
                    IconButton(onClick = {
                        viewModel.updateNote(editedTitle, editedContent)
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Lưu")
                    }
                    // Delete button
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Xóa",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (note == null) {
            // Loading or note not found
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Đang tải...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Created date
                Text(
                    text = "Tạo lúc: ${DateUtils.formatDateTime(note!!.createdAt)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (note!!.updatedAt != note!!.createdAt) {
                    Text(
                        text = "Sửa lúc: ${DateUtils.formatDateTime(note!!.updatedAt)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    label = { Text("Tiêu đề") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Content
                OutlinedTextField(
                    value = editedContent,
                    onValueChange = { editedContent = it },
                    label = { Text("Nội dung") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    maxLines = Int.MAX_VALUE
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa ghi chú?") },
            text = { Text("Bạn có chắc muốn xóa ghi chú này không? Hành động này không thể hoàn tác.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNote()
                    showDeleteDialog = false
                    onBack()
                }) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
