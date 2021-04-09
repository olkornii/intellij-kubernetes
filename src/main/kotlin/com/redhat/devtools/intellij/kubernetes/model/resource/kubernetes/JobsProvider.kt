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
package com.redhat.devtools.intellij.kubernetes.model.resource.kubernetes

import io.fabric8.kubernetes.api.model.batch.Job
import io.fabric8.kubernetes.client.BatchAPIGroupClient
import com.redhat.devtools.intellij.kubernetes.model.resource.NamespacedResourcesProvider
import com.redhat.devtools.intellij.kubernetes.model.resource.ResourceKind
import com.redhat.devtools.intellij.kubernetes.model.resource.ResourceOperation
import java.util.function.Supplier

class JobsProvider(client: BatchAPIGroupClient)
    : NamespacedResourcesProvider<Job, BatchAPIGroupClient>(client) {

    companion object {
        val KIND = ResourceKind.create(Job::class.java)
    }

    override val kind = KIND

    override fun getNamespacedOperation(namespace: String): Supplier<ResourceOperation<Job>?> {
        return Supplier { client.jobs().inNamespace(namespace) }
    }

    override fun getNonNamespacedOperation(): Supplier<ResourceOperation<Job>?> {
        return Supplier { client.jobs().inAnyNamespace() as ResourceOperation<Job> }
    }
}
