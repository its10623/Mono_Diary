package com.example.diaryapp.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diaryapp.domain.usecase.user.RegisterResult
import com.example.diaryapp.domain.usecase.user.RegisterUseCase
import com.example.diaryapp.presentation.ui.event.RegisterEvent
import com.example.diaryapp.presentation.ui.uiState.RegisterUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    var uiState by mutableStateOf(RegisterUiState())
        private set

    private val _event = MutableSharedFlow<RegisterEvent>()
    val event = _event.asSharedFlow()

    fun onIdChange(value: String) {
        uiState = uiState.copy(id = value, idError = null)
    }

    fun onPwChange(value: String) {
        uiState = uiState.copy(pw = value, pwError = null)
    }

    fun onConfirmPwChange(value: String) {
        uiState = uiState.copy(confirmPw = value, confirmPwError = null)
    }

    fun register() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)

            val result = registerUseCase(
                uiState.id,
                uiState.pw,
                uiState.confirmPw
            )

            uiState = uiState.copy(isLoading = false)

            when (result) {

                is RegisterResult.Success -> {
                    uiState = uiState.copy(isLoading = false)
                    _event.emit(RegisterEvent.Success)
                }

                is RegisterResult.Fail -> {
                    uiState = uiState.copy(isLoading = false)

                    applyFieldError(result.message)

                    _event.emit(RegisterEvent.Fail(result.message))
                }
            }
        }
    }

    private fun applyFieldError(msg: String) {
        when {
            "아이디" in msg -> uiState = uiState.copy(idError = msg)
            "비밀번호" in msg && "확인" !in msg -> uiState = uiState.copy(pwError = msg)
            "일치" in msg -> uiState = uiState.copy(confirmPwError = msg)
        }
    }
}
