package io.hotmoka.tools.internal.cli;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;

import io.hotmoka.beans.responses.NonInitialTransactionResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseFailed;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.remote.RemoteNodeConfig;

public abstract class AbstractCommand implements Runnable {
	protected static final BigInteger _10_000 = BigInteger.valueOf(10_000L);
	protected static final String ANSI_RESET = "\u001B[0m";
	protected static final String ANSI_BLACK = "\u001B[30m";
	protected static final String ANSI_RED = "\u001B[31m";
	protected static final String ANSI_GREEN = "\u001B[32m";
	protected static final String ANSI_YELLOW = "\u001B[33m";
	protected static final String ANSI_BLUE = "\u001B[34m";
	protected static final String ANSI_PURPLE = "\u001B[35m";
	protected static final String ANSI_CYAN = "\u001B[36m";
	protected static final String ANSI_WHITE = "\u001B[37m";

	@Override
	public final void run() {
		try {
			execute();
		}
		catch (Exception e) {
			throw new CommandException(e);
		}
	}

	protected abstract void execute() throws Exception;

	protected RemoteNodeConfig remoteNodeConfig(String url) {
		return new RemoteNodeConfig.Builder().setURL(url).build();
	}

	protected String dumpKeys(StorageReference account, KeyPair keys) throws FileNotFoundException, IOException {
		String fileName = fileFor(account);
	    
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
			oos.writeObject(keys);
		}

		return fileName;
	}

	protected KeyPair readKeys(StorageReference account) throws FileNotFoundException, IOException, ClassNotFoundException {
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileFor(account)))) {
			return (KeyPair) ois.readObject();
		}
		catch (FileNotFoundException e) {
			throw new CommandException("cannot find the keys of " + account);
		}
	}

	protected String fileFor(StorageReference account) {
		return account.toString() + ".keys";
	}

	protected void printCosts(TransactionResponse response) {
		if (response instanceof NonInitialTransactionResponse) {
			NonInitialTransactionResponse installResponse = (NonInitialTransactionResponse) response;
			BigInteger sum = BigInteger.ZERO;
			sum = sum.add(installResponse.gasConsumedForCPU);
			sum = sum.add(installResponse.gasConsumedForRAM);
			sum = sum.add(installResponse.gasConsumedForStorage);
			BigInteger gasConsumedForPenalty;

			if (installResponse instanceof TransactionResponseFailed) {
				gasConsumedForPenalty = ((TransactionResponseFailed) installResponse).gasConsumedForPenalty();
				System.out.println("gas consumed for penalty: " + gasConsumedForPenalty);
				sum = sum.add(gasConsumedForPenalty);
			}
			else
				gasConsumedForPenalty = BigInteger.ZERO;

			System.out.println(ANSI_CYAN + "total gas consumed: " + sum);
			System.out.println(ANSI_GREEN + "  for CPU: " + installResponse.gasConsumedForCPU);
			System.out.println("  for RAM: " + installResponse.gasConsumedForRAM);
			System.out.println("  for storage: " + installResponse.gasConsumedForStorage);
			System.out.println("  for penalty: " + gasConsumedForPenalty + ANSI_RESET);
		}
	}
}