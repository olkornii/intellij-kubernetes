/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.kubernetes.editor

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.redhat.devtools.intellij.common.utils.UIHelper
import com.redhat.devtools.intellij.kubernetes.editor.notification.DeletedNotification
import com.redhat.devtools.intellij.kubernetes.editor.notification.ErrorNotification
import com.redhat.devtools.intellij.kubernetes.editor.notification.PushNotification
import com.redhat.devtools.intellij.kubernetes.editor.notification.ReloadNotification
import com.redhat.devtools.intellij.kubernetes.model.ClientConfig
import com.redhat.devtools.intellij.kubernetes.model.ClusterResource
import com.redhat.devtools.intellij.kubernetes.model.ModelChangeObservable
import com.redhat.devtools.intellij.kubernetes.model.Notification
import com.redhat.devtools.intellij.kubernetes.model.ResourceException
import com.redhat.devtools.intellij.kubernetes.model.util.Clients
import com.redhat.devtools.intellij.kubernetes.model.util.createClients
import com.redhat.devtools.intellij.kubernetes.model.util.trimWithEllipsis
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.client.KubernetesClient

/**
 * An adapter for [FileEditor] instances that allows to push or load the editor content to/from a remote cluster.
 */
open class ResourceEditor protected constructor(
    private var localCopy: HasMetadata?,
    private val editor: FileEditor,
    private val project: Project,
    private val clients: Clients<out KubernetesClient>,
    private val resourceFactory: (editor: FileEditor, clients: Clients<out KubernetesClient>) -> HasMetadata? =
        EditorResourceFactory::create,
    // for mocking purposes
    private val createFileForVirtual: (file: VirtualFile?) -> ResourceFile? =
        ResourceFile.Factory::create,
    // for mocking purposes
    private val createFileForResource: (resource: HasMetadata) -> ResourceFile? =
        ResourceFile.Factory::create
) {
    companion object {
        private val KEY_RESOURCE_EDITOR = Key<ResourceEditor>(ResourceEditor::class.java.name)

        /**
         * Opens a new editor for the given [HasMetadata] and [Project].
         *
         * @param resource to edit
         * @param project that this editor belongs to
         * @return the new [ResourceEditor] that was opened
         */
        fun open(resource: HasMetadata, project: Project): ResourceEditor? {
            val file = ResourceFile.create(resource)?.write(resource) ?: return null
            val editor = FileEditorManager.getInstance(project)
                .openFile(file, true, true)
                .getOrNull(0)
                ?: return null
            val clients = createClients(ClientConfig {}) ?: return null
            return create(resource, editor, project, clients)
        }

        /**
         * Returns the existing or creates a new [ResourceEditor] for the given [FileEditor] and [Project].
         * Returns `null` if the given [FileEditor] is `null`, not a [ResourceEditor] or the given [Project] is `null`.
         *
         * @return the existing or a new [ResourceEditor].
         */
        fun get(editor: FileEditor?, project: Project?): ResourceEditor? {
            if (editor == null
                || !isResourceEditor(editor)
                || project == null) {
                return null
            }
            return editor.getUserData(KEY_RESOURCE_EDITOR)
                ?: create(editor, project)
        }

        private fun create(editor: FileEditor, project: Project): ResourceEditor? {
            try {
                val clients = createClients(ClientConfig {}) ?: return null
                val resource = EditorResourceFactory.create(editor, clients) ?: return null
                return create(resource, editor, project, clients)
            } catch(e: ResourceException) {
                ErrorNotification.show(editor, project,e.message ?: "", e.cause?.message)
                return null
            }
        }

        private fun create(
            resource: HasMetadata,
            editor: FileEditor,
            project: Project,
            clients: Clients<out KubernetesClient>
        ): ResourceEditor {
            val resourceEditor = ResourceEditor(resource, editor, project, clients)
            editor.putUserData(KEY_RESOURCE_EDITOR, resourceEditor)
            return resourceEditor
        }

        private fun isResourceEditor(editor: FileEditor?): Boolean {
            return ResourceFile.isResourceFile(editor?.file)
        }
    }

    private var editorResource: HasMetadata? = null
    private var oldClusterResource: ClusterResource? = null
    private var _clusterResource: ClusterResource? = null
    private val clusterResource: ClusterResource?
        get() {
            synchronized(this) {
                if (_clusterResource == null
                    || false == _clusterResource?.isSameResource(editorResource)) {
                    oldClusterResource = _clusterResource
                    oldClusterResource?.close()
                    _clusterResource = createClusterResource(editorResource, clients)
                }
                return _clusterResource
            }
        }

    /**
     * Updates this editors notifications and title.
     *
     * @see [FileEditor.isValid]
     */
    fun updateEditor() {
        try {
            this.editorResource = resourceFactory.invoke(editor, clients) ?: return
            updateEditor(editorResource, clusterResource, oldClusterResource)
        } catch (e: ResourceException) {
            showErrorNotification(editor, project, e)
        }
    }

    private fun updateEditor(resource: HasMetadata?, clusterResource: ClusterResource?, oldClusterResource: ClusterResource?) {
            if (resource == null
                || clusterResource == null) {
                return
            }
            if (clusterResource != oldClusterResource) {
                // new cluster resource was created, resource has thus changed
                renameEditor(resource, editor)
            }
            showNotifications(clusterResource, resource, editor, project)
    }

    private fun renameEditor(resource: HasMetadata, editor: FileEditor) {
        val file = createFileForVirtual.invoke(editor.file) ?: return
        val newFile = createFileForResource.invoke(resource)
        if (!file.hasEqualBasePath(newFile)) {
            file.rename(resource)
        }
    }

    private fun showNotifications(
        clusterResource: ClusterResource,
        resource: HasMetadata,
        editor: FileEditor,
        project: Project
    ) {
        hideNotifications(editor, project)
        when {
            clusterResource.isDeleted()
                    && !clusterResource.isModified(resource) ->
                showDeletedNotification(editor, resource, project)
            clusterResource.isOutdated(resource) ->
                replaceEditorOrShowReloadNotification(editor, resource, project)
            clusterResource.canPush(resource) ->
                PushNotification.show(editor, project)
        }
    }

    protected open fun showDeletedNotification(
        editor: FileEditor,
        resource: HasMetadata,
        project: Project
    ) {
        DeletedNotification.show(editor, resource, project)
    }

    private fun replaceEditorOrShowReloadNotification(
        editor: FileEditor,
        resource: HasMetadata,
        project: Project
    ) {
        val resourceOnCluster = clusterResource?.get(false)
        if (!hasLocalChanges(resource)
            && resourceOnCluster != null) {
            replaceEditorContent(resourceOnCluster)
        } else {
            showReloadNotification(editor, resource, project)
        }
    }

    protected open fun showReloadNotification(
        editor: FileEditor,
        resource: HasMetadata,
        project: Project
    ) {
        ReloadNotification.show(editor, resource, project)
    }

    private fun showErrorNotification(editor: FileEditor, project: Project, e: Throwable) {
        hideNotifications(editor, project)
        ErrorNotification.show(
            editor, project,
            e.message ?: "", e.cause?.message
        )
    }

    private fun hideNotifications(editor: FileEditor, project: Project) {
        ErrorNotification.hide(editor, project)
        ReloadNotification.hide(editor, project)
        DeletedNotification.hide(editor, project)
        PushNotification.hide(editor, project)
    }

    /**
     * Replaces the content of this editor with the resource that exists on the cluster.
     * Does nothing if it doesn't exist.
     */
    fun replaceEditorContent() {
        val resource = clusterResource?.get(false) ?: return
        replaceEditorContent(resource)
    }

    private fun replaceEditorContent(resource: HasMetadata) {
        UIHelper.executeInUI {
            if (editor.file != null) {
                val file = createFileForVirtual.invoke(editor.file)?.write(resource)
                if (file != null) {
                    FileDocumentManager.getInstance().reloadFiles(file)
                    this.localCopy = resource
                }
            }
        }
    }

    /**
     * Returns `true` if the resource in this editor exists on the cluster. Returns `false` otherwise.
     * @return true if the resource in this editor exists on the cluster
     */
    fun existsOnCluster(): Boolean {
        return clusterResource?.exists() ?: false
    }

    fun isOutdated(): Boolean {
        this.editorResource = resourceFactory.invoke(editor, clients)
        return clusterResource?.isOutdated(editorResource) ?: false
    }

    /**
     * Pushes the editor content to the cluster.
     */
    fun push() {
        try {
            this.editorResource = resourceFactory.invoke(editor, clients) ?: return
            this.localCopy = editorResource
            push(localCopy, clusterResource)
        } catch (e: Exception) {
            showErrorNotification(editor, project, e)
        }
    }

    private fun push(resource: HasMetadata?, clusterResource: ClusterResource?) {
        try {
            if (resource == null
                || clusterResource == null) {
                return
            }
            hideNotifications(editor, project)
            val updatedResource = clusterResource.push(resource) ?: return
            replaceEditorContent(updatedResource)
        } catch (e: ResourceException) {
            logger<ResourceEditor>().warn(e)
            Notification().error(
                "Could not save ${
                    if (resource != null) {
                        "${resource.kind} ${resource.metadata.name}"
                    } else {
                        ""
                    }
                } to cluster",
                trimWithEllipsis(e.message, 300) ?: ""
            )
        }
    }

    /**
     * Returns `true` if the given resource is dirty aka has modifications that were not pushed.
     *
     * @param resource to be checked for modification
     * @return true if the resource is dirty
     */
    private fun hasLocalChanges(resource: HasMetadata): Boolean {
        return resource != this.localCopy
    }

    fun startWatch(): ResourceEditor {
        clusterResource?.watch()
        return this
    }

    fun stopWatch() {
        clusterResource?.stopWatch()
    }

    private fun createClusterResource(resource: HasMetadata?, clients: Clients<out KubernetesClient>?): ClusterResource? {
        if (resource == null
            || clients == null) {
            return null
        }
        val clusterResource = ClusterResource(resource, clients)
        clusterResource.addListener(onResourceChanged())
        clusterResource.watch()
        return clusterResource
    }

    private fun onResourceChanged(): ModelChangeObservable.IResourceChangeListener {
        return object : ModelChangeObservable.IResourceChangeListener {
            override fun added(added: Any) {
            }

            override fun removed(removed: Any) {
                updateEditor()
            }

            override fun modified(modified: Any) {
                updateEditor()
            }
        }
    }

    fun closeClusterResource() {
        clusterResource?.close()
    }

    /**
     * Enables editing of non project files for the given file. This prevents the IDE from presenting the
     * "edit non-project" files dialog.
     *
     * @param file to enable the (non-project file) editing for
     */
    fun enableNonProjectFileEditing(file: VirtualFile? = editor.file) {
        if (file == null) {
            return
        }
        createFileForVirtual(file)?.enableNonProjectFileEditing()
    }
}

