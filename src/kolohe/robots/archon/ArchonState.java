package kolohe.robots.archon;

import kolohe.state.machine.State;
import kolohe.state.machine.Stimulus;

public enum ArchonState implements State {
    START,
    END,
    ;

    @Override
    public State react(Stimulus stimulus) {
        switch (this) {
            case START: return END;
            case END: return START;
        }

        return END;
    }
}
