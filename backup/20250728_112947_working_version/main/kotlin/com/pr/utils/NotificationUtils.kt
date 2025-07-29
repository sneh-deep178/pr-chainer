package com.pr.utils

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import javax.swing.JOptionPane

object NotificationUtils {
    
    fun showSuccess(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("PR Chainer")
            .createNotification(message, NotificationType.INFORMATION)
            .notify(project)
        
        JOptionPane.showMessageDialog(null, message, "Success", JOptionPane.INFORMATION_MESSAGE)
    }

    fun showError(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("PR Chainer")
            .createNotification("Error: $message", NotificationType.ERROR)
            .notify(project)
        
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE)
    }

    fun showInfo(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("PR Chainer")
            .createNotification(message, NotificationType.INFORMATION)
            .notify(project)
    }

    fun showWarning(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("PR Chainer")
            .createNotification(message, NotificationType.WARNING)
            .notify(project)
    }
} 