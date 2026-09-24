package org.hoods.pocket.data.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.hoods.pocket.data.local.dao.SyncOutboxDao
import org.hoods.pocket.data.remote.SupabaseConfig
import org.hoods.pocket.domain.model.AppSyncState
import org.hoods.pocket.domain.model.AuthState
import org.hoods.pocket.domain.repository.AuthRepository
import org.hoods.pocket.domain.service.SyncCoordinator
import kotlin.time.Clock

class PocketSyncCoordinator(
    private val syncEngine: SyncEngine,
    private val authRepository: AuthRepository,
    private val syncOutboxDao: SyncOutboxDao,
    private val config: SupabaseConfig,
    private val connectivityObserver: org.hoods.pocket.domain.service.ConnectivityObserver? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : SyncCoordinator {

    private val _syncState = MutableStateFlow<AppSyncState>(AppSyncState.Idle)
    override val syncState: StateFlow<AppSyncState> = _syncState.asStateFlow()

    private val _pendingCount = MutableStateFlow(0)
    override val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val syncMutex = Mutex()
    private var pendingReSync = false
    private var userObservationJob: Job? = null

    init {
        coroutineScope.launch {
            authRepository.authState.collect { authState ->
                userObservationJob?.cancel()
                userObservationJob = null
                when (authState) {
                    is AuthState.Authenticated -> {
                        if (config.isConfigured) {
                            userObservationJob = coroutineScope.launch {
                                // 1. Initial sync attempt on sign-in
                                launch {
                                    performSync(authState.user.id, isInitial = true)
                                }
                                // 2. Reactive observation of outbox mutations
                                syncOutboxDao.observePendingMutationCount(authState.user.id)
                                    .collect { pendingCount ->
                                        _pendingCount.value = pendingCount
                                        if (pendingCount > 0) {
                                            if (_syncState.value !is AppSyncState.Syncing) {
                                                _syncState.value = AppSyncState.Pending(pendingCount)
                                            }
                                            performSync(authState.user.id, isInitial = false)
                                        }
                                    }
                            }
                        } else {
                            _pendingCount.value = 0
                            _syncState.value = AppSyncState.Idle
                        }
                    }
                    is AuthState.Unauthenticated -> {
                        _pendingCount.value = 0
                        _syncState.value = AppSyncState.Idle
                    }
                }
            }
        }

        connectivityObserver?.let { observer ->
            coroutineScope.launch {
                var wasOnline = observer.isOnline.value
                observer.isOnline.collect { isOnline ->
                    if (!wasOnline && isOnline) {
                        val currentAuth = authRepository.authState.value
                        if (currentAuth is AuthState.Authenticated && config.isConfigured) {
                            if (syncOutboxDao.getPendingMutationCount(currentAuth.user.id) > 0) {
                                performSync(currentAuth.user.id, isInitial = false)
                            }
                        }
                    }
                    wasOnline = isOnline
                }
            }
        }
    }

    override fun requestSync(): Job {
        val authState = authRepository.authState.value
        return if (authState is AuthState.Authenticated && config.isConfigured) {
            coroutineScope.launch {
                performSync(authState.user.id, isInitial = false)
            }
        } else {
            Job().apply { complete() }
        }
    }

    private suspend fun performSync(userId: String, isInitial: Boolean) {
        if (!config.isConfigured) return

        syncMutex.withLock {
            val currentAuth = authRepository.authState.value
            if (currentAuth !is AuthState.Authenticated || currentAuth.user.id != userId) {
                _syncState.value = AppSyncState.Idle
                return@withLock
            }

            val pendingCount = syncOutboxDao.getPendingMutationCount(userId)
            if (!isInitial && pendingCount == 0) {
                return@withLock
            }

            if (connectivityObserver?.isOnline?.value == false) {
                _syncState.value = AppSyncState.Error(
                    message = "Device is offline",
                    pendingCount = pendingCount,
                    canRetry = true
                )
                return@withLock
            }

            _syncState.value = AppSyncState.Syncing(pendingCount)
            try {
                when (val result = syncEngine.synchronize(userId)) {
                    is SyncResult.Success -> {
                        if (authRepository.authState.value is AuthState.Authenticated) {
                            val now = Clock.System.now().toEpochMilliseconds()
                            _syncState.value = AppSyncState.Success(
                                lastSyncedAtEpochMillis = now,
                                pushedCount = result.pushedCount,
                                pulledCount = result.pulledCount
                            )
                        } else {
                            _syncState.value = AppSyncState.Idle
                        }
                    }
                    is SyncResult.Failure -> {
                        if (authRepository.authState.value is AuthState.Authenticated) {
                            val remaining = syncOutboxDao.getPendingMutationCount(userId)
                            _syncState.value = AppSyncState.Error(
                                message = result.message,
                                pendingCount = remaining,
                                canRetry = result.canRetry
                            )
                        } else {
                            _syncState.value = AppSyncState.Idle
                        }
                    }
                    is SyncResult.Unavailable -> {
                        _syncState.value = AppSyncState.Idle
                    }
                }
            } finally {
                if (authRepository.authState.value !is AuthState.Authenticated) {
                    _syncState.value = AppSyncState.Idle
                }
            }
        }
    }
}
