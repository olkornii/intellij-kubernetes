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

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class LazyInitializedPropertyTest {

    private var initializerCalled = false
    private val value = "Garfield"
    private var character: String? by LazyInitializedProperty {
        initializerCalled = true
        value
    }

    @Test
    fun `#get should call initializer`() {
        // given
        assertThat(initializerCalled).isFalse()
        // when
        character
        // then
        assertThat(initializerCalled).isTrue()
    }

    @Test
    fun `#get should NOT call initializer if value is set`() {
        // given
        assertThat(initializerCalled).isFalse()
        // when
        character = "lazy cat"
        // then
        assertThat(initializerCalled).isFalse()
    }

    @Test
    fun `#get should return value that was set`() {
        // given
        character = "lazy cat"
        // when
        val value = character
        // then
        assertThat(value).isEqualTo("lazy cat")
    }
}