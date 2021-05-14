package romanow.abc.dataserver;

import romanow.abc.core.UniException;
import romanow.abc.core.entity.FileSource;


import java.util.ArrayList;
import java.util.HashMap;

public interface I_FileSource {
    public void start(DataServer db, I_FileModuleDir module, HashMap<String, String> params) throws UniException;
    public void stop();
    public void tryToMake() throws UniException;
    public FileSource getNext() throws UniException;
    public void deleteFile(String fname);
    public String getName();
    public String getPath();
}
