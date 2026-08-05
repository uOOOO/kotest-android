package br.com.colman.kotest.android.extensions.robolectric

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Installs a Dispatchers.Main delegate backed by the Robolectric main Looper.
 *
 * The sandbox classloader does not acquire kotlinx.coroutines (see
 * [ContainedRobolectricRunner.createClassLoaderConfig]), so coroutines-android's
 * dispatcher factory never sees the sandbox Looper and Dispatchers.Main stays
 * unusable inside tests. This object is loaded through the sandbox classloader
 * (AndroidSandbox.bootstrappedClass) so its android.* references resolve to the
 * sandbox while kotlinx.coroutines resolves to the shared parent copy, letting it
 * hand the sandbox main Looper to the parent's Dispatchers.Main via setMain.
 *
 * [CoroutineDispatcher.isDispatchNeeded] mirrors Dispatchers.Main.immediate:
 * code already on the main looper runs inline, which lifecycle-aware collectors
 * (repeatOnLifecycle and friends) rely on. A Main dispatcher installed by the
 * consumer beforehand (e.g. Dispatchers.setMain with a TestDispatcher in a spec
 * listener) is respected: [install] probes Main and backs off when it is already
 * usable, because overriding it would detach Main-dispatched work from the
 * scheduler the consumer's virtual-time machinery controls and deadlock
 * runTest-style bodies. [uninstall] only resets what [install] actually set.
 */
internal object MainDispatcherInstaller {

  private var installed = false

  @JvmStatic
  fun install() {
    if (mainAlreadyUsable()) return
    val handler = Handler(Looper.getMainLooper())
    val dispatcher = object : CoroutineDispatcher() {
      override fun dispatch(context: CoroutineContext, block: Runnable) {
        handler.post(block)
      }

      override fun isDispatchNeeded(context: CoroutineContext): Boolean =
        Looper.myLooper() != Looper.getMainLooper()
    }
    Dispatchers.setMain(dispatcher)
    installed = true
  }

  /** Counterpart of [install] — resets Dispatchers.Main only if [install] set it. */
  @JvmStatic
  fun uninstall() {
    if (!installed) return
    installed = false
    Dispatchers.resetMain()
  }

  /**
   * An unset coroutines-test TestMainDispatcher throws on use; a usable Main means
   * either the platform provided one or the consumer already called setMain.
   */
  private fun mainAlreadyUsable(): Boolean = runCatching {
    Dispatchers.Main.isDispatchNeeded(EmptyCoroutineContext)
  }.isSuccess
}
