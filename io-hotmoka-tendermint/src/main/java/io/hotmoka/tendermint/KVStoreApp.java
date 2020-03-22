package io.hotmoka.tendermint;

import com.google.protobuf.ByteString;

import io.grpc.stub.StreamObserver;
import io.hotmoka.tendermint.Automaton.State;
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
    private final Automaton automaton;
    private int counter = 0;
    
    KVStoreApp(Environment env) {
        this.env = env;
        automaton = new Automaton();
    }

    @Override
    public void echo(RequestEcho req, StreamObserver<ResponseEcho> responseObserver) {
    	System.out.println("echo start");
        ResponseEcho resp = ResponseEcho.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        System.out.println("echo end");
    }

    @Override
    public void info(RequestInfo req, StreamObserver<ResponseInfo> responseObserver) {
    	System.out.println("info start");
        ResponseInfo resp = ResponseInfo.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        System.out.println("info end");
    }

    @Override
    public void setOption(RequestSetOption req, StreamObserver<ResponseSetOption> responseObserver) {
    	System.out.println("setOption start");
        ResponseSetOption resp = ResponseSetOption.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        System.out.println("setOption end");
    }

    @Override
    public void checkTx(RequestCheckTx req, StreamObserver<ResponseCheckTx> responseObserver) {
    	System.out.println("checkTx start");
        ByteString tx = req.getTx();
        int code = validate(tx);
        ResponseCheckTx resp = ResponseCheckTx.newBuilder()
                .setCode(code)
                .setGasWanted(1)
                .build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        System.out.println("checkTx end");
    }

    @Override
    public void initChain(RequestInitChain req, StreamObserver<ResponseInitChain> responseObserver) {
    	System.out.println("initChain start");
        ResponseInitChain resp = ResponseInitChain.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        System.out.println("initChain end");
    }

    @Override
    public void beginBlock(RequestBeginBlock req, StreamObserver<ResponseBeginBlock> responseObserver) {
    	System.out.println("beginBlock start");
        txn = env.beginTransaction();
        store = env.openStore("store", StoreConfig.WITHOUT_DUPLICATES, txn);
        ResponseBeginBlock resp = ResponseBeginBlock.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        System.out.println("beginBlock end");
    }

    @Override
    public void deliverTx(RequestDeliverTx req, StreamObserver<ResponseDeliverTx> responseObserver) {
    	System.out.println("deliverTx start");
        ByteString tx = req.getTx();
        int code = validate(tx);

        if (code == 0) {
        	char c = tx.toStringUtf8().charAt(0);
        	State preState = automaton.getCurrentState();
        	automaton.process(c);
        	State postState = automaton.getCurrentState();
        	String infoToStore = "Input: '" + c + "', PreState: " + preState + ", PostState: " + postState;
        	ArrayByteIterable value = new ArrayByteIterable(infoToStore.getBytes());
        	ArrayByteIterable key = new ArrayByteIterable((counter+"").getBytes());
            store.put(txn, key , value);
            counter++;
        }

        ResponseDeliverTx resp = ResponseDeliverTx.newBuilder()
                .setCode(code)
                .build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        System.out.println("deliverTx end");
    }

    @Override
    public void endBlock(RequestEndBlock req, StreamObserver<ResponseEndBlock> responseObserver) {
    	System.out.println("endBlock start");
        ResponseEndBlock resp = ResponseEndBlock.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        System.out.println("endBlock end");
    }

    @Override
    public void commit(RequestCommit req, StreamObserver<ResponseCommit> responseObserver) {
    	System.out.println("commit start");
        txn.commit();
        ResponseCommit resp = ResponseCommit.newBuilder()
                .setData(ByteString.copyFrom(new byte[8]))
                .build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
        System.out.println("commit end");
    }

    @Override
    public void query(RequestQuery req, StreamObserver<ResponseQuery> responseObserver) {
    	System.out.println("query start");
        byte[] k = req.getData().toByteArray();
        byte[] v = getPersistedValue(k);
        Builder builder = ResponseQuery.newBuilder();
        if (v == null) {
            builder.setLog("does not exist");
        }
        else {
            builder.setLog("exists");
            builder.setKey(ByteString.copyFrom(k));
            builder.setValue(ByteString.copyFrom(v));
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
        System.out.println("query end");
    }

    @Override
    public void flush(RequestFlush request, StreamObserver<ResponseFlush> responseObserver) {
    	System.out.println("flush start");
    	ResponseFlush resp = ResponseFlush.newBuilder().build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    	System.out.println("flush end");
    }

    private int validate(ByteString tx) {
    	// check if tx is a single character and if c == [A-Z]
    	return tx.toStringUtf8().length() == 1 && tx.toStringUtf8().charAt(0) >= 'A' && tx.toStringUtf8().charAt(0) <= 'Z' ? 0 : -1 ;
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
