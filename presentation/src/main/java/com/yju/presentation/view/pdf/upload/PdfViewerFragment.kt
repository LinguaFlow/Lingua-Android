package com.yju.presentation.view.pdf.upload

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.yju.presentation.R
import com.yju.presentation.base.BaseFragment
import com.yju.presentation.base.BaseViewModel
import com.yju.presentation.databinding.FragmentPdfViewerBinding
import com.yju.presentation.ext.repeatOnStarted
import com.yju.domain.pdf.model.UiPdfListModel
import com.yju.presentation.view.home.HomeViewModel
import com.yju.presentation.view.pdf.adapter.PdfListAdapter

import com.yju.presentation.view.pdf.sheet.MenuDrawerFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PdfViewerFragment : BaseFragment<FragmentPdfViewerBinding, PdfViewerViewModel>(
    R.layout.fragment_pdf_viewer
), UiPdfListModel.OnItemClickListener {

    private val activityViewModel: HomeViewModel by activityViewModels()
    override val applyTransition: Boolean = true
    private lateinit var pickPdfLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var permLauncher: ActivityResultLauncher<String>
    private lateinit var uploader: UploadPdfDelegate
    private val uploadCancelDialog by lazy { SimpleUploadCancelDialog(requireContext()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActivityResultLaunchers()
        initializeUploadDelegate()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUserInterface()
        setupDataObservers()
        handleArguments()
        viewModel.refreshList()
    }

    override fun onDestroyView() {
        viewModel.cancelAllUploadTasks()
        uploader.cleanup()
        cleanupResources()
        super.onDestroyView()
    }

    override fun onPause() {
        super.onPause()
        if (isRemoving || requireActivity().isFinishing) {
            viewModel.cancelAllUploadTasks()
        }
    }
    private fun setupActivityResultLaunchers() {
        // PDF 파일 선택 런처
        pickPdfLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            uri?.let { selectedUri ->
                handleSelectedFile(selectedUri)
            }
        }

        permLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted: Boolean ->
            if (granted) {
                uploader.performUpload()
            } else {
                viewModel.baseEvent(
                    BaseViewModel.UiEvent.Toast.Normal("저장소 권한이 필요합니다")
                )
            }
        }
    }

    private fun initializeUploadDelegate() {
        uploader = UploadPdfDelegate(
            owner = this,
            cancelUploadUseCase = viewModel.getCancelUploadUseCase(),
            asyncUpload = viewModel.getAsyncUploadUseCase(),
            installKanji = viewModel.getInstallKanjiUseCase(),
            coroutineScope = lifecycleScope,
            onFileSelected = { uri, name ->
                viewModel.setSelectedFile(uri, name)
                activityViewModel.setSelectedFileUri(uri, name)
            },
            onUploadStarted = {
                showUploadCancelDialog()
                viewModel.startUpload()
                activityViewModel.setUploading(true)
            },
            onUploadSuccess = { pdfModel ->
                hideUploadCancelDialog()
                viewModel.handleUploadSuccess()
                activityViewModel.setUploading(false)
                viewModel.setSelectedFile(null, null)
                activityViewModel.setSelectedFileUri(null, null)
            },
            onUploadFailed = { message ->
                hideUploadCancelDialog()
                viewModel.handleUploadError(message)
                activityViewModel.setUploading(false)
                viewModel.setSelectedFile(null, null)
                activityViewModel.setSelectedFileUri(null, null)
            },
            pickPdfLauncher = pickPdfLauncher,
            permLauncher = permLauncher
        )

        lifecycle.addObserver(uploader)
    }

    private fun showUploadCancelDialog() {
        uploadCancelDialog.show(
            onCancel = {
                uploader.cancelUpload()
                // 취소 시 파일 선택 초기화
                viewModel.setSelectedFile(null, null)
                activityViewModel.setSelectedFileUri(null, null)
            }
        )
    }

    private fun hideUploadCancelDialog() {
        uploadCancelDialog.dismiss()
    }

    private fun setupUserInterface() {
        setupPdfRecyclerView()
        setupUploadUI()
    }

    private fun setupPdfRecyclerView() {
        binding.rvPdf.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = PdfListAdapter(
                onItemDeleteClick = { id ->
                    viewModel.deletePdf(id)
                },
                onItemViewClick = { id ->
                    viewModel.navigateToPdfChapter(id)
                }
            ).apply {
                isDeleteMode = true
            }
        }
    }

    private fun setupUploadUI() = with(binding) {
        cardUpload.setOnClickListener {
            uploader.launchPicker()
        }
        btnUploadPdf.setOnClickListener {
            handleUploadButtonClick()
        }
        fileInfoBox.setOnClickListener {
            uploader.launchPicker()
        }
        cardUploadStatus.setOnClickListener {
            uploader.launchPicker()
        }
    }

    private fun setupDataObservers() {
        observePdfList()
        observeFileSelection()
        observeNavigationEvents()
        observeMenu()  // 메뉴 관찰 활성화
    }

    private fun observePdfList() {
        repeatOnStarted {
            viewModel.pdfList.asFlow().collect { pdfList ->
                (binding.rvPdf.adapter as PdfListAdapter).submitList(pdfList)
                showEmptyView(pdfList.isEmpty())
            }
        }
    }

    private fun observeFileSelection() {
        repeatOnStarted {
            viewModel.selectedFileUri.asFlow().collect { uri ->
                toggleUploadCard(uri != null)
            }
        }

        repeatOnStarted {
            viewModel.selectedFileName.asFlow().collect { fileName ->
                binding.tvSelectedPdfName.text = fileName ?: ""
            }
        }
    }

    private fun observeNavigationEvents() {
        repeatOnStarted {
            viewModel.onClickNavigateToChapter.collect { pdfId ->
                activityViewModel.navigateToPdfChapter(pdfId)
            }
        }

        repeatOnStarted {
            viewModel.onClickNavigateToChapter.collect { pdfId ->
                activityViewModel.navigateToPdfChapter(pdfId)
            }
        }
    }

    // 메뉴 관찰 메서드 구현
    private fun observeMenu() {
        repeatOnStarted {
            viewModel.showMenu.collect {
                showMenuDrawer()  // 메뉴 드로어 표시
            }
        }
    }

    // 메뉴 드로어 표시 메서드 추가
    private fun showMenuDrawer() {
        val menuDrawer = MenuDrawerFragment.newInstance()
        menuDrawer.show(parentFragmentManager, "MenuDrawer")
    }

    private fun handleSelectedFile(uri: Uri) {
        try {
            requireContext().contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            uploader.handleSelectedFile(uri)

        } catch (exception: Exception) {
            viewModel.baseEvent(
                BaseViewModel.UiEvent.Toast.Normal("파일 선택 오류: ${exception.message}")
            )
        }
    }

    private fun handleUploadButtonClick() {
        val uri = viewModel.selectedFileUri.value
        val name = viewModel.selectedFileName.value
        if (uri != null && name != null) {
            uploader.uploadFile(uri, name)
        } else {
            viewModel.baseEvent(
                BaseViewModel.UiEvent.Toast.Normal("업로드할 파일을 선택해주세요")
            )
        }
    }

    private fun handleArguments() {
        arguments?.getString("book_id")?.let { bookId ->
            viewModel.baseEvent(
                BaseViewModel.UiEvent.Toast.Success("PDF ID: $bookId 불러옴")
            )
        }
    }

    private fun toggleUploadCard(fileSelected: Boolean) {
        binding.cardUpload.visibility = if (fileSelected) View.GONE else View.VISIBLE
        binding.cardUploadStatus.visibility = if (fileSelected) View.VISIBLE else View.GONE
    }

    private fun showEmptyView(isEmpty: Boolean) = with(binding) {
        rvPdf.visibility = if (isEmpty) View.GONE else View.VISIBLE
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        cardUpload.visibility = View.VISIBLE
    }

    private fun cleanupResources() {
        uploadCancelDialog.dismiss()
        with(binding) {
            rvPdf.adapter = null
            rvPdf.layoutManager = null
            cardUpload.setOnClickListener(null)
            btnUploadPdf.setOnClickListener(null)
            cardUploadStatus.setOnClickListener(null)
            fileInfoBox.setOnClickListener(null)
        }
        uploader.cleanup()
    }

    override fun onItemClick(id: Long) {
        viewModel.deletePdf(id)
    }
}