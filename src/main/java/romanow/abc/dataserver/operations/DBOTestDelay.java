package romanow.abc.dataserver.operations;

import romanow.abc.core.ErrorList;
import romanow.abc.dataserver.APIAdmin;
import romanow.abc.dataserver.DataServer;

public class DBOTestDelay implements I_SystemOperations{
    @Override
    public void execute(DataServer db, APIAdmin apiAdmin, ErrorList errors) {
        try {
            Thread.sleep(1000 * 60);
            } catch (Exception ee){}
    }
}
