package io.hotmoka.tendermint.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.util.Base64;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;

import io.grpc.stub.StreamObserver;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import types.ABCIApplicationGrpc;
import types.Types.Header;
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

class ABCI extends ABCIApplicationGrpc.ABCIApplicationImplBase {
	private final TendermintBlockchainImpl node;

    /**
     * The transaction reference that can be used for the next transaction that will be delivered.
     */
    private TendermintTransactionReference next;

    /**
    * The current time of the blockchain, set at the time of creation of each block.
    */
    private long now;

    ABCI(TendermintBlockchainImpl node) {
    	this.node = node;
    }

    @Override
    public void echo(RequestEcho req, StreamObserver<ResponseEcho> responseObserver) {
    	System.out.print("[echo");
        ResponseEcho resp = ResponseEcho.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        System.out.println("]");
    }

    @Override
    public void info(RequestInfo req, StreamObserver<ResponseInfo> responseObserver) {
    	System.out.print("[info");
        ResponseInfo resp = ResponseInfo.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        System.out.println("]");
    }

    @Override
    public void setOption(RequestSetOption req, StreamObserver<ResponseSetOption> responseObserver) {
    	System.out.print("[setOption");
        ResponseSetOption resp = ResponseSetOption.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        System.out.println("]");
    }

    @Override
    public void checkTx(RequestCheckTx req, StreamObserver<ResponseCheckTx> responseObserver) {
    	System.out.print("[checkTx");
        ByteString tx = req.getTx();

        Object data;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(tx.toByteArray()))) {
        	data = ois.readObject();
        }
        catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        int code = validate(tx);
        ResponseCheckTx resp = ResponseCheckTx.newBuilder()
                .setCode(code)
                //.setGasWanted(1)
                .build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        System.out.println("]");
    }

    @Override
    public void initChain(RequestInitChain req, StreamObserver<ResponseInitChain> responseObserver) {
    	System.out.print("[initChain");
        ResponseInitChain resp = ResponseInitChain.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        System.out.println("]");
    }

    @Override
    public void beginBlock(RequestBeginBlock req, StreamObserver<ResponseBeginBlock> responseObserver) {
    	//System.out.print("[beginBlock");
    	node.state.beginTransaction();
        Header header = req.getHeader();
        next = new TendermintTransactionReference(BigInteger.valueOf(header.getHeight()), (short) 0);
    	Timestamp time = header.getTime();
    	now = time.getSeconds() * 1000L + time.getNanos() / 1000L;
        ResponseBeginBlock resp = ResponseBeginBlock.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        //System.out.print("]");
    }

    @Override
    public void deliverTx(RequestDeliverTx req, StreamObserver<ResponseDeliverTx> responseObserver) {
    	System.out.print("[deliverTx");
        ByteString tx = req.getTx();
        int code = validate(tx);

        if (code == 0) {
        	Object data;
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(tx.toByteArray()))) {
            	data = ois.readObject();
            	if (data instanceof JarStoreInitialTransactionRequest) {
            		JarStoreInitialTransactionRequest request = (JarStoreInitialTransactionRequest) data;
            		JarStoreInitialTransactionResponse response = io.takamaka.code.engine.Transaction.mkFor(request, next, node).getResponse();
            		node.state.keepJar(next, response);
            	}
            }
            catch (ClassNotFoundException e) {
            	code = 1;
    		}
            catch (IOException e) {
            	code = 1;
    		}
            catch (TransactionException e) {
            	e.printStackTrace();
				code = 2;
			}
        }

        next = new TendermintTransactionReference(next.blockNumber, next.transactionNumber + 1);

        ResponseDeliverTx resp = ResponseDeliverTx.newBuilder()
                .setCode(code)
                .build();

        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        System.out.println("]");
    }

    @Override
    public void endBlock(RequestEndBlock req, StreamObserver<ResponseEndBlock> responseObserver) {
    	//System.out.print("[endBlock");
        ResponseEndBlock resp = ResponseEndBlock.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        //System.out.print("]");
    }

    @Override
    public void commit(RequestCommit req, StreamObserver<ResponseCommit> responseObserver) {
    	//System.out.print("[commit");
    	node.state.commitTransaction();
        ResponseCommit resp = ResponseCommit.newBuilder()
                .setData(ByteString.copyFrom(new byte[8])) // hash of the Merkle root of the application state
                .build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        //System.out.print("]");
    }

    @Override
    public void query(RequestQuery req, StreamObserver<ResponseQuery> responseObserver) {
    	System.out.print("[query");
        /*byte[] k = req.getData().toByteArray();
        byte[] v = getPersistedValue(k);*/
        Builder builder = ResponseQuery.newBuilder();
        /*if (v == null)
            builder.setLog("does not exist");
        else {*/
            builder.setLog("exists");
            //builder.setKey(ByteString.copyFrom(k));
            //builder.setValue(ByteString.copyFrom(v));
        //}
        ResponseQuery resp = builder.build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        System.out.println("]");
    }

    @Override
    public void flush(RequestFlush request, StreamObserver<ResponseFlush> responseObserver) {
    	System.out.print("[flush");
    	ResponseFlush resp = ResponseFlush.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    	System.out.println("]");
    }

    /**
     * Yields the current time of the blockchain, set at the time of
     * creation of each block.
     * 
     * @return the current time
     */
    long getNow() {
		return now;
	}

	private static int validate(ByteString tx) {
    	return 0;
    }
}