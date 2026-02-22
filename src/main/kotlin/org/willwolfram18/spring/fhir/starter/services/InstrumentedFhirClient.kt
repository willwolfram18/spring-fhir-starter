package org.willwolfram18.spring.fhir.starter.services

import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.gclient.IQuery
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.tracing.Span
import io.micrometer.tracing.Tracer
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Resource
import org.springframework.stereotype.Service

@Service
class InstrumentedFhirClient(
    private val fhirClient: IGenericClient,
    private val tracer: Tracer,
    private val meterRegistry: MeterRegistry,
) {

    fun <T : Resource> search(resource: Class<T>, queryBuilder: IQuery<Bundle>.() -> IQuery<Bundle>): Bundle {
        val spanBuilder = tracer.spanBuilder()
            .kind(Span.Kind.CLIENT)
            // TODO consider using "host name instead of full serverBase
            .remoteServiceName(fhirClient.serverBase)
            .name("${resource.simpleName}.Search (${fhirClient.fhirContext.version.version.name})")

        if (tracer.currentSpan() != null) {
            spanBuilder.setParent(tracer.currentSpan()!!.context())
        }

        val searchSpan = spanBuilder.start()
        var result: Bundle
        tracer.withSpan(searchSpan).use {
            val query = fhirClient.search<Bundle>()
                .forResource(resource)

            query.queryBuilder()

            try {
                result = query.returnBundle(Bundle::class.java).execute()
            } catch (e: Exception) {
                searchSpan.error(e)
                throw e
            } finally {
                searchSpan.end()
            }
        }

        return result
    }
}