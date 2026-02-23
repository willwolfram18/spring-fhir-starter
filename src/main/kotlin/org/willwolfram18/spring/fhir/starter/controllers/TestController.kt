package org.willwolfram18.spring.fhir.starter.controllers

import org.hl7.fhir.r4.model.Patient
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.willwolfram18.spring.fhir.starter.services.InstrumentedFhirClient

@Controller
class TestController(
    private val fhirClient: InstrumentedFhirClient
) {
    @GetMapping("/test/patients/search")
    fun searchPatients(): String {
        val result = fhirClient.search(Patient::class.java) {
            where(Patient.NAME.matches().value("Smith"))
        }

        return "Success"
    }
}