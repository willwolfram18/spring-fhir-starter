package org.willwolfram18.spring.fhir.starter

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class FhirConfig(
    @Value("\${app.fhir.server}")
    private val url: String
) {
    @Bean
    open fun fhirClient(): IGenericClient {
        val context = FhirContext.forR4()
        val client = context.newRestfulGenericClient(url)

        with(client){
            registerInterceptor(LoggingInterceptor()
                .setLogRequestBody(true)
                .setLogRequestHeaders(true)
                .setLogResponseHeaders(true)
                .setLogResponseBody(true)
            )
        }

        return client
    }
}