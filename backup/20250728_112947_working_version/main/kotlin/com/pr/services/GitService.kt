package com.pr.services

import com.intellij.openapi.diagnostic.thisLogger
import java.io.File

class GitService {
    companion object {
        private val LOG = thisLogger()
    }

    fun getTotalLineChangesFromGit(projectDir: File): Int {
        return try {
            LOG.info("GitService: Running git diff HEAD --numstat")
            
            val process = ProcessBuilder("git", "diff", "HEAD", "--numstat", "--no-ext-diff")
                .directory(projectDir)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            process.waitFor()
            
            if (process.exitValue() != 0) {
                LOG.warn("Git diff command failed. Error: $error")
                return 0
            }

            LOG.info("GitService: Git diff output: '$output'")

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
                        LOG.info("GitService: File '$fileName': $additions additions, $deletions deletions, total: $fileChanges")
                    }
                }
            }
            
            LOG.info("GitService: Total changes calculated: $totalChanges")
            totalChanges
        } catch (e: Exception) {
            LOG.error("Error executing git diff command", e)
            0
        }
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