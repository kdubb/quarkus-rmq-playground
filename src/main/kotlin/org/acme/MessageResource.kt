package org.acme

import io.smallrye.common.annotation.Blocking
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Message
import org.eclipse.microprofile.reactive.messaging.Metadata
import org.threeten.extra.AmountFormats
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import javax.enterprise.event.ObservesAsync
import javax.ws.rs.DefaultValue
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.QueryParam

@Path("/messages")
class MessageResource(
    @Channel("out") private val outEmitter: Emitter<String>,
) {

    private var sendLatches = ConcurrentHashMap<String, CountDownLatch>()

    fun messageProcessed(@ObservesAsync event: MessageProcessedEvent) {
        sendLatches[event.group]?.countDown()
    }

    @POST
    @Path("send")
    @Blocking
    fun sendBatch(
        @QueryParam("style") style: MessageProcessingStyle,
        @QueryParam("count") @DefaultValue("1000") count: Int,
        @QueryParam("delay") @DefaultValue("0") delay: Int
    ): Map<String, Any> {

        val group = "${UUID.randomUUID()}"

        sendLatches[group] = CountDownLatch(count)

        val start = Instant.now()

        for (idx in 0 until count) {
            send(style, idx, group, delay)
        }

        sendLatches[group]!!.await()
        sendLatches.remove(group)

        val duration = Duration.between(start, Instant.now())

        println("Sent $count messages in ${AmountFormats.wordBased(duration, Locale.getDefault())}")

        return mapOf("sent" to count, "duration" to duration)
    }

    fun send(style: MessageProcessingStyle, index: Int, group: String, delay: Int) {
        val metaData =
            Metadata.of(
                OutgoingRabbitMQMetadata.builder()
                    .withContentType("application/json")
                    .withRoutingKey(style.name)
                    .withHeader("group", group)
                    .apply {
                        if (delay > 0) {
                            withHeader("delay", delay)
                        }
                    }
                    .build()
            )

        val message = Message.of("""{"index": ${index}}""", metaData)

        outEmitter.send(message)
    }
}
