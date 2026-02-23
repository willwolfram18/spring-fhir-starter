package org.willwolfram18.spring.fhir.starter.services

import ca.uhn.fhir.rest.gclient.*
import org.hl7.fhir.r4.model.*

inline fun <reified T : Resource> InstrumentedFhirClient.search(
    noinline block: (IQuery<Bundle>.() -> IQuery<Bundle>) = {
        this
    }
): Bundle =
    search(T::class.java, block)
