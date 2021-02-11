package io.hotmoka.service.internal.websockets.config;

import io.hotmoka.service.common.GsonMessageConverter;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketsConfig implements WebSocketMessageBrokerConfigurer {

	/**
     * The message size limit that the server and client can exchange.
     * It is very important for the size of the jar exchanged.
     */
    public final static int MESSAGE_SIZE_LIMIT = 4 * 512 * 1024;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker( "/user", "/topic");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/node").setHandshakeHandler(new SessionHandshake());
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        messageConverters.add(new GsonMessageConverter());
        return false;
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(MESSAGE_SIZE_LIMIT); // default : 64 * 1024
        registry.setSendBufferSizeLimit(512*1024); // default : 512 * 1024
    }

    @Bean
    public ApplicationListener<SessionSubscribeEvent> webSocketEventListener(AbstractSubscribableChannel clientOutboundChannel) {
        return event -> {
            Message<byte[]> message = event.getMessage();
            StompHeaderAccessor stompHeaderAccessor = StompHeaderAccessor.wrap(message);
            if (stompHeaderAccessor.getCommand() == StompCommand.SUBSCRIBE && stompHeaderAccessor.getReceipt() != null) {
                stompHeaderAccessor.setHeader("stompCommand", StompCommand.RECEIPT);
                stompHeaderAccessor.setReceiptId(stompHeaderAccessor.getReceipt());
                clientOutboundChannel.send(MessageBuilder.createMessage(new byte[0], stompHeaderAccessor.getMessageHeaders()));
            }
        };
    }

    private static class SessionHandshake extends DefaultHandshakeHandler {

        /**
         * Create a principal with the UUID key of the user of the current session.
         * 
         * @param request the request
         * @param wsHandler the websockets handler
         * @param attributes the attributes
         * @return the user of the current session
         */
        @Override
        protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
            Principal principal = request.getPrincipal();
            if (principal != null)
            	return principal;
            else
            	return () -> {
            		List<String> UUID = request.getHeaders().get("uuid");
            		return UUID != null && !UUID.isEmpty() ? UUID.get(0) : null;
            	};
        }
    }
}