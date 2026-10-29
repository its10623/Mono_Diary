package com.example.diaryapp.domain.usecase.user

import com.example.diaryapp.domain.repository.UserRepository
import javax.inject.Inject

class FindPasswordUseCase @Inject constructor(
    private val repo: UserRepository
) {
    // 비밀번호 찾기 1단계: 입력한 아이디가 가입된 계정인지 확인
    suspend fun isRegistered(userName: String): Boolean =
        repo.findUser(userName) != null

    suspend operator fun invoke(
        userName: String,
        newPassword: String,
        confirmPassword: String
    ): ResetPasswordResult {

        val user = repo.findUser(userName)
            ?: return ResetPasswordResult.Fail("존재하지 않는 아이디입니다.")

        val number = newPassword.any { it.isDigit() }
        val letter = newPassword.any { it.isLetter() }
        val special = newPassword.any { !it.isLetterOrDigit() }

        if (newPassword.length !in 8..16)
            return ResetPasswordResult.Fail("비밀번호는 8~16자여야 합니다.")

        if (!(number && letter && special))
            return ResetPasswordResult.Fail("영문, 숫자, 특수문자를 모두 포함하세요.")

        if (newPassword != confirmPassword)
            return ResetPasswordResult.Fail("비밀번호가 일치하지 않습니다.")

        val success = repo.updatePassword(userName, newPassword)

        return if (success) ResetPasswordResult.Success
        else ResetPasswordResult.Fail("비밀번호 변경 실패")
    }
}

sealed class ResetPasswordResult {
    object Success : ResetPasswordResult()
    data class Fail(val message: String) : ResetPasswordResult()
}
