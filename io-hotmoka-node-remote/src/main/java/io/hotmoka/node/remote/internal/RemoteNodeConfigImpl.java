/*
Copyright 2023 Dinu Berinde and Fausto Spoto

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

package io.hotmoka.node.remote.internal;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Objects;

import com.moandjiezana.toml.Toml;

import io.hotmoka.node.remote.api.RemoteNodeConfig;
import io.hotmoka.node.remote.api.RemoteNodeConfigBuilder;

/**
 * The configuration of a node that forwards all its calls to a remote network service.
 */
public class RemoteNodeConfigImpl implements RemoteNodeConfig {

    /**
     * The URL of the remote service, without the protocol. This defaults
     * to {@code localhost:8080}.
     */
    public final String url;

    /**
     * Builds the configuration from a builder.
     *
     * @param builder the builder
     */
    private RemoteNodeConfigImpl(RemoteNodeConfigBuilderImpl builder) {
        this.url = builder.url;
    }

    @Override
	public String getURL() {
		return url;
	}

	@Override
	public String toToml() {
		var sb = new StringBuilder();

		sb.append("# This is a TOML config file for a remote Hotmoka node.\n");
		sb.append("# For more information about TOML, see https://github.com/toml-lang/toml\n");
		sb.append("# For more information about Hotmoka, see https://www.hotmoka.io\n");
		sb.append("\n");
		sb.append("# the URL of the remote service, without the protocol\n");
		sb.append("url = \"" + url + "\"\n");

		return sb.toString();
	}

	@Override
	public RemoteNodeConfigBuilderImpl toBuilder() {
		return new RemoteNodeConfigBuilderImpl(this);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof RemoteNodeConfig rnc && url.equals(rnc.getURL());
	}

	@Override
	public int hashCode() {
		return url.hashCode();
	}

	@Override
	public String toString() {
		return toToml();
	}

	/**
	 * The builder of a configuration of a remote node.
	 */
	public static class RemoteNodeConfigBuilderImpl implements RemoteNodeConfigBuilder {
	    private String url = "localhost:8080";
	
	    /**
		 * Creates a builder with default values for the properties.
		 */
		public RemoteNodeConfigBuilderImpl() {}

		/**
		 * Creates a builder with properties initialized to those of the given configuration object.
		 * 
		 * @param config the configuration object
		 */
	    private RemoteNodeConfigBuilderImpl(RemoteNodeConfigImpl config) {
			setURL(config.url);
		}

	    /**
		 * Creates a builder by reading the properties of the given TOML file and
		 * setting them for the corresponding fields of the builder.
		 * 
		 * @param toml the file
		 */
		public RemoteNodeConfigBuilderImpl(Toml toml) {
			var url = toml.getString("url");
			if (url != null)
				setURL(url);
		}

		/**
		 * Creates a builder by reading the properties of the given TOML file and sets them for
		 * the corresponding fields of this builder.
		 * 
		 * @param toml the file
		 * @throws FileNotFoundException if the file cannot be found
		 */
		public RemoteNodeConfigBuilderImpl(Path toml) throws FileNotFoundException {
			this(readToml(toml));
		}

		@Override
	    public RemoteNodeConfigBuilder setURL(String url) {
			Objects.requireNonNull(url, "url cannot be null");
	        this.url = url;
	        return this;
	    }

	    /**
	     * Builds the configuration from this builder.
	     *
	     * @return the configuration
	     */
	    public RemoteNodeConfig build() {
	        return new RemoteNodeConfigImpl(this);
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
				if (cause instanceof FileNotFoundException fne)
					throw fne;
				else
					throw e;
			}
		}
	}
}