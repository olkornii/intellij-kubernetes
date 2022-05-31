package com.redhat.devtools.intellij.kubernetes

import com.intellij.ide.plugins.cl.PluginClassLoader
import com.intellij.openapi.application.PreloadingActivity
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import java.lang.reflect.Field

class KubernetesPreloadingActivity : PreloadingActivity() {

    override fun preload(progress: ProgressIndicator) {
        hackClassLoader();
    }

    private fun hackClassLoader() {
        val loader: ClassLoader = KubernetesPreloadingActivity::class.java.classLoader
        if (loader !is PluginClassLoader) {
            return
        }
        try {
            val parentsField: Field = loader.javaClass.getDeclaredField("myParents")
            parentsField.isAccessible = true
            val parents = parentsField.get(loader) as? Array<ClassLoader> ?: return
            if (parents.size > 1) {
                val first = parents[0]
                parents[0] = parents[parents.size - 1]
                parents[parents.size - 1] = first
            }
        } catch (e: NoSuchFieldException) {
            logger<KubernetesPreloadingActivity>().error(e.localizedMessage, e)
        } catch (e: IllegalAccessException) {
            logger<KubernetesPreloadingActivity>().error(e.localizedMessage, e)
        }
    }
}