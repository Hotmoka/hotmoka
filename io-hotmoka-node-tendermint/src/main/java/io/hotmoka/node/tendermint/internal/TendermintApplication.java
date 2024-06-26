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

package io.hotmoka.node.tendermint.internal;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.protobuf.ByteString;

import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.crypto.Hex;
import io.hotmoka.node.NodeUnmarshallingContexts;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.tendermint.abci.ABCI;
import tendermint.abci.Types.Evidence;
import tendermint.abci.Types.RequestBeginBlock;
import tendermint.abci.Types.RequestCheckTx;
import tendermint.abci.Types.RequestCommit;
import tendermint.abci.Types.RequestDeliverTx;
import tendermint.abci.Types.RequestEcho;
import tendermint.abci.Types.RequestEndBlock;
import tendermint.abci.Types.RequestFlush;
import tendermint.abci.Types.RequestInfo;
import tendermint.abci.Types.RequestInitChain;
import tendermint.abci.Types.RequestQuery;
import tendermint.abci.Types.ResponseBeginBlock;
import tendermint.abci.Types.ResponseCheckTx;
import tendermint.abci.Types.ResponseCommit;
import tendermint.abci.Types.ResponseDeliverTx;
import tendermint.abci.Types.ResponseEcho;
import tendermint.abci.Types.ResponseEndBlock;
import tendermint.abci.Types.ResponseFlush;
import tendermint.abci.Types.ResponseInfo;
import tendermint.abci.Types.ResponseInitChain;
import tendermint.abci.Types.ResponseQuery;
import tendermint.abci.Types.Validator;
import tendermint.abci.Types.ValidatorUpdate;
import tendermint.abci.Types.VoteInfo;
import tendermint.crypto.Keys.PublicKey;

/**
 * The Tendermint interface that links a Hotmoka Tendermint node to a Tendermint process.
 * It implements a set of handlers that Tendermint calls to notify events.
 */
class TendermintApplication extends ABCI {

	private final static Logger LOGGER = Logger.getLogger(TendermintApplication.class.getName());

	/**
	 * The Tendermint validators at the time of the last {@link #beginBlock(RequestBeginBlock, StreamObserver)}
	 * that has been executed.
	 */
	private volatile TendermintValidator[] validatorsAtPreviousBlock;

	private volatile String behaving;

	private volatile String misbehaving;

	/**
	 * The current transaction, if any.
	 */
	private volatile TendermintStoreTransformation transaction;

	private final TendermintNodeImpl node;

	private final TendermintPoster poster;

	/**
     * Builds the Tendermint ABCI interface that executes Takamaka transactions.
     * 
     * @param node the node whose transactions are executed
     */
    TendermintApplication(TendermintNodeImpl node, TendermintPoster poster) {
    	this.node = node;
    	this.poster = poster;
    }

    private static String getAddressOfValidator(Validator validator) {
    	return Hex.toHexString(validator.getAddress().toByteArray()).toUpperCase();
    }

    private static long timeOfBlock(RequestBeginBlock request) {
    	var time = request.getHeader().getTime();
    	return time.getSeconds() * 1_000L + time.getNanos() / 1_000_000L;
    }

    private static String spaceSeparatedSequenceOfMisbehavingValidatorsAddresses(RequestBeginBlock request) {
		return request.getByzantineValidatorsList().stream()
    		.map(Evidence::getValidator)
    		.map(TendermintApplication::getAddressOfValidator)
    		.collect(Collectors.joining(" "));
	}

	private static String spaceSeparatedSequenceOfBehavingValidatorsAddresses(RequestBeginBlock request) {
		return request.getLastCommitInfo().getVotesList().stream()
    		.filter(VoteInfo::getSignedLastBlock)
    		.map(VoteInfo::getValidator)
    		.map(TendermintApplication::getAddressOfValidator)
    		.collect(Collectors.joining(" "));
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
		/*String current = Stream.of(currentValidators).map(validator -> validator.address).collect(Collectors.joining(",", "[", "]"));
		String next = Stream.of(nextValidators).map(validator -> validator.address).collect(Collectors.joining(",", "[", "]"));
		LOGGER.info("validators remove: " + current + " -> " + next);*/
		Stream.of(currentValidators)
			.filter(validator -> isNotContained(validator.address, nextValidators))
			.forEachOrdered(validator -> removeValidator(validator, builder));
	}

    private static void removeValidator(TendermintValidator tv, ResponseEndBlock.Builder builder) {
    	builder.addValidatorUpdates(intoValidatorUpdate(tv, 0L));
    	LOGGER.info("removed Tendermint validator with address " + tv.address + " and power " + tv.power);
    }

    private static void addValidator(TendermintValidator tv, ResponseEndBlock.Builder builder) {
    	builder.addValidatorUpdates(intoValidatorUpdate(tv, tv.power));
    	LOGGER.info("added Tendermint validator with address " + tv.address + " and power " + tv.power);
    }

    private static void updateValidator(TendermintValidator tv, ResponseEndBlock.Builder builder) {
    	builder.addValidatorUpdates(intoValidatorUpdate(tv, tv.power));
    	LOGGER.info("updated Tendermint validator with address " + tv.address + " by setting its new power to " + tv.power);
    }

    private static ValidatorUpdate intoValidatorUpdate(TendermintValidator validator, long newPower) {
    	byte[] raw;

    	try {
    		raw = Base64.fromBase64String(validator.publicKey);
    	}
    	catch (Base64ConversionException e) {
    		throw new RuntimeException(e); // TODO
    	}
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
	protected ResponseInitChain initChain(RequestInitChain request) {
		return ResponseInitChain.newBuilder().build();
	}

	@Override
	protected ResponseEcho echo(RequestEcho request) {
		return ResponseEcho.newBuilder().build();
	}

	@Override
	protected ResponseInfo info(RequestInfo request) throws NodeException {
		return ResponseInfo.newBuilder()
			.setLastBlockAppHash(ByteString.copyFrom(node.getTendermintHash())) // hash of the store used for consensus
			.setLastBlockHeight(node.getBlockHeight()).build();
	}

	@Override
	protected ResponseCheckTx checkTx(RequestCheckTx request) {
		var tx = request.getTx();
        ResponseCheckTx.Builder responseBuilder = ResponseCheckTx.newBuilder();

        try (var context = NodeUnmarshallingContexts.of(new ByteArrayInputStream(tx.toByteArray()))) {
        	var hotmokaRequest = TransactionRequests.from(context);

        	try {
        		node.checkTransaction(hotmokaRequest);
        	}
        	catch (TransactionRejectedException e) {
        		node.signalRejected(hotmokaRequest, e);
        		throw e;
        	}

        	responseBuilder.setCode(0);
        }
        catch (Throwable t) {
        	responseBuilder.setCode(t instanceof TransactionRejectedException ? 1 : 2);
        	responseBuilder.setData(ByteString.copyFromUtf8(t.getMessage()));
		}

        return responseBuilder.build();
	}

	@Override
	protected ResponseBeginBlock beginBlock(RequestBeginBlock request) {
		behaving = spaceSeparatedSequenceOfBehavingValidatorsAddresses(request);
    	misbehaving = spaceSeparatedSequenceOfMisbehavingValidatorsAddresses(request);

    	try {
    		transaction = node.beginTransaction(timeOfBlock(request));
    	}
    	catch (NodeException e) {
    		throw new RuntimeException(e); // TODO
    	}

    	// the ABCI might start too early, before the Tendermint process is up
        if (validatorsAtPreviousBlock == null)
        	validatorsAtPreviousBlock = poster.getTendermintValidators().toArray(TendermintValidator[]::new);

        return ResponseBeginBlock.newBuilder().build();
	}

	@Override
	protected ResponseDeliverTx deliverTx(RequestDeliverTx request) {
		var tx = request.getTx();
        ResponseDeliverTx.Builder responseBuilder = ResponseDeliverTx.newBuilder();

        try (var context = NodeUnmarshallingContexts.of(new ByteArrayInputStream(tx.toByteArray()))) {
        	var hotmokaRequest = TransactionRequests.from(context);

        	try {
        		transaction.deliverTransaction(hotmokaRequest);
        		responseBuilder.setCode(0);
        	}
        	catch (TransactionRejectedException e) {
        		node.signalRejected(hotmokaRequest, e);
        		responseBuilder.setCode(1);
            	responseBuilder.setData(ByteString.copyFromUtf8(e.getMessage()));
        	}
        }
        catch (Throwable t) {
        	responseBuilder.setCode(2);
        	responseBuilder.setData(ByteString.copyFromUtf8(t.getMessage()));
        }

        return responseBuilder.build();
	}

	@Override
	protected ResponseEndBlock endBlock(RequestEndBlock request) {
    	ResponseEndBlock.Builder builder = ResponseEndBlock.newBuilder();
    	TendermintValidator[] currentValidators = validatorsAtPreviousBlock;

    	if (currentValidators != null) {
    		Optional<TendermintValidator[]> validatorsInStore = transaction.getTendermintValidators();
    		if (validatorsInStore.isPresent()) {
    			TendermintValidator[] nextValidators = validatorsInStore.get();
    			if (nextValidators.length == 0)
    				LOGGER.info("refusing to remove all validators; please initialize the node with TendermintInitializedNode");
    			else {
    				removeCurrentValidatorsThatAreNotNextValidators(currentValidators, nextValidators, builder);
    				addNextValidatorsThatAreNotCurrentValidators(currentValidators, nextValidators, builder);
    				updateValidatorsThatChangedPower(currentValidators, nextValidators, builder);
    				validatorsAtPreviousBlock = nextValidators;
    			}
    		}
    	}

    	return builder.build();
	}

	@Override
	protected ResponseCommit commit(RequestCommit request) throws NodeException {
		try {
			transaction.deliverRewardTransaction(behaving, misbehaving);
		}
		catch (StoreException e) {
			throw new RuntimeException(e);
		}

		node.moveToFinalStoreOf(transaction);
		byte[] hash = node.getTendermintHash();
		LOGGER.info("committed Tendermint state " + Hex.toHexString(hash).toUpperCase());
		return ResponseCommit.newBuilder().setData(ByteString.copyFrom(hash)).build();
	}

	@Override
	protected ResponseQuery query(RequestQuery request) {
		return ResponseQuery.newBuilder().setLog("nop").build();
	}

	@Override
	protected ResponseFlush flush(RequestFlush request) {
		return ResponseFlush.newBuilder().build();
	}
}