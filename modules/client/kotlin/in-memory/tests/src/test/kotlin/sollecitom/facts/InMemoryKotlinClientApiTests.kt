package sollecitom.facts

import assertk.assertThat
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import sollecitom.libs.swissknife.test.utils.execution.utils.test

class InMemoryKotlinClientApiTests : KotlinClientApiTestSpecification {

    @Test
    fun `something who knows`() = test {

        assertThat(true).isTrue()
    }
}