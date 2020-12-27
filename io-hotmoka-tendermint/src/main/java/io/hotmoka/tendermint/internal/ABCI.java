package io.hotmoka.tendermint.internal;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
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
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.tendermint.TendermintValidator;
import types.ABCIApplicationGrpc;
import types.Types.Evidence;
import types.Types.PubKey;
import types.Types.RequestBeginBlock;
import types.Types.RequestCheckTx;
import types.Types.RequestCommit;
import types.Types.RequestDeliverTx;
import types.Types.RequestEcho;
import types.Types.RequestEndBlock;
import types.Types.RequestFlush;
import types.Types.RequestInfo;
import types.Types.RequestInitChain;
import types.Types.RequestQuery;
import types.Types.RequestSetOption;
import types.Types.ResponseBeginBlock;
import types.Types.ResponseCheckTx;
import types.Types.ResponseCommit;
import types.Types.ResponseDeliverTx;
import types.Types.ResponseEcho;
import types.Types.ResponseEndBlock;
import types.Types.ResponseFlush;
import types.Types.ResponseInfo;
import types.Types.ResponseInitChain;
import types.Types.ResponseQuery;
import types.Types.ResponseQuery.Builder;
import types.Types.ResponseSetOption;
import types.Types.Validator;
import types.Types.ValidatorUpdate;
import types.Types.VoteInfo;

/**
 * The Tendermint interface that links a Hotmoka Tendermint node to a Tendermint process.
 * It implements a set of handlers that Tendermint calls to notify events.
 */
class ABCI extends ABCIApplicationGrpc.ABCIApplicationImplBase {

	private final static Logger logger = LoggerFactory.getLogger(ABCI.class);

	/**
	 * The Tendermint blockchain linked to Tendermint.
	 */
	private final TendermintBlockchainImpl node;

	/**
	 * The Tendermint validators at the time of the last {@link #beginBlock(RequestBeginBlock, StreamObserver)}
	 * that has been executed.
	 */
	private volatile TendermintValidator[] validatorsAtLastBeginBlock;

	/**
     * Builds the Tendermint ABCI interface that executes Takamaka transactions.
     * 
     * @param node
     */
    ABCI(TendermintBlockchainImpl node) {
    	this.node = node;
    }

    @Override
	public void initChain(RequestInitChain req, StreamObserver<ResponseInitChain> responseObserver) {
    	try {
    		ResponseInitChain resp = ResponseInitChain.newBuilder().build();
    		responseObserver.onNext(resp);
    		responseObserver.onCompleted();
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    		throw new RuntimeException(e);
    	}
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
    public void setOption(RequestSetOption req, StreamObserver<ResponseSetOption> responseObserver) {
        ResponseSetOption resp = ResponseSetOption.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void checkTx(RequestCheckTx tendermintRequest, StreamObserver<ResponseCheckTx> responseObserver) {
        ByteString tx = tendermintRequest.getTx();
        ResponseCheckTx.Builder responseBuilder = ResponseCheckTx.newBuilder();

        TransactionRequest<?> request = null;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(tx.toByteArray()))) {
        	request = TransactionRequest.from(ois);
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

        validatorsAtLastBeginBlock = node.getTendermintValidators().toArray(TendermintValidator[]::new);
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
    public synchronized void deliverTx(RequestDeliverTx tendermintRequest, StreamObserver<ResponseDeliverTx> responseObserver) {
    	ByteString tx = tendermintRequest.getTx();
        ResponseDeliverTx.Builder responseBuilder = ResponseDeliverTx.newBuilder();

        TransactionRequest<?> request = null;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(tx.toByteArray()))) {
        	request = TransactionRequest.from(ois);
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
    	types.Types.ResponseEndBlock.Builder builder = ResponseEndBlock.newBuilder();

    	if (validatorsAtLastBeginBlock != null) {
    		try {
    			TendermintValidator[] currentValidators = validatorsAtLastBeginBlock;
    			Optional<TendermintValidator[]> validatorsInStore = node.getTendermintValidatorsInStore();
    			if (validatorsInStore.isPresent()) {
    				TendermintValidator[] nextValidators = validatorsInStore.get();
    				removeCurrentValidatorsThatAreNotNextValidators(currentValidators, nextValidators, builder);
    				addNextValidatorsThatAreNotCurrentValidators(currentValidators, nextValidators, builder);
    				updateValidatorsThatChangedPower(currentValidators, nextValidators, builder);
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

	private static void updateValidatorsThatChangedPower(TendermintValidator[] currentValidators, TendermintValidator[] nextValidators, types.Types.ResponseEndBlock.Builder builder) {
		Stream.of(nextValidators)
			.filter(validator -> isContainedWithDistinctPower(validator.address, validator.power, currentValidators))
			.forEachOrdered(validator -> updateValidator(validator, builder));
	}

	private static void addNextValidatorsThatAreNotCurrentValidators(TendermintValidator[] currentValidators, TendermintValidator[] nextValidators, types.Types.ResponseEndBlock.Builder builder) {
		Stream.of(nextValidators)
			.filter(validator -> !isContained(validator.address, currentValidators))
			.forEachOrdered(validator -> addValidator(validator, builder));
	}

	private static void removeCurrentValidatorsThatAreNotNextValidators(TendermintValidator[] currentValidators, TendermintValidator[] nextValidators, types.Types.ResponseEndBlock.Builder builder) {
		Stream.of(currentValidators)
			.filter(validator -> !isContained(validator.address, nextValidators))
			.forEachOrdered(validator -> removeValidator(validator, builder));
	}

    private static void removeValidator(TendermintValidator tv, types.Types.ResponseEndBlock.Builder builder) {
    	builder.addValidatorUpdates(intoValidatorUpdate(tv, 0L));
    	logger.info("removed Tendermint validator with address " + tv.address + " and power " + tv.power);
    }

    private static void addValidator(TendermintValidator tv, types.Types.ResponseEndBlock.Builder builder) {
    	builder.addValidatorUpdates(intoValidatorUpdate(tv, tv.power));
    	logger.info("added Tendermint validator with address " + tv.address + " and power " + tv.power);
    }

    private static void updateValidator(TendermintValidator tv, types.Types.ResponseEndBlock.Builder builder) {
    	builder.addValidatorUpdates(intoValidatorUpdate(tv, tv.power));
    	logger.info("updated Tendermint validator with address " + tv.address + " by setting its new power to " + tv.power);
    }

    private static ValidatorUpdate intoValidatorUpdate(TendermintValidator validator, long newPower) {
    	byte[] raw = Base64.getDecoder().decode(validator.publicKey);
    	PubKey publicKey = PubKey.newBuilder().setData(ByteString.copyFrom(raw)).setType("ed25519").build();

    	return ValidatorUpdate.newBuilder()
    		.setPubKey(publicKey)
    		.setPower(newPower)
    		.build();
    }

    private static boolean isContained(String address, TendermintValidator[] validators) {
    	return Stream.of(validators).map(validator -> validator.address).anyMatch(address::equals);
    }

    private static boolean isContainedWithDistinctPower(String address, long power, TendermintValidator[] validators) {
    	return Stream.of(validators).anyMatch(validator -> validator.address.equals(address) && validator.power != power);
    }

    @Override
    public void commit(RequestCommit req, StreamObserver<ResponseCommit> responseObserver) {
    	Store store = node.getStore();
    	store.commitTransactionAndCheckout();
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
    	String message = t.getMessage();
		int length = message.length();
		if (length > node.config.maxErrorLength)
			message = message.substring(0, node.config.maxErrorLength) + "...";

		return ByteString.copyFromUtf8(message);
    }
}