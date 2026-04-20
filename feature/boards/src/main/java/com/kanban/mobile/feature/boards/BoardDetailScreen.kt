package com.kanban.mobile.feature.boards

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
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
    onNavigateToAiLab: () -> Unit = {},
    onNavigateToBoards: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var newColumnTitle by rememberSaveable { mutableStateOf("") }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BoardDetailEffect.Snackbar -> snackbarHostState.showSnackbar(effect.message)
                BoardDetailEffect.NavigateToBoards -> onNavigateToBoards()
                BoardDetailEffect.ColumnCreated -> newColumnTitle = ""
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
                    TextButton(onClick = onNavigateToAiLab) {
                        Text("AI")
                    }
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
                        newColumnTitle = newColumnTitle,
                        onNewColumnTitleChange = { newColumnTitle = it },
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

private data class BoardCardDragSession(
    val card: BoardCard,
    val sourceColumnId: String,
    val sourceIndex: Int,
    val previewSize: Size,
)

@Composable
private fun BoardKanbanContent(
    state: BoardDetailUiState,
    viewModel: BoardDetailViewModel,
    newColumnTitle: String,
    onNewColumnTitleChange: (String) -> Unit,
) {
    val board = state.board ?: return
    var renameTargetId by remember { mutableStateOf<String?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }

    var dragSession by remember { mutableStateOf<BoardCardDragSession?>(null) }
    var fingerRoot by remember { mutableStateOf(Offset.Zero) }
    var columnBounds by remember { mutableStateOf(mapOf<String, Rect>()) }
    var cardBounds by remember { mutableStateOf(mapOf<Pair<String, String>, Rect>()) }

    val dropPreview = remember(fingerRoot, board, columnBounds, cardBounds) {
        resolveDropTarget(fingerRoot, board, columnBounds, cardBounds)
    }

    val hoverColumnId = dropPreview?.columnId
    val hoverInsertSlot = dropPreview?.insertBeforeSlot

    val insertLineYRoot: Float? = remember(hoverColumnId, hoverInsertSlot, cardBounds, board, columnBounds) {
        val colId = hoverColumnId ?: return@remember null
        val slot = hoverInsertSlot ?: return@remember null
        val column = board.columns.firstOrNull { it.id == colId } ?: return@remember null
        val rects = column.cards.mapNotNull { c -> cardBounds[colId to c.id] }.sortedBy { it.top }
        val colRect = columnBounds[colId]
        if (rects.isEmpty()) {
            return@remember colRect?.let { it.top + it.height * 0.45f }
        }
        when {
            slot <= 0 -> rects.first().top
            slot >= rects.size -> rects.last().bottom
            else -> (rects[slot - 1].bottom + rects[slot].top) / 2f
        }
    }

    val insertLineLocalYInHoveredColumn: Float? = remember(hoverColumnId, insertLineYRoot, columnBounds) {
        val cid = hoverColumnId ?: return@remember null
        val lineY = insertLineYRoot ?: return@remember null
        val top = columnBounds[cid]?.top ?: return@remember null
        lineY - top
    }

    fun registerColumnBounds(columnId: String, rect: Rect) {
        columnBounds = columnBounds + (columnId to rect)
    }

    fun registerCardBounds(columnId: String, cardId: String, rect: Rect) {
        cardBounds = cardBounds + ((columnId to cardId) to rect)
    }

    fun startDrag(session: BoardCardDragSession, finger: Offset) {
        dragSession = session
        fingerRoot = finger
    }

    fun moveFinger(offset: Offset) {
        fingerRoot = offset
    }

    fun endDrag(fingerInRoot: Offset) {
        val session = dragSession ?: return
        dragSession = null
        val drop = resolveDropTarget(fingerInRoot, board, columnBounds, cardBounds) ?: return
        val targetColumn = board.columns.firstOrNull { it.id == drop.columnId } ?: return
        val newOrder = newOrderAfterRemoval(
            sourceColumnId = session.sourceColumnId,
            sourceIndex = session.sourceIndex,
            targetColumnId = drop.columnId,
            insertBeforeSlot = drop.insertBeforeSlot,
            targetCardsIncludingDragged = targetColumn.cards,
        )
        viewModel.moveCardToDropTarget(session.card.id, drop.columnId, newOrder)
    }

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
                        onValueChange = onNewColumnTitleChange,
                        placeholder = { Text("New column") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val t = newColumnTitle.trim()
                                if (t.isNotEmpty()) viewModel.createColumn(newColumnTitle)
                            },
                        ),
                    )
                    OutlinedButton(
                        onClick = { viewModel.createColumn(newColumnTitle) },
                        enabled = newColumnTitle.trim().isNotEmpty(),
                    ) {
                        Text("Add")
                    }
                }
            }
        }

        var kanbanBoxOriginInRoot by remember { mutableStateOf(Offset.Zero) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onGloballyPositioned { coords ->
                    kanbanBoxOriginInRoot = coords.boundsInRoot().topLeft
                },
        ) {
            LazyRow(
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = dragSession == null,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(board.columns.sortedBy { it.order }, key = { it.id }) { column ->
                    KanbanColumnCard(
                        column = column,
                        board = board,
                        viewModel = viewModel,
                        pendingInlineDraft = state.pendingInlineCard,
                        isDragging = dragSession != null,
                        draggedCardId = dragSession?.card?.id,
                        hoverColumnId = hoverColumnId,
                        insertLineLocalY = if (hoverColumnId == column.id) insertLineLocalYInHoveredColumn else null,
                        onRenameRequest = {
                            renameTargetId = column.id
                            renameText = column.title
                        },
                        onRegisterColumnBounds = { id, rect -> registerColumnBounds(id, rect) },
                        onRegisterCardBounds = { colId, cId, rect -> registerCardBounds(colId, cId, rect) },
                        onStartDrag = { card, colId, idx, finger, size ->
                            startDrag(
                                BoardCardDragSession(card, colId, idx, size),
                                finger,
                            )
                        },
                        onDragMove = { moveFinger(it) },
                        onDragEnd = { finger -> endDrag(finger) },
                    )
                }
            }

            dragSession?.let { session ->
                val x = (fingerRoot.x - kanbanBoxOriginInRoot.x - session.previewSize.width * 0.35f).roundToInt()
                val y = (fingerRoot.y - kanbanBoxOriginInRoot.y - session.previewSize.height * 0.35f).roundToInt()
                Card(
                    modifier = Modifier
                        .offset { IntOffset(x, y) }
                        .width(240.dp)
                        .graphicsLayer { alpha = 0.92f },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = session.card.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
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
}

@Composable
private fun InlineNewCardDraftBlock(
    draft: InlineNewCardDraft,
    viewModel: BoardDetailViewModel,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Quick edit", style = MaterialTheme.typography.labelSmall)
            OutlinedTextField(
                value = draft.title,
                onValueChange = { v ->
                    viewModel.updateInlineNewCardDraft { it.copy(title = v) }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                singleLine = true,
            )
            OutlinedTextField(
                value = draft.deadlineStart,
                onValueChange = { v ->
                    viewModel.updateInlineNewCardDraft { it.copy(deadlineStart = v) }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Start date (ISO)") },
                singleLine = true,
            )
            OutlinedTextField(
                value = draft.deadlineEnd,
                onValueChange = { v ->
                    viewModel.updateInlineNewCardDraft { it.copy(deadlineEnd = v) }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("End date (ISO)") },
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.submitInlineNewCard() }) {
                    Text("Save")
                }
                OutlinedButton(onClick = { viewModel.cancelInlineNewCard() }) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun KanbanColumnCard(
    column: BoardColumn,
    board: BoardDetails,
    viewModel: BoardDetailViewModel,
    pendingInlineDraft: InlineNewCardDraft?,
    isDragging: Boolean,
    draggedCardId: String?,
    hoverColumnId: String?,
    insertLineLocalY: Float?,
    onRenameRequest: () -> Unit,
    onRegisterColumnBounds: (String, Rect) -> Unit,
    onRegisterCardBounds: (String, String, Rect) -> Unit,
    onStartDrag: (BoardCard, String, Int, Offset, Size) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val sortedCols = board.columns.sortedBy { it.order }
    val showHover = isDragging && hoverColumnId == column.id

    Card(
        modifier = Modifier
            .width(280.dp)
            .height(460.dp)
            .onGloballyPositioned { coords ->
                onRegisterColumnBounds(column.id, coords.boundsInRoot())
            }
            .then(
                if (showHover) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                },
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                        enabled = pendingInlineDraft == null,
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
                    itemsIndexed(column.cards, key = { _, c -> c.id }) { indexInColumn, card ->
                        if (pendingInlineDraft?.cardId == card.id) {
                            InlineNewCardDraftBlock(
                                draft = pendingInlineDraft,
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            KanbanCardRow(
                                card = card,
                                columnId = column.id,
                                sourceIndex = indexInColumn,
                                viewModel = viewModel,
                                isDraggedCard = draggedCardId == card.id,
                                onOpen = { viewModel.selectCard(card.id) },
                                onRegisterCardBounds = { rect ->
                                    onRegisterCardBounds(column.id, card.id, rect)
                                },
                                onStartDrag = onStartDrag,
                                onDragMove = onDragMove,
                                onDragEnd = onDragEnd,
                            )
                        }
                    }
                }
            }

            insertLineLocalY?.let { ly ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, ly.roundToInt()) }
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Composable
private fun KanbanCardRow(
    card: BoardCard,
    columnId: String,
    sourceIndex: Int,
    viewModel: BoardDetailViewModel,
    isDraggedCard: Boolean,
    onOpen: () -> Unit,
    onRegisterCardBounds: (Rect) -> Unit,
    onStartDrag: (BoardCard, String, Int, Offset, Size) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit,
) {
    var layoutCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var lastFingerInRoot by remember { mutableStateOf(Offset.Zero) }
    val canMove = viewModel.can(BoardPermission.MOVE_CARD)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                layoutCoords = coords
                onRegisterCardBounds(coords.boundsInRoot())
            }
            .graphicsLayer { alpha = if (isDraggedCard) 0.35f else 1f }
            .then(
                if (canMove) {
                    Modifier.pointerInput(card.id, columnId) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                val lc = layoutCoords ?: return@detectDragGesturesAfterLongPress
                                val root = lc.toRootOffset(offset)
                                lastFingerInRoot = root
                                val sz = Size(lc.size.width.toFloat(), lc.size.height.toFloat())
                                onStartDrag(card, columnId, sourceIndex, root, sz)
                            },
                            onDrag = { change, _ ->
                                val lc = layoutCoords ?: return@detectDragGesturesAfterLongPress
                                val root = lc.toRootOffset(change.position)
                                lastFingerInRoot = root
                                onDragMove(root)
                                change.consume()
                            },
                            onDragEnd = { onDragEnd(lastFingerInRoot) },
                            onDragCancel = { onDragEnd(lastFingerInRoot) },
                        )
                    }
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onOpen),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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
        Text("Priority", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CardPriorityOptions.LABELS.forEach { (value, label) ->
                FilterChip(
                    selected = draft.priority == value,
                    onClick = {
                        if (canEdit) onDraftChange(draft.copy(priority = value))
                    },
                    enabled = canEdit,
                    label = { Text(label) },
                )
            }
        }
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
            value = draft.deadlineStart,
            onValueChange = { onDraftChange(draft.copy(deadlineStart = it)) },
            enabled = canEdit,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Start date (ISO)") },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = draft.deadlineEnd,
            onValueChange = { onDraftChange(draft.copy(deadlineEnd = it)) },
            enabled = canEdit,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("End date (ISO)") },
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

private fun LayoutCoordinates.toRootOffset(localOffset: Offset): Offset =
    boundsInRoot().topLeft + localOffset
