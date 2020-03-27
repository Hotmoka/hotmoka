package io.hotmoka.tendermint.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;

import io.grpc.stub.StreamObserver;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.nodes.Node;
import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import jetbrains.exodus.env.Transaction;
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

class ABCI extends ABCIApplicationGrpc.ABCIApplicationImplBase implements AutoCloseable {
	private final Node node;
    private final Environment env;
    private Transaction txn;
    private Store store;

    /**
     * The transaction reference that can be used for the next transaction that will be delivered.
     */
    private TendermintTransactionReference next;

    /**
    * The current time of the blockchain, set at the time of creation of each block.
    */
    private long now;

    ABCI(Node node) {
    	this.node = node;
        this.env = Environments.newInstance("tmp/storage");
    }

    @Override
    public void close() {
    	env.close();
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
    	Timestamp time = req.getTime();
		long millis = time.getSeconds() * 1000L + time.getNanos() / 1000L;
        ResponseInitChain resp = ResponseInitChain.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        System.out.println("]");
    }

    @Override
    public void beginBlock(RequestBeginBlock req, StreamObserver<ResponseBeginBlock> responseObserver) {
    	//System.out.print("[beginBlock");
        txn = env.beginTransaction();
        store = env.openStore("store", StoreConfig.WITHOUT_DUPLICATES, txn);
        Header header = req.getHeader();
        next = new TendermintTransactionReference(BigInteger.valueOf(header.getHeight()), (short) 0);
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
            		System.out.println(response);
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
        	
            /*String s = tx.toStringUtf8();
        	int separator = s.indexOf('=');
        	// the transaction is validated, hence there is an =, not at the beginning, followed by a number
        	String name = s.substring(0, separator);
        	String value = s.substring(separator + 1);
        	store.put(txn, new ArrayByteIterable(name.getBytes()), new ArrayByteIterable(value.getBytes()));*/
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
        txn.commit();
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
        byte[] k = req.getData().toByteArray();
        req.getHeight();
        byte[] v = getPersistedValue(k);
        Builder builder = ResponseQuery.newBuilder();
        if (v == null)
            builder.setLog("does not exist");
        else {
            builder.setLog("exists");
            builder.setKey(ByteString.copyFrom(k));
            builder.setValue(ByteString.copyFrom(v));
        }
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

    private byte[] getPersistedValue(byte[] k) {
        return env.computeInReadonlyTransaction(txn -> {
            Store store = env.openStore("store", StoreConfig.WITHOUT_DUPLICATES, txn);
            ByteIterable byteIterable = store.get(txn, new ArrayByteIterable(k));
            if (byteIterable == null)
                return null;

            return byteIterable.getBytesUnsafe();
        });
    }
}