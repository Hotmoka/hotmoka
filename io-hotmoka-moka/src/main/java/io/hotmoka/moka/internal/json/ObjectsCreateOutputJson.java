/*
Copyright 2025 Fausto Spoto

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

package io.hotmoka.moka.internal.json;

import java.util.Optional;

import io.hotmoka.moka.api.objects.ObjectsCreateOutput;
import io.hotmoka.moka.internal.objects.Create;
import io.hotmoka.node.StorageValues;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of the output of the {@code moka objects create} command.
 */
public class ObjectsCreateOutputJson extends GasCostOutputJson implements JsonRepresentation<ObjectsCreateOutput> {
	private final StorageValues.Json object;

	protected ObjectsCreateOutputJson(ObjectsCreateOutput output) {
		super(output);

		this.object = output.getObject().map(StorageValues.Json::new).orElse(null);
	}

	public Optional<StorageValues.Json> getObject() {
		return Optional.ofNullable(object);
	}

	@Override
	public ObjectsCreateOutput unmap() throws InconsistentJsonException {
		return new Create.Output(this);
	}
}