import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;

@Exported
public class C extends Storage {
    private final int x;

    public C() {
	x = 13;
    }

    public int get() {
	return x;
    }
}
