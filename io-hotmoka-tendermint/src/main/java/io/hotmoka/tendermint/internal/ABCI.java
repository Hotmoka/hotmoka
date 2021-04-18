package io.hotmoka.tendermint.internal;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;

import io.grpc.stub.StreamObserver;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.UnmarshallingContext;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.tendermint.TendermintValidator;
import tendermint.abci.types.ABCIApplicationGrpc;
import tendermint.abci.types.Types.Evidence;
import tendermint.abci.types.Types.RequestBeginBlock;
import tendermint.abci.types.Types.RequestCheckTx;
import tendermint.abci.types.Types.RequestCommit;
import tendermint.abci.types.Types.RequestDeliverTx;
import tendermint.abci.types.Types.RequestEcho;
import tendermint.abci.types.Types.RequestEndBlock;
import tendermint.abci.types.Types.RequestFlush;
import tendermint.abci.types.Types.RequestInfo;
import tendermint.abci.types.Types.RequestInitChain;
import tendermint.abci.types.Types.RequestQuery;
import tendermint.abci.types.Types.ResponseBeginBlock;
import tendermint.abci.types.Types.ResponseCheckTx;
import tendermint.abci.types.Types.ResponseCommit;
import tendermint.abci.types.Types.ResponseDeliverTx;
import tendermint.abci.types.Types.ResponseEcho;
import tendermint.abci.types.Types.ResponseEndBlock;
import tendermint.abci.types.Types.ResponseFlush;
import tendermint.abci.types.Types.ResponseInfo;
import tendermint.abci.types.Types.ResponseInitChain;
import tendermint.abci.types.Types.ResponseQuery;
import tendermint.abci.types.Types.ResponseQuery.Builder;
import tendermint.abci.types.Types.Validator;
import tendermint.abci.types.Types.ValidatorUpdate;
import tendermint.abci.types.Types.VoteInfo;
import tendermint.crypto.Keys.PublicKey;

/**
 * The Tendermint interface that links a Hotmoka Tendermint node to a Tendermint process.
 * It implements a set of handlers that Tendermint calls to notify events.
 */
class ABCI extends ABCIApplicationGrpc.ABCIApplicationImplBase {

	private final static Logger logger = LoggerFactory.getLogger(ABCI.class);

	/**
	 * The Tendermint blockchain.
	 */
	private final TendermintBlockchainInternal node;

	/**
	 * The Tendermint validators at the time of the last {@link #beginBlock(RequestBeginBlock, StreamObserver)}
	 * that has been executed.
	 */
	private volatile TendermintValidator[] validatorsAtLastBeginBlock;

	/**
     * Builds the Tendermint ABCI interface that executes Takamaka transactions.
     * 
     * @param node the node whose transactions are executed
     */
    ABCI(TendermintBlockchainInternal node) {
    	this.node = node;
    }

    @Override
	public void initChain(RequestInitChain req, StreamObserver<ResponseInitChain> responseObserver) {
    	ResponseInitChain resp = ResponseInitChain.newBuilder().build();
    	responseObserver.onNext(resp);
    	responseObserver.onCompleted();
    }

    @Override
    public void echo(RequestEcho req, StreamObserver<ResponseEcho> responseObserver) {
        ResponseEcho resp = ResponseEcho.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void info(RequestInfo req, StreamObserver<ResponseInfo> responseObserver) {
        ResponseInfo resp = ResponseInfo.newBuilder()
       		.setLastBlockAppHash(ByteString.copyFrom(node.getStore().getHash())) // hash of the store used for consensus
       		.setLastBlockHeight(node.getStore().getNumberOfCommits()).build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void checkTx(RequestCheckTx tendermintRequest, StreamObserver<ResponseCheckTx> responseObserver) {
        ByteString tx = tendermintRequest.getTx();
        ResponseCheckTx.Builder responseBuilder = ResponseCheckTx.newBuilder();

        try (UnmarshallingContext context = new UnmarshallingContext(new ByteArrayInputStream(tx.toByteArray()))) {
        	TransactionRequest<?> request = TransactionRequest.from(context);
        	node.checkTransaction(request);
        	responseBuilder.setCode(0);
        }
        catch (Throwable t) {
        	responseBuilder.setCode(t instanceof TransactionRejectedException ? 1 : 2);
        	responseBuilder.setData(trimmedMessage(t));
		}

        ResponseCheckTx resp = responseBuilder.build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    private static String getAddressOfValidator(Validator validator) {
    	return Hex.toHexString(validator.getAddress().toByteArray()).toUpperCase();
    }

    @Override
    public void beginBlock(RequestBeginBlock req, StreamObserver<ResponseBeginBlock> responseObserver) {
    	String behaving = commaSeparatedSequenceOfBehavingValidatorsAddresses(req);
    	String misbehaving = commaSeparatedSequenceOfMisbehavingValidatorsAddresses(req);
    	long now = timeNow(req);

    	node.getStore().beginTransaction(now);
		node.rewardValidators(behaving, misbehaving);

		ResponseBeginBlock resp = ResponseBeginBlock.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();

        // the ABCI might start too early, before the Tendermint process is up
        if (node.getPoster() != null)
        	validatorsAtLastBeginBlock = node.getPoster().getTendermintValidators().toArray(TendermintValidator[]::new);
    }

    private static long timeNow(RequestBeginBlock req) {
    	Timestamp time = req.getHeader().getTime();
    	return time.getSeconds() * 1_000L + time.getNanos() / 1_000_000L;
    }

    private static String commaSeparatedSequenceOfMisbehavingValidatorsAddresses(RequestBeginBlock req) {
		return req.getByzantineValidatorsList().stream()
    		.map(Evidence::getValidator)
    		.map(ABCI::getAddressOfValidator)
    		.collect(Collectors.joining(" "));
	}

	private static String commaSeparatedSequenceOfBehavingValidatorsAddresses(RequestBeginBlock req) {
		return req.getLastCommitInfo().getVotesList().stream()
    		.filter(VoteInfo::getSignedLastBlock)
    		.map(VoteInfo::getValidator)
    		.map(ABCI::getAddressOfValidator)
    		.collect(Collectors.joining(" "));
	}

	@Override
    public void deliverTx(RequestDeliverTx tendermintRequest, StreamObserver<ResponseDeliverTx> responseObserver) {
    	ByteString tx = tendermintRequest.getTx();
        ResponseDeliverTx.Builder responseBuilder = ResponseDeliverTx.newBuilder();

        try (UnmarshallingContext context = new UnmarshallingContext(new ByteArrayInputStream(tx.toByteArray()))) {
        	TransactionRequest<?> request = TransactionRequest.from(context);
        	node.deliverTransaction(request);
        	responseBuilder.setCode(0);
        }
        catch (Throwable t) {
        	responseBuilder.setCode(t instanceof TransactionRejectedException ? 1 : 2);
        	responseBuilder.setData(trimmedMessage(t));
        }

        ResponseDeliverTx resp = responseBuilder.build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void endBlock(RequestEndBlock req, StreamObserver<ResponseEndBlock> responseObserver) {
    	ResponseEndBlock.Builder builder = ResponseEndBlock.newBuilder();

    	if (validatorsAtLastBeginBlock != null) {
    		try {
    			TendermintValidator[] currentValidators = validatorsAtLastBeginBlock;
    			Optional<TendermintValidator[]> validatorsInStore = node.getTendermintValidatorsInStore();
    			if (validatorsInStore.isPresent()) {
    				TendermintValidator[] nextValidators = validatorsInStore.get();
    				if (nextValidators.length == 0)
    					logger.info("refusing to remove all validators; please initialize the node with TendermintInitializedNode");
    				else {
    					removeCurrentValidatorsThatAreNotNextValidators(currentValidators, nextValidators, builder);
    					addNextValidatorsThatAreNotCurrentValidators(currentValidators, nextValidators, builder);
    					updateValidatorsThatChangedPower(currentValidators, nextValidators, builder);
    				}
    			}
    		}
    		catch (Exception e) {
    			throw InternalFailureException.of("could not determine the new validators set", e);
    		}
    	}

    	ResponseEndBlock resp = builder.build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

	private static void updateValidatorsThatChangedPower(TendermintValidator[] currentValidators, TendermintValidator[] nextValidators, ResponseEndBlock.Builder builder) {
		Stream.of(nextValidators)
			.filter(validator -> isContainedWithDistinctPower(validator.address, validator.power, currentValidators))
			.forEachOrdered(validator -> updateValidator(validator, builder));
	}

	private static void addNextValidatorsThatAreNotCurrentValidators(TendermintValidator[] currentValidators, TendermintValidator[] nextValidators, ResponseEndBlock.Builder builder) {
		Stream.of(nextValidators)
			.filter(validator -> isNotContained(validator.address, currentValidators))
			.forEachOrdered(validator -> addValidator(validator, builder));
	}

	private static void removeCurrentValidatorsThatAreNotNextValidators(TendermintValidator[] currentValidators, TendermintValidator[] nextValidators, ResponseEndBlock.Builder builder) {
		Stream.of(currentValidators)
			.filter(validator -> isNotContained(validator.address, nextValidators))
			.forEachOrdered(validator -> removeValidator(validator, builder));
	}

    private static void removeValidator(TendermintValidator tv, ResponseEndBlock.Builder builder) {
    	builder.addValidatorUpdates(intoValidatorUpdate(tv, 0L));
    	logger.info("removed Tendermint validator with address " + tv.address + " and power " + tv.power);
    }

    private static void addValidator(TendermintValidator tv, ResponseEndBlock.Builder builder) {
    	builder.addValidatorUpdates(intoValidatorUpdate(tv, tv.power));
    	logger.info("added Tendermint validator with address " + tv.address + " and power " + tv.power);
    }

    private static void updateValidator(TendermintValidator tv, ResponseEndBlock.Builder builder) {
    	builder.addValidatorUpdates(intoValidatorUpdate(tv, tv.power));
    	logger.info("updated Tendermint validator with address " + tv.address + " by setting its new power to " + tv.power);
    }

    private static ValidatorUpdate intoValidatorUpdate(TendermintValidator validator, long newPower) {
    	byte[] raw = Base64.getDecoder().decode(validator.publicKey);
    	PublicKey publicKey = PublicKey.newBuilder().setEd25519(ByteString.copyFrom(raw)).build();

    	return ValidatorUpdate.newBuilder()
    		.setPubKey(publicKey)
    		.setPower(newPower)
    		.build();
    }

    private static boolean isNotContained(String address, TendermintValidator[] validators) {
    	return Stream.of(validators).map(validator -> validator.address).noneMatch(address::equals);
    }

    private static boolean isContainedWithDistinctPower(String address, long power, TendermintValidator[] validators) {
    	return Stream.of(validators).anyMatch(validator -> validator.address.equals(address) && validator.power != power);
    }

    @Override
    public void commit(RequestCommit req, StreamObserver<ResponseCommit> responseObserver) {
    	Store store = node.getStore();
    	node.commitTransactionAndCheckout();
        ResponseCommit resp = ResponseCommit.newBuilder()
       		.setData(ByteString.copyFrom(store.getHash())) // hash of the store, used for consensus
       		.build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void query(RequestQuery req, StreamObserver<ResponseQuery> responseObserver) {
        Builder builder = ResponseQuery.newBuilder().setLog("nop");
        ResponseQuery resp = builder.build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void flush(RequestFlush request, StreamObserver<ResponseFlush> responseObserver) {
    	ResponseFlush resp = ResponseFlush.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    /**
     * Yields the error message in a format that can be put in the data field
     * of Tendermint responses. The message is trimmed, to avoid overflow.
     * It will be automatically Base64 encoded by Tendermint, so that there
     * is no risk of injections.
     *
     * @param t the throwable whose error message is processed
     * @return the resulting message
     */
    private ByteString trimmedMessage(Throwable t) {
		return ByteString.copyFromUtf8(node.trimmedMessage(t));
    }
}