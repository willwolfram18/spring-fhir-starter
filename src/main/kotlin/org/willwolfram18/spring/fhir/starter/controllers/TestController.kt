package org.willwolfram18.spring.fhir.starter.controllers

import org.hl7.fhir.r4.model.Patient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.willwolfram18.spring.fhir.starter.services.InstrumentedFhirClient

@RestController
@RequestMapping("/test")
class TestController(
    private val instrumentedFhirClient: InstrumentedFhirClient
) {
    @GetMapping("/patients/search")
    fun patientSearch(): String {
        try {
            val result = instrumentedFhirClient.search(Patient::class.java) {
                where(Patient.NAME.matches().value("Smith"))
            }

            return "Success"
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error: ${e.message}"
        }
    }
}