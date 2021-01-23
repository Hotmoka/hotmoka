package io.hotmoka.beans.signatures;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;

/**
 * The signature of a method or constructor.
 */
@Immutable
public abstract class CodeSignature extends Marshallable {

	/**
	 * The class of the method or constructor.
	 */
	public final ClassType definingClass;

	/**
	 * The formal arguments of the method or constructor.
	 */
	private final StorageType[] formals;

	/**
	 * The method {@code getBalance} of a test externally-owned account.
	 */
	public final static MethodSignature GET_BALANCE = new NonVoidMethodSignature(ClassType.TEOA, "getBalance", ClassType.BIG_INTEGER);

	/**
	 * The method {@code nonce} of an account.
	 */
	public final static MethodSignature NONCE = new NonVoidMethodSignature(ClassType.ACCOUNT, "nonce", ClassType.BIG_INTEGER);

	/**
	 * The method {@code getChainId} of the manifest.
	 */
	public final static MethodSignature GET_CHAIN_ID = new NonVoidMethodSignature(ClassType.MANIFEST, "getChainId", ClassType.STRING);

	/**
	 * The method {@code getMaxErrorLength} of the manifest.
	 */
	public final static MethodSignature GET_MAX_ERROR_LENGTH = new NonVoidMethodSignature(ClassType.MANIFEST, "getMaxErrorLength", BasicTypes.INT);

	/**
	 * The method {@code allowsSelfCharged} of the manifest.
	 */
	public final static MethodSignature ALLOWS_SELF_CHARGED = new NonVoidMethodSignature(ClassType.MANIFEST, "allowsSelfCharged", BasicTypes.BOOLEAN);

	/**
	 * The method {@code getSignature} of the manifest.
	 */
	public final static MethodSignature GET_SIGNATURE = new NonVoidMethodSignature(ClassType.MANIFEST, "getSignature", ClassType.STRING);

	/**
	 * The method {@code getGamete} of the manifest.
	 */
	public final static MethodSignature GET_GAMETE = new NonVoidMethodSignature(ClassType.MANIFEST, "getGamete", ClassType.ACCOUNT);

	/**
	 * The method {@code getGasStation} of the manifest.
	 */
	public final static MethodSignature GET_GAS_STATION = new NonVoidMethodSignature(ClassType.MANIFEST, "getGasStation", ClassType.GAS_STATION);

	/**
	 * The method {@code getVersions} of the manifest.
	 */
	public final static MethodSignature GET_VERSIONS = new NonVoidMethodSignature(ClassType.MANIFEST, "getVersions", ClassType.VERSIONS);

	/**
	 * The method {@code getGasPrice} of the gas station.
	 */
	public final static MethodSignature GET_GAS_PRICE = new NonVoidMethodSignature(ClassType.GAS_STATION, "getGasPrice", ClassType.BIG_INTEGER);

	/**
	 * The method {@code getMaxGasPerTransaction} of the gas station.
	 */
	public final static MethodSignature GET_MAX_GAS_PER_TRANSACTION = new NonVoidMethodSignature(ClassType.GAS_STATION, "getMaxGasPerTransaction", ClassType.BIG_INTEGER);

	/**
	 * The method {@code getTargetGasAtReward} of the gas station.
	 */
	public final static MethodSignature GET_TARGET_GAS_AT_REWARD = new NonVoidMethodSignature(ClassType.GAS_STATION, "getTargetGasAtReward", ClassType.BIG_INTEGER);

	/**
	 * The method {@code getOblivion} of the gas station.
	 */
	public final static MethodSignature GET_OBLIVION = new NonVoidMethodSignature(ClassType.GAS_STATION, "getOblivion", BasicTypes.LONG);

	/**
	 * The method {@code ignoresGasPrice} of the gas station.
	 */
	public final static MethodSignature IGNORES_GAS_PRICE = new NonVoidMethodSignature(ClassType.GAS_STATION, "ignoresGasPrice", BasicTypes.BOOLEAN);

	/**
	 * The method {@code getValidators} of the manifest.
	 */
	public final static MethodSignature GET_VALIDATORS = new NonVoidMethodSignature(ClassType.MANIFEST, "getValidators", ClassType.VALIDATORS);

	/**
	 * The method {@code getVerificationVersion} of the versions object.
	 */
	public final static MethodSignature GET_VERIFICATION_VERSION = new NonVoidMethodSignature(ClassType.VERSIONS, "getVerificationVersion", BasicTypes.INT);

	 /**
	 * The method {@code id} of a validator.
	 */
	public final static MethodSignature ID = new NonVoidMethodSignature(ClassType.VALIDATOR, "id", ClassType.STRING);

	/**
	 * The method {@code receive} of a payable contract, with a big integer argument.
	 */
	public final static MethodSignature RECEIVE_BIG_INTEGER = new VoidMethodSignature(ClassType.PAYABLE_CONTRACT, "receive", ClassType.BIG_INTEGER);

	/**
	 * The method {@code receive} of a payable contract, with an int argument.
	 */
	public final static MethodSignature RECEIVE_INT = new VoidMethodSignature(ClassType.PAYABLE_CONTRACT, "receive", BasicTypes.INT);

	/**
	 * The method {@code receive} of a payable contract, with a long argument.
	 */
	public final static MethodSignature RECEIVE_LONG = new VoidMethodSignature(ClassType.PAYABLE_CONTRACT, "receive", BasicTypes.LONG);

	/**
	 * The method {@code reward} of the validators contract.
	 */
	public final static MethodSignature REWARD = new VoidMethodSignature(ClassType.VALIDATORS, "reward", ClassType.STRING, ClassType.STRING, ClassType.BIG_INTEGER);

	/**
	 * The method {@code newPoll} of the generic validators contract.
	 */
	public final static MethodSignature NEW_POLL = new NonVoidMethodSignature(ClassType.GENERIC_VALIDATORS, "newPoll", ClassType.POLL);
	
	/**
	 * The method {@code newPollWithTimeParams} of the generic validators contract with time parameters.
	 */
	public final static MethodSignature NEW_POLL_WITH_TIME_PARAMS = new NonVoidMethodSignature(ClassType.GENERIC_VALIDATORS, "newPollWithTimeParams", ClassType.POLL, ClassType.BIG_INTEGER, ClassType.BIG_INTEGER);
	
	/**
	 * The method {@code isVoteOver} of the Poll contract.
	 */
	public final static MethodSignature IS_VOTE_OVER = new NonVoidMethodSignature(ClassType.POLL, "isVoteOver", BasicTypes.BOOLEAN);
	
	/**
	 * The method {@code closePoll} of the Poll contract.
	 */
	public final static MethodSignature CLOSE_POLL = new VoidMethodSignature(ClassType.POLL, "closePoll");
	
	/**
	 * The method {@code vote} of the Poll contract.
	 */
	public final static MethodSignature VOTE = new VoidMethodSignature(ClassType.POLL, "vote");
	
	/**
	 * The method {@code vote} of the Poll contract with the share parameter.
	 */
	public final static MethodSignature VOTE_WITH_SHARE = new VoidMethodSignature(ClassType.POLL, "vote", ClassType.BIG_INTEGER);
	
	/**
	 * Builds the signature of a method or constructor.
	 * 
	 * @param definingClass the class of the method or constructor
	 * @param formals the formal arguments of the method or constructor
	 */
	protected CodeSignature(ClassType definingClass, StorageType... formals) {
		if (definingClass == null)
			throw new IllegalArgumentException("definingClass cannot be null");

		if (formals == null)
			throw new IllegalArgumentException("formals cannot be null");

		for (StorageType formal: formals)
			if (formal == null)
				throw new IllegalArgumentException("formals cannot hold null");

		this.definingClass = definingClass;
		this.formals = formals;
	}

	/**
	 * Builds the signature of a method or constructor.
	 * 
	 * @param definingClass the name of the class of the method or constructor
	 * @param formals the formal arguments of the method or constructor
	 */
	public CodeSignature(String definingClass, StorageType... formals) {
		this(new ClassType(definingClass), formals);
	}

	/**
	 * Yields the formal arguments of the method or constructor, ordered left to right.
	 * 
	 * @return the formal arguments
	 */
	public final Stream<StorageType> formals() {
		return Stream.of(formals);
	}

	/**
	 * Yields a comma-separated string of the formal arguments of the method or constructor, ordered left to right.
	 * 
	 * @return the string
	 */
	protected final String commaSeparatedFormals() {
		return formals()
			.map(StorageType::toString)
			.collect(Collectors.joining(",", "(", ")"));
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof CodeSignature && ((CodeSignature) other).definingClass.equals(definingClass)
			&& Arrays.equals(((CodeSignature) other).formals, formals);
	}

	@Override
	public int hashCode() {
		return definingClass.hashCode() ^ Arrays.hashCode(formals);
	}

	/**
	 * Yields the size of this code signature, in terms of gas units consumed in store.
	 * 
	 * @param gasCostModel the model of the costs
	 * @return the size
	 */
	public BigInteger size(GasCostModel gasCostModel) {
		return BigInteger.valueOf(gasCostModel.storageCostPerSlot())
			.add(definingClass.size(gasCostModel))
			.add(formals().map(type -> type.size(gasCostModel)).reduce(BigInteger.ZERO, BigInteger::add));
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		definingClass.into(context);
		context.oos.writeInt(formals.length);
		for (StorageType formal: formals)
			formal.into(context);
	}

	/**
	 * Factory method that unmarshals a code signature from the given stream.
	 * 
	 * @param ois the stream
	 * @return the code signature
	 * @throws IOException if the code signature could not be unmarshalled
	 * @throws ClassNotFoundException if the code signature could not be unmarshalled
	 */
	public static CodeSignature from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		byte selector = ois.readByte();
		ClassType definingClass = (ClassType) StorageType.from(ois);
		int formalsCount = ois.readInt();
		StorageType[] formals = new StorageType[formalsCount];
		for (int pos = 0; pos < formalsCount; pos++)
			formals[pos] = StorageType.from(ois);

		switch (selector) {
		case ConstructorSignature.SELECTOR: return new ConstructorSignature(definingClass, formals);
		case VoidMethodSignature.SELECTOR: return new VoidMethodSignature(definingClass, ois.readUTF(), formals);
		case NonVoidMethodSignature.SELECTOR: return new NonVoidMethodSignature(definingClass, ois.readUTF(), StorageType.from(ois), formals);
		default: throw new IOException("unexpected code signature selector: " + selector);
		}
	}
}