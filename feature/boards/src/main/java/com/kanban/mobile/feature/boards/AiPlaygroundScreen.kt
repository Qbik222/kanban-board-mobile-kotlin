package com.kanban.mobile.feature.boards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPlaygroundScreen(
    viewModel: AiPlaygroundViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { e ->
            when (e) {
                is AiPlaygroundEffect.Toast -> snackbarHostState.showSnackbar(e.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI playground") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Action type",
                style = MaterialTheme.typography.titleSmall,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AiBoardAction.entries.forEach { action ->
                    FilterChip(
                        selected = state.selectedAction == action,
                        onClick = { viewModel.onActionSelected(action) },
                        enabled = !state.busy,
                        label = { Text(action.name) },
                    )
                }
            }
            Text(
                text = "JSON parameters",
                style = MaterialTheme.typography.titleSmall,
            )
            OutlinedTextField(
                value = state.jsonText,
                onValueChange = viewModel::onJsonChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 10,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )
            Text(
                text = "Examples: {\"title\":\"Column\"} · {\"cardId\":\"…\",\"title\":\"T\"} · {\"cardId\":\"…\",\"targetColumnId\":\"…\",\"newOrder\":0}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.runSelectedAction() },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.busy) "Running…" else "Run")
            }
        }
    }
}
