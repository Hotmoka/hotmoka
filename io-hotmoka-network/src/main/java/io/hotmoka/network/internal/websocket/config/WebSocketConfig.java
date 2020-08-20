package io.hotmoka.network.internal.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/node")
                .setHandshakeHandler(new SessionHandshake());
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        messageConverters.add(new GsonMessageConverter());
        return false;
    }

    private static class SessionHandshake extends DefaultHandshakeHandler {

        /**
         * Create a Principal with the UUID key of the user of the current session
         * @param request the request
         * @param wsHandler the websocket handler
         * @param attributes the attributes
         * @return the user of the current session
         */
        @Override
        protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {

            Principal principal = request.getPrincipal();
            if (principal == null) {
                principal = () -> {

                    if (request.getHeaders().get("uuid") != null && !request.getHeaders().get("uuid").isEmpty()) {
                        return request.getHeaders().get("uuid").get(0);
                    }

                    return null;
                };
            }
            return principal;
        }
    }


}
