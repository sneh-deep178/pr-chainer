package com.pr.services

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import java.io.File

class PRChainerService(private val project: Project) {
    private val gitService = GitService()
    private var changeThreshold = 5
    private val originalThreshold = 5
    private var lastCancelTime = 0L
    private val cancelCooldownMs = 10000L
    
    companion object {
        private val LOG = thisLogger()
    }

    fun getProjectDirectory(): File? {
        val projectDir = project.guessProjectDir()?.toNioPath()?.toFile()
        if (projectDir == null || !File(projectDir, ".git").exists()) {
            LOG.warn("Project directory not found or not a Git repository")
            return null
        }
        return projectDir
    }

    fun checkIfShouldPrompt(): PromptDecision {
        LOG.info("PRChainerService: Checking if should prompt...")
        val projectDir = getProjectDirectory() ?: return PromptDecision.Skip("No project directory")
        
        val totalChanges = gitService.getTotalLineChangesFromGit(projectDir)
        LOG.info("Total repository changes detected: $totalChanges lines (threshold: $changeThreshold)")
        
        val currentTime = System.currentTimeMillis()
        val timeSinceCancel = currentTime - lastCancelTime
        
        return when {
            totalChanges < changeThreshold -> {
                LOG.info("PRChainerService: Changes $totalChanges below threshold $changeThreshold")
                PromptDecision.Skip("Below threshold")
            }
            timeSinceCancel <= cancelCooldownMs -> {
                LOG.info("PRChainerService: Dialog suppressed due to cooldown (${(cancelCooldownMs - timeSinceCancel) / 1000}s remaining)")
                PromptDecision.Skip("In cooldown period")
            }
            else -> {
                LOG.info("PRChainerService: Threshold crossed! Should show dialog for $totalChanges/$changeThreshold")
                val currentBranch = gitService.getCurrentBranch(projectDir) ?: "main"
                PromptDecision.Show(totalChanges, changeThreshold, currentBranch)
            }
        }
    }

    fun increaseThreshold(additionalLines: Int = 5): Int {
        val projectDir = getProjectDirectory() ?: return changeThreshold
        val currentChanges = gitService.getTotalLineChangesFromGit(projectDir)
        changeThreshold = currentChanges + additionalLines
        LOG.info("PRChainerService: Threshold increased to $changeThreshold (current $currentChanges + $additionalLines)")
        return changeThreshold
    }

    fun handleCancel(): Int {
        lastCancelTime = System.currentTimeMillis()
        return increaseThreshold(5)
    }

    fun resetThreshold() {
        changeThreshold = originalThreshold
        LOG.info("PRChainerService: Threshold reset to original value: $originalThreshold")
    }

    fun createMasterBranchWorkflow(branchName: String, selectedFiles: List<String>, commitMessage: String): WorkflowResult {
        val projectDir = getProjectDirectory() ?: return WorkflowResult.Error("No project directory")
        
        return try {
            // Stage and commit
            gitService.stageFiles(projectDir, selectedFiles)
            gitService.commitChanges(projectDir, commitMessage)
            
            val hasOrigin = gitService.hasRemoteOrigin(projectDir)
            
            // Create first branch
            val firstBranchName = "$branchName-1"
            gitService.createAndCheckoutBranch(projectDir, firstBranchName)
            
            // Push first branch if origin exists
            if (hasOrigin) {
                gitService.pushWithUpstream(projectDir, firstBranchName)
            }
            
            // Create next branch
            val nextBranchName = gitService.generateNewBranchName(projectDir, firstBranchName)
            gitService.createAndCheckoutBranch(projectDir, nextBranchName)
            
            resetThreshold()
            
            val message = if (hasOrigin) {
                "Master protected! Created branch '$firstBranchName' and switched to '$nextBranchName'"
            } else {
                "Created branch '$firstBranchName' and switched to '$nextBranchName' (local only)"
            }
            
            WorkflowResult.Success(message, "master → $firstBranchName → $nextBranchName (current)")
            
        } catch (e: Exception) {
            LOG.error("Error in master branch workflow", e)
            WorkflowResult.Error("Master branch workflow error: ${e.message}")
        }
    }

    fun createRegularBranchWorkflow(currentBranch: String, selectedFiles: List<String>, commitMessage: String): WorkflowResult {
        val projectDir = getProjectDirectory() ?: return WorkflowResult.Error("No project directory")
        
        return try {
            // Stage and commit
            gitService.stageFiles(projectDir, selectedFiles)
            gitService.commitChanges(projectDir, commitMessage)
            
            val hasOrigin = gitService.hasRemoteOrigin(projectDir)
            
            // Push current branch
            if (hasOrigin) {
                gitService.pushBranch(projectDir, currentBranch)
            }
            
            // Create new branch
            val newBranchName = gitService.generateNewBranchName(projectDir, currentBranch)
            gitService.createAndCheckoutBranch(projectDir, newBranchName)
            
            resetThreshold()
            
            val message = if (hasOrigin) {
                "Successfully pushed and switched to branch '$newBranchName'"
            } else {
                "Created and switched to branch '$newBranchName' (local only)"
            }
            
            WorkflowResult.Success(message, null)
            
        } catch (e: Exception) {
            LOG.error("Error in regular branch workflow", e)
            WorkflowResult.Error("Branch workflow error: ${e.message}")
        }
    }

    fun getChangedFiles(): List<String> {
        val projectDir = getProjectDirectory() ?: return emptyList()
        return gitService.getChangedFiles(projectDir)
    }

    sealed class PromptDecision {
        data class Show(val totalChanges: Int, val threshold: Int, val currentBranch: String) : PromptDecision()
        data class Skip(val reason: String) : PromptDecision()
    }

    sealed class WorkflowResult {
        data class Success(val message: String, val workflow: String?) : WorkflowResult()
        data class Error(val message: String) : WorkflowResult()
    }
} 