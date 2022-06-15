package org.acme

import io.quarkus.smallrye.reactivemessaging.ackSuspending
import io.smallrye.reactive.messaging.rabbitmq.IncomingRabbitMQMetadata
import kotlinx.coroutines.future.await
import org.eclipse.microprofile.reactive.messaging.Acknowledgment
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Message
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jboss.logging.Logger
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.event.Event

@ApplicationScoped
class CoroutineMessageProcessor(
    @RestClient private val externalServiceAPI: ExternalServiceAPI,
    private val messageProcessedEvents: Event<MessageProcessedEvent>,
) {

    init {
        val logger = Logger.getLogger(CoroutineMessageProcessor::class.java)
        logger.info("Message Conversion is enabled")
    }

    @Incoming("coroutine")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    suspend fun handleMessage(message: Message<String>) {

        val metaData = message.getMetadata(IncomingRabbitMQMetadata::class.java).get()

        try {

            val delay = metaData.headers["delay"]?.toString()?.toInt()

            externalServiceAPI.fetchSuspending(delay)

            message.ackSuspending()

        } catch (x: Throwable) {

            message.nack(x)

        } finally {

            val event =
                MessageProcessedEvent(
                    metaData.messageId.orElse(""),
                    metaData.headers["group"]?.toString() ?: ""
                )

            messageProcessedEvents.fireAsync(event)
                .await()
        }
    }

}
