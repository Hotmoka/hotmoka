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

package io.hotmoka.tendermint.internal;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bouncycastle.util.encoders.Hex;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.UnmarshallingContext;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.tendermint.TendermintValidator;
import io.hotmoka.tendermint_abci.ABCI;
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

	private final static Logger logger = Logger.getLogger(TendermintApplication.class.getName());

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
    TendermintApplication(TendermintBlockchainInternal node) {
    	this.node = node;
    }

    private static String getAddressOfValidator(Validator validator) {
    	return Hex.toHexString(validator.getAddress().toByteArray()).toUpperCase();
    }

    private static long timeNow(RequestBeginBlock request) {
    	Timestamp time = request.getHeader().getTime();
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
		logger.info("validators remove: " + current + " -> " + next);*/
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

	@Override
	protected ResponseInitChain initChain(RequestInitChain request) {
		return ResponseInitChain.newBuilder().build();
	}

	@Override
	protected ResponseEcho echo(RequestEcho request) {
		return ResponseEcho.newBuilder().build();
	}

	@Override
	protected ResponseInfo info(RequestInfo request) {
		return ResponseInfo.newBuilder()
	       .setLastBlockAppHash(ByteString.copyFrom(node.getStore().getHash())) // hash of the store used for consensus
	       .setLastBlockHeight(node.getStore().getNumberOfCommits()).build();
	}

	@Override
	protected ResponseCheckTx checkTx(RequestCheckTx request) {
		ByteString tx = request.getTx();
        ResponseCheckTx.Builder responseBuilder = ResponseCheckTx.newBuilder();

        try (UnmarshallingContext context = new UnmarshallingContext(new ByteArrayInputStream(tx.toByteArray()))) {
        	node.checkTransaction(TransactionRequest.from(context));
        	responseBuilder.setCode(0);
        }
        catch (Throwable t) {
        	responseBuilder.setCode(t instanceof TransactionRejectedException ? 1 : 2);
        	responseBuilder.setData(trimmedMessage(t));
		}

        return responseBuilder.build();
	}

	@Override
	protected ResponseBeginBlock beginBlock(RequestBeginBlock request) {
		String behaving = spaceSeparatedSequenceOfBehavingValidatorsAddresses(request);
    	String misbehaving = spaceSeparatedSequenceOfMisbehavingValidatorsAddresses(request);
    	long now = timeNow(request);

    	node.getStore().beginTransaction(now);
    	logger.info("validators reward: behaving: " + behaving + ", misbehaving: " + misbehaving);
    	node.rewardValidators(behaving, misbehaving);

    	// the ABCI might start too early, before the Tendermint process is up
        if (node.getPoster() != null)
        	validatorsAtLastBeginBlock = node.getPoster().getTendermintValidators().toArray(TendermintValidator[]::new);

        return ResponseBeginBlock.newBuilder().build();
	}

	@Override
	protected ResponseDeliverTx deliverTx(RequestDeliverTx request) {
		ByteString tx = request.getTx();
        ResponseDeliverTx.Builder responseBuilder = ResponseDeliverTx.newBuilder();

        try (UnmarshallingContext context = new UnmarshallingContext(new ByteArrayInputStream(tx.toByteArray()))) {
        	node.deliverTransaction(TransactionRequest.from(context));
        	responseBuilder.setCode(0);
        }
        catch (Throwable t) {
        	responseBuilder.setCode(t instanceof TransactionRejectedException ? 1 : 2);
        	responseBuilder.setData(trimmedMessage(t));
        }

        return responseBuilder.build();
	}

	@Override
	protected ResponseEndBlock endBlock(RequestEndBlock request) {
    	ResponseEndBlock.Builder builder = ResponseEndBlock.newBuilder();
    	TendermintValidator[] currentValidators = validatorsAtLastBeginBlock;

    	if (currentValidators != null) {
    		try {
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

    	return builder.build();
	}

	@Override
	protected ResponseCommit commit(RequestCommit request) {
		Store store = node.getStore();
    	node.commitTransactionAndCheckout();
    	// hash of the store, used for consensus
    	byte[] hash = store.getHash();
    	logger.info("Committed state with hash = " + Hex.toHexString(hash).toUpperCase());
    	return ResponseCommit.newBuilder()
       		.setData(ByteString.copyFrom(hash))
       		.build();
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