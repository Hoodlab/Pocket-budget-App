package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import org.hoods.pocket.domain.model.AuthState
import org.hoods.pocket.domain.repository.AuthRepository

class GetAuthStateUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): StateFlow<AuthState> {
        return authRepository.authState
    }
}
