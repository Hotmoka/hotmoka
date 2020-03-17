package io.takamaka.tests.errors.loop3;

import java.util.ArrayList;
import java.util.List;

/**
 * An example of a method that might run into an infinite loop. This
 * must be rejected by the verification layer of Takamaka.
 */
public class Loop {

	public static void loop() {
		List<Object> l = new ArrayList<>();
		List<Object> ll = new ArrayList<>();
		ll.add(l);
		l.add(ll);
		l.hashCode(); // this diverges
	}
}