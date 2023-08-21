/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.xodus.env;

import jetbrains.exodus.ExodusException;
import jetbrains.exodus.env.Environment;

/**
 * The configuration of a Xodus environment.
 */
public class EnvironmentConfig {

	private final jetbrains.exodus.env.EnvironmentConfig parent;

	/**
	 * Creates a default environment config.
	 */
	public EnvironmentConfig() {
		this.parent = new jetbrains.exodus.env.EnvironmentConfig();
	}

	public jetbrains.exodus.env.EnvironmentConfig toNative() {
		return parent;
	}

	/**
     * Returns {@code true} if {@linkplain io.hotmoka.xodus.env.Environment#close()} shouldn't check if there are unfinished
     * transactions. Otherwise it should check and throw {@linkplain io.hotmoka.xodus.ExodusException} if there are.
     * Default value is {@code false}.
     * <p>Mutable at runtime: yes
     *
     * @return {@code true} if {@linkplain io.hotmoka.xodus.env.Environment#close()} shouldn't check unfinished transactions
     * @see Environment#close()
     */
    public boolean getEnvCloseForcedly() {
    	return parent.getEnvCloseForcedly();
    }

    /**
     * Set {@code true} if {@linkplain io.hotmoka.xodus.env.Environment#close()} shouldn't check if there are unfinished
     * transactions. Set {@code false} if it should check and throw {@linkplain ExodusException} if there are unfinished
     * transactions. Default value is {@code false}.
     * <p>Mutable at runtime: yes
     *
     * @param closeForcedly {@code true} if {@linkplain io.hotmoka.xodus.env.Environment#close()} should ignore unfinished transactions
     * @return this instance
     * @see Environment#close()
     */
    public EnvironmentConfig setEnvCloseForcedly(final boolean closeForcedly) {
    	parent.setEnvCloseForcedly(closeForcedly);
        return this;
    }
}