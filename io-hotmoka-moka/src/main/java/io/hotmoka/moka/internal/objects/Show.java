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
import java.security.NoSuchAlgorithmException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.cli.CommandException;
import io.hotmoka.helpers.ClassLoaderHelpers;
import io.hotmoka.moka.api.objects.ObjectsShowOutput;
import io.hotmoka.moka.api.objects.ObjectsShowOutput.ConstructorDescription;
import io.hotmoka.moka.api.objects.ObjectsShowOutput.MethodDescription;
import io.hotmoka.moka.internal.AbstractMokaRpcCommand;
import io.hotmoka.moka.internal.converters.StorageReferenceOptionConverter;
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
import io.hotmoka.whitelisting.api.WhiteListingWizard;
import io.takamaka.code.constants.Constants;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "show", description = "Show the state of a storage object.")
public class Show extends AbstractMokaRpcCommand {

	@Parameters(index = "0", description = "the storage reference of the object", converter = StorageReferenceOptionConverter.class)
    private StorageReference object;

	@Option(names = "--api", description = "print the public API of the object")
    private boolean api;

	@Override
	protected void body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException, CommandException {
		System.out.print(new Output(remote, object, api));
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

	public static class ConstructorDescriptionImpl implements ConstructorDescription {
		private final String annotations;
		private final String signature;

		private ConstructorDescriptionImpl(String annotations, String signature) {
			this.annotations = annotations;
			this.signature = signature;
		}

		private ConstructorDescriptionImpl(Constructor<?> constructor) {
			this.annotations = annotationsAsString(constructor);
			var declaringClass = constructor.getDeclaringClass();
			this.signature = constructor.toString().replace(declaringClass.getName() + "(", declaringClass.getSimpleName() + "(");
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

	private static class MethodDescriptionImpl implements MethodDescription {
		private final String annotations;
		private final String declaringClass;
		private final String signature;

		private MethodDescriptionImpl(String annotations, String definingClass, String signature) {
			this.annotations = annotations;
			this.declaringClass = definingClass;
			this.signature = signature;
		}

		private MethodDescriptionImpl(Method method) {
			this.annotations = annotationsAsString(method);
			var declaringClass = method.getDeclaringClass();
			this.declaringClass = declaringClass.getName();
			this.signature = method.toString().replace(declaringClass + "." + method.getName(), method.getName());
		}

		@Override
		public String getAnnotations() {
			return annotations;
		}

		@Override
		public String getDefiningClass() {
			return declaringClass;
		}

		public String getSignature() {
			return signature;
		}

		@Override
		public int compareTo(MethodDescription other) {
			int diff = declaringClass.compareTo(other.getDefiningClass());
			if (diff != 0)
				return diff;
			else
				return signature.compareTo(other.getSignature());
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof MethodDescription md && declaringClass.equals(md.getDefiningClass()) && signature.equals(md.getSignature());
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

		@Override
		public ClassTag getClassTag() {
			return tag;
		}

		@Override
		public Stream<UpdateOfField> getFields() {
			return fields.stream();
		}

		@Override
		public Stream<ConstructorDescription> getConstructorDescriptions() {
			return constructors.stream();
		}

		@Override
		public Stream<MethodDescription> getMethodDescriptions() {
			return methods.stream();
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 * @throws NoSuchAlgorithmException if {@code json} refers to a non-available cryptographic algorithm
		 */
		/*public Output(AccountsShowOutputJson json) throws InconsistentJsonException, NoSuchAlgorithmException {
			ExceptionSupplier<InconsistentJsonException> exp = InconsistentJsonException::new;

			this.balance = Objects.requireNonNull(json.getBalance(), "balance cannot be null", exp);
			if (balance.signum() < 0)
				throw new InconsistentJsonException("The balance of the account cannot be negative");

			this.signature = SignatureAlgorithms.of(Objects.requireNonNull(json.getSignature(), "signature cannot be null", exp));
			this.publicKeyBase58 = Base58.requireBase58(Objects.requireNonNull(json.getPublicKeyBase58(), "publicKeyBase58 cannot be null", exp), exp);
			this.publicKeyBase64 = Base64.requireBase64(Objects.requireNonNull(json.getPublicKeyBase64(), "publicKeyBase64 cannot be null", exp), exp);
		}*/

		@Override
		public String toString() {
			var sb = new StringBuilder();

			sb.append(green("class " + tag.getClazz() + " (from jar installed at " + tag.getJar() + ")\n"));

			getFields().filter(update -> update.getField().getDefiningClass().equals(tag.getClazz()))
				.forEachOrdered(update -> sb.append("  " + update.getField().getName() + ":" + update.getField().getType() + " = " + valueToPrint(update) + "\n"));

			getFields().filter(update -> !update.getField().getDefiningClass().equals(tag.getClazz()))
				.forEachOrdered(update -> sb.append("  " + green(update.getField().getDefiningClass().toString()) + "." + update.getField().getName() + ":" + update.getField().getType() + " = " + valueToPrint(update) + "\n"));

			if (!constructors.isEmpty()) {
				sb.append("\n");
				getConstructorDescriptions().forEachOrdered(constructor -> printConstructor(constructor, sb));
			}

			if (!methods.isEmpty()) {
				sb.append("\n");
				getMethodDescriptions().forEachOrdered(constructor -> printMethod(constructor, sb));
			}

			return sb.toString();
		}

		private TakamakaClassLoader mkClassLoader(RemoteNode remote, StorageReference object) throws NodeException, TimeoutException, InterruptedException, CommandException {
			try {
				return ClassLoaderHelpers.of(remote).classloaderFor(tag.getJar());
			}
			catch (UnknownReferenceException e) {
				throw new CommandException("The object " + object + " has been installed by transaction " + tag.getJar() + " but the latter cannot be found in store!");
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
				if (tag.getClazz().getName().equals(method.getDefiningClass()))
					sb.append("  " + method.getSignature() + "\n");
				else {
					int lastSpace = method.getSignature().lastIndexOf(' ');
					String methodAsString = method.getSignature().substring(0, lastSpace + 1) + green(method.getDefiningClass()) + "." + method.getSignature().substring(lastSpace + 1);
					sb.append("  " + methodAsString + "\n");
				}
			else
				if (tag.getClazz().getName().equals(method.getDefiningClass()))
					sb.append("  " + red(method.getAnnotations()) + " " + method.getSignature() + "\n");
				else {
					int lastSpace = method.getSignature().lastIndexOf(' ');
					String methodAsString = method.getSignature().substring(0, lastSpace + 1) + green(method.getDefiningClass()) + "." + method.getSignature().substring(lastSpace + 1);
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