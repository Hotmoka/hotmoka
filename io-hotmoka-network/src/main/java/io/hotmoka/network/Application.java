package io.hotmoka.network;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import io.hotmoka.nodes.Node;

/**
 * Simple Spring boot application with Tomcat embedded which exposes some REST APIs to access an instance of a node {@link io.hotmoka.nodes.Node}
 */
@SpringBootApplication
public class Application {

	/**
     * Start the Spring boot application and register the node {@link io.hotmoka.nodes.Node} to the beans map of the Spring framework in order
     * to be accessible by the application
     * @param config the configuration of the application
     * @param node the node
     */
    public ConfigurableApplicationContext start(Config config, Node node) {
    	ConfigurableApplicationContext configurableApplicationContext = SpringApplication.run(Application.class, springArgumentsFor(config));
    	configurableApplicationContext.getBeanFactory().registerSingleton("node", node);
    	return configurableApplicationContext;
    }

    /**
     * Builds, from the configuration, the array of arguments required by Spring in order to start the application.
     * 
     * @param config the configuration
     * @return the array of arguments required by Spring
     */
    private static String[] springArgumentsFor(Config config) {
    	return new String[] {
   			"--server.port=" + config.port,
   			"--spring.main.banner-mode=" + (config.showSpringBanner ? Banner.Mode.CONSOLE : Banner.Mode.OFF)
    	};
    }
}