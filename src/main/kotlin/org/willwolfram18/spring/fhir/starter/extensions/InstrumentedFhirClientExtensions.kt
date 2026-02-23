package org.willwolfram18.spring.fhir.starter.extensions

import ca.uhn.fhir.rest.gclient.*
import org.hl7.fhir.r4.model.*
import org.willwolfram18.spring.fhir.starter.services.*

inline fun <reified T : Resource> InstrumentedFhirClient.search(
    noinline block: (IQuery<Bundle>.() -> IQuery<Bundle>) = {
        this
    }
): Bundle =
    search(T::class.java, block)

inline fun <reified T : Resource> InstrumentedFhirClient.read(
    noinline block: (IReadTyped<T>.() -> IReadExecutable<T>)
): T = read(T::class.java, block)

inline fun <reified T : IClientExecutable<*, R>, R> IClientExecutable<T, R>.executeJson(): R =
    encodedJson().execute()
