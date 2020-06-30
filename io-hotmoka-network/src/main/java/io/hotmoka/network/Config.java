package io.hotmoka.network;

/**
 * Config class for the Spring boot application
 */
public class Config {
    private final String port;
    private final boolean springBannerModeOn;

    private Config(String port, boolean showSpringBanner) {
        this.port = port;
        this.springBannerModeOn = showSpringBanner;
    }

    public String getPort() {
        return port;
    }

    public boolean isSpringBannerModeOn() {
        return springBannerModeOn;
    }


    public static class Builder {
        private int port;
        private boolean springBannerModeOn;

        public Builder setSpringBannerModeOn(boolean springBannerModeOn) {
            this.springBannerModeOn = springBannerModeOn;
            return this;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public Config build() {
            return new Config(String.valueOf(this.port), this.springBannerModeOn);
        }
    }
}
