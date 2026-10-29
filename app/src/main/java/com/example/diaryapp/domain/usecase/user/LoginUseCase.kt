package com.example.diaryapp.domain.usecase.user

import com.example.diaryapp.domain.repository.UserRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repo: UserRepository
) {
    suspend operator fun invoke(userName: String, password: String): Boolean {

        if (userName.isBlank()) return false
        if (password.isBlank()) return false

        return repo.login(userName, password)
    }

    // Google 로그인: Credential Manager로 받은 계정 정보를 로그인 상태로 저장
    suspend fun saveGoogleLogin(email: String, idToken: String) =
        repo.saveGoogleLoginInfo(email = email, idToken = idToken)
}