package org.acme

import io.smallrye.reactive.messaging.MessageConverter
import org.eclipse.microprofile.reactive.messaging.Message
import java.lang.reflect.Type
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class CustomMessageConverter : MessageConverter {

    override fun canConvert(`in`: Message<*>, target: Type): Boolean {
        return true
    }

    override fun convert(`in`: Message<*>, target: Type): Message<*> {
        return `in`.withPayload("Converted!")
    }

}
