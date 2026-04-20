package com.kanban.mobile.feature.boards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanban.mobile.feature.boards.permissions.BoardAdminPermission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardSettingsScreen(
    viewModel: BoardSettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToBoardsAfterDelete: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BoardSettingsUiEffect.Snackbar -> snackbarHostState.showSnackbar(effect.message)
                BoardSettingsUiEffect.NavigateToBoardsAfterDelete -> onNavigateToBoardsAfterDelete()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Налаштування дошки") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Назад")
                    }
                },
                actions = {
                    if (viewModel.can(BoardAdminPermission.BOARD_DELETE)) {
                        TextButton(onClick = { showDeleteDialog = true }) {
                            Text("Видалити дошку")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = state.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(onClick = { viewModel.reload() }) {
                        Text("Повторити")
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        Text("Дошка", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.titleDraft,
                            onValueChange = viewModel::onTitleChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Назва") },
                            singleLine = true,
                            enabled = viewModel.can(BoardAdminPermission.BOARD_UPDATE),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Project IDs", style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = state.newProjectIdInput,
                                onValueChange = viewModel::onNewProjectIdChange,
                                modifier = Modifier.weight(1f),
                                label = { Text("Новий ID") },
                                singleLine = true,
                                enabled = viewModel.can(BoardAdminPermission.BOARD_UPDATE),
                            )
                            OutlinedButton(
                                onClick = { viewModel.addProjectIdChip() },
                                enabled = viewModel.can(BoardAdminPermission.BOARD_UPDATE),
                            ) {
                                Text("Додати")
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.projectIds.forEach { pid ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    AssistChip(
                                        onClick = { },
                                        label = { Text(pid) },
                                        enabled = false,
                                    )
                                    if (viewModel.can(BoardAdminPermission.BOARD_UPDATE)) {
                                        TextButton(onClick = { viewModel.removeProjectId(pid) }) {
                                            Text("×")
                                        }
                                    }
                                }
                            }
                        }
                        Button(
                            onClick = { viewModel.saveBoard() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = viewModel.can(BoardAdminPermission.BOARD_UPDATE) &&
                                !state.isSaving &&
                                state.titleDraft.trim().isNotEmpty() &&
                                state.hasBoardMetaChanges(),
                        ) {
                            Text(
                                if (state.isSaving) {
                                    "Збереження…"
                                } else {
                                    "Зберегти зміни"
                                },
                            )
                        }
                    }

                    item {
                        Text("Учасники", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (viewModel.can(BoardAdminPermission.MEMBER_INVITE)) {
                            OutlinedTextField(
                                value = state.inviteSearchQuery,
                                onValueChange = viewModel::onInviteSearchQueryChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Пошук учасника команди (email, ім'я, id)") },
                                singleLine = true,
                            )
                            if (state.inviteCandidates.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                state.inviteCandidates.forEach { c ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.inviteMember(c.userId) }
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(c.email ?: c.userId)
                                            Text(
                                                listOfNotNull(c.name, c.userId).joinToString(" · "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        OutlinedButton(onClick = { viewModel.inviteMember(c.userId) }) {
                                            Text("Запросити")
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    if (state.members.isEmpty()) {
                        item {
                            Text(
                                "Немає учасників окрім власника",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    items(state.members, key = { it.userId }) { member ->
                        MemberRow(
                            member = member,
                            busy = state.memberActionInProgressUserId == member.userId,
                            canChangeRole = viewModel.canEditMember(member),
                            canRemove = viewModel.canRemoveMember(member),
                            onRoleChange = { role -> viewModel.updateMemberRole(member.userId, role) },
                            onRemove = { viewModel.removeMember(member.userId) },
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Видалити дошку?") },
            text = { Text("Цю дію неможливо скасувати.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteBoard()
                    },
                    enabled = !state.isDeleting,
                ) {
                    Text("Видалити")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Скасувати")
                }
            },
        )
    }
}

@Composable
private fun MemberRow(
    member: BoardMember,
    busy: Boolean,
    canChangeRole: Boolean,
    canRemove: Boolean,
    onRoleChange: (BoardRole) -> Unit,
    onRemove: () -> Unit,
) {
    val scroll = rememberScrollState()
    val label = member.name ?: member.email ?: member.userId
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        member.email?.takeIf { it != label }?.let { secondary ->
            Text(
                text = secondary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "Роль:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BoardRole.entries.forEach { role ->
                FilterChip(
                    selected = member.role == role,
                    onClick = {
                        if (canChangeRole && !busy && member.role != role) {
                            onRoleChange(role)
                        }
                    },
                    enabled = canChangeRole && !busy,
                    label = { Text(role.name) },
                )
            }
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .height(24.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
        if (canRemove && !busy) {
            TextButton(onClick = onRemove) {
                Text("Видалити")
            }
        }
    }
}
