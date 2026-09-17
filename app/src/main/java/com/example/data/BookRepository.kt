package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.model.Book
import com.example.pdf.PdfEngine
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class BookRepository(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val TAG = "BookRepository"

    private var firebaseAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private var firebaseStorage: FirebaseStorage? = null

    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books.asStateFlow()

    private val _isAdminLoggedIn = MutableStateFlow(false)
    val isAdminLoggedIn: StateFlow<Boolean> = _isAdminLoggedIn.asStateFlow()

    private val _adminEmail = MutableStateFlow<String?>(null)
    val adminEmail: StateFlow<String?> = _adminEmail.asStateFlow()

    private val _isFirebaseConfigured = MutableStateFlow(false)
    val isFirebaseConfigured: StateFlow<Boolean> = _isFirebaseConfigured.asStateFlow()

    // Default curated books available out of the box
    private val defaultBooks = listOf(
        Book(
            id = "default_1",
            title = "The Art of Reading",
            author = "Arthur Quiller-Couch",
            coverUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&auto=format&fit=crop&q=80",
            pdfUrl = "", // Will be generated on demand
            createdAt = 1710000000000L
        ),
        Book(
            id = "default_2",
            title = "Pride and Prejudice",
            author = "Jane Austen",
            coverUrl = "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&auto=format&fit=crop&q=80",
            pdfUrl = "",
            createdAt = 1710001000000L
        ),
        Book(
            id = "default_3",
            title = "Frankenstein",
            author = "Mary Shelley",
            coverUrl = "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=600&auto=format&fit=crop&q=80",
            pdfUrl = "",
            createdAt = 1710002000000L
        ),
        Book(
            id = "default_4",
            title = "The Great Gatsby",
            author = "F. Scott Fitzgerald",
            coverUrl = "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=600&auto=format&fit=crop&q=80",
            pdfUrl = "",
            createdAt = 1710003000000L
        )
    )

    init {
        initializeFirebase()
        populateInitialBooks()
    }

    private fun initializeFirebase() {
        try {
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            } else {
                FirebaseApp.getInstance()
            }

            if (app != null) {
                firebaseAuth = FirebaseAuth.getInstance()
                firestore = FirebaseFirestore.getInstance()
                firebaseStorage = FirebaseStorage.getInstance()
                _isFirebaseConfigured.value = true

                // Check auth status
                val currentUser = firebaseAuth?.currentUser
                if (currentUser != null) {
                    _isAdminLoggedIn.value = true
                    _adminEmail.value = currentUser.email ?: "admin@library.com"
                }

                listenToFirestoreBooks()
                Log.d(TAG, "Firebase initialized successfully")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase not configured with google-services.json. Falling back to local mode.", e)
            _isFirebaseConfigured.value = false
        }
    }

    private fun populateInitialBooks() {
        // Prepare local copies of sample PDFs for default books
        scope.launch {
            val readyBooks = defaultBooks.map { book ->
                val sampleFile = PdfEngine.createSamplePdf(context, book.title, book.author)
                book.copy(pdfUrl = sampleFile.absolutePath)
            }
            if (_books.value.isEmpty()) {
                _books.value = readyBooks
            }
        }
    }

    private fun listenToFirestoreBooks() {
        val db = firestore ?: return
        try {
            db.collection("books")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Firestore listen failed", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val cloudBooks = snapshot.documents.mapNotNull { doc ->
                            val data = doc.data ?: return@mapNotNull null
                            Book.fromMap(doc.id, data)
                        }
                        if (cloudBooks.isNotEmpty()) {
                            // Merge with default books that have local PDFs
                            val existingDefaults = _books.value.filter { it.id.startsWith("default_") }
                            _books.value = cloudBooks + existingDefaults.filter { def ->
                                cloudBooks.none { it.id == def.id }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching Firestore snapshot listener", e)
        }
    }

    /**
     * Admin login with email and password (Firebase Auth)
     */
    suspend fun signInAdmin(email: String, pass: String): Result<String> = withContext(Dispatchers.IO) {
        val auth = firebaseAuth
        if (auth != null && _isFirebaseConfigured.value) {
            try {
                val authResult = auth.signInWithEmailAndPassword(email, pass).await()
                val userEmail = authResult.user?.email ?: email
                _isAdminLoggedIn.value = true
                _adminEmail.value = userEmail
                return@withContext Result.success(userEmail)
            } catch (e: Exception) {
                // If user not found, try to create user for ease of setup
                try {
                    val createResult = auth.createUserWithEmailAndPassword(email, pass).await()
                    val userEmail = createResult.user?.email ?: email
                    _isAdminLoggedIn.value = true
                    _adminEmail.value = userEmail
                    return@withContext Result.success(userEmail)
                } catch (createError: Exception) {
                    return@withContext Result.failure(createError)
                }
            }
        } else {
            // Local fallback login
            if (email.isNotBlank() && pass.length >= 6) {
                _isAdminLoggedIn.value = true
                _adminEmail.value = email
                return@withContext Result.success(email)
            } else {
                return@withContext Result.failure(Exception("Please enter a valid email and password (min 6 chars)"))
            }
        }
    }

    /**
     * Demo admin sign-in for testing
     */
    fun signInDemoAdmin() {
        _isAdminLoggedIn.value = true
        _adminEmail.value = "admin@bookreader.com"
    }

    /**
     * Sign out admin
     */
    fun signOutAdmin() {
        firebaseAuth?.signOut()
        _isAdminLoggedIn.value = false
        _adminEmail.value = null
    }

    /**
     * Upload a book with title, author, coverUrl and a PDF (Uri or File).
     * Uploads the PDF to Firebase Storage (or saves locally if offline),
     * and creates the document in Firestore 'books' collection.
     */
    suspend fun uploadBook(
        title: String,
        author: String,
        coverUrl: String,
        pdfUri: Uri?,
        pdfFile: File?,
        onProgress: (String) -> Unit = {}
    ): Result<Book> = withContext(Dispatchers.IO) {
        try {
            onProgress("Preparing document...")
            val bookId = "book_${System.currentTimeMillis()}"

            // 1. Resolve local file
            val sourceFile = when {
                pdfFile != null && pdfFile.exists() -> pdfFile
                pdfUri != null -> {
                    PdfEngine.copyUriToTempFile(context, pdfUri, "${title.replace(" ", "_")}.pdf")
                }
                else -> {
                    PdfEngine.createSamplePdf(context, title, author)
                }
            }

            var finalPdfUrl = sourceFile.absolutePath

            // 2. Upload to Firebase Storage if available
            val storage = firebaseStorage
            if (storage != null && _isFirebaseConfigured.value) {
                try {
                    onProgress("Uploading PDF to Firebase Storage...")
                    val storageRef = storage.reference.child("pdfs/${bookId}_${sourceFile.name}")
                    val uploadTask = storageRef.putFile(Uri.fromFile(sourceFile)).await()
                    val downloadUri = storageRef.downloadUrl.await()
                    finalPdfUrl = downloadUri.toString()
                    Log.d(TAG, "Uploaded to Firebase Storage: $finalPdfUrl")
                } catch (storageError: Exception) {
                    Log.w(TAG, "Firebase Storage upload failed, using local file: ${storageError.message}")
                    finalPdfUrl = sourceFile.absolutePath
                }
            }

            val finalCover = if (coverUrl.isBlank()) {
                "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&auto=format&fit=crop&q=80"
            } else {
                coverUrl
            }

            val newBook = Book(
                id = bookId,
                title = title,
                author = author,
                coverUrl = finalCover,
                pdfUrl = finalPdfUrl,
                createdAt = System.currentTimeMillis()
            )

            // 3. Save to Firestore 'books' collection if available
            val db = firestore
            if (db != null && _isFirebaseConfigured.value) {
                try {
                    onProgress("Saving book metadata to Firestore...")
                    db.collection("books").document(bookId).set(newBook.toMap()).await()
                    Log.d(TAG, "Book saved to Firestore collection 'books'")
                } catch (dbError: Exception) {
                    Log.w(TAG, "Firestore write failed: ${dbError.message}")
                }
            }

            // Also ensure it is present in local StateFlow
            _books.value = listOf(newBook) + _books.value.filter { it.id != newBook.id }

            onProgress("Complete")
            return@withContext Result.success(newBook)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload book", e)
            return@withContext Result.failure(e)
        }
    }

    fun getBookById(id: String): Book? {
        return _books.value.find { it.id == id }
    }
}
