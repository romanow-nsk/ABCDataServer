package romanow.abc.dataserver;

import romanow.abc.core.ServerState;

public interface I_ServerState {
    public void onStateChanged(ServerState serverState);
    }
