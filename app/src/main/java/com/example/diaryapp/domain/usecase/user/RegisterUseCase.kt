package com.example.diaryapp.domain.usecase.user

import com.example.diaryapp.domain.repository.UserRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val repo: UserRepository
) {
    suspend operator fun invoke(userName: String, password: String, confirmPassword: String)
            : RegisterResult {

        val existingUser = repo.findUser(userName)

        val number = password.any { it.isDigit() }
        val letter = password.any { it.isLetter() }
        val special = password.any { !it.isLetterOrDigit() }

        if (userName.isBlank()) return RegisterResult.Fail("아이디를 입력하세요")
        if (userName.contains(" ")) return RegisterResult.Fail("공백을 포함할 수 없습니다")
        if (!userName.matches("^[a-zA-Z0-9]+$".toRegex())) return RegisterResult.Fail("아이디는 영문과 숫자만 입력해주세요")
        if (userName.length !in 4..20) return RegisterResult.Fail("아이디는 4자~20자 사이로 입력해주세요")
        if (existingUser != null) return RegisterResult.Fail("존재하는 아이디 입니다.")

        if (password.isBlank()) return RegisterResult.Fail("비밀번호를 입력하세요")
        if (password.length !in 8..16) return RegisterResult.Fail("비밀번호는 8자~16자 사이로 입력해주세요")
        if (password.contains(" ")) return RegisterResult.Fail("공백을 포함할 수 없습니다")
        if (!(number && letter && special)) return RegisterResult.Fail("비밀번호는 영문,숫자,특수문자를 포함해야 합니다")
        if (password != confirmPassword) return RegisterResult.Fail("비밀번호가 일치하지 않습니다")

        val success = repo.register(userName, password)
        return if (success) RegisterResult.Success else RegisterResult.Fail("이미 존재하는 아이디 입니다")
    }
}

// 회원가입 결과: domain 계층에 두어 presentation에 의존하지 않도록 한다
sealed class RegisterResult {
    object Success : RegisterResult()
    data class Fail(val message: String) : RegisterResult()
}
