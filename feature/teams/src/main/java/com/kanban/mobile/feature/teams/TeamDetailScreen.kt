package com.kanban.mobile.feature.teams

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailScreen(
    viewModel: TeamDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var renameOpen by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var removeUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(state.team?.name) {
        renameText = state.team?.name ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.team?.name ?: "Team") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            renameText = state.team?.name ?: ""
                            renameOpen = true
                        },
                        enabled = state.team != null && !state.pendingMutation,
                    ) {
                        Text("Rename")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.loading && state.team == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val screenError = state.screenError
                    if (screenError != null) {
                        item {
                            Text(
                                text = screenError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            TextButton(onClick = { viewModel.load() }) {
                                Text("Retry")
                            }
                        }
                    }

                    item {
                        Text("Invite search", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = state.inviteSearchQuery,
                            onValueChange = { viewModel.onInviteSearchQueryChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Search users (name, email, id)") },
                            singleLine = true,
                            enabled = !state.pendingMutation,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        )
                        if (state.inviteCandidates.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            state.inviteCandidates.forEach { c ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !state.pendingMutation) {
                                            viewModel.inviteCandidateUser(c.userId)
                                        }
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(c.email ?: c.userId)
                                        val label = listOfNotNull(c.name, c.userId).joinToString(" · ")
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        text = "Invite",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text("Members", style = MaterialTheme.typography.titleMedium)
                    }

                    items(state.members, key = { it.userId }) { member ->
                        MemberRow(
                            member = member,
                            pending = state.pendingMutation,
                            self = viewModel.isCurrentUser(member.userId),
                            onChangeRole = { role -> viewModel.changeMemberRole(member.userId, role) },
                            onRemove = { removeUserId = member.userId },
                        )
                    }
                }
            }
        }
    }

    if (renameOpen) {
        AlertDialog(
            onDismissRequest = { renameOpen = false },
            title = { Text("Rename team") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.patchTeamName(renameText)
                        renameOpen = false
                    },
                    enabled = renameText.trim().isNotBlank() &&
                        renameText.trim() != state.loadedTeamName.trim() &&
                        !state.pendingMutation,
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameOpen = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    removeUserId?.let { uid ->
        AlertDialog(
            onDismissRequest = { removeUserId = null },
            title = { Text("Remove member") },
            text = { Text("Remove this member from the team?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeMember(uid)
                        removeUserId = null
                    },
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { removeUserId = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun MemberRow(
    member: TeamMember,
    pending: Boolean,
    self: Boolean,
    onChangeRole: (TeamMemberRole) -> Unit,
    onRemove: () -> Unit,
) {
    var roleMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(member.name ?: member.email ?: member.userId, style = MaterialTheme.typography.titleSmall)
        Text(
            member.userId,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                TextButton(
                    onClick = { roleMenu = true },
                    enabled = !pending,
                ) {
                    Text("Role: ${member.role.name}")
                }
                DropdownMenu(
                    expanded = roleMenu,
                    onDismissRequest = { roleMenu = false },
                ) {
                    TeamMemberRole.entries.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role.name) },
                            onClick = {
                                roleMenu = false
                                if (role != member.role) {
                                    onChangeRole(role)
                                }
                            },
                        )
                    }
                }
            }
            TextButton(onClick = onRemove, enabled = !pending) {
                Text("Remove")
            }
            if (self) {
                Text("(you)", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
