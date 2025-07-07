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

package io.hotmoka.moka.internal.objects;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.cli.CommandException;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.helpers.ClassLoaderHelpers;
import io.hotmoka.moka.ObjectsShowOutputs;
import io.hotmoka.moka.api.objects.ObjectsShowOutput;
import io.hotmoka.moka.api.objects.ObjectsShowOutput.ConstructorDescription;
import io.hotmoka.moka.api.objects.ObjectsShowOutput.MethodDescription;
import io.hotmoka.moka.internal.AbstractMokaRpcCommand;
import io.hotmoka.moka.internal.converters.StorageReferenceOptionConverter;
import io.hotmoka.moka.internal.json.ObjectsShowOutputJson;
import io.hotmoka.moka.internal.json.ObjectsShowOutputJson.ConstructorDescriptionJson;
import io.hotmoka.moka.internal.json.ObjectsShowOutputJson.MethodDescriptionJson;
import io.hotmoka.node.Updates;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.MisbehavingNodeException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.updates.UpdateOfField;
import io.hotmoka.node.api.updates.UpdateOfString;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.verification.api.TakamakaClassLoader;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;
import io.hotmoka.whitelisting.api.WhiteListingWizard;
import io.takamaka.code.constants.Constants;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "show", header = "Show the state of a storage object.", showDefaultValues = true)
public class Show extends AbstractMokaRpcCommand {

	@Parameters(index = "0", description = "the object", converter = StorageReferenceOptionConverter.class)
    private StorageReference object;

	@Option(names = "--api", description = "print the public API of the object")
    private boolean api;

	@Override
	protected void body(RemoteNode remote) throws TimeoutException, InterruptedException, CommandException {
		try {
			report(json(), new Output(remote, object, api), ObjectsShowOutputs.Encoder::new);
		}
		catch (NodeException e) {
			throw new RuntimeException(e); // TODO
		}
	}

	private static String annotationsAsString(Executable executable) {
		String prefix = Constants.IO_TAKAMAKA_CODE_LANG_PACKAGE_NAME;
		return Stream.of(executable.getAnnotations())
			.filter(annotation -> annotation.annotationType().getName().startsWith(prefix))
			.map(Annotation::toString)
			.collect(Collectors.joining(" "))
			.replace(prefix, "")
			.replace("()", "")
			.replace("(Contract.class)", "");
	}

	/**
	 * Implementation of the description of a constructor.
	 */
	public static class ConstructorDescriptionImpl implements ConstructorDescription {
		private final String annotations;
		private final String signature;

		private ConstructorDescriptionImpl(Constructor<?> constructor) {
			this.annotations = annotationsAsString(constructor);
			var declaringClass = constructor.getDeclaringClass();
			this.signature = constructor.toString().replace(declaringClass.getName() + "(", declaringClass.getSimpleName() + "(");
		}

		/**
		 * Builds a constructor description from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public ConstructorDescriptionImpl(ConstructorDescriptionJson json) throws InconsistentJsonException {
			this.annotations = Objects.requireNonNull(json.getAnnotations(), "annotations cannot be null", InconsistentJsonException::new);
			this.signature = Objects.requireNonNull(json.getSignature(), "signature cannot be null", InconsistentJsonException::new);
		}

		@Override
		public String getAnnotations() {
			return annotations;
		}

		@Override
		public String getSignature() {
			return signature;
		}

		@Override
		public int compareTo(ConstructorDescription other) {
			return signature.compareTo(other.getSignature());
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof ConstructorDescription cd && signature.equals(cd.getSignature());
		}

		@Override
		public int hashCode() {
			return signature.hashCode();
		}
	}

	/**
	 * Implementation of the description of a method.
	 */
	public static class MethodDescriptionImpl implements MethodDescription {
		private final String annotations;
		private final String declaringClass;
		private final String signature;

		private MethodDescriptionImpl(Method method) {
			this.annotations = annotationsAsString(method);
			var declaringClass = method.getDeclaringClass();
			this.declaringClass = declaringClass.getName();
			this.signature = method.toString().replace(declaringClass + "." + method.getName(), method.getName());
		}

		/**
		 * Builds a method description from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public MethodDescriptionImpl(MethodDescriptionJson json) throws InconsistentJsonException {
			this.annotations = Objects.requireNonNull(json.getAnnotations(), "annotations cannot be null", InconsistentJsonException::new);
			this.declaringClass = Objects.requireNonNull(json.getDeclaringClass(), "declaringClass cannot be null", InconsistentJsonException::new);
			this.signature = Objects.requireNonNull(json.getSignature(), "signature cannot be null", InconsistentJsonException::new);
		}

		@Override
		public String getAnnotations() {
			return annotations;
		}

		@Override
		public String getDeclaringClass() {
			return declaringClass;
		}

		public String getSignature() {
			return signature;
		}

		@Override
		public int compareTo(MethodDescription other) {
			int diff = declaringClass.compareTo(other.getDeclaringClass());
			if (diff != 0)
				return diff;
			else
				return signature.compareTo(other.getSignature());
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof MethodDescription md && declaringClass.equals(md.getDeclaringClass()) && signature.equals(md.getSignature());
		}

		@Override
		public int hashCode() {
			return signature.hashCode();
		}
	}

	/**
	 * The output of this command.
	 */
	public static class Output implements ObjectsShowOutput {
		private final ClassTag tag;
		private final SortedSet<UpdateOfField> fields;
		private final SortedSet<ConstructorDescription> constructors;
		private final SortedSet<MethodDescription> methods;

		private Output(RemoteNode remote, StorageReference object, boolean api) throws TimeoutException, InterruptedException, NodeException, CommandException {
			Update[] updates = updates(remote, object);

			this.tag = getClassTag(updates, object);
			this.fields = fields(updates);

			if (api) {
				TakamakaClassLoader classLoader = mkClassLoader(remote, object);
				Class<?> clazz = mkClass(classLoader, object);
				this.constructors = constructors(clazz, classLoader.getWhiteListingWizard());
				this.methods = methods(clazz, classLoader.getWhiteListingWizard());
			}
			else {
				this.constructors = null;
				this.methods = null;
			}
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public Output(ObjectsShowOutputJson json) throws InconsistentJsonException {
			var unmappedTag = Objects.requireNonNull(json.getTag(), "tag cannot be null", InconsistentJsonException::new).unmap();
			if (unmappedTag instanceof ClassTag tag)
				this.tag = tag;
			else
				throw new InconsistentJsonException("The class tag information should be a ClassTag update, not a " + unmappedTag.getClass().getSimpleName());

			var fieldsJson = json.getFields().toArray(Updates.Json[]::new);
			this.fields = new TreeSet<>();
			for (Updates.Json updateJson: fieldsJson) {
				var unmappedUpdate = Objects.requireNonNull(updateJson, "fields cannot hold a null element", InconsistentJsonException::new).unmap();
				if (unmappedUpdate instanceof UpdateOfField uof)
					this.fields.add(uof);
				else
					throw new InconsistentJsonException("A field update should be an UpdateOfField object, not a " + unmappedUpdate.getClass().getSimpleName());
			}

			if (json.getConstructorDescriptions().isPresent()) {
				this.constructors = new TreeSet<>();

				for (var constructorJson: json.getConstructorDescriptions().get().toArray(ConstructorDescriptionJson[]::new)) {
					var unmappedConstructor = Objects.requireNonNull(constructorJson, "constructors cannot hold a null element", InconsistentJsonException::new).unmap();
					this.constructors.add(unmappedConstructor);
				}
			}
			else
				this.constructors = null;

			if (json.getMethodDescriptions().isPresent()) {
				this.methods = new TreeSet<>();

				for (var methodJson: json.getMethodDescriptions().get().toArray(MethodDescriptionJson[]::new)) {
					var unmappedMethod = Objects.requireNonNull(methodJson, "methods cannot hold a null element", InconsistentJsonException::new).unmap();
					this.methods.add(unmappedMethod);
				}
			}
			else
				this.methods = null;
		}

		@Override
		public ClassTag getClassTag() {
			return tag;
		}

		@Override
		public Stream<UpdateOfField> getFields() {
			return fields.stream();
		}

		@Override
		public Optional<Stream<ConstructorDescription>> getConstructorDescriptions() {
			return Optional.ofNullable(constructors).map(Collection::stream);
		}

		@Override
		public Optional<Stream<MethodDescription>> getMethodDescriptions() {
			return Optional.ofNullable(methods).map(Collection::stream);
		}

		@Override
		public String toString() {
			var sb = new StringBuilder();

			sb.append(red("class " + tag.getClazz() + " (from jar installed at " + tag.getJar() + ")\n"));

			getFields().filter(update -> update.getField().getDefiningClass().equals(tag.getClazz()))
				.forEachOrdered(update -> sb.append("  " + update.getField().getName() + ":" + update.getField().getType() + " = " + valueToPrint(update) + "\n"));

			getFields().filter(update -> !update.getField().getDefiningClass().equals(tag.getClazz()))
				.forEachOrdered(update -> sb.append("  " + green(update.getField().getDefiningClass().toString()) + "." + update.getField().getName() + ":" + update.getField().getType() + " = " + valueToPrint(update) + "\n"));

			if (constructors != null && !constructors.isEmpty()) {
				sb.append("\n");
				constructors.stream().forEachOrdered(constructor -> printConstructor(constructor, sb));
			}

			if (methods != null && !methods.isEmpty()) {
				sb.append("\n");
				methods.stream().forEachOrdered(constructor -> printMethod(constructor, sb));
			}

			return sb.toString();
		}

		private TakamakaClassLoader mkClassLoader(RemoteNode remote, StorageReference object) throws TimeoutException, InterruptedException, CommandException, ClosedNodeException, MisbehavingNodeException {
			try {
				return ClassLoaderHelpers.of(remote).classloaderFor(tag.getJar());
			}
			catch (UnknownReferenceException e) {
				throw new CommandException("The object " + object + " has been installed by transaction " + tag.getJar() + " but the latter cannot be found in store!");
			}
			catch (UnsupportedVerificationVersionException e) {
				throw new CommandException("The node uses a verification version that is not available");
			}
		}

		private Class<?> mkClass(TakamakaClassLoader classLoader, StorageReference object) throws CommandException {
			try {
				return classLoader.loadClass(tag.getClazz().getName());
			}
			catch (ClassNotFoundException e) {
				throw new CommandException("Cannot find the class of object " + object + " although it is in store!");
			}
		}

		private Update[] updates(RemoteNode remote, StorageReference object) throws NodeException, TimeoutException, InterruptedException, CommandException {
			try {
				return remote.getState(object).toArray(Update[]::new);
			}
			catch (UnknownReferenceException e) {
				throw new CommandException("The object " + object + " cannot be found in the store of the node.");
			}
		}

		private ClassTag getClassTag(Update[] updates, StorageReference object) throws CommandException {
			return Stream.of(updates)
					.filter(update -> update instanceof ClassTag)
					.map(update -> (ClassTag) update)
					.findFirst().orElseThrow(() -> new CommandException("Object " + object + " has no class tag in the store of the node!"));
		}

		private SortedSet<UpdateOfField> fields(Update[] updates) {
			return Stream.of(updates)
				.filter(update -> update instanceof UpdateOfField)
				.map(update -> (UpdateOfField) update)
				.collect(Collectors.toCollection(TreeSet::new));
		}

		private SortedSet<ConstructorDescription> constructors(Class<?> clazz, WhiteListingWizard whiteListingWizard) {
			return Stream.of(clazz.getConstructors())
				.filter(constructor -> whiteListingWizard.whiteListingModelOf(constructor).isPresent())
				.map(ConstructorDescriptionImpl::new)
				.collect(Collectors.toCollection(TreeSet::new));
		}

		private SortedSet<MethodDescription> methods(Class<?> clazz, WhiteListingWizard whiteListingWizard) {
			return Stream.of(clazz.getMethods())
				.filter(method -> whiteListingWizard.whiteListingModelOf(method).isPresent())
				.map(MethodDescriptionImpl::new)
				.collect(Collectors.toCollection(TreeSet::new));
		}

		private String valueToPrint(UpdateOfField update) {
			if (update instanceof UpdateOfString)
				return cyan('\"' + update.getValue().toString() + '\"');
			else
				return cyan(update.getValue().toString());
		}

		private void printMethod(MethodDescription method, StringBuilder sb) {
			if (method.getAnnotations().isEmpty())
				if (tag.getClazz().getName().equals(method.getDeclaringClass()))
					sb.append("  " + method.getSignature() + "\n");
				else {
					int lastSpace = method.getSignature().lastIndexOf(' ');
					String methodAsString = method.getSignature().substring(0, lastSpace + 1) + green(method.getDeclaringClass()) + "." + method.getSignature().substring(lastSpace + 1);
					sb.append("  " + methodAsString + "\n");
				}
			else
				if (tag.getClazz().getName().equals(method.getDeclaringClass()))
					sb.append("  " + red(method.getAnnotations()) + " " + method.getSignature() + "\n");
				else {
					int lastSpace = method.getSignature().lastIndexOf(' ');
					String methodAsString = method.getSignature().substring(0, lastSpace + 1) + green(method.getDeclaringClass()) + "." + method.getSignature().substring(lastSpace + 1);
					sb.append("  " + red(method.getAnnotations()) + " " + methodAsString + "\n");
				}
		}

		private void printConstructor(ConstructorDescription constructor, StringBuilder sb) {
			if (constructor.getAnnotations().isEmpty())
				sb.append("  " + constructor.getSignature() + "\n");
			else
				sb.append("  " + red(constructor.getAnnotations()) + " " + constructor.getSignature() + "\n");
		}
	}
}