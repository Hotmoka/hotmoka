package io.hotmoka.beans.signatures;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;
import io.takamaka.code.constants.Constants;

/**
 * The signature of a field of a class.
 */
@Immutable
public final class FieldSignature extends Marshallable implements Comparable<FieldSignature> {

	/**
	 * The field that holds the balance in contracts.
	 */
	public final static FieldSignature BALANCE_FIELD = new FieldSignature(ClassType.CONTRACT, "balance", ClassType.BIG_INTEGER);

	/**
	 * The field that holds the red balance in red/green contracts.
	 */
	public final static FieldSignature RED_BALANCE_FIELD = new FieldSignature(ClassType.RGCONTRACT, "balanceRed", ClassType.BIG_INTEGER);

	/**
	 * The field that holds the nonce in externally owned accounts.
	 */
	public final static FieldSignature EOA_NONCE_FIELD = new FieldSignature(ClassType.EOA, "nonce", ClassType.BIG_INTEGER);

	/**
	 * The field that holds the nonce in red/green externally owned accounts.
	 */
	public final static FieldSignature RGEOA_NONCE_FIELD = new FieldSignature(ClassType.RGEOA, "nonce", ClassType.BIG_INTEGER);

	/**
	 * The field of the manifest that holds the chain identifier of the node.
	 */
	public final static FieldSignature MANIFEST_CHAIN_ID = new FieldSignature(Constants.MANIFEST_NAME, "chainId", ClassType.STRING);

	/**
	 * The class of the field.
	 */
	public final ClassType definingClass;

	/**
	 * The name of the field.
	 */
	public final String name;

	/**
	 * The type of the field.
	 */
	public final StorageType type;

	/**
	 * Builds the signature of a field.
	 * 
	 * @param definingClass the class of the field
	 * @param name the name of the field
	 * @param type the type of the field
	 */
	public FieldSignature(ClassType definingClass, String name, StorageType type) {
		this.definingClass = definingClass;
		this.name = name;
		this.type = type;
	}

	/**
	 * Builds the signature of a field.
	 * 
	 * @param definingClass the name of the class of the field
	 * @param name the name of the field
	 * @param type the type of the field
	 */
	public FieldSignature(String definingClass, String name, StorageType type) {
		this(new ClassType(definingClass), name, type);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof FieldSignature && ((FieldSignature) other).definingClass.equals(definingClass)
			&& ((FieldSignature) other).name.equals(name) && ((FieldSignature) other).type.equals(type);
	}

	@Override
	public int hashCode() {
		return definingClass.hashCode() ^ name.hashCode() ^ type.hashCode();
	}

	@Override
	public String toString() {
		return definingClass + "." + name + ":" + type;
	}

	@Override
	public int compareTo(FieldSignature other) {
		int diff = definingClass.compareAgainst(other.definingClass);
		if (diff != 0)
			return diff;

		diff = name.compareTo(other.name);
		return diff != 0 ? diff : type.compareAgainst(other.type);
	}

	/**
	 * Yields the size of this field, in terms of gas units consumed in store.
	 * 
	 * @param gasCostModel the model of the costs
	 * @return the size
	 */
	public BigInteger size(GasCostModel gasCostModel) {
		return BigInteger.valueOf(gasCostModel.storageCostPerSlot()).add(definingClass.size(gasCostModel))
			.add(gasCostModel.storageCostOf(name)).add(type.size(gasCostModel));
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		definingClass.into(context);
		context.oos.writeUTF(name);
		type.into(context);
	}

	/**
	 * Factory method that unmarshals a field signature from the given stream.
	 * 
	 * @param ois the stream
	 * @return the field signature
	 * @throws IOException if the field signature could not be unmarshalled
	 * @throws ClassNotFoundException if the field signature could not be unmarshalled
	 */
	public static FieldSignature from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		return new FieldSignature((ClassType) StorageType.from(ois), ois.readUTF(), StorageType.from(ois));
	}
}