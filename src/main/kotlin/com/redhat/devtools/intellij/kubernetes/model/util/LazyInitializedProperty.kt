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
package com.redhat.devtools.intellij.kubernetes.model.util

import kotlin.reflect.KProperty

/**
 * A writable and nullable property that is lazily initialized.
 * Kotlin `lazy` provides a read-only property. `lateinit` may only be used for non-nullable properties.
 */
open class LazyInitializedProperty<T>(private val initializer: () -> T?) {

    private var initialized = false
    private var value: T? = null

    @Volatile
    var lazyHolder = lazy { initializer.invoke() }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        this.initialized = true
        this.value = value
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        if (!initialized) {
            this.value = lazyHolder.value
        }
        return value
    }
}