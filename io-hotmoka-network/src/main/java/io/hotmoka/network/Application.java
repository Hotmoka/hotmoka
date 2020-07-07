package io.hotmoka.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final static Logger LOGGER = LoggerFactory.getLogger(Application.class);
    private ConfigurableApplicationContext configurableApplicationContext;


    public static void main(String[] args) {
        // needed by spring
        throw new IllegalStateException("Cannot run NodeService application as a standalone jar");
    }

    /**
     * Start the Spring boot application and register the node {@link io.hotmoka.nodes.Node} to the beans map of the Spring framework in order
     * to be accessible by the application
     * @param config the configuration of the application
     * @param node the node
     */
    public void start(Config config, Node node) {
        LOGGER.info("Starting NodeService application");
        this.configurableApplicationContext = SpringApplication.run(Application.class, springArgumentsFor(config));
        this.configurableApplicationContext.getBeanFactory().registerSingleton("node", node);
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

    /**
     * Shutdown the Spring boot application
     */
    public void stop() {
        int times = 3;
        int counter = 0;
        int halfMinute = 30000;

        while (counter++ <= times) {

            if (this.configurableApplicationContext != null) {
                LOGGER.info("Stopping NodeService application");
                SpringApplication.exit(configurableApplicationContext);
                break;
            } else {

                try {
                    Thread.sleep(halfMinute);
                } catch (InterruptedException e) {
                    LOGGER.error("Error stopping NodeService application", e);
                }
            }
        }
    }

    public ConfigurableApplicationContext getConfigurableApplicationContext() {
        return this.configurableApplicationContext;
    }
}
