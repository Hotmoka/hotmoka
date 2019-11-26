package io.takamaka.tests.errors.illegalmethodname1;

import java.util.List;
import java.util.Set;

import io.takamaka.code.lang.Storage;

public class A extends Storage {
	public void extractUpdates(Set<String> s1, Set<String> s2, List<String> l) {};
}