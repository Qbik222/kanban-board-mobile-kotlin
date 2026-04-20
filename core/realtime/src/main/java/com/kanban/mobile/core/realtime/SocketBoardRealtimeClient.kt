package com.kanban.mobile.core.realtime

import com.kanban.mobile.core.network.NetworkConfig
import com.kanban.mobile.core.session.SessionRepository
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

@Singleton
class SocketBoardRealtimeClient @Inject constructor(
    private val networkConfig: NetworkConfig,
    private val sessionRepository: SessionRepository,
) : BoardRealtimeClient {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val joinMutex = Mutex()
    private val connectionLock = Any()

    private val _events = MutableSharedFlow<BoardRealtimeEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<BoardRealtimeEvent> = _events.asSharedFlow()

    @Volatile
    private var activeBoardId: String? = null

    private val lastJoinSignature = AtomicReference<String?>(null)
    private var socket: Socket? = null

    init {
        scope.launch {
            sessionRepository.accessTokenFlow.collect { token ->
                val id = activeBoardId ?: return@collect
                if (token.isNullOrBlank()) return@collect
                launch(Dispatchers.IO) {
                    joinMutex.withLock {
                        val s = socket ?: return@withLock
                        if (!s.connected()) return@withLock
                        emitJoinLocked(id, token)
                    }
                }
            }
        }
    }

    override fun connectIfNeeded() {
        synchronized(connectionLock) {
            if (socket?.connected() == true) return
            if (socket == null) {
                val url = networkConfig.apiBaseUrl.trimEnd('/')
                val opts = IO.Options().apply {
                    transports = arrayOf("websocket", "polling")
                    reconnection = true
                }
                val s = IO.socket(url, opts)
                socket = s
                wireHandlers(s)
                s.connect()
            } else {
                socket?.connect()
            }
        }
    }

    override fun joinBoard(boardId: String) {
        activeBoardId = boardId
        connectIfNeeded()
        scope.launch(Dispatchers.IO) {
            joinMutex.withLock {
                val t = sessionRepository.getAccessToken() ?: return@withLock
                val s = socket ?: return@withLock
                if (!s.connected()) return@withLock
                emitJoinLocked(boardId, t)
            }
        }
    }

    override fun leaveBoard() {
        activeBoardId = null
        lastJoinSignature.set(null)
    }

    override fun disconnect() {
        synchronized(connectionLock) {
            activeBoardId = null
            lastJoinSignature.set(null)
            socket?.let { s ->
                detachHandlers(s)
                s.disconnect()
            }
            socket = null
        }
    }

    private fun wireHandlers(s: Socket) {
        s.on(Socket.EVENT_CONNECT) {
            _events.tryEmit(BoardRealtimeEvent.SocketConnected)
            scope.launch(Dispatchers.IO) {
                joinMutex.withLock {
                    lastJoinSignature.set(null)
                    val id = activeBoardId ?: return@withLock
                    val t = sessionRepository.getAccessToken() ?: return@withLock
                    emitJoinLocked(id, t)
                }
            }
        }
        s.on(Socket.EVENT_DISCONNECT) {
            _events.tryEmit(BoardRealtimeEvent.SocketDisconnected)
        }
        s.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val msg = (args?.getOrNull(0) as? Throwable)?.message
                ?: args?.firstOrNull()?.toString()
                ?: "connect_error"
            _events.tryEmit(BoardRealtimeEvent.SocketConnectError(msg))
        }
        s.io().on(Manager.EVENT_RECONNECT) {
            scope.launch(Dispatchers.IO) {
                joinMutex.withLock {
                    lastJoinSignature.set(null)
                    val id = activeBoardId ?: return@withLock
                    val t = sessionRepository.getAccessToken() ?: return@withLock
                    emitJoinLocked(id, t)
                }
            }
        }

        s.on("board:joined") { args ->
            _events.tryEmit(BoardRealtimeEvent.BoardJoined(firstArgToJson(args)))
        }
        s.on("board:join_error") { args ->
            val msg = joinErrorMessage(args)
            _events.tryEmit(BoardRealtimeEvent.BoardJoinError(msg))
        }
        s.on("board:updated") { args ->
            firstArgToJson(args)?.let { json ->
                _events.tryEmit(BoardRealtimeEvent.BoardUpdated(json))
            }
        }
        s.on("board:deleted") {
            _events.tryEmit(BoardRealtimeEvent.BoardDeleted)
        }
        s.on("columns:updated") { args ->
            firstArgToJson(args)?.let { json ->
                _events.tryEmit(BoardRealtimeEvent.ColumnsUpdated(json))
            }
        }
        s.on("card:created") { args ->
            firstArgToJson(args)?.let { json ->
                _events.tryEmit(BoardRealtimeEvent.CardCreated(json))
            }
        }
        s.on("card:updated") { args ->
            firstArgToJson(args)?.let { json ->
                _events.tryEmit(BoardRealtimeEvent.CardUpdated(json))
            }
        }
        s.on("card:moved") { args ->
            firstArgToJson(args)?.let { json ->
                _events.tryEmit(BoardRealtimeEvent.CardMoved(json))
            }
        }
        s.on("comment:added") { args ->
            firstArgToJson(args)?.let { json ->
                _events.tryEmit(BoardRealtimeEvent.CommentAdded(json))
            }
        }
    }

    private fun detachHandlers(s: Socket) {
        s.off(Socket.EVENT_CONNECT)
        s.off(Socket.EVENT_DISCONNECT)
        s.off(Socket.EVENT_CONNECT_ERROR)
        s.io().off(Manager.EVENT_RECONNECT)
        s.off("board:joined")
        s.off("board:join_error")
        s.off("board:updated")
        s.off("board:deleted")
        s.off("columns:updated")
        s.off("card:created")
        s.off("card:updated")
        s.off("card:moved")
        s.off("comment:added")
    }

    private suspend fun emitJoinLocked(boardId: String, token: String) {
        val s = socket ?: return
        val normalized = normalizeAccessToken(token)
        val sig = "$boardId|$normalized"
        if (lastJoinSignature.get() == sig && s.connected()) return
        val payload = JSONObject().apply {
            put("boardId", boardId)
            put("token", normalized)
        }
        s.emit("joinBoard", payload)
        lastJoinSignature.set(sig)
    }

    private fun firstArgToJson(args: Array<out Any>?): String? {
        if (args.isNullOrEmpty()) return null
        return when (val a = args[0]) {
            is JSONObject -> a.toString()
            is org.json.JSONArray -> a.toString()
            is String -> a
            else -> a.toString()
        }
    }

    private fun joinErrorMessage(args: Array<out Any>?): String {
        if (args.isNullOrEmpty()) return "join_error"
        return when (val a = args[0]) {
            is String -> a
            is JSONObject -> a.optString("message", a.toString())
            else -> a.toString()
        }
    }

    private fun normalizeAccessToken(token: String): String {
        val t = token.trim()
        return if (t.startsWith("Bearer ", ignoreCase = true)) {
            t.substring(7).trim()
        } else {
            t
        }
    }
}
