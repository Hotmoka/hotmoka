package io.hotmoka.tendermint;

public final class Automaton {
	
	public enum State { S0, S1, S2, S3, S4, STOP }
	
	private State currentState = State.S0;

	public void process(char c) {
		if (c == 'H' && currentState == State.S0)
			currentState = State.S1;
		else if (c == 'E' && currentState == State.S1)
			currentState = State.S2;
		else if (c == 'L' && currentState == State.S2)
			currentState = State.S3;
		else if (c == 'L' && currentState == State.S3)
			currentState = State.S4;
		else if (c == 'O' && currentState == State.S4)
			currentState = State.STOP;
		else if (currentState == State.STOP)
			currentState = State.STOP;
		else
			currentState = State.S0;

		System.out.println("------> current state is now " + currentState);
	}
	
	public State getCurrentState() {
		return currentState;
	}
}