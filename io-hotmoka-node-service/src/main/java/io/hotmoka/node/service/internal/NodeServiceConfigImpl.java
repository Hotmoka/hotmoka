/*
Copyright 2021 Dinu Berinde and Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.node.service.internal;

import java.io.FileNotFoundException;
import java.nio.file.Path;

import com.moandjiezana.toml.Toml;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.service.NodeServiceConfig;
import io.hotmoka.node.service.NodeServiceConfigBuilder;

/**
 * The configuration of a network service supported by a Hotmoka node.
 */
@Immutable
public class NodeServiceConfigImpl implements NodeServiceConfig {

	/**
	 * The HTTP port of the server.
	 */
	public final int port;

    /**
     * Builds the configuration from a builder.
     * 
     * @param builder the builder
     */
    private NodeServiceConfigImpl(NodeServiceConfigBuilderImpl builder) {
        this.port = builder.port;
    }

    @Override
    public int getPort() {
    	return port;
    }

    @Override
	public String toToml() {
		var sb = new StringBuilder();

		sb.append("# This is a TOML config file for a Hotmoka node service.\n");
		sb.append("# For more information about TOML, see https://github.com/toml-lang/toml\n");
		sb.append("# For more information about Hotmoka, see https://www.hotmoka.io\n");
		sb.append("\n");
		sb.append("# the  HTTP port of the server\n");
		sb.append("port = \"" + port + "\"\n");

		return sb.toString();
	}

	@Override
	public NodeServiceConfigBuilderImpl toBuilder() {
		return new NodeServiceConfigBuilderImpl(this);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof NodeServiceConfig nsc && port == nsc.getPort();
	}

	@Override
	public int hashCode() {
		return port;
	}

	@Override
	public String toString() {
		return toToml();
	}

	/**
     * The builder of a configuration of a network service.
     */
    public static class NodeServiceConfigBuilderImpl implements NodeServiceConfigBuilder {
        private int port = 8080;

        /**
		 * Creates a builder with default values for the properties.
		 */
		public NodeServiceConfigBuilderImpl() {}

		/**
		 * Creates a builder with properties initialized to those of the given configuration object.
		 * 
		 * @param config the configuration object
		 */
	    private NodeServiceConfigBuilderImpl(NodeServiceConfigImpl config) {
			setPort(config.port);
		}

	    /**
		 * Creates a builder by reading the properties of the given TOML file and
		 * setting them for the corresponding fields of the builder.
		 * 
		 * @param toml the file
		 */
		public NodeServiceConfigBuilderImpl(Toml toml) {
			var port = toml.getLong("port");
			if (port != null)
				setPort(port);
		}

		/**
		 * Creates a builder by reading the properties of the given TOML file and sets them for
		 * the corresponding fields of this builder.
		 * 
		 * @param toml the file
		 * @throws FileNotFoundException if the file cannot be found
		 */
		public NodeServiceConfigBuilderImpl(Path toml) throws FileNotFoundException {
			this(readToml(toml));
		}

		private NodeServiceConfigBuilder setPort(long port) {
			if (port < 0 || port > 65353)
				throw new IllegalArgumentException("port must be between 0 and 65353 inclusive");

			this.port = (int) port;
            return this;
        }

		@Override
        public NodeServiceConfigBuilder setPort(int port) {
			if (port < 0 || port > 65353)
				throw new IllegalArgumentException("port must be between 0 and 65353 inclusive");

			this.port = port;
            return this;
        }

        @Override
        public NodeServiceConfig build() {
            return new NodeServiceConfigImpl(this);
        }

        /**
		 * Loads the TOML file at the given path.
		 * 
		 * @param path the path
		 * @return the file
		 * @throws FileNotFoundException if {@code path} cannot be found
		 */
		protected static Toml readToml(Path path) throws FileNotFoundException {
			try {
				return new Toml().read(path.toFile());
			}
			catch (RuntimeException e) {
				// the toml4j library wraps the FileNotFoundException inside a RuntimeException...
				Throwable cause = e.getCause();
				if (cause instanceof FileNotFoundException)
					throw (FileNotFoundException) cause;
				else
					throw e;
			}
		}
    }
}