package io.hotmoka.memory;

import io.hotmoka.beans.annotations.Immutable;

/**
 * The configuration of a blockchain on disk memory.
 */
@Immutable
public class Config extends io.takamaka.code.engine.Config {

	/**
	 * Full constructor for the builder pattern.
	 */
	protected Config(io.takamaka.code.engine.Config superConfig) {
		super(superConfig);
	}

	/**
	 * The builder of a configuration object.
	 */
	public static class Builder extends io.takamaka.code.engine.Config.Builder {

		@Override
		public Config build() {
			return new Config(super.build());
		}
	}
}