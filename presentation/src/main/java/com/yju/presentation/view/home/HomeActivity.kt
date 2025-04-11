package com.yju.presentation.view.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.yju.presentation.R
import com.yju.presentation.base.BaseActivity
import com.yju.presentation.base.BaseViewModel
import com.yju.presentation.databinding.ActivityHomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : BaseActivity<ActivityHomeBinding, HomeViewModel>(R.layout.activity_home) {

    // PDF 파일만 선택할 수 있도록 설정
    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handleSelectedFile(it) }
    }

    // 권한 요청 런처
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            saveAndUploadFile()
        } else {
            viewModel.baseEvent(BaseViewModel.UiEvent.Toast.Normal("저장소 권한이 필요합니다"))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupClickListeners()
        observeViewModel()
    }

//    // 프로그레스바를 표시하기 위해 BaseActivity의 showLoading 오버라이드
//    override fun showLoading() {
//        binding.progressBar.visibility = View.VISIBLE
//    }
//
//    // 프로그레스바를 숨기기 위해 BaseActivity의 hideLoading 오버라이드
//    override fun hideLoading() {
//        binding.progressBar.visibility = View.GONE
//    }

    private fun observeViewModel() {
        // 파일 URI 변경 감지하여 UI 업데이트
        viewModel.selectedFileUri.observe(this) { uri ->
            // URI가 있으면 업로드 상태 카드 표시, 없으면 업로드 카드 표시
            showUploadCard(uri == null)
        }

        // 파일 이름 변경 감지
        viewModel.selectedFileName.observe(this) { fileName ->
            binding.tvSelectedFileName.text = fileName
        }

        // 업로드 응답 감지
        viewModel.uploadResponse.observe(this) { response ->
            response?.let {
                // 업로드 성공 시 결과 화면으로 이동
            }
        }
    }

    private fun setupClickListeners() {
        // 파일 선택 버튼
        binding.btnSelectFile.setOnClickListener {
            openPdfSelector()
        }

        // 업로드 버튼 (cardUploadStatus 내부에 있음)
        binding.btnUpload.setOnClickListener {
            viewModel.selectedFileUri.value?.let {
                checkStoragePermissionAndUpload()
            }
        }

        // 메뉴 버튼
        binding.btnMenu.setOnClickListener {
            viewModel.baseEvent(BaseViewModel.UiEvent.Toast.Normal("메뉴 버튼 클릭"))
        }

        // 이미 업로드한 파일 영역을 다시 누르면 재선택
        binding.cardUploadStatus.setOnClickListener {
            viewModel.resetUiState()
            openPdfSelector()
        }
    }

    private fun openPdfSelector() {
        openDocumentLauncher.launch(arrayOf("application/pdf"))
    }

    private fun handleSelectedFile(uri: Uri) {
        // SAF 영구 권한(읽기)을 요청
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        // 파일 이름 추출
        val fileName = getFileName(uri)

        // ViewModel에 파일 정보 저장
        viewModel.setSelectedFileUri(uri, fileName)
        viewModel.baseEvent(BaseViewModel.UiEvent.Toast.Success("PDF 파일이 선택되었습니다: $fileName"))

        showUploadCard(false)
    }

    private fun checkStoragePermissionAndUpload() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            saveAndUploadFile()
        }
    }

    private fun saveAndUploadFile() {
        val uri = viewModel.selectedFileUri.value ?: return
        val fileName = viewModel.selectedFileName.value ?: "unknown.pdf"

        viewModel.uploadFile(this, uri, fileName)
    }

    private fun getFileName(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use { c ->
            if (c.moveToFirst()) {
                val displayNameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    return c.getString(displayNameIndex)
                }
            }
            uri.lastPathSegment ?: "unknown.pdf"
        } ?: uri.lastPathSegment ?: "unknown.pdf"
    }

    private fun showUploadCard(showUploadCard: Boolean) {
        binding.cardUpload.visibility = if (showUploadCard) View.VISIBLE else View.GONE
        binding.cardUploadStatus.visibility = if (showUploadCard) View.GONE else View.VISIBLE
    }
}