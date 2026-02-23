package org.willwolfram18.spring.fhir.starter.services

import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.gclient.ICreateTyped
import ca.uhn.fhir.rest.gclient.IQuery
import ca.uhn.fhir.rest.gclient.IReadExecutable
import ca.uhn.fhir.rest.gclient.IReadTyped
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

    private val fhirVersion by lazy {
        fhirClient.fhirContext.version.version.name
    }

    fun <T : Resource> search(resource: Class<T>, queryBuilder: (IQuery<Bundle>.() -> IQuery<Bundle>) = { this }): Bundle {
        val operationName = operation(resource, "Search")
        val spanBuilder = newSpan(operationName)

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
                .run(queryBuilder)

            try {
                result = query.returnBundle(Bundle::class.java).execute()
            } catch (e: Exception) {
                outcome = "error"
                searchSpan.tag("outcome", outcome.uppercase())
                    .error(e)
                throw e
            } finally {
                searchSpan.end()
                // Register (or get) a Timer with the operation and outcome tags and stop the sample
                val timer = Timer.builder("fhir.client.request.duration")
                    .description("Duration of FHIR client operations")
                    .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                    .tags("operation", operationName, "fhir_version", fhirVersion, "outcome", outcome.uppercase())
                    .register(meterRegistry)

                runningTimer.stop(timer)
            }
        }

        return result
    }

    fun <T : Resource> create(resource: T, block: (ICreateTyped.() -> ICreateTyped) = { this }): MethodOutcome? {
        val operationName = operation(resource.javaClass, "Create")
        val spanBuilder = newSpan(operationName)

        if (tracer.currentSpan() != null) {
            spanBuilder.setParent(tracer.currentSpan()!!.context())
        }

        val searchSpan = spanBuilder.start()
        // start a Micrometer Timer sample so we can record duration with outcome-tag afterwards
        val runningTimer = Timer.start(meterRegistry)
        var outcome = "success"
        var result: MethodOutcome
        tracer.withSpan(searchSpan).use {
            val createRequest = fhirClient.create()
                .resource(resource)
                .run(block)

            try {
                result = createRequest.execute()
            } catch (e: Exception) {
                outcome = "error"
                searchSpan.tag("outcome", outcome.uppercase())
                    .error(e)
                throw e
            } finally {
                searchSpan.end()
                // Register (or get) a Timer with the operation and outcome tags and stop the sample
                val timer = Timer.builder("fhir.client.request.duration")
                    .description("Duration of FHIR client operations")
                    .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                    .tags("operation", operationName, "fhir_version", fhirVersion, "outcome", outcome.uppercase())
                    .register(meterRegistry)

                runningTimer.stop(timer)
            }
        }

        return result
    }

    fun <T : Resource> read(resource: Class<T>, block: IReadTyped<T>.() -> IReadExecutable<T>): T {
        val operationName = operation(resource, "Read")
        val spanBuilder = newSpan(operationName)

        if (tracer.currentSpan() != null) {
            spanBuilder.setParent(tracer.currentSpan()!!.context())
        }

        val searchSpan = spanBuilder.start()
        // start a Micrometer Timer sample so we can record duration with outcome-tag afterwards
        val runningTimer = Timer.start(meterRegistry)
        var outcome = "success"
        var result: T
        tracer.withSpan(searchSpan).use {
            try {
                result = fhirClient.read()
                    .resource(resource)
                    .run(block)
                    .execute()
            } catch (e: Exception) {
                outcome = "error"
                searchSpan.tag("outcome", outcome.uppercase())
                    .error(e)
                throw e
            } finally {
                searchSpan.end()
                // Register (or get) a Timer with the operation and outcome tags and stop the sample
                val timer = Timer.builder("fhir.client.request.duration")
                    .description("Duration of FHIR client operations")
                    .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                    .tags("operation", operationName, "fhir_version", fhirVersion, "outcome", outcome.uppercase())
                    .register(meterRegistry)

                runningTimer.stop(timer)
            }
        }

        return result
    }

    private fun operation(resource: Class<*>, name: String): String =
        "${resource.simpleName}.$name"

    private fun newSpan(operation: String): Span.Builder = tracer.spanBuilder()
        .kind(Span.Kind.CLIENT)
        // TODO consider using "host name instead of full serverBase
        .remoteServiceName(fhirClient.serverBase)
        .name(operation)
        .tag("fhir.version", fhirVersion)
}