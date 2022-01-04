package kolohe.state.machine;

public class StateMachine<T extends State> {
    private T currState;

    private StateMachine(T initialState) {
        this.currState = initialState;
    }

    public static <T extends State> StateMachine<T> startingAt(T initialState) {
        return new StateMachine<>(initialState);
    }

    public T getCurrState() {
        return this.currState;
    }

    public void transition(Stimulus stimulus) {
        if (this.currState == null) {
            return;
        }

        this.currState = (T) this.currState.react(stimulus);
    }
}
