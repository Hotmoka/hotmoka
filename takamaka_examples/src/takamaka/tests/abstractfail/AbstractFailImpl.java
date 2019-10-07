package takamaka.tests.abstractfail;

public class AbstractFailImpl extends AbstractFail {
	
	private int n;
	
	public AbstractFailImpl(int n){
		this.n = n;
	}
	
	public int getN(){
		return n;
	}

	@Override
	public AbstractFail method() {
		return new AbstractFailImpl(5);
	}
	
}
