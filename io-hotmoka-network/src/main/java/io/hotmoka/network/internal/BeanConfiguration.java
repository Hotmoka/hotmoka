package io.hotmoka.network.internal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import io.hotmoka.network.internal.websockets.config.WebSocketsConfig;

@Configuration
public class BeanConfiguration {

    @Bean
    public ServletServerContainerFactoryBean tomcatSetup() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(WebSocketsConfig.MESSAGE_SIZE_LIMIT); // default 8192
        container.setMaxBinaryMessageBufferSize(WebSocketsConfig.MESSAGE_SIZE_LIMIT); // default 8192
        return container;
    }
}