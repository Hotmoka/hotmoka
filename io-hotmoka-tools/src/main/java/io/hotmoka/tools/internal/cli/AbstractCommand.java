package io.hotmoka.tools.internal.cli;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;

import io.hotmoka.beans.values.StorageReference;

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
	}

	protected String fileFor(StorageReference account) {
		return account.toString() + ".keys";
	}
}