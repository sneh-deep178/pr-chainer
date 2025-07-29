package com.pr.startup

import com.pr.listeners.LineChangeListener
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.diagnostic.thisLogger

class PRChainerStartupActivity : ProjectActivity {
    companion object {
        private val LOG = thisLogger()
    }

    override suspend fun execute(project: Project) {
        LOG.info("PR Chainer: StartupActivity executed for project: ${project.name}")
        
        try {
            // Create an instance of LineChangeListener
            val lineChangeListener = LineChangeListener(project)
            LOG.info("PR Chainer: LineChangeListener created successfully")

            // Get the EditorFactory instance
            val editorFactory = EditorFactory.getInstance()

            // Add document listener to all editors with proper disposal
            editorFactory.eventMulticaster.addDocumentListener(lineChangeListener, project)
            LOG.info("PR Chainer: Document listener added successfully")

            // Add file document manager listener for save events using message bus
            val connection = project.messageBus.connect()
            connection.subscribe(com.intellij.AppTopics.FILE_DOCUMENT_SYNC, lineChangeListener)
            LOG.info("PR Chainer: File save listener added successfully")
            
        } catch (e: Exception) {
            LOG.error("PR Chainer: Error during startup activity", e)
        }
    }
} 