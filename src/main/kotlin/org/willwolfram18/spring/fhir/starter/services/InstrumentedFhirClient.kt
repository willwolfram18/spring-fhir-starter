package org.willwolfram18.spring.fhir.starter.services

import ca.uhn.fhir.rest.api.*
import ca.uhn.fhir.rest.client.api.*
import ca.uhn.fhir.rest.gclient.*
import io.micrometer.core.instrument.*
import io.micrometer.tracing.*
import org.hl7.fhir.r4.model.*
import org.springframework.stereotype.*
import org.willwolfram18.spring.fhir.starter.extensions.*
import org.willwolfram18.spring.fhir.starter.model.OperationOutcome

@Service
class InstrumentedFhirClient(
    private val fhirClient: IGenericClient,
    private val tracer: Tracer,
    private val meterRegistry: MeterRegistry,
) {

    companion object {
        const val FHIR_VERSION_TAG = "fhir.version"
    }

    private val fhirVersion by lazy {
        fhirClient.fhirContext.version.version.name
    }

    fun <T : Resource> search(
        resource: Class<T>,
        queryBuilder: (IQuery<Bundle>.() -> IQuery<Bundle>) = { this }
    ): Bundle {
        val operationName = operation(resource, "Search")

        return executeWithTelemetry(operationName) {
            val query = fhirClient.search<Bundle>()
                .forResource(resource)
                .run(queryBuilder)

            query.returnBundle(Bundle::class.java).executeJson()
        }
    }

    fun <T : Resource> create(resource: T, block: (ICreateTyped.() -> ICreateTyped) = { this }): MethodOutcome? {
        val operationName = operation(resource.javaClass, "Create")

        return executeWithTelemetry(operationName) {
            val createRequest = fhirClient.create()
                .resource(resource)
                .run(block)

            createRequest.executeJson()
        }
    }

    fun <T : Resource> read(resource: Class<T>, block: IReadTyped<T>.() -> IReadExecutable<T>): T {
        val operationName = operation(resource, "Read")

        return executeWithTelemetry(operationName) {
            fhirClient.read()
                .resource(resource)
                .run(block)
                .executeJson()
        }
    }

    private fun operation(resource: Class<*>, name: String): String =
        "${resource.simpleName}.$name"

    private fun newSpan(operation: String): Span.Builder = tracer.spanBuilder()
        .kind(Span.Kind.CLIENT)
        // TODO consider using "host name instead of full serverBase
        .remoteServiceName(fhirClient.serverBase)
        .name(operation)
        .tag(FHIR_VERSION_TAG, fhirVersion)

    private fun <R> executeWithTelemetry(operationName: String, block: () -> R): R =
        executeWithTracing(operationName) {
            executeWithTimer(operationName) {
                block()
            }
        }

    // Helper that handles tracing/span lifecycle and error tagging.
    private fun <R> executeWithTracing(operationName: String, block: () -> R): R {
        val spanBuilder = newSpan(operationName)

        if (tracer.currentSpan() != null) {
            spanBuilder.setParent(tracer.currentSpan()!!.context())
        }

        val span = spanBuilder.start()
        tracer.withSpan(span).use {
            try {
                return block()
            } catch (e: Exception) {
                span.error(e)
                throw e
            } finally {
                span.end()
            }
        }
    }

    // Helper that handles Micrometer Timer lifecycle and outcome tagging for metrics.
    private fun <R> executeWithTimer(operationName: String, block: () -> R): R {
        val runningTimer = Timer.start(meterRegistry)
        var outcome = OperationOutcome.SUCCESS

        try {
            return block()
        } catch (e: Exception) {
            outcome = OperationOutcome.ERROR
            throw e
        } finally {
            val timer = Timer.builder("fhir.client.request.duration")
                .description("Duration of HTTP FHIR client operations")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .tags(
                    "operation", operationName,
                    FHIR_VERSION_TAG, fhirVersion,
                    "outcome", outcome.name
                )
                .register(meterRegistry)

            runningTimer.stop(timer)
        }
    }
}
