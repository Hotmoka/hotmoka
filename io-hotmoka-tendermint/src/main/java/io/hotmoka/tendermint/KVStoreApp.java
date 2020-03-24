package io.hotmoka.tendermint;

import com.google.protobuf.ByteString;

import io.grpc.BindableService;
import io.grpc.stub.StreamObserver;
import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import jetbrains.exodus.env.Transaction;
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

class KVStoreApp extends ABCIApplicationGrpc.ABCIApplicationImplBase {
    private final Environment env;
    private Transaction txn = null;
    private Store store = null;
    
    KVStoreApp(Environment env) {
        this.env = env;
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
        txn = env.beginTransaction();
        store = env.openStore("store", StoreConfig.WITHOUT_DUPLICATES, txn);
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
        	String s = tx.toStringUtf8();
        	int separator = s.indexOf('=');
        	// the transaction is validated, hence there is an =, not at the beginning, followed by a number
        	String name = s.substring(0, separator);
        	String value = s.substring(separator + 1);
        	store.put(txn, new ArrayByteIterable(name.getBytes()), new ArrayByteIterable(value.getBytes()));
        }

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

    private static int validate(ByteString tx) {
    	// check if tx is a single character in [A-Z]
    	String s = tx.toStringUtf8();
    	int separator = s.indexOf('=');
    	if (separator <= 0) // it must contain but not start with =
    		return 1;
    	else { // there is an =, hence after it there must be an integer
    		String value = s.substring(separator + 1);
    		try {
    			Integer.parseInt(value);
    			return 0;
    		}
    		catch (NumberFormatException e) {
    			return 2; // there is no integer after =
    		}
    	}
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