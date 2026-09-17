package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BookRepository
import com.example.model.Book
import com.example.model.ReaderTheme
import com.example.pdf.PdfEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed class Screen {
    object Library : Screen()
    object Admin : Screen() // /admin route
    data class Reader(val bookId: String) : Screen()
}

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    val repository = BookRepository(application)

    // Current navigation state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Library)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Search query in library
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filtered books
    val filteredBooks: StateFlow<List<Book>> = combine(
        repository.books,
        _searchQuery
    ) { allBooks, query ->
        if (query.isBlank()) {
            allBooks
        } else {
            allBooks.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.author.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isAdminLoggedIn: StateFlow<Boolean> = repository.isAdminLoggedIn
    val adminEmail: StateFlow<String?> = repository.adminEmail
    val isFirebaseConfigured: StateFlow<Boolean> = repository.isFirebaseConfigured

    // Reader state
    private val _activeBook = MutableStateFlow<Book?>(null)
    val activeBook: StateFlow<Book?> = _activeBook.asStateFlow()

    private val _readerTheme = MutableStateFlow(ReaderTheme.PAPER_LIGHT)
    val readerTheme: StateFlow<ReaderTheme> = _readerTheme.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    private val _pageBitmap = MutableStateFlow<Bitmap?>(null)
    val pageBitmap: StateFlow<Bitmap?> = _pageBitmap.asStateFlow()

    private val _isReaderLoading = MutableStateFlow(false)
    val isReaderLoading: StateFlow<Boolean> = _isReaderLoading.asStateFlow()

    private var currentPdfFile: File? = null

    // Admin Upload Form state
    val uploadTitle = MutableStateFlow("")
    val uploadAuthor = MutableStateFlow("")
    val uploadCoverUrl = MutableStateFlow("")
    val selectedPdfUri = MutableStateFlow<Uri?>(null)
    val selectedPdfFile = MutableStateFlow<File?>(null)
    val selectedPdfName = MutableStateFlow<String?>(null)

    val isUploading = MutableStateFlow(false)
    val uploadProgressMessage = MutableStateFlow<String?>(null)
    val showUploadSuccessAlert = MutableStateFlow(false)
    val uploadErrorMessage = MutableStateFlow<String?>(null)

    // Admin Login state
    val adminLoginEmail = MutableStateFlow("")
    val adminLoginPassword = MutableStateFlow("")
    val isLoggingIn = MutableStateFlow(false)
    val loginErrorMessage = MutableStateFlow<String?>(null)

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    // Reader Actions
    fun openReaderForBook(book: Book) {
        _activeBook.value = book
        _currentPage.value = 0
        _currentScreen.value = Screen.Reader(book.id)
        loadPdfDocument(book)
    }

    private fun loadPdfDocument(book: Book) {
        viewModelScope.launch {
            _isReaderLoading.value = true
            _pageBitmap.value = null
            try {
                val file = PdfEngine.resolvePdfFile(getApplication(), book.pdfUrl.ifBlank {
                    // Fallback to sample PDF
                    PdfEngine.createSamplePdf(getApplication(), book.title, book.author).absolutePath
                })
                currentPdfFile = file
                val count = PdfEngine.getPageCount(file)
                _totalPages.value = if (count > 0) count else 1
                loadCurrentPageBitmap()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isReaderLoading.value = false
            }
        }
    }

    fun setReaderTheme(theme: ReaderTheme) {
        _readerTheme.value = theme
    }

    fun nextPage() {
        if (_currentPage.value < _totalPages.value - 1) {
            _currentPage.value += 1
            loadCurrentPageBitmap()
        }
    }

    fun prevPage() {
        if (_currentPage.value > 0) {
            _currentPage.value -= 1
            loadCurrentPageBitmap()
        }
    }

    fun jumpToPage(pageIndex: Int) {
        val target = pageIndex.coerceIn(0, (_totalPages.value - 1).coerceAtLeast(0))
        if (_currentPage.value != target) {
            _currentPage.value = target
            loadCurrentPageBitmap()
        }
    }

    private fun loadCurrentPageBitmap() {
        val file = currentPdfFile ?: return
        viewModelScope.launch {
            _isReaderLoading.value = true
            val bitmap = PdfEngine.renderPage(file, _currentPage.value)
            _pageBitmap.value = bitmap
            _isReaderLoading.value = false
        }
    }

    // Admin Auth Actions
    fun signInAdmin() {
        val email = adminLoginEmail.value.trim()
        val pass = adminLoginPassword.value.trim()
        if (email.isEmpty() || pass.isEmpty()) {
            loginErrorMessage.value = "Please enter both email and password"
            return
        }

        viewModelScope.launch {
            isLoggingIn.value = true
            loginErrorMessage.value = null
            val result = repository.signInAdmin(email, pass)
            result.onSuccess {
                loginErrorMessage.value = null
            }.onFailure { err ->
                loginErrorMessage.value = err.message ?: "Authentication failed"
            }
            isLoggingIn.value = false
        }
    }

    fun signInDemoAdmin() {
        repository.signInDemoAdmin()
    }

    fun signOutAdmin() {
        repository.signOutAdmin()
    }

    // Admin Book Upload Actions
    fun onPdfSelected(uri: Uri?, name: String?) {
        selectedPdfUri.value = uri
        selectedPdfFile.value = null
        selectedPdfName.value = name ?: "Selected document.pdf"
    }

    fun generateAndSelectTestPdf() {
        val testTitle = uploadTitle.value.ifBlank { "Sample Modern PDF" }
        val testAuthor = uploadAuthor.value.ifBlank { "Demo Author" }
        val testPdf = PdfEngine.createSamplePdf(getApplication(), testTitle, testAuthor)
        selectedPdfFile.value = testPdf
        selectedPdfUri.value = null
        selectedPdfName.value = "${testPdf.name} (${testPdf.length() / 1024} KB)"
    }

    fun uploadBook() {
        val title = uploadTitle.value.trim()
        val author = uploadAuthor.value.trim()

        if (title.isEmpty()) {
            uploadErrorMessage.value = "Please enter a book title"
            return
        }
        if (author.isEmpty()) {
            uploadErrorMessage.value = "Please enter an author name"
            return
        }

        viewModelScope.launch {
            isUploading.value = true
            uploadErrorMessage.value = null
            uploadProgressMessage.value = "Starting upload..."

            val result = repository.uploadBook(
                title = title,
                author = author,
                coverUrl = uploadCoverUrl.value.trim(),
                pdfUri = selectedPdfUri.value,
                pdfFile = selectedPdfFile.value,
                onProgress = { msg ->
                    uploadProgressMessage.value = msg
                }
            )

            result.onSuccess {
                // Clear fields
                uploadTitle.value = ""
                uploadAuthor.value = ""
                uploadCoverUrl.value = ""
                selectedPdfUri.value = null
                selectedPdfFile.value = null
                selectedPdfName.value = null
                uploadProgressMessage.value = null

                // Show Prompt 4 required alert: 'Book uploaded!'
                showUploadSuccessAlert.value = true
            }.onFailure { err ->
                uploadErrorMessage.value = err.message ?: "Upload failed"
            }
            isUploading.value = false
        }
    }

    fun dismissSuccessAlert() {
        showUploadSuccessAlert.value = false
    }
}
