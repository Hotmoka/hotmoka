package io.hotmoka.tools.internal.cli;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;

import org.apache.commons.cli.CommandLine;

import io.hotmoka.beans.values.StorageReference;

public abstract class AbstractCommand implements Runnable {
	protected final static BigInteger _10_000 = BigInteger.valueOf(10_000L);

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

	protected BigInteger getBigIntegerOption(CommandLine line, String name, BigInteger _default) {
		if (line.hasOption(name))
			return new BigInteger(line.getOptionValue(name));
		else
			return _default;
	}

	protected String getStringOption(CommandLine line, String name, String _default) {
		if (line.hasOption(name))
			return line.getOptionValue(name);
		else
			return _default;
	}
}