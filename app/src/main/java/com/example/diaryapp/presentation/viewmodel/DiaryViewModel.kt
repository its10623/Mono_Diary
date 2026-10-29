package com.example.diaryapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diaryapp.domain.usecase.diary.DiaryUseCase
import com.example.diaryapp.domain.model.Diary
import com.example.diaryapp.presentation.ui.component.timeline.SortType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val diaryUseCase: DiaryUseCase
) : ViewModel() {

    // UI 상태
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _writeSuccess = MutableSharedFlow<Boolean>()
    val writeSuccess = _writeSuccess.asSharedFlow()

    private val _updateSuccess = MutableSharedFlow<Boolean>()
    val updateSuccess = _updateSuccess.asSharedFlow()

    private val _deleteSuccess = MutableSharedFlow<Boolean>()
    val deleteSuccess = _deleteSuccess.asSharedFlow()

    // 타임라인 리스트 상태
    // load*()마다 collect를 새로 시작하면 이전 구독이 남아 서로 덮어쓰므로,
    // "지금 무엇을 볼지"를 상태 하나로 두고 flatMapLatest로 이전 구독을 취소하며 전환한다
    private val timelineSource = MutableStateFlow<TimelineSource?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val diaryList: StateFlow<List<Diary>> = timelineSource
        .filterNotNull()
        .flatMapLatest { source ->
            when (source) {
                is TimelineSource.All -> diaryUseCase.getDiariesByUser(source.userName)
                is TimelineSource.Favorites -> diaryUseCase.getFavoriteDiaries(source.userName)
                is TimelineSource.Search -> diaryUseCase.searchTimeline(source.userName, source.keyword)
                is TimelineSource.DateRange ->
                    diaryUseCase.filterByDate(source.userName, source.start, source.end)
            }
                // 안쪽에서 잡아야 오류가 나도 바깥 스트림이 끝나지 않고 다음 소스로 계속 전환된다
                .catch { e -> _errorMessage.value = e.message }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // 폴더 화면 리스트 상태 (타임라인과 같은 방식: 입력 상태 + flatMapLatest)
    private val folderSource = MutableStateFlow<FolderSource?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val folderDiaryList: StateFlow<List<Diary>> = folderSource
        .filterNotNull()
        .flatMapLatest { source ->
            when (source) {
                is FolderSource.All ->
                    diaryUseCase.getDiariesByFolder(source.userName, source.folder)
                is FolderSource.Search ->
                    diaryUseCase.searchInFolder(source.userName, source.folder, source.keyword)
            }
                .catch { e -> _errorMessage.value = e.message }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _timelineSort = MutableStateFlow(SortType.RECENT)
    val timelineSortType = _timelineSort.asStateFlow()

    private val _timelineDates = MutableStateFlow<Set<LocalDate>>(emptySet())
    val timelineDates = _timelineDates.asStateFlow()

    private val _folderSort = MutableStateFlow(SortType.RECENT)
    val folderSortType = _folderSort.asStateFlow()

    private val _folderDates = MutableStateFlow<Set<LocalDate>>(emptySet())
    val folderDates = _folderDates.asStateFlow()

    // 상세·수정 화면에서 보고 있는 일기 id (null이면 선택 해제)
    private val selectedId = MutableStateFlow<Int?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedDiary: StateFlow<Diary?> = selectedId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else diaryUseCase.getDiaryById(id).catch { e -> _errorMessage.value = e.message }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _activeFilter = MutableStateFlow(false)
    val activeFilter = _activeFilter.asStateFlow()

    private val _pendingDeleteId = MutableStateFlow<Int?>(null)
    val pendingDeleteId: StateFlow<Int?> = _pendingDeleteId

    fun prepareDelete(id: Int) {
        _pendingDeleteId.value = id
    }

    fun clearPendingDelete() {
        _pendingDeleteId.value = null
    }

    // 어떤 화면에서 필터를 사용하는지 저장
    private val _currentFilterScope = MutableStateFlow(FilterScope.TIMELINE)
    val currentFilterScope = _currentFilterScope.asStateFlow()

    // 타임라인 스크린 적용 필터
    val filteredDiaryList: StateFlow<List<Diary>> =
        combine(
            diaryList,
            _timelineSort,
            _timelineDates
        ) { diaries, sort, dates ->
            diaries
                .filter { diary ->
                    if (dates.isEmpty()) true
                    else {
                        val diaryDate = Instant.ofEpochMilli(diary.createDate.time)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()

                        diaryDate in dates
                    }
                }
                .let { list ->
                    when (sort) {
                        SortType.NONE -> list
                        SortType.RECENT -> list.sortedByDescending {
                            it.createDate.time
                        }

                        SortType.OLD -> list.sortedBy {
                            it.createDate.time
                        }
                    }
                }
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

    // 폴더 스크린 적용 필터
    val filteredFolderList: StateFlow<List<Diary>> =
        combine(
            folderDiaryList,
            _folderSort,
            _folderDates
        ) { diaries, sort, dates ->
            diaries
                .filter { diary ->
                    if (dates.isEmpty()) true
                    else {
                        val diaryDate = Instant.ofEpochMilli(diary.createDate.time)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        diaryDate in dates
                    }
                }
                .let { list ->
                    when (sort) {
                        SortType.NONE -> list
                        SortType.RECENT -> list.sortedByDescending { it.createDate.time }
                        SortType.OLD -> list.sortedBy { it.createDate.time }
                    }
                }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun loadDiary(id: Int) {
        selectedId.value = id
    }

    // 드로어 폴더 목록: 사용자 이름을 입력 상태로 두고 구독을 하나로 유지
    private val folderUser = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val folders: StateFlow<List<String>> = folderUser
        .filterNotNull()
        .flatMapLatest { userName ->
            diaryUseCase.getFolders(userName).catch { e -> _errorMessage.value = e.message }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun loadFolders(userName: String) {
        folderUser.value = userName
    }

    fun loadUserDiaries(userName: String) {
        timelineSource.value = TimelineSource.All(userName)
    }

    fun loadFolderDiaries(userName: String, folder: String) {
        folderSource.value = FolderSource.All(userName, folder)
    }

    fun loadFavoriteDiaries(userName: String) {
        timelineSource.value = TimelineSource.Favorites(userName)
    }

    // 아래 쓰기 함수들은 목록을 다시 불러오지 않는다: 구독 중인 Room Flow가 변경을 자동으로 다시 방출한다
    fun toggleFavoriteStatus(id: Int, isFavorite: Boolean, userName: String) {
        viewModelScope.launch {
            try {
                diaryUseCase.toggleFavoriteStatus(id, isFavorite)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun searchInFolder(user: String, folder: String, keyword: String) {
        folderSource.value = FolderSource.Search(user, folder, keyword)
    }

    fun searchTimeline(userName: String, keyword: String) {
        timelineSource.value = TimelineSource.Search(userName, keyword)
    }

    fun filterDate(userName: String, start: Long, end: Long) {
        timelineSource.value = TimelineSource.DateRange(userName, start, end)
    }

    fun writeDiary(diary: Diary) {
        viewModelScope.launch {
            try {
                diaryUseCase.writeDiary(diary)
                _writeSuccess.emit(true)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun writeDiary(
        userName: String,
        folder: String?,
        title: String,
        content: String
    ) {
        viewModelScope.launch {
            val now = java.util.Date()
            val diary = Diary(
                userName = userName,
                folder = folder,
                title = title,
                content = content,
                createDate = now,
                updateDate = now
            )
            writeDiary(diary)
        }
    }

    fun updateDiary(diary: Diary) {
        viewModelScope.launch {
            try {
                diaryUseCase.updateDiary(diary)
                _updateSuccess.emit(true)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun deleteDiary(id: Int, userName: String) {
        viewModelScope.launch {
            try {
                diaryUseCase.deleteDiary(id)
                selectedId.value = null
                _deleteSuccess.emit(true)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun addFolder(userName: String, folderName: String) {
        viewModelScope.launch {
            try {
                diaryUseCase.addFolder(userName, folderName)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun deleteFolder(userName: String, folderName: String) {
        viewModelScope.launch {
            try {
                diaryUseCase.deleteFolder(userName, folderName)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun renameFolder(userName: String, oldName: String, newName: String) {
        viewModelScope.launch {
            try {
                diaryUseCase.renameFolder(userName, oldName, newName)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun setFilterScope(scope: FilterScope) {
        _currentFilterScope.value = scope
    }

    //필터 적용
    fun applyFilter(sort: SortType, dates: Set<LocalDate>) {
        when (_currentFilterScope.value) {
            FilterScope.TIMELINE -> {
                _timelineSort.value = sort
                _timelineDates.value = dates
            }

            FilterScope.FOLDER -> {
                _folderSort.value = sort
                _folderDates.value = dates
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearSelected() {
        selectedId.value = null
    }
}

enum class FilterScope { TIMELINE, FOLDER }

// 타임라인(전체·즐겨찾기 화면 공용)에 무엇을 보여줄지 나타내는 입력 상태
private sealed interface TimelineSource {
    val userName: String

    data class All(override val userName: String) : TimelineSource
    data class Favorites(override val userName: String) : TimelineSource
    data class Search(override val userName: String, val keyword: String) : TimelineSource
    data class DateRange(override val userName: String, val start: Long, val end: Long) : TimelineSource
}

// 폴더 화면에 무엇을 보여줄지 나타내는 입력 상태
private sealed interface FolderSource {
    val userName: String
    val folder: String

    data class All(override val userName: String, override val folder: String) : FolderSource
    data class Search(
        override val userName: String,
        override val folder: String,
        val keyword: String
    ) : FolderSource
}