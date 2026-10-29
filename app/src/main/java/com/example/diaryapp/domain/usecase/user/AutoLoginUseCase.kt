package com.example.diaryapp.domain.usecase.user

import com.example.diaryapp.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AutoLoginUseCase @Inject constructor(
    private val repo: UserRepository
) {
    fun getAutoLogin(): Flow<Boolean> = repo.getAutoLogin()
    suspend fun setAutoLogin(value: Boolean) = repo.setAutoLogin(value)

    fun getSavedUserId(): Flow<String?> = repo.getSavedUserId()
    suspend fun saveUserId(id: String?) = repo.saveUserId(id)

    fun getSavedPasswordHash(): Flow<String?> = repo.getSavedPasswordHash()
    suspend fun savePasswordHash(hash: String?) = repo.savePasswordHash(hash)

    // 저장된 로그인 정보로 자동 로그인할 수 있으면 로그인 키(Google은 이메일, 일반은 아이디)를, 아니면 null을 돌려준다
    suspend fun tryAutoLogin(): String? {
        val isLoggedIn = repo.getLoginStatus().first()
        val loginType = repo.getLoginType().first()
        val email = repo.getUserEmail().first()

        if (isLoggedIn && loginType == "GOOGLE" && !email.isNullOrBlank()) return email

        val id = repo.getSavedUserId().first()
        val hash = repo.getSavedPasswordHash().first()

        if (!id.isNullOrBlank() && !hash.isNullOrBlank()) {
            val user = repo.findUser(id)
            if (user != null && user.password == hash) return id
        }
        return null
    }

    // 일반 로그인 성공 후 다음 실행 때 자동 로그인할 수 있도록 아이디와 비밀번호 해시를 저장
    suspend fun saveLoginCredentials(userName: String) {
        val user = repo.findUser(userName) ?: return
        repo.saveUserId(userName)
        repo.savePasswordHash(user.password)
    }

    // 로그아웃: 자동 로그인 설정과 저장된 로그인 정보를 모두 지운다
    suspend fun clearLoginState() {
        repo.setAutoLogin(false)
        repo.saveUserId(null)
        repo.savePasswordHash(null)
        repo.clearLoginInfo()
    }
}
