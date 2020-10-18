package asceapps.weatheria

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

	private var appCtx: Context? = null

	@Before
	@Throws(Exception::class)
	fun setUp() {
		appCtx = InstrumentationRegistry.getInstrumentation().targetContext
	}

	@After
	@Throws(Exception::class)
	fun tearDown() {
		appCtx = null
	}

	@Test
	fun ensureAppContext() {
		assertEquals("asceapps.weatheria", appCtx?.packageName)
	}

	@Test
	fun initNewEntry() {
	}
}