package takamaka.tests.errors.legalcall2;

import java.util.ArrayList;
import java.util.List;

public class C {
	private String s = "";

	public void test() {
		List<String> list = new ArrayList<>();
		list.add("hello");
		list.add("how");
		list.add("are");
		list.add("you");
		list.add("?");
		
		list.stream()
			.map(String::length)
			.forEachOrdered(this::process);
	}

	private void process(int length) {
		s += length;
	}

	@Override
	public String toString() {
		return s;
	}
}