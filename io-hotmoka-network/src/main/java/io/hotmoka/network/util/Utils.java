package io.hotmoka.network.util;

import io.hotmoka.network.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class
 */
public class Utils {

    /**
     * Build the array of arguments required by Spring in order to start the application. We build the arguments from the config object.
     * @param config the config object {@link io.hotmoka.network.Config}
     * @return an array of arguments required by Spring
     */
    public static String[] buildSpringArguments(Config config) {
        if (config == null)
            return new String[]{};

        List<String> args = new ArrayList<>();

        if (config.getPort() != null)
            args.add("--server.port=" + config.getPort());

        if (!config.isSpringBannerModeOn())
            args.add("--spring.main.banner-mode=false");

        String [] result = new String[args.size()];
        return args.toArray(result);
    }
}
