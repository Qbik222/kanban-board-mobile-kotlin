package com.kanban.mobile.feature.boards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanban.mobile.feature.boards.permissions.BoardPermission

private const val DESCRIPTION_PREVIEW_CHARS = 120

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun BoardDetailScreen(
    viewModel: BoardDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBoards: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BoardDetailEffect.Snackbar -> snackbarHostState.showSnackbar(effect.message)
                BoardDetailEffect.NavigateToBoards -> onNavigateToBoards()
            }
        }
    }

    val refreshing = state.loading && state.board != null
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = { viewModel.refresh() },
    )

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.board?.title ?: "Board") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateToSettings) {
                        Text("Settings")
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
                    BoardKanbanContent(
                        state = state,
                        viewModel = viewModel,
                    )
                }
            }
            PullRefreshIndicator(
                refreshing = refreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }

    if (state.selectedCardId != null && state.cardDraft != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissCardDetail() },
            sheetState = sheetState,
        ) {
            CardDetailSheet(
                draft = state.cardDraft!!,
                comments = state.board!!.columns
                    .flatMap { it.cards }
                    .firstOrNull { it.id == state.selectedCardId }
                    ?.comments.orEmpty(),
                newCommentText = state.newCommentText,
                canEdit = viewModel.can(BoardPermission.UPDATE_CARD),
                canDeleteCard = viewModel.can(BoardPermission.DELETE_CARD),
                canComment = viewModel.can(BoardPermission.COMMENT_CREATE),
                onDraftChange = viewModel::updateDraft,
                onSave = { viewModel.saveCardDraft() },
                onDelete = {
                    viewModel.deleteSelectedCard()
                    viewModel.dismissCardDetail()
                },
                onDismiss = viewModel::dismissCardDetail,
                onNewCommentChange = viewModel::updateNewCommentText,
                onSubmitComment = viewModel::submitComment,
                canRemoveComment = viewModel::canRemoveComment,
                onDeleteComment = viewModel::deleteComment,
            )
        }
    }
}

@Composable
private fun BoardKanbanContent(
    state: BoardDetailUiState,
    viewModel: BoardDetailViewModel,
) {
    val board = state.board ?: return

    var newColumnTitle by rememberSaveable { mutableStateOf("") }
    var renameTargetId by remember { mutableStateOf<String?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }
    var moveCardId by remember { mutableStateOf<String?>(null) }

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
                text = "Role: ${state.effectiveRole.name}",
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
            if (viewModel.can(BoardPermission.CREATE_COLUMN)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = newColumnTitle,
                        onValueChange = { newColumnTitle = it },
                        placeholder = { Text("New column") },
                        singleLine = true,
                    )
                    OutlinedButton(
                        onClick = {
                            viewModel.createColumn(newColumnTitle)
                            newColumnTitle = ""
                        },
                    ) {
                        Text("Add")
                    }
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            items(board.columns.sortedBy { it.order }, key = { it.id }) { column ->
                KanbanColumnCard(
                    column = column,
                    board = board,
                    viewModel = viewModel,
                    onRenameRequest = {
                        renameTargetId = column.id
                        renameText = column.title
                    },
                    onPickMoveTarget = { cardId ->
                        moveCardId = cardId
                    },
                )
            }
        }
    }

    if (renameTargetId != null) {
        AlertDialog(
            onDismissRequest = { renameTargetId = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        renameTargetId?.let { viewModel.renameColumn(it, renameText) }
                        renameTargetId = null
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTargetId = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Rename column") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            },
        )
    }

    val moveId = moveCardId
    if (moveId != null) {
        AlertDialog(
            onDismissRequest = { moveCardId = null },
            confirmButton = {
                TextButton(onClick = { moveCardId = null }) {
                    Text("Close")
                }
            },
            title = { Text("Move to column") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    board.columns.sortedBy { it.order }.forEach { col ->
                        TextButton(
                            onClick = {
                                viewModel.moveCardToColumn(moveId, col.id)
                                moveCardId = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(col.title, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun KanbanColumnCard(
    column: BoardColumn,
    board: BoardDetails,
    viewModel: BoardDetailViewModel,
    onRenameRequest: () -> Unit,
    onPickMoveTarget: (String) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val sortedCols = board.columns.sortedBy { it.order }

    Card(
        modifier = Modifier
            .width(280.dp)
            .height(460.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = column.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (viewModel.can(BoardPermission.REORDER_COLUMNS)) {
                    Row {
                        IconButton(
                            enabled = sortedCols.indexOfFirst { it.id == column.id } > 0,
                            onClick = { viewModel.moveColumn(column.id, -1) },
                            modifier = Modifier.padding(0.dp),
                        ) {
                            Text("◀")
                        }
                        IconButton(
                            enabled = sortedCols.indexOfFirst { it.id == column.id } < sortedCols.lastIndex,
                            onClick = { viewModel.moveColumn(column.id, +1) },
                            modifier = Modifier.padding(0.dp),
                        ) {
                            Text("▶")
                        }
                    }
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Text("⋯")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (viewModel.can(BoardPermission.UPDATE_COLUMN)) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = {
                                    menuOpen = false
                                    onRenameRequest()
                                },
                            )
                        }
                        if (viewModel.can(BoardPermission.DELETE_COLUMN)) {
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    menuOpen = false
                                    viewModel.deleteColumn(column.id)
                                },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (viewModel.can(BoardPermission.CREATE_CARD)) {
                OutlinedButton(
                    onClick = { viewModel.createCard(column.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("+ Card")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(column.cards, key = { it.id }) { card ->
                    KanbanCardRow(
                        card = card,
                        viewModel = viewModel,
                        onOpen = { viewModel.selectCard(card.id) },
                        onMoveOtherColumn = { onPickMoveTarget(card.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun KanbanCardRow(
    card: BoardCard,
    viewModel: BoardDetailViewModel,
    onOpen: () -> Unit,
    onMoveOtherColumn: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (viewModel.can(BoardPermission.MOVE_CARD)) {
                    Row {
                        IconButton(
                            onClick = { viewModel.moveCardWithinColumn(card.id, -1) },
                            modifier = Modifier.padding(0.dp),
                        ) {
                            Text("↑")
                        }
                        IconButton(
                            onClick = { viewModel.moveCardWithinColumn(card.id, +1) },
                            modifier = Modifier.padding(0.dp),
                        ) {
                            Text("↓")
                        }
                        IconButton(
                            onClick = onMoveOtherColumn,
                            modifier = Modifier.padding(0.dp),
                        ) {
                            Text("⇄")
                        }
                    }
                }
            }
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

@Composable
private fun CardDetailSheet(
    draft: CardDraft,
    comments: List<CardComment>,
    newCommentText: String,
    canEdit: Boolean,
    canDeleteCard: Boolean,
    canComment: Boolean,
    onDraftChange: (CardDraft) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onNewCommentChange: (String) -> Unit,
    onSubmitComment: () -> Unit,
    canRemoveComment: (CardComment) -> Boolean,
    onDeleteComment: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Card details", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = draft.title,
            onValueChange = { onDraftChange(draft.copy(title = it)) },
            enabled = canEdit,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Title") },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = draft.description,
            onValueChange = { onDraftChange(draft.copy(description = it)) },
            enabled = canEdit,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Description") },
            minLines = 3,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = draft.priority,
            onValueChange = { onDraftChange(draft.copy(priority = it)) },
            enabled = canEdit,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Priority") },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = draft.assigneeId,
            onValueChange = { onDraftChange(draft.copy(assigneeId = it)) },
            enabled = canEdit,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Assignee user id") },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = draft.projectIdsText,
            onValueChange = { onDraftChange(draft.copy(projectIdsText = it)) },
            enabled = canEdit,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Project IDs (comma-separated)") },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = draft.deadlineDueAt,
            onValueChange = { onDraftChange(draft.copy(deadlineDueAt = it)) },
            enabled = canEdit,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Deadline (ISO)") },
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (canEdit) {
                FilledTonalButton(onClick = onSave) {
                    Text("Save")
                }
            }
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
            if (canDeleteCard) {
                OutlinedButton(
                    onClick = onDelete,
                ) {
                    Text("Delete")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Comments", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        comments.forEach { c ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(c.body.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                    c.userId?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (canRemoveComment(c)) {
                    TextButton(onClick = { onDeleteComment(c.id) }) {
                        Text("Delete")
                    }
                }
            }
        }

        if (canComment) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = newCommentText,
                onValueChange = onNewCommentChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("New comment") },
                minLines = 2,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSubmitComment,
                enabled = newCommentText.isNotBlank(),
            ) {
                Text("Post comment")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
