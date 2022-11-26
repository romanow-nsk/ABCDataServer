package romanow.abc.dataserver.operations;

import romanow.abc.core.ErrorList;
import romanow.abc.dataserver.APIAdmin;
import romanow.abc.dataserver.DataServer;

public class DBOBackLinks extends DBOProcArtifacts {
        @Override
        public void execute(DataServer db, APIAdmin apiAdmin, ErrorList errors) {
            execute(db,apiAdmin,errors,false,false);
        }
    }

