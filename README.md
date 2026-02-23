# Spring HAPI FHIR starter
A sample library that aims to add Micrometer observability support to the HAPI FHIR client for Spring Boot applications.

## Alternatives considered

### Micrometer instrumentation of underlying HTTP client
- HAPI FHIR client depends on the Apache HttpClient library for it's outbound HTTP requests
- Micrometer _is able_ to instrument the client with `MicrometerHttpRequestExecutor`
- This was not seen as "ideal" for the following reasons
  1. The `MicrometerHttpRequestExecutor` is deprecated, likely due to the release of HttpClient5 (HAPI FHIR depends on 4.x)
  2. The traces produced include the whole URI, like `baseR4/Patient/123?_format=json`, which is not ideal for observability purposes. The path should be templated, like `baseR4/Patient/{id}`.
  3. Even worse, the metrics produced _also_ use the whole URI instead of URI templates like `baseR4/Patient/{id}`, and creating templates would require extra code and work
  4. FHIR API calls seem to be referred to with `{Resource}.{Operation}` terminology, like `Patient.Read`, `Condition.Search`, etc. The custom instrumentation aligns with that better than trying to shoehorn the `HttpRequestExecutor` would