package com.pr.listeners

import com.pr.services.PRChainerService
import com.pr.ui.PRChainerDialogs
import com.pr.utils.NotificationUtils
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.editor.Document
import com.intellij.openapi.vfs.VfsUtil
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class LineChangeListener(private val project: Project) : DocumentListener, FileDocumentManagerListener {
    private val debounceExecutor = Executors.newSingleThreadScheduledExecutor()
    private val service = PRChainerService(project)
    private val dialogs = PRChainerDialogs(project, service)
    private var isDialogShowing = false
    
    companion object {
        private val LOG = thisLogger()
    }

    init {
        LOG.info("PR Chainer: LineChangeListener initialized for project: ${project.name}")
        NotificationUtils.showInfo(project, "PR Chainer initialized for project: ${project.name}")
    }

    override fun documentChanged(event: DocumentEvent) {
        LOG.info("PR Chainer: Document changed event received")
        // Force save immediately with minimal debounce
        debounceExecutor.schedule({
            // Force immediate save of the specific document
            forceImmediateSave(event.document)
            // Also trigger global save
            triggerAutoSave()
            // Then check for changes
            checkRepositoryChanges()
        }, 300, TimeUnit.MILLISECONDS) // Very short debounce just to batch rapid typing
    }

    override fun beforeDocumentSaving(document: Document) {
        // Keep this for when manual saves happen
        LOG.info("PR Chainer: File being saved, triggering immediate Git check")
        debounceExecutor.schedule({
            checkRepositoryChanges()
        }, 50, TimeUnit.MILLISECONDS)
    }

    private fun forceImmediateSave(document: Document) {
        try {
            LOG.info("PR Chainer: Force saving specific document")
            
            // Get the virtual file for this document
            val virtualFile = FileDocumentManager.getInstance().getFile(document)
            if (virtualFile != null) {
                // Force save this specific document
                FileDocumentManager.getInstance().saveDocument(document)
                LOG.info("PR Chainer: Successfully saved document: ${virtualFile.name}")
            }
        } catch (e: Exception) {
            LOG.warn("PR Chainer: Failed to force save specific document", e)
        }
    }

    private fun triggerAutoSave() {
        try {
            LOG.info("PR Chainer: Triggering comprehensive auto-save")
            
            // Multiple approaches to ensure everything is saved
            FileDocumentManager.getInstance().saveAllDocuments()
            com.intellij.openapi.application.ApplicationManager.getApplication().saveAll()
            
            // Force file system sync
            com.intellij.openapi.vfs.VfsUtil.markDirtyAndRefresh(false, true, true, 
                project.guessProjectDir())
            
            LOG.info("PR Chainer: Comprehensive auto-save completed")
        } catch (e: Exception) {
            LOG.warn("PR Chainer: Failed to trigger comprehensive auto-save", e)
        }
    }

    private fun checkRepositoryChanges() {
        if (isDialogShowing) {
            LOG.info("PR Chainer: Dialog already showing, skipping check")
            return
        }

        when (val decision = service.checkIfShouldPrompt()) {
            is PRChainerService.PromptDecision.Skip -> {
                LOG.info("PR Chainer: Skipping prompt - ${decision.reason}")
            }
            is PRChainerService.PromptDecision.Show -> {
                LOG.info("PR Chainer: Showing main prompt dialog")
                isDialogShowing = true
                showMainDialog(decision.totalChanges, decision.threshold, decision.currentBranch)
            }
        }
    }

    private fun showMainDialog(totalChanges: Int, threshold: Int, currentBranch: String) {
        dialogs.showMainPrompt(totalChanges, threshold, currentBranch) { result ->
            when (result) {
                is PRChainerDialogs.MainDialogResult.Proceed -> {
                    // Don't reset isDialogShowing yet - let file selection dialog handle it
                    showFileSelectionDialog(currentBranch)
                }
                is PRChainerDialogs.MainDialogResult.IncreaseThreshold -> {
                    val newThreshold = service.increaseThreshold(5)
                    NotificationUtils.showInfo(project, "Threshold increased to $newThreshold lines. You can continue working.")
                    isDialogShowing = false
                }
                is PRChainerDialogs.MainDialogResult.Cancel -> {
                    val newThreshold = service.handleCancel()
                    NotificationUtils.showInfo(project, "Cancelled. Threshold increased to $newThreshold lines. You can continue working.")
                    isDialogShowing = false
                }
            }
        }
    }

    private fun showFileSelectionDialog(currentBranch: String) {
        val changedFiles = service.getChangedFiles()
        
        dialogs.showFileSelectionDialog(currentBranch, changedFiles) { result ->
            when (result) {
                is PRChainerDialogs.FileSelectionResult.Success -> {
                    NotificationUtils.showSuccess(project, result.message)
                    result.workflow?.let { workflow ->
                        NotificationUtils.showInfo(project, "Workflow: $workflow")
                    }
                }
                is PRChainerDialogs.FileSelectionResult.Error -> {
                    NotificationUtils.showError(project, result.message)
                }
                is PRChainerDialogs.FileSelectionResult.Cancelled -> {
                    // Do nothing for cancel
                }
            }
            // Always reset dialog flag when file selection is complete
            isDialogShowing = false
        }
    }
}