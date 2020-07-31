package io.hotmoka.network.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.network.RemoteNode;
import io.hotmoka.network.RemoteNodeConfig;
import io.hotmoka.network.internal.services.NetworkExceptionResponse;
import io.hotmoka.network.internal.services.RestClientService;
import io.hotmoka.network.models.updates.ClassTagModel;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;

/**
 * The implementation of a node that forwards all its calls to a remote service.
 */
public class RemoteNodeImpl implements RemoteNode {

	/**
	 * The configuration of the node.
	 */
	private final RemoteNodeConfig config;

	/**
	 * A parser to and from JSON.
	 */
	private final Gson gson = new Gson();



	public RemoteNodeImpl(RemoteNodeConfig config) {
		this.config = config;
	}

	@Override
	public void close() {}

	@Override
	public TransactionReference getTakamakaCode() throws NoSuchElementException {

		try {
			return RestClientService.get(config.url + "/get/takamakaCode", TransactionReferenceModel.class).toBean();
		}
		catch (NetworkExceptionResponse exceptionResponse) {

			if (exceptionResponse.getExceptionType().equals(NoSuchElementException.class.getName())) {
				throw new NoSuchElementException(exceptionResponse.getMessage());
			}

			throw new InternalFailureException(exceptionResponse.getMessage());
		}
	}

	@Override
	public StorageReference getManifest() throws NoSuchElementException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ClassTag getClassTag(StorageReference reference) throws NoSuchElementException {


		try {
			return gson.fromJson(get(config.url + "/get/classTag", gson.toJson(new StorageReferenceModel(reference))), ClassTagModel.class).toBean(reference);
		}
		catch (IOException e) {
			// TODO
			// questo non e' corretto: bisogna capire quando il fallimento e' dovuto
			// al fatto che il metodo remoto ha lanciato una NoSuchElementException
			// e in tal caso lanciare anche qui una NoSuchElementException con lo stesso messaggio
			throw InternalFailureException.of(e);
		}
	}

	@Override
	public Stream<Update> getState(StorageReference reference) throws NoSuchElementException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SignatureAlgorithm<NonInitialTransactionRequest<?>> getSignatureAlgorithmForRequests() throws NoSuchAlgorithmException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TransactionRequest<?> getRequestAt(TransactionReference reference) throws NoSuchElementException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TransactionResponse getResponseAt(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TransactionResponse getPolledResponseAt(TransactionReference reference) throws TransactionRejectedException, TimeoutException, InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StorageReference addRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequest request) throws TransactionRejectedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException {
		// TODO Auto-generated method stub

	}

	@Override
	public TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StorageValue addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StorageValue runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StorageValue runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JarSupplier postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CodeSupplier<StorageReference> postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CodeSupplier<StorageValue> postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CodeSupplier<StorageValue> postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException {
		// TODO Auto-generated method stub
		return null;
	}

	private static String post(String url, String json) throws IOException {
		URL urlObj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json; utf-8");
		con.setRequestProperty("Accept", "application/json");
		con.setDoOutput(true);

		try(OutputStream os = con.getOutputStream()) {
			byte[] input = json.getBytes("utf-8");
			os.write(input, 0, input.length);
		}

		try(BufferedReader br = new BufferedReader(new InputStreamReader(con.getResponseCode() > 299 ? con.getErrorStream() : con.getInputStream(), "utf-8"))) {
			return br.lines().collect(Collectors.joining("\n"));
		}
	}

	private static String get(String url, String json) throws IOException {
		URL urlObj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
		con.setRequestMethod("GET");

		if (!json.isEmpty()) {
			con.setRequestProperty("Content-Type", "application/json; utf-8");
			con.setRequestProperty("Accept", "application/json");
			con.setDoOutput(true);
			try(OutputStream os = con.getOutputStream()) {
				byte[] input = json.getBytes("utf-8");
				os.write(input, 0, input.length);
			}
		}

		try(BufferedReader br = new BufferedReader(new InputStreamReader(con.getResponseCode() > 299 ? con.getErrorStream() : con.getInputStream(), "utf-8"))) {
			return br.lines().collect(Collectors.joining("\n"));
		}
	}
}