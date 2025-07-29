package com.pr.ui

import com.pr.services.PRChainerService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

class PRChainerDialogs(private val project: Project, private val service: PRChainerService) {

    fun showMainPrompt(totalChanges: Int, threshold: Int, currentBranch: String, onResult: (MainDialogResult) -> Unit) {
        val branchMessage = if (currentBranch == "master") {
            "⚠️ You're on the 'master' branch!\n\n📊 Status: $totalChanges/$threshold lines (${(totalChanges * 100) / threshold}%)\n\nThis will:\n1. Prompt you to enter a branch name\n2. Create a branch with your-name-1\n3. Move your changes to that branch\n4. Create another branch with incremented number\n5. Keep master clean and protected"
        } else {
            "📊 Status: $totalChanges/$threshold lines (${(totalChanges * 100) / threshold}%)\n\nRepository changes exceeded threshold!\n\nCommit current changes and create a new branch?"
        }
        
        val options = arrayOf("Commit & Create Branch", "Increase Threshold (+5)", "Cancel & Increase Threshold")
        val result = JOptionPane.showOptionDialog(
            null,
            branchMessage,
            "Branch Threshold Exceeded",
            JOptionPane.YES_NO_CANCEL_OPTION,
            if (currentBranch == "master") JOptionPane.WARNING_MESSAGE else JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            null // No default option - user must click explicitly
        )

        when (result) {
            0 -> onResult(MainDialogResult.Proceed)
            1 -> onResult(MainDialogResult.IncreaseThreshold)
            2, JOptionPane.CLOSED_OPTION -> onResult(MainDialogResult.Cancel)
        }
    }

    fun showFileSelectionDialog(
        currentBranch: String,
        changedFiles: List<String>,
        onResult: (FileSelectionResult) -> Unit
    ) {
        SwingUtilities.invokeLater {
            val dialog = FileSelectionDialog(currentBranch, changedFiles, onResult)
            dialog.showAndGet()
        }
    }

    private inner class FileSelectionDialog(
        private val currentBranch: String,
        private val changedFiles: List<String>,
        private val onResult: (FileSelectionResult) -> Unit
    ) : DialogWrapper(project) {
        
        private val commitMessageArea = JBTextArea(3, 50)
        private val fileCheckboxes = mutableListOf<JBCheckBox>()
        private val progressLabel = JLabel(" ")
        private var isProcessing = false
        
        init {
            title = "Select Files and Commit Message"
            setOKButtonText("Commit & Create Branch")
            setCancelButtonText("Cancel")
            init()
        }
        
        override fun createCenterPanel(): JComponent {
            val panel = JPanel(BorderLayout())
            
            // Commit message section
            val messagePanel = JPanel(BorderLayout())
            messagePanel.add(JLabel("Commit Message:"), BorderLayout.NORTH)
            commitMessageArea.text = "Auto-commit: Changes exceeded threshold"
            commitMessageArea.lineWrap = true
            commitMessageArea.wrapStyleWord = true
            messagePanel.add(JBScrollPane(commitMessageArea), BorderLayout.CENTER)
            
            // Files section
            val filesPanel = JPanel(BorderLayout())
            filesPanel.add(JLabel("Select files to commit:"), BorderLayout.NORTH)
            
            val fileListPanel = JPanel()
            fileListPanel.layout = BoxLayout(fileListPanel, BoxLayout.Y_AXIS)
            
            if (changedFiles.isEmpty()) {
                fileListPanel.add(JLabel("No changed files detected"))
            } else {
                changedFiles.forEach { file ->
                    val checkbox = JBCheckBox(file, true) // Pre-select all files
                    fileCheckboxes.add(checkbox)
                    fileListPanel.add(checkbox)
                }
            }
            
            val scrollPane = JBScrollPane(fileListPanel)
            scrollPane.preferredSize = Dimension(500, 200)
            filesPanel.add(scrollPane, BorderLayout.CENTER)
            
            // Progress section
            val progressPanel = JPanel(BorderLayout())
            progressLabel.foreground = java.awt.Color.BLUE
            progressPanel.add(progressLabel, BorderLayout.CENTER)
            
            // Combine panels
            panel.add(messagePanel, BorderLayout.NORTH)
            panel.add(filesPanel, BorderLayout.CENTER)
            panel.add(progressPanel, BorderLayout.SOUTH)
            
            return panel
        }
        
        override fun doOKAction() {
            if (isProcessing) return
            
            val selectedFiles = getSelectedFiles()
            val commitMessage = getCommitMessage()
            
            // Validate
            if (selectedFiles.isEmpty()) {
                Messages.showErrorDialog(project, "Please select at least one file to commit.", "No Files Selected")
                return
            }
            
            if (commitMessage.isBlank()) {
                Messages.showErrorDialog(project, "Please provide a commit message.", "No Commit Message")
                return
            }
            
            // Start processing
            startProcessing(selectedFiles, commitMessage)
        }
        
        private fun startProcessing(selectedFiles: List<String>, commitMessage: String) {
            isProcessing = true
            okAction.isEnabled = false
            cancelAction.isEnabled = false
            progressLabel.text = "🔄 Processing... Please wait..."
            
            SwingUtilities.invokeLater {
                Thread {
                    try {
                        if (currentBranch == "master") {
                            handleMasterBranch(selectedFiles, commitMessage)
                        } else {
                            handleRegularBranch(selectedFiles, commitMessage)
                        }
                    } catch (e: Exception) {
                        SwingUtilities.invokeLater {
                            progressLabel.text = "❌ Error: ${e.message}"
                            onResult(FileSelectionResult.Error(e.message ?: "Unknown error"))
                            resetDialog()
                        }
                    }
                }.start()
            }
        }
        
        private fun handleMasterBranch(selectedFiles: List<String>, commitMessage: String) {
            SwingUtilities.invokeLater {
                progressLabel.text = "📝 Enter branch name..."
            }
            
            val userBranchName = JOptionPane.showInputDialog(
                this.contentPanel,
                "Enter a name for the new branch:",
                "Branch Name",
                JOptionPane.QUESTION_MESSAGE
            )
            
            if (userBranchName.isNullOrBlank()) {
                SwingUtilities.invokeLater {
                    progressLabel.text = "❌ Branch name cannot be empty"
                    resetDialog()
                }
                return
            }
            
            SwingUtilities.invokeLater {
                progressLabel.text = "🌿 Creating master workflow..."
            }
            
            val result = service.createMasterBranchWorkflow(userBranchName, selectedFiles, commitMessage)
            
            SwingUtilities.invokeLater {
                when (result) {
                    is PRChainerService.WorkflowResult.Success -> {
                        progressLabel.text = "✅ Success!"
                        onResult(FileSelectionResult.Success(result.message, result.workflow))
                        close(OK_EXIT_CODE)
                    }
                    is PRChainerService.WorkflowResult.Error -> {
                        progressLabel.text = "❌ Error: ${result.message}"
                        onResult(FileSelectionResult.Error(result.message))
                        resetDialog()
                    }
                }
            }
        }
        
        private fun handleRegularBranch(selectedFiles: List<String>, commitMessage: String) {
            SwingUtilities.invokeLater {
                progressLabel.text = "🌿 Creating branch workflow..."
            }
            
            val result = service.createRegularBranchWorkflow(currentBranch, selectedFiles, commitMessage)
            
            SwingUtilities.invokeLater {
                when (result) {
                    is PRChainerService.WorkflowResult.Success -> {
                        progressLabel.text = "✅ Success!"
                        onResult(FileSelectionResult.Success(result.message, result.workflow))
                        close(OK_EXIT_CODE)
                    }
                    is PRChainerService.WorkflowResult.Error -> {
                        progressLabel.text = "❌ Error: ${result.message}"
                        onResult(FileSelectionResult.Error(result.message))
                        resetDialog()
                    }
                }
            }
        }
        
        private fun resetDialog() {
            isProcessing = false
            okAction.isEnabled = true
            cancelAction.isEnabled = true
            progressLabel.text = " "
        }
        
        override fun doCancelAction() {
            if (isProcessing) return
            super.doCancelAction()
            onResult(FileSelectionResult.Cancelled)
        }
        
        private fun getSelectedFiles(): List<String> {
            return fileCheckboxes.filter { it.isSelected }.map { it.text }
        }
        
        private fun getCommitMessage(): String {
            return commitMessageArea.text.trim()
        }
    }

    sealed class MainDialogResult {
        object Proceed : MainDialogResult()
        object IncreaseThreshold : MainDialogResult()
        object Cancel : MainDialogResult()
    }

    sealed class FileSelectionResult {
        data class Success(val message: String, val workflow: String?) : FileSelectionResult()
        data class Error(val message: String) : FileSelectionResult()
        object Cancelled : FileSelectionResult()
    }
} 