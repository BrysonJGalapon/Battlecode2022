package kolohe.state.machine;

public interface State {
    State react(Stimulus stimulus);
}
