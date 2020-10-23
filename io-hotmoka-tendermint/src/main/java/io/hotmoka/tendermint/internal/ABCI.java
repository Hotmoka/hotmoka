package io.hotmoka.tendermint.internal;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;

import io.grpc.stub.StreamObserver;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.crypto.HashingAlgorithm;
import types.ABCIApplicationGrpc;
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
import types.Types.ValidatorUpdate;

/**
 * The Tendermint interface that links a Hotmoka Tendermint node to a Tendermint process.
 * It implements a set of handlers that Tendermint calls to notify events.
 */
class ABCI extends ABCIApplicationGrpc.ABCIApplicationImplBase {

	/**
	 * The Tendermint blockchain linked to Tendermint.
	 */
	private final TendermintBlockchainImpl node;

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
    		HashingAlgorithm<byte[]> hashing = HashingAlgorithm.sha256(bytes -> bytes);
    		//SignatureAlgorithm<NonInitialTransactionRequest<?>> signature = node.getSignatureAlgorithmForRequests();

    		int index = 0;
    		for (ValidatorUpdate validator: req.getValidatorsList()) {
    			//System.out.println("key type: " + v.getPubKey().getType());
    			//System.out.println("pubKey: " + new String(Base64.getEncoder().encode(v.getPubKey().getData().toByteArray())));
    			String address = bytesToHex(hashing.hash(validator.getPubKey().getData().toByteArray())).substring(0, 40);
    			long power = validator.getPower();
    			node.getStore().setOriginalValidator(index++, new TendermintValidator(address, power));
    		}

    		/*
    		KeyPair keyPair = signature.getKeyPair();
    		System.out.println("setting public key: " + new String(Base64.getEncoder().encode(keyPair.getPublic().getEncoded())));
    		PubKey publicKey = PubKey.newBuilder().setData(ByteString.copyFrom(keyPair.getPublic().getEncoded())).setType("ed25519").build();
    		publicKey = req.getValidatorsList().get(0).getPubKey();
    		ValidatorUpdate update = ValidatorUpdate.newBuilder().setPubKey(publicKey).setPower(1000L).build();
    		System.out.println(update);
    		*/
    		node.getStore().setChainId(req.getChainId());
    		ResponseInitChain resp = ResponseInitChain.newBuilder()
    		//		.addValidators(update)
    				.build();
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

        ResponseCheckTx resp = responseBuilder
                //.setGasWanted(1)
                .build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void beginBlock(RequestBeginBlock req, StreamObserver<ResponseBeginBlock> responseObserver) {
    	Timestamp time = req.getHeader().getTime();
    	// TODO
    	/*for (VoteInfo vote: req.getLastCommitInfo().getVotesList()) {
    		if (vote.getSignedLastBlock())
    			System.out.print("signed and validated by ");
    		else
    			System.out.print("validated by ");

    		System.out.print(bytesToHex(vote.getValidator().getAddress().toByteArray()));
    		System.out.println(" with power " + vote.getValidator().getPower());
    	}*/

    	// you can check who misbehaved at the previous block:
    	// req.getByzantineValidatorsList().get(0).getValidator();
    	// Evidence evidence = req.getByzantineValidatorsList().get(0);
    	node.getStore().beginTransaction(time.getSeconds() * 1_000L + time.getNanos() / 1_000_000L);
        ResponseBeginBlock resp = ResponseBeginBlock.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

	/**
	 * Translates an array of bytes into a hexadecimal string.
	 * 
	 * @param bytes the bytes
	 * @return the string
	 */
	private static String bytesToHex(byte[] bytes) {
	    byte[] hexChars = new byte[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	
	    return new String(hexChars, StandardCharsets.UTF_8);
	}

	/**
	 * The string of the hexadecimal digits.
	 */
	private final static String HEX_CHARS = "0123456789abcdef";

	/**
	 * The array of hexadecimal digits.
	 */
	private final static byte[] HEX_ARRAY = HEX_CHARS.getBytes();

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
        ResponseEndBlock resp = ResponseEndBlock.newBuilder()
        	// TODO
        	//.addValidatorUpdates(update(s))
        	.build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void commit(RequestCommit req, StreamObserver<ResponseCommit> responseObserver) {
    	Store store = node.getStore();
    	store.commitTransactionAndCheckout();
        ResponseCommit resp = ResponseCommit.newBuilder()
        		.setData(ByteString.copyFrom(store.getHash())) // hash of the store used for consensus
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