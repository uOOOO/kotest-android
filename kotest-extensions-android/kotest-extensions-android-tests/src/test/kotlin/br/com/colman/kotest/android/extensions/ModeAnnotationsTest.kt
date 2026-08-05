package br.com.colman.kotest.android.extensions

import android.os.Looper
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.GetInstallerPackageNameMode
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import org.robolectric.annotation.ResourcesMode
import org.robolectric.annotation.SQLiteMode
import org.robolectric.config.ConfigurationRegistry

@RobolectricTest
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@SQLiteMode(SQLiteMode.Mode.LEGACY)
@ConscryptMode(ConscryptMode.Mode.OFF)
@ResourcesMode(ResourcesMode.Mode.NATIVE)
@GetInstallerPackageNameMode(GetInstallerPackageNameMode.Mode.REALISTIC)
class ModeAnnotationsOverrideTest : StringSpec({
  "Mode annotations on the spec class override the Robolectric defaults" {
    ConfigurationRegistry.get(LooperMode.Mode::class.java) shouldBe
      LooperMode.Mode.INSTRUMENTATION_TEST
    ConfigurationRegistry.get(GraphicsMode.Mode::class.java) shouldBe GraphicsMode.Mode.NATIVE
    ConfigurationRegistry.get(SQLiteMode.Mode::class.java) shouldBe SQLiteMode.Mode.LEGACY
    ConfigurationRegistry.get(ConscryptMode.Mode::class.java) shouldBe ConscryptMode.Mode.OFF
    ConfigurationRegistry.get(ResourcesMode.Mode::class.java) shouldBe ResourcesMode.Mode.NATIVE
    ConfigurationRegistry.get(GetInstallerPackageNameMode.Mode::class.java) shouldBe
      GetInstallerPackageNameMode.Mode.REALISTIC
  }

  "INSTRUMENTATION_TEST runs the main looper on a thread of its own" {
    Looper.getMainLooper().thread shouldNotBe Thread.currentThread()
  }
})

@RobolectricTest
class ModeAnnotationsDefaultTest : StringSpec({
  // ConscryptMode is left out: its default depends on the host (OFF on mac/aarch64, ON elsewhere).
  "Without mode annotations the Robolectric defaults apply" {
    ConfigurationRegistry.get(LooperMode.Mode::class.java) shouldBe LooperMode.Mode.PAUSED
    ConfigurationRegistry.get(GraphicsMode.Mode::class.java) shouldBe GraphicsMode.Mode.LEGACY
    ConfigurationRegistry.get(SQLiteMode.Mode::class.java) shouldBe SQLiteMode.Mode.NATIVE
    ConfigurationRegistry.get(ResourcesMode.Mode::class.java) shouldBe ResourcesMode.Mode.BINARY
    ConfigurationRegistry.get(GetInstallerPackageNameMode.Mode::class.java) shouldBe
      GetInstallerPackageNameMode.Mode.LEGACY
  }

  "PAUSED runs the test body on the main looper thread" {
    Looper.getMainLooper().thread shouldBe Thread.currentThread()
  }
})

@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
abstract class ModeAnnotationsParentTest : StringSpec()

@RobolectricTest
class ModeAnnotationsInheritedTest : ModeAnnotationsParentTest() {
  init {
    "Mode annotations are picked up from parent classes" {
      ConfigurationRegistry.get(LooperMode.Mode::class.java) shouldBe
        LooperMode.Mode.INSTRUMENTATION_TEST
    }
  }
}
