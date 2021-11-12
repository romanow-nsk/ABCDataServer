package romanow.abc.dataserver;

import romanow.abc.core.constants.ValuesBase;
import romanow.abc.core.dll.JarClassLoader;

public class ServerJarClassLoader extends JarClassLoader {
    public ServerJarClassLoader(boolean root,DataServer db, String moduleName) {
        super((root ? db.rootServerFileDir():db.dataServerFileDir())+"/"+moduleName+".jar");
        }
    public ServerJarClassLoader(boolean root, DataServer db) {
        super((root ? db.rootServerFileDir():db.dataServerFileDir())
                +"/"+ ValuesBase.env().applicationName(ValuesBase.AppNameDBName)+"DLL.jar");
        }
}
