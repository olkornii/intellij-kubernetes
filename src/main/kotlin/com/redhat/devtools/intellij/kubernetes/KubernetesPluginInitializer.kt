/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.kubernetes

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.psi.impl.PsiDocumentTransactionListener
import com.redhat.devtools.intellij.kubernetes.editor.EditorFocusListener
import com.redhat.devtools.intellij.kubernetes.editor.EditorTransactionListener
import com.redhat.devtools.intellij.kubernetes.editor.ResourceEditor
import com.redhat.devtools.intellij.kubernetes.editor.notification.ErrorNotification

class KubernetesPluginInitializer : StartupActivity {

    override fun runActivity(project: Project) {
        project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER,
            EditorFocusListener(project)
        )
        enableNonProjectEditing(project)
        showResourceEditorNotifications(project)

        project.messageBus.connect().subscribe(PsiDocumentTransactionListener.TOPIC,
            EditorTransactionListener()
        )
    }

    private fun enableNonProjectEditing(project: Project) {
        FileEditorManager.getInstance(project).allEditors
            .filter { editor -> ResourceEditor.isResourceEditor(editor) }
            .mapNotNull { editor -> ResourceEditor.getResourceFile(editor) }
            .forEach { file -> ResourceEditor.enableNonProjectFileEditing(file) }
    }

    private fun showResourceEditorNotifications(project: Project) {
        val selected = FileEditorManager.getInstance(project).selectedEditor ?: return
        if (ResourceEditor.isResourceEditor(selected)) {
            try {
                ResourceEditor.updateEditor(selected, project)
            } catch (e: Exception) {
                ErrorNotification.show(
                    selected,
                    project,
                    "Error contacting cluster. Make sure it's reachable, document valid, api version supported etc.",
                    e.cause ?: e)
            }
        }
    }
}