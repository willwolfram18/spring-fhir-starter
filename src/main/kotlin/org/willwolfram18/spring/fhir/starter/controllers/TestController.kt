package org.willwolfram18.spring.fhir.starter.controllers

import org.hl7.fhir.r4.model.*
import org.springframework.stereotype.*
import org.springframework.web.bind.annotation.*
import org.willwolfram18.spring.fhir.starter.extensions.read
import org.willwolfram18.spring.fhir.starter.extensions.search
import org.willwolfram18.spring.fhir.starter.services.*

@Controller
class TestController(
    private val fhirClient: InstrumentedFhirClient
) {
    @GetMapping("/test/patients/search")
    fun searchPatients(): List<String> {
        val result = fhirClient.search<Patient> {
            where(Patient.NAME.matches().value("Smith"))
        }

        return result.entry.mapNotNull { it.resource as? Patient }
            .map { it.idPart }
    }

    @GetMapping("/test/conditions/create")
    fun create(): String {
        val condition = Condition()

        val result = fhirClient.create(condition)

        return "Success"
    }

    @GetMapping("/test/patient/read/{id}")
    fun readPatient(@PathVariable id: String): String {
        val result = fhirClient.read<Patient> {
            withId(id)
        }

        return result.name.joinToString()
    }
}