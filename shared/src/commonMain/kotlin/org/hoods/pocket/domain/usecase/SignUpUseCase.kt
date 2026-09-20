package org.hoods.pocket.domain.usecase

import org.hoods.pocket.domain.repository.AuthRepository
import org.hoods.pocket.domain.repository.AuthResult

class SignUpUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): AuthResult {
        if (email.isBlank() || !email.contains("@")) {
            return AuthResult.Error("Invalid email address")
        }
        if (password.length < 6) {
            return AuthResult.Error("Password must be at least 6 characters")
        }
        return authRepository.signUp(email.trim(), password)
    }
}
