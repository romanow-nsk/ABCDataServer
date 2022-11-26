package romanow.abc.dataserver.operations;

import romanow.abc.core.ErrorList;
import romanow.abc.dataserver.APIAdmin;
import romanow.abc.dataserver.DataServer;

public interface I_SystemOperations {
    public void execute(DataServer db, APIAdmin apiAdmin, ErrorList errors);
}
