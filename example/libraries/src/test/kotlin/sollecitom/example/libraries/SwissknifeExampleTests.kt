package sollecitom.example.libraries

import assertk.assertThat
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import sollecitom.libs.swissknife.test.utils.execution.utils.test

@TestInstance(PER_CLASS)
class SwissknifeExampleTests { // TODO remove

    @Test
    fun `example test`() = test {

        assertThat(true).isTrue()
    }
}