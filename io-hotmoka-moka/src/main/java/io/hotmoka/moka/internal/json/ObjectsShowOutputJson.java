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
import java.util.stream.Stream;

import io.hotmoka.moka.api.objects.ObjectsShowOutput;
import io.hotmoka.moka.api.objects.ObjectsShowOutput.ConstructorDescription;
import io.hotmoka.moka.api.objects.ObjectsShowOutput.MethodDescription;
import io.hotmoka.moka.internal.objects.Show;
import io.hotmoka.moka.internal.objects.Show.ConstructorDescriptionImpl;
import io.hotmoka.moka.internal.objects.Show.MethodDescriptionImpl;
import io.hotmoka.node.Updates;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of the output of the {@code moka objects show} command.
 */
public abstract class ObjectsShowOutputJson implements JsonRepresentation<ObjectsShowOutput> {
	private final Updates.Json tag;
	private final Updates.Json[] fields;
	private final ConstructorDescriptionJson[] constructors;
	private final MethodDescriptionJson[] methods;

	protected ObjectsShowOutputJson(ObjectsShowOutput output) {
		this.tag = new Updates.Json(output.getClassTag());
		this.fields = output.getFields().map(Updates.Json::new).toArray(Updates.Json[]::new);
		this.constructors = output.getConstructorDescriptions().map(stream -> stream.map(ConstructorDescriptionJson::new).toArray(ConstructorDescriptionJson[]::new)).orElse(null);
		this.methods = output.getMethodDescriptions().map(stream -> stream.map(MethodDescriptionJson::new).toArray(MethodDescriptionJson[]::new)).orElse(null);
	}

	public Updates.Json getTag() {
		return tag;
	}

	public Stream<Updates.Json> getFields() {
		return Stream.of(fields);
	}

	public Optional<Stream<ConstructorDescriptionJson>> getConstructorDescriptions() {
		return Optional.ofNullable(constructors).map(Stream::of);
	}

	public Optional<Stream<MethodDescriptionJson>> getMethodDescriptions() {
		return Optional.ofNullable(methods).map(Stream::of);
	}

	@Override
	public ObjectsShowOutput unmap() throws InconsistentJsonException {
		return new Show.Output(this);
	}

	/**
	 * The JSON representation of a constructor description.
	 */
	public static class ConstructorDescriptionJson implements JsonRepresentation<ConstructorDescription> {
		private final String annotations;
		private final String signature;

		private ConstructorDescriptionJson(ConstructorDescription constructor) {
			this.annotations = constructor.getAnnotations();
			this.signature = constructor.getSignature();
		}

		@Override
		public ConstructorDescription unmap() throws InconsistentJsonException {
			return new ConstructorDescriptionImpl(this);
		}

		public String getAnnotations() {
			return annotations;
		}

		public String getSignature() {
			return signature;
		}
	}

	/**
	 * The JSON representation of a method description.
	 */
	public static class MethodDescriptionJson implements JsonRepresentation<MethodDescription> {
		private final String annotations;
		private final String declaringClass;
		private final String signature;

		private MethodDescriptionJson(MethodDescription method) {
			this.annotations = method.getAnnotations();
			this.declaringClass = method.getDeclaringClass();
			this.signature = method.getSignature();
		}

		@Override
		public MethodDescription unmap() throws InconsistentJsonException {
			return new MethodDescriptionImpl(this);
		}

		public String getAnnotations() {
			return annotations;
		}

		public String getDeclaringClass() {
			return declaringClass;
		}

		public String getSignature() {
			return signature;
		}
	}
}