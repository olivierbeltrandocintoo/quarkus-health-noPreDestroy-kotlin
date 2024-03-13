package org.acme

import io.quarkus.logging.Log;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/")
@RequestScoped
class GreetingResource {

    @Inject lateinit var  dbSource:DbSource

    @GET
    @Path("")
    fun manual() :String{
        Log.info("manual")
        try {
            dbSource.acquire()
            return "manual"
        } finally {
            dbSource.release()
        }
    }

    @GET
    @Path("auto")
    public fun auto(): String {
        Log.info("auto")
        dbSource.acquire()
        return "auto"
    }
}