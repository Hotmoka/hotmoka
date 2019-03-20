package takamaka.lang;

public abstract class PayableContract extends Contract {
	public final @Entry @Payable void receive(int amount) {}
}