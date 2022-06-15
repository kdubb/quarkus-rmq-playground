package org.acme

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.ResponseDefinition
import io.quarkus.runtime.ShutdownEvent
import io.quarkus.runtime.StartupEvent
import javax.enterprise.event.Observes

object ExternalService {

    private val wireMockServer =
        WireMockServer(
            options()
                .port(9000)
                .extensions(DynamicDelay)
        )

    init {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/fetch"))
        )
    }

    fun start(@Observes event: StartupEvent) {
        wireMockServer.start()
    }

    fun stop(@Observes event: ShutdownEvent) {
        wireMockServer.stop()
    }

}

object DynamicDelay : ResponseDefinitionTransformer() {
    override fun getName() = "dynamic-delay"

    override fun transform(
        request: Request,
        response: ResponseDefinition,
        files: FileSource,
        parameters: Parameters
    ): ResponseDefinition {
        val delayParam = request.queryParameter("delay")
        if (!delayParam.isPresent) {
            return response
        }

        val delay =
            delayParam.firstValue()?.toDoubleOrNull()
                ?: return response

        return ResponseDefinitionBuilder
            .like(response)
            .withLogNormalRandomDelay(delay, 0.2)
            .build()
    }
}
