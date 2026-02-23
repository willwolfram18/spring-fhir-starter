package org.willwolfram18.spring.fhir.starter.integration

import ca.uhn.fhir.rest.gclient.StringClientParam
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Patient
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.willwolfram18.spring.fhir.starter.services.InstrumentedFhirClient
import org.willwolfram18.spring.fhir.starter.services.search
import org.willwolfram18.spring.fhir.starter.services.read
import kotlin.test.assertEquals

class PatientResourceTests @Autowired constructor(
    private val instrumentedFhirClient: InstrumentedFhirClient,
) : IntegrationTestBase() {

    // TODO need to validate traces
    @Test
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
    fun `instrumented search`() {
        val response = instrumentedFhirClient.search<Patient> {
            where(StringClientParam("name").matches().value("Smith"))
        }

        // Assert that the response contains patients with the name 'Smith'
        assert(response.entry.any { it.resource.resourceType.name == "Patient" })
    }

    @Test
    fun `instrumented read`() {
        val response = instrumentedFhirClient.read<Patient> {
            withId("89185911")
        }

        assertEquals("89185911", response.idElement.idPart)
    }
}