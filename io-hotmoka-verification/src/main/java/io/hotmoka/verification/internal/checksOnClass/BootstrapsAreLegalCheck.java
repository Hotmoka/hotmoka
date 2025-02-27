/*
Copyright 2021 Fausto Spoto

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

package io.hotmoka.verification.internal.checksOnClass;

import org.apache.bcel.classfile.BootstrapMethod;

import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.errors.IllegalBootstrapMethodError;
import io.hotmoka.verification.internal.CheckOnClasses;
import io.hotmoka.verification.internal.VerifiedClassImpl;

/**
 * A check that lambda bootstraps are only among those allowed by Takamaka.
 */
public class BootstrapsAreLegalCheck extends CheckOnClasses {

	public BootstrapsAreLegalCheck(VerifiedClassImpl.Verification builder) throws IllegalJarException {
		super(builder);

		for (var bootstrap: bootstraps.getBootstraps().toArray(BootstrapMethod[]::new))
			if (bootstraps.getTargetOf(bootstrap).isEmpty())
				issue(new IllegalBootstrapMethodError(inferSourceFile()));
	}
}