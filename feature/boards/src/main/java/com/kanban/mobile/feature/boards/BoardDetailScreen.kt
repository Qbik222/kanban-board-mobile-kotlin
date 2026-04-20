package com.kanban.mobile.feature.boards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private const val DESCRIPTION_PREVIEW_CHARS = 120

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun BoardDetailScreen(
    viewModel: BoardDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val refreshing = state.loading && state.board != null
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = { viewModel.refresh() },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.board?.title ?: "Board") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState),
        ) {
            when {
                state.loading && state.board == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.error != null && state.board == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = state.error ?: "Error",
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text("Retry")
                        }
                    }
                }

                state.board != null -> {
                    val board = state.board!!
                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Team: ${board.teamId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Owner: ${board.ownerId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (board.projectIds.isNotEmpty()) {
                                Text(
                                    text = "Projects: ${board.projectIds.joinToString()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        LazyRow(
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        ) {
                            items(board.columns, key = { it.id }) { column ->
                                Card(
                                    modifier = Modifier
                                        .width(280.dp)
                                        .height(420.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                    ) {
                                        Text(
                                            text = column.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(bottom = 8.dp),
                                        )
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxSize(),
                                        ) {
                                            items(column.cards, key = { it.id }) { card ->
                                                Card(
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.surface,
                                                    ),
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(10.dp),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                                    ) {
                                                        Text(
                                                            text = card.title,
                                                            style = MaterialTheme.typography.titleSmall,
                                                        )
                                                        card.priority?.let { p ->
                                                            Text(
                                                                text = "Priority: $p",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.primary,
                                                            )
                                                        }
                                                        card.description?.takeIf { it.isNotBlank() }?.let { d ->
                                                            Text(
                                                                text = if (d.length > DESCRIPTION_PREVIEW_CHARS) {
                                                                    d.take(DESCRIPTION_PREVIEW_CHARS) + "…"
                                                                } else {
                                                                    d
                                                                },
                                                                style = MaterialTheme.typography.bodySmall,
                                                                maxLines = 4,
                                                                overflow = TextOverflow.Ellipsis,
                                                            )
                                                        }
                                                        Text(
                                                            text = "${card.commentCount} comments",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            PullRefreshIndicator(
                refreshing = refreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}
