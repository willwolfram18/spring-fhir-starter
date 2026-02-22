package org.willwolfram18.spring.fhir.starter.integration

import ca.uhn.fhir.rest.gclient.StringClientParam
import io.micrometer.tracing.annotation.NewSpan
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.trace.SdkTracerProvider
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Patient
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.willwolfram18.spring.fhir.starter.services.InstrumentedFhirClient

class PatientResourceTests @Autowired constructor(
    private val instrumentedFhirClient: InstrumentedFhirClient,
    private val tracer: SdkTracerProvider
) : IntegrationTestBase() {

    @Test
    @NewSpan("native-client")
    fun `search for patients with name 'Smith'`() {
        val searchBundle = fhirClient.search<Bundle>()
        val forBundle = searchBundle.forResource(Patient::class.java)
        val where = forBundle.where(StringClientParam("name").matches().value("Smith"))
        val response = where
            .returnBundle(Bundle::class.java)
            .execute()

        // Assert that the response contains patients with the name 'Smith'
        assert(response.entry.any { it.resource.resourceType.name == "Patient" })
    }

    @Test
    @NewSpan("custom-client")
    fun `instrumented search`() {
        val response = instrumentedFhirClient.search(Patient::class.java) {
            where(StringClientParam("name").matches().value("Smith"))
        }

        // Assert that the response contains patients with the name 'Smith'
        assert(response.entry.any { it.resource.resourceType.name == "Patient" })
    }
}