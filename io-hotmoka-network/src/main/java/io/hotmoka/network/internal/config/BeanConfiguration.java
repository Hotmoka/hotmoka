package io.hotmoka.network.internal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
public class BeanConfiguration {

    @Bean
    public ServletServerContainerFactoryBean tomcatSetup() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(100*1024); // default 8192
        container.setMaxBinaryMessageBufferSize(100*1024); // default 8192
        return container;
    }

}
