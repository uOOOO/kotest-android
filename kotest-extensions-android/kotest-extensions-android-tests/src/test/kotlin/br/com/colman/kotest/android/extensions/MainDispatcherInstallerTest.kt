package br.com.colman.kotest.android.extensions

import android.os.Looper
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@RobolectricTest
class MainDispatcherInstallerTest : StringSpec({
  "Dispatchers.Main is usable and runs on the Robolectric main looper" {
    // Without the installed delegate this either throws
    // ("Module with the Main dispatcher is missing") or hangs on dispatch.
    withContext(Dispatchers.Main) {
      Looper.myLooper() shouldBe Looper.getMainLooper()
    }
  }
})
