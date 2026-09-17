package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: LibraryViewModel,
    onNavigateBack: () -> Unit
) {
    val isAdmin by viewModel.isAdminLoggedIn.collectAsState()
    val adminEmail by viewModel.adminEmail.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadProgress by viewModel.uploadProgressMessage.collectAsState()
    val showSuccessAlert by viewModel.showUploadSuccessAlert.collectAsState()
    val uploadError by viewModel.uploadErrorMessage.collectAsState()

    val title by viewModel.uploadTitle.collectAsState()
    val author by viewModel.uploadAuthor.collectAsState()
    val coverUrl by viewModel.uploadCoverUrl.collectAsState()
    val selectedPdfName by viewModel.selectedPdfName.collectAsState()

    // Activity Result Launcher for selecting PDF documents
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "Document.pdf"
            viewModel.onPdfSelected(uri, fileName)
        }
    }

    // Prompt 4 Requirement: Add alert 'Book uploaded!' on successful admin upload
    if (showSuccessAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSuccessAlert() },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Upload Complete",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("upload_success_title")
                )
            },
            text = {
                Text(
                    text = "Book uploaded!",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("upload_success_message")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissSuccessAlert()
                        onNavigateBack() // Return to Library to view uploaded book
                    },
                    modifier = Modifier.testTag("upload_success_ok_button")
                ) {
                    Text("View in Library")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissSuccessAlert() }
                ) {
                    Text("Upload Another")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Admin Portal",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "/admin",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("admin_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Library"
                        )
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(
                            onClick = { viewModel.signOutAdmin() },
                            modifier = Modifier.testTag("admin_logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Sign Out"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isAdmin) {
                // Prompt 2: Protect /admin route with Firebase Authentication (admin only)
                AdminLoginView(viewModel = viewModel)
            } else {
                // Prompt 1 & 4: Admin PDF Upload Screen
                AdminUploadView(
                    viewModel = viewModel,
                    adminEmail = adminEmail,
                    title = title,
                    author = author,
                    coverUrl = coverUrl,
                    selectedPdfName = selectedPdfName,
                    isUploading = isUploading,
                    uploadProgress = uploadProgress,
                    uploadError = uploadError,
                    onPickPdf = {
                        pdfPickerLauncher.launch(arrayOf("application/pdf"))
                    }
                )
            }
        }
    }
}

@Composable
fun AdminLoginView(viewModel: LibraryViewModel) {
    var email by remember { mutableStateOf("admin@library.com") }
    var password by remember { mutableStateOf("admin123") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isLoggingIn by viewModel.isLoggingIn.collectAsState()
    val loginError by viewModel.loginErrorMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = "Admin lock",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Administrator Login",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Regular readers need no login to browse and read. Sign in to manage and upload PDF documents to Firebase.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        viewModel.adminLoginEmail.value = it
                    },
                    label = { Text("Admin Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_email_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        viewModel.adminLoginPassword.value = it
                    },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_password_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (loginError != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = loginError ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.adminLoginEmail.value = email
                        viewModel.adminLoginPassword.value = password
                        viewModel.signInAdmin()
                    },
                    enabled = !isLoggingIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("admin_login_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoggingIn) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign In as Admin", fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider()

                OutlinedButton(
                    onClick = { viewModel.signInDemoAdmin() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_demo_login_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Quick Demo Admin Sign-In")
                }
            }
        }
    }
}

@Composable
fun AdminUploadView(
    viewModel: LibraryViewModel,
    adminEmail: String?,
    title: String,
    author: String,
    coverUrl: String,
    selectedPdfName: String?,
    isUploading: Boolean,
    uploadProgress: String?,
    uploadError: String?,
    onPickPdf: () -> Unit
) {
    val presetCovers = listOf(
        "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=600&auto=format&fit=crop&q=80"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Admin Status Header
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Authenticated as Admin",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = adminEmail ?: "admin@library.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Upload Form Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Upload New Book to Firebase",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Text(
                    text = "Provide book information, select a PDF to upload to Firebase Storage, and publish to the 'books' Firestore collection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { viewModel.uploadTitle.value = it },
                    label = { Text("Book Title *") },
                    placeholder = { Text("e.g. Modern Architecture") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = author,
                    onValueChange = { viewModel.uploadAuthor.value = it },
                    label = { Text("Author Name *") },
                    placeholder = { Text("e.g. Eleanor Vance") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_author_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = coverUrl,
                    onValueChange = { viewModel.uploadCoverUrl.value = it },
                    label = { Text("Cover Image URL (Optional)") },
                    placeholder = { Text("https://example.com/cover.jpg") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_cover_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Quick preset cover selector
                Text(
                    text = "Or choose cover style:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presetCovers.forEachIndexed { index, url ->
                        val isSelected = coverUrl == url
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.uploadCoverUrl.value = url },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cover ${index + 1}",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // PDF File Picker Section
                Text(
                    text = "PDF Document File",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                // Selected File Banner
                if (selectedPdfName != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color(0xFFDC2626)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedPdfName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Ready to upload to Firebase Storage",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onPickPdf,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("select_pdf_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Select PDF")
                    }

                    // Prompt 4 Requirement: Test with a small PDF
                    Button(
                        onClick = { viewModel.generateAndSelectTestPdf() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("generate_test_pdf_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test with Small PDF")
                    }
                }

                if (uploadError != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uploadError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (isUploading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = uploadProgress ?: "Uploading...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Button(
                    onClick = { viewModel.uploadBook() },
                    enabled = !isUploading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("upload_book_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Uploading to Firebase...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Upload PDF & Publish Book",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
