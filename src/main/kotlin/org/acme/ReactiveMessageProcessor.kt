package org.acme

import io.smallrye.mutiny.Uni
import io.smallrye.reactive.messaging.rabbitmq.IncomingRabbitMQMessage
import io.smallrye.reactive.messaging.rabbitmq.IncomingRabbitMQMetadata
import org.eclipse.microprofile.reactive.messaging.Acknowledgment
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Message
import org.eclipse.microprofile.rest.client.inject.RestClient
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.event.Event

@ApplicationScoped
class ReactiveMessageProcessor(
    @RestClient private val externalServiceAPI: ExternalServiceAPI,
    private val messageProcessedEvents: Event<MessageProcessedEvent>,
) {

    @Incoming("reactive")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    fun handleMessage(message: Message<String>): Uni<Unit> {
        val metaData = message.getMetadata(IncomingRabbitMQMetadata::class.java).get()
        return Uni.createFrom().item(message)
            .onItem().transformToUni { msg ->
                val delay = metaData.headers["delay"]?.toString()?.toInt()
                externalServiceAPI.fetchReactive(delay)
                    .onItem().call { _ -> Uni.createFrom().completionStage(msg.ack()) }
                    .map { }
                    .onFailure().call { x -> Uni.createFrom().completionStage(msg.nack(x)) }
                    .onItemOrFailure().invoke(Runnable {
                        messageProcessedEvents.fireAsync(
                            MessageProcessedEvent(
                                metaData.messageId.orElse(""),
                                metaData.headers["group"]?.toString() ?: ""
                            )
                        )
                    })
            }
    }

}
