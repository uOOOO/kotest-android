package br.com.colman.kotest.android.extensions.robolectric

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.model.FrameworkMethod
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.internal.bytecode.InstrumentationConfiguration
import org.robolectric.pluginapi.config.ConfigurationStrategy
import org.robolectric.pluginapi.config.Configurer
import org.robolectric.plugins.HierarchicalConfigurationStrategy
import org.robolectric.util.inject.Injector
import java.lang.reflect.Method
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@RunWith(Enclosed::class)
internal class ContainedRobolectricRunner(
  config: Config
) : RobolectricTestRunner(PlaceholderTest::class.java, kotestInjector(config)) {

  private val placeHolderMethod: FrameworkMethod = children[0]
  val sdkEnvironment = getSandbox(placeHolderMethod).also {
    configureSandbox(it, placeHolderMethod)
  }
  private val bootStrapMethod = sdkEnvironment.bootstrappedClass<Any>(testClass.javaClass)
    .getMethod(PlaceholderTest::bootStrapMethod.name)

  /**
   * Single dedicated thread that hosts both the Robolectric environment setup ([containedBefore])
   * and the test body. Robolectric binds thread-sensitive state (most notably the main Looper in
   * PAUSED mode) to the thread that runs the environment setup, so the test body must execute on
   * that same thread for main-looper-bound APIs (Robolectric.buildActivity, ShadowLooper.idle, ...)
   * to work. Coroutine dispatch gives no such guarantee by itself, hence the explicit pinning.
   */
  val environmentDispatcher: ExecutorCoroutineDispatcher =
    Executors.newSingleThreadExecutor { runnable ->
      Thread(runnable, "kotest-robolectric-${environmentThreadCount.incrementAndGet()}").apply {
        isDaemon = true
      }
    }.asCoroutineDispatcher()

  fun containedBefore() {
    Thread.currentThread().contextClassLoader = sdkEnvironment.robolectricClassLoader
    super.beforeTest(sdkEnvironment, placeHolderMethod, bootStrapMethod)
  }

  fun containedAfter() {
    super.afterTest(placeHolderMethod, bootStrapMethod)
    super.finallyAfterTest(placeHolderMethod)
    Thread.currentThread().contextClassLoader = ContainedRobolectricRunner::class.java.classLoader
  }

  override fun createClassLoaderConfig(method: FrameworkMethod?): InstrumentationConfiguration {
    return InstrumentationConfiguration.Builder(super.createClassLoaderConfig(method))
      .doNotAcquirePackage("io.kotest")
      .doNotAcquirePackage("kotlinx.coroutines")
      .build()
  }

  class PlaceholderTest {
    @org.junit.Test
    fun testPlaceholder() {
    }

    fun bootStrapMethod() {
    }
  }

  class KotestHierarchicalConfigurationStrategy(
    private val config: Config,
    configurers: Array<Configurer<*>>
  ) : HierarchicalConfigurationStrategy(*configurers) {
    override fun getConfig(testClass: Class<*>?, method: Method?): ConfigurationImpl {
      val configurationImpl = super.getConfig(testClass, method)
      val config = (configurationImpl.get(Config::class.java) as Config)
      val newConfig = Config.Builder(config).overlay(this.config).build()
      configurationImpl.map()[Config::class.java] = newConfig
      return configurationImpl
    }
  }

  companion object {
    private val environmentThreadCount = AtomicInteger()

    private fun kotestInjector(config: Config): Injector {
      val defaultInjector = defaultInjector()
        .bind(Config::class.java, config)
        .bind(ConfigurationStrategy::class.java, KotestHierarchicalConfigurationStrategy::class.java)
        .build()
      return Injector.Builder(defaultInjector, ContainedRobolectricRunner::class.java.classLoader)
        .build()
    }
  }
}
