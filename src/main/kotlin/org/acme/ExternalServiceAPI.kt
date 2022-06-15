package org.acme

import io.smallrye.mutiny.Uni
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.QueryParam

@RegisterRestClient(baseUri = "http://localhost:9000")
interface ExternalServiceAPI {

    @GET
    @Path("/fetch")
    fun fetchReactive(@QueryParam("delay") index: Int?): Uni<String>

    @GET
    @Path("/fetch")
    suspend fun fetchSuspending(@QueryParam("delay") index: Int?)

}
