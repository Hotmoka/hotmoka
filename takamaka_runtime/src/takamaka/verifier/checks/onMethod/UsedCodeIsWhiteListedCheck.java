package takamaka.verifier.checks.onMethod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.util.Optional;

import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;

import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.IllegalAccessToNonWhiteListedFieldError;
import takamaka.verifier.errors.IllegalCallToNonWhiteListedConstructorError;
import takamaka.verifier.errors.IllegalCallToNonWhiteListedMethodError;
import takamaka.verifier.errors.UnresolvedCallError;

/**
 * A check that a method calls white-listed methods only and accesses white-listed fields only.
 */
public class UsedCodeIsWhiteListedCheck extends VerifiedClassGen.Verifier.MethodVerifier.Check {

	public UsedCodeIsWhiteListedCheck(VerifiedClassGen.Verifier.MethodVerifier verifier) {
		verifier.super();

		instructions().forEach(ih -> {
			Instruction ins = ih.getInstruction();
			if (ins instanceof FieldInstruction) {
				FieldInstruction fi = (FieldInstruction) ins;
				Optional<Field> field = resolvedFieldFor(fi);
				if (!field.isPresent() || !classLoader.whiteListingWizard.whiteListingModelOf(field.get()).isPresent())
					issue(new IllegalAccessToNonWhiteListedFieldError(clazz, method, lineOf(ih), fi.getLoadClassType(cpg).getClassName(), fi.getFieldName(cpg)));
			}

			if (ins instanceof InvokeInstruction) {
				InvokeInstruction invoke = (InvokeInstruction) ins;
				Optional<? extends Executable> executable = resolvedExecutableFor(invoke);
				if (!executable.isPresent())
					issue(new UnresolvedCallError(clazz, method, lineOf(ih), invoke.getReferenceType(cpg).toString(), invoke.getMethodName(cpg)));
				else {
					Executable target = executable.get();
					if (!whiteListingModelOf(target, invoke).isPresent())
						if (target instanceof Constructor<?>)
							issue(new IllegalCallToNonWhiteListedConstructorError(clazz, method, lineOf(ih), target.getDeclaringClass().getName()));
						else
							issue(new IllegalCallToNonWhiteListedMethodError(clazz, method, lineOf(ih), target.getDeclaringClass().getName(), target.getName()));
				}
			}
		});
	}
}