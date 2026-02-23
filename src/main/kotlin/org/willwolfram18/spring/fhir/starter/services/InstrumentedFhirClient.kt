package org.willwolfram18.spring.fhir.starter.services

import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.gclient.IQuery
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
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
        val fhirOperation = "${resource.simpleName}.Search (${fhirClient.fhirContext.version.version.name})"
        val spanBuilder = tracer.spanBuilder()
            .kind(Span.Kind.CLIENT)
            // TODO consider using "host name instead of full serverBase
            .remoteServiceName(fhirClient.serverBase)
            .name(fhirOperation)

        if (tracer.currentSpan() != null) {
            spanBuilder.setParent(tracer.currentSpan()!!.context())
        }

        val searchSpan = spanBuilder.start()
        // start a Micrometer Timer sample so we can record duration with outcome-tag afterwards
        val runningTimer = Timer.start(meterRegistry)
        var outcome = "success"
        var result: Bundle
        tracer.withSpan(searchSpan).use {
            val query = fhirClient.search<Bundle>()
                .forResource(resource)

            query.queryBuilder()

            try {
                result = query.returnBundle(Bundle::class.java).execute()
            } catch (e: Exception) {
                outcome = "error"
                searchSpan.error(e)
                throw e
            } finally {
                searchSpan.end()
                // Register (or get) a Timer with the operation and outcome tags and stop the sample
                val timer = Timer.builder("fhir.client.request.duration")
                    .description("Duration of FHIR client operations")
                    .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                    .tags("operation", fhirOperation, "outcome", outcome)
                    .register(meterRegistry)

                runningTimer.stop(timer)
            }
        }

        return result
    }
}