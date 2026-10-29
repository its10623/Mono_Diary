package com.example.diaryapp.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diaryapp.domain.usecase.user.AutoLoginUseCase
import com.example.diaryapp.domain.usecase.user.LoginUseCase
import com.example.diaryapp.presentation.ui.event.LoginEvent
import com.example.diaryapp.presentation.ui.uiState.LoginUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val autoLoginUseCase: AutoLoginUseCase,
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<LoginEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    var uiState by mutableStateOf(LoginUiState())
        private set

    val autoLogin = autoLoginUseCase.getAutoLogin().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    fun loginWithGoogle(email: String, idToken: String) {
        viewModelScope.launch {
            try {
                uiState = uiState.copy(isLoading = true)

                loginUseCase.saveGoogleLogin(
                    email = email,
                    idToken = idToken
                )

                _uiEvent.emit(LoginEvent.LoginSuccess(email))
            } catch (e: Exception) {
                _uiEvent.emit(
                    LoginEvent.LoginFailed("구글 로그인에 실패했습니다.")
                )
            } finally {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }


    fun onIdChange(value: String) {
        uiState = uiState.copy(userName = value, idError = null)
    }

    fun onPwChange(value: String) {
        uiState = uiState.copy(password = value, pwError = null)
    }

    fun onAutoLoginChange(value: Boolean) {
        viewModelScope.launch {
            uiState = uiState.copy()
            autoLoginUseCase.setAutoLogin(value)
        }
    }

    suspend fun tryAutoLogin() {
        try {
            // 자동 로그인 가능 여부 판단은 domain(UseCase)에 맡긴다
            val loginKey = autoLoginUseCase.tryAutoLogin()
            if (loginKey != null) {
                _uiEvent.emit(LoginEvent.LoginSuccess(loginKey))
                return
            }

            // 자동 로그인 실패 시 로그인 화면 표시
            uiState = uiState.copy(isCheckingAutoLogin = false)
        } catch (e: Exception) {
            uiState = uiState.copy(isCheckingAutoLogin = false)
            _uiEvent.emit(LoginEvent.LoginFailed("자동 로그인 확인에 실패했습니다."))
        }
    }

    fun login() {
        val id = uiState.userName
        val pw = uiState.password

        if (id.isBlank()) {
            uiState = uiState.copy(idError = "아이디를 입력해주세요")
            return
        }
        if (id.length !in 4..20) {
            uiState = uiState.copy(idError = "아이디는 4~20자여야 합니다.")
            return
        }

        if (!id.all { it.isLetterOrDigit() }) {
            uiState = uiState.copy(idError = "특수문자는 사용할 수 없습니다.")
            return
        }

        if (pw.length < 8) {
            uiState = uiState.copy(pwError = "비밀번호는 8자리 이상이어야 합니다.")
            return
        }

        if (
            !pw.any { it.isDigit() } ||
            !pw.any { it.isLetter() } ||
            !pw.any { !it.isLetterOrDigit() }
        ) {
            uiState = uiState.copy(pwError = "영문 + 숫자 + 특수문자를 포함해야 합니다.")
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)

            val success = loginUseCase(id, pw)

            uiState = uiState.copy(isLoading = false)
            if (success) {
                autoLoginUseCase.saveLoginCredentials(id)

                _uiEvent.emit(LoginEvent.LoginSuccess(id))
            } else {
                _uiEvent.emit(LoginEvent.LoginFailed("아이디 또는 비밀번호가 올바르지 않습니다."))
            }
        }
    }

    suspend fun logout() {
        autoLoginUseCase.clearLoginState()
    }
}