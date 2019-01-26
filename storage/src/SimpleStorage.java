import takamaka.lang.Contract;
import takamaka.lang.View;

public class SimpleStorage extends Contract {
	private int storedData;
	
	public void set(int x) {
		storedData = x;
	}

	public @View int get() {
		return storedData;
	}
}