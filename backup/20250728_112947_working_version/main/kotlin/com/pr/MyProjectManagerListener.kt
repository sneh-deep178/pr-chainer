package com.pr

import com.pr.listeners.LineChangeListener
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.diagnostic.thisLogger

class MyProjectManagerListener : ProjectManagerListener {
    companion object {
        private val LOG = thisLogger()
    }
    
    override fun projectOpened(project: Project) {
        LOG.info("PR Chainer: Project opened - ${project.name}")
        
        try {
            // Create an instance of LineChangeListener
            val lineChangeListener = LineChangeListener(project)
            LOG.info("PR Chainer: LineChangeListener created successfully")

            // Get the EditorFactory instance
            val editorFactory = EditorFactory.getInstance()

            // Add document listener to all editors with proper disposal
            editorFactory.eventMulticaster.addDocumentListener(lineChangeListener, project)
            LOG.info("PR Chainer: Document listener added successfully")
            
        } catch (e: Exception) {
            LOG.error("PR Chainer: Error during initialization", e)
        }
    }
}