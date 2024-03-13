package org.acme

import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.HealthCheckResponseBuilder
import org.eclipse.microprofile.health.Liveness
//import org.eclipse.microprofile.health.Readiness


@Liveness
//@Readiness
@RequestScoped
internal class ReadinessAndLiveness : HealthCheck {
    @Inject
    var dbSource: DbSource? = null

    override fun call(): HealthCheckResponse {
        val builder: HealthCheckResponseBuilder = HealthCheckResponse.builder().name("DbConnection")
        try {
            dbSource!!.acquire()
            return builder.up().build()
        } catch (e: Exception) {
            return builder.withData("Exception", e.message).down().build()
        } finally {
            // manual release because preDestroy is not called
            // dbSource!!.release()
        }
    }
}