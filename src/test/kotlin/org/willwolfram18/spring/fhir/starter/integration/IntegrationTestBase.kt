package org.willwolfram18.spring.fhir.starter.integration

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.client.api.IGenericClient
import io.opentelemetry.sdk.trace.SdkTracerProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.willwolfram18.spring.fhir.starter.InstrumentedFhirClient
import kotlin.time.Duration.Companion.seconds

@SpringBootTest(
    classes = [
        InstrumentedFhirClient::class
    ]
)
@ActiveProfiles("integration")
@Import(IntegrationTestBase.FhirClientConfig::class)
@EnableAutoConfiguration
@AutoConfigureObservability
abstract class IntegrationTestBase {
    @Autowired
    lateinit var fhirClient: IGenericClient

    @Autowired
    private lateinit var otelTracer: SdkTracerProvider

    @AfterEach
    fun tearDown() {
        otelTracer.forceFlush()
        runBlocking {
            delay(5.seconds)
        }
    }

    @TestConfiguration
    open class FhirClientConfig {
        @Bean
        open fun fhirClient(@Value("\${app.fhir.server}") fhirServer: String): IGenericClient {
            val context = FhirContext.forR4()
            return context.newRestfulGenericClient(fhirServer)
        }
    }
}