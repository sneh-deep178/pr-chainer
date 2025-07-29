package com.pr.services

import com.intellij.openapi.diagnostic.thisLogger
import java.io.File

class GitService {
    companion object {
        private val LOG = thisLogger()
    }

    fun getTotalLineChangesFromGit(projectDir: File): Int {
        return try {
            val currentBranch = getCurrentBranch(projectDir) ?: "master"
            val parentBranch = getParentBranch(projectDir, currentBranch)
            
            LOG.info("GitService: Current branch: $currentBranch, Parent branch: $parentBranch")
            
            var totalChanges = 0
            
            // For master branch, only check working directory changes
            if (parentBranch == "HEAD") {
                LOG.info("GitService: Running git diff HEAD --numstat (working directory changes)")
                totalChanges = getWorkingDirectoryChanges(projectDir)
            } else {
                // For feature branches, check both branch differences AND working directory
                LOG.info("GitService: Checking both branch differences and working directory changes")
                
                // 1. Get committed differences between branches
                val branchChanges = getBranchDifferences(projectDir, parentBranch)
                LOG.info("GitService: Branch differences ($currentBranch vs $parentBranch): $branchChanges lines")
                
                // 2. Get uncommitted working directory changes
                val workingChanges = getWorkingDirectoryChanges(projectDir)
                LOG.info("GitService: Working directory changes: $workingChanges lines")
                
                // 3. Combine both for total
                totalChanges = branchChanges + workingChanges
                LOG.info("GitService: Combined total: $branchChanges (branch) + $workingChanges (working) = $totalChanges lines")
            }
            
            LOG.info("GitService: Total changes calculated: $totalChanges (comparing $currentBranch with $parentBranch)")
            totalChanges
        } catch (e: Exception) {
            LOG.error("Error executing git diff command", e)
            0
        }
    }

    private fun getBranchDifferences(projectDir: File, parentBranch: String): Int {
        return try {
            val diffCommand = listOf("git", "diff", "$parentBranch...HEAD", "--numstat", "--no-ext-diff")
            val process = ProcessBuilder(diffCommand)
                .directory(projectDir)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            process.waitFor()
            
            if (process.exitValue() != 0) {
                LOG.warn("Git branch diff command failed. Error: $error")
                return 0
            }

            LOG.info("GitService: Branch diff output: '$output'")
            parseGitDiffOutput(output, "branch")
        } catch (e: Exception) {
            LOG.error("Error getting branch differences", e)
            0
        }
    }

    private fun getWorkingDirectoryChanges(projectDir: File): Int {
        return try {
            val diffCommand = listOf("git", "diff", "HEAD", "--numstat", "--no-ext-diff")
            val process = ProcessBuilder(diffCommand)
                .directory(projectDir)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            process.waitFor()
            
            if (process.exitValue() != 0) {
                LOG.warn("Git working directory diff command failed. Error: $error")
                return 0
            }

            LOG.info("GitService: Working directory diff output: '$output'")
            parseGitDiffOutput(output, "working directory")
        } catch (e: Exception) {
            LOG.error("Error getting working directory changes", e)
            0
        }
    }

    private fun parseGitDiffOutput(output: String, type: String): Int {
        var totalChanges = 0
        output.lines().forEach { line ->
            if (line.isNotBlank()) {
                val parts = line.trim().split("\t")
                if (parts.size >= 3) {
                    val additions = parts[0].toIntOrNull() ?: 0
                    val deletions = parts[1].toIntOrNull() ?: 0
                    val fileName = parts[2]
                    val fileChanges = additions + deletions
                    totalChanges += fileChanges
                    LOG.info("GitService: [$type] File '$fileName': $additions additions, $deletions deletions, total: $fileChanges")
                }
            }
        }
        return totalChanges
    }

    fun getCurrentBranch(projectDir: File): String? {
        return try {
            executeGitCommand(projectDir, listOf("branch", "--show-current")).trim()
        } catch (e: Exception) {
            LOG.warn("Failed to get current branch name", e)
            null
        }
    }

    fun getChangedFiles(projectDir: File): List<String> {
        return try {
            val process = ProcessBuilder("git", "diff", "HEAD", "--name-only")
                .directory(projectDir)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            
            if (process.exitValue() == 0) {
                output.lines().filter { it.isNotBlank() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            LOG.error("Error getting changed files", e)
            emptyList()
        }
    }

    fun stageFiles(projectDir: File, files: List<String>) {
        files.forEach { file ->
            executeGitCommand(projectDir, listOf("add", file))
        }
    }

    fun commitChanges(projectDir: File, message: String) {
        executeGitCommand(projectDir, listOf("commit", "-m", message))
    }

    fun pushBranch(projectDir: File, branchName: String) {
        executeGitCommand(projectDir, listOf("push", "origin", branchName))
    }

    fun pushWithUpstream(projectDir: File, branchName: String) {
        executeGitCommand(projectDir, listOf("push", "-u", "origin", branchName))
    }

    fun createAndCheckoutBranch(projectDir: File, branchName: String) {
        executeGitCommand(projectDir, listOf("checkout", "-b", branchName))
    }

    fun hasRemoteOrigin(projectDir: File): Boolean {
        return try {
            val output = executeGitCommand(projectDir, listOf("remote", "-v"))
            output.contains("origin")
        } catch (e: Exception) {
            LOG.warn("Failed to check for origin remote", e)
            false
        }
    }

    fun getGitUsername(projectDir: File): String {
        return try {
            val output = executeGitCommand(projectDir, listOf("config", "user.name"))
            output.trim().replace(" ", "-").lowercase()
        } catch (e: Exception) {
            LOG.warn("Failed to get git username, using default", e)
            "user"
        }
    }

    fun generateNewBranchName(projectDir: File, baseBranch: String): String {
        return try {
            val username = getGitUsername(projectDir)
            val repoName = projectDir.name
            
            if (baseBranch == "master") {
                val branchPrefix = "$repoName-master-$username"
                
                val output = executeGitCommand(projectDir, listOf("branch", "-a"))
                val branches = output.lines()
                    .map { it.trim().removePrefix("* ").removePrefix("remotes/origin/") }
                    .filter { it.startsWith("$branchPrefix-") }
                
                val numbers = branches.mapNotNull { branch ->
                    val suffix = branch.removePrefix("$branchPrefix-")
                    suffix.toIntOrNull()
                }
                
                val nextNumber = if (numbers.isEmpty()) 1 else numbers.maxOrNull()!! + 1
                "$branchPrefix-$nextNumber"
            } else {
                val lastDashIndex = baseBranch.lastIndexOf('-')
                
                if (lastDashIndex != -1) {
                    val possibleNumber = baseBranch.substring(lastDashIndex + 1)
                    if (possibleNumber.toIntOrNull() != null) {
                        val baseWithoutNumber = baseBranch.substring(0, lastDashIndex)
                        val currentNumber = possibleNumber.toInt()
                        "$baseWithoutNumber-${currentNumber + 1}"
                    } else {
                        "$baseBranch-1"
                    }
                } else {
                    "$baseBranch-1"
                }
            }
        } catch (e: Exception) {
            LOG.warn("Failed to generate branch name", e)
            "$baseBranch-1"
        }
    }

    fun getParentBranch(projectDir: File, currentBranch: String): String {
        return try {
            when {
                // Master/main branches compare with HEAD (working directory changes)
                currentBranch == "master" || currentBranch == "main" -> {
                    LOG.info("GitService: Master branch detected, using HEAD comparison")
                    "HEAD"
                }
                
                // Pattern: name-master-1 → master
                currentBranch.matches(Regex(".*-master-1$")) -> {
                    LOG.info("GitService: First master branch detected: $currentBranch → master")
                    "master"
                }
                
                // Pattern: name-master-N → name-master-(N-1)
                currentBranch.matches(Regex(".*-master-\\d+$")) -> {
                    val parts = currentBranch.split("-")
                    val number = parts.last().toInt()
                    val base = parts.dropLast(1).joinToString("-")
                    val parentBranch = "$base-${number - 1}"
                    
                    if (branchExists(projectDir, parentBranch)) {
                        LOG.info("GitService: Master chain detected: $currentBranch → $parentBranch")
                        parentBranch
                    } else {
                        LOG.warn("GitService: Parent branch $parentBranch not found, falling back to master")
                        "master"
                    }
                }
                
                // Pattern: name-1 → base name (if exists) or master
                currentBranch.matches(Regex(".*-1$")) -> {
                    val baseName = currentBranch.substringBeforeLast("-1")
                    if (branchExists(projectDir, baseName)) {
                        LOG.info("GitService: First branch in chain: $currentBranch → $baseName")
                        baseName
                    } else {
                        LOG.info("GitService: First branch, no base found: $currentBranch → master")
                        "master"
                    }
                }
                
                // Pattern: name-N → name-(N-1)
                currentBranch.matches(Regex(".*-\\d+$")) -> {
                    val parts = currentBranch.split("-")
                    val number = parts.last().toInt()
                    val base = parts.dropLast(1).joinToString("-")
                    val parentBranch = "$base-${number - 1}"
                    
                    if (branchExists(projectDir, parentBranch)) {
                        LOG.info("GitService: Chain detected: $currentBranch → $parentBranch")
                        parentBranch
                    } else {
                        LOG.warn("GitService: Parent branch $parentBranch not found, falling back to master")
                        "master"
                    }
                }
                
                // Default: compare with master
                else -> {
                    if (branchExists(projectDir, "master")) {
                        LOG.info("GitService: No pattern matched, using master as parent for: $currentBranch")
                        "master"
                    } else if (branchExists(projectDir, "main")) {
                        LOG.info("GitService: No pattern matched, using main as parent for: $currentBranch")
                        "main"
                    } else {
                        LOG.warn("GitService: No master/main found, falling back to HEAD")
                        "HEAD"
                    }
                }
            }
        } catch (e: Exception) {
            LOG.error("Error detecting parent branch for $currentBranch", e)
            "master"
        }
    }

    fun branchExists(projectDir: File, branchName: String): Boolean {
        return try {
            val output = executeGitCommand(projectDir, listOf("branch", "-a"))
            val branchExists = output.lines().any { line ->
                val cleanLine = line.trim().removePrefix("* ").removePrefix("remotes/origin/")
                cleanLine == branchName
            }
            LOG.info("GitService: Branch '$branchName' exists: $branchExists")
            branchExists
        } catch (e: Exception) {
            LOG.warn("GitService: Error checking if branch '$branchName' exists", e)
            false
        }
    }

    private fun executeGitCommand(projectDir: File, command: List<String>): String {
        val fullCommand = listOf("git") + command
        val process = ProcessBuilder(fullCommand)
            .directory(projectDir)
            .start()
        
        val output = process.inputStream.bufferedReader().readText()
        val error = process.errorStream.bufferedReader().readText()
        process.waitFor()
        
        if (process.exitValue() != 0) {
            throw RuntimeException("Git command failed: ${fullCommand.joinToString(" ")}\nError: $error")
        }
        
        return output
    }
} 