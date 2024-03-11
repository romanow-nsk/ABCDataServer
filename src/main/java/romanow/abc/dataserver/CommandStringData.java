package romanow.abc.dataserver;

import lombok.Getter;
import lombok.Setter;
import romanow.abc.core.ErrorList;

public class CommandStringData {
    //---------------------------------- командная строка
    //  port:4569      - порт
    //  user:xxxx      - имя суперпользователя
    //  pass:xxxx      - пароль  суперпользователя
    //  conf:xxxx      - имя разворачиваемой конфигурации
    //  dbase:xxxx     - тип СУБД
    //  consolelog:bbbb - разрешение вывода лога в консоль
    @Getter private ErrorList errors=new ErrorList();
    @Getter @Setter private int port = 4567;
    @Getter private String user=null;
    @Getter private String host=null;
    @Getter private String pass=null;
    @Getter private String network=null;
    @Getter private boolean consoleLogEnable=false;
    private boolean consoleLog=false;
    @Getter private int timeZone=0;
    @Getter @Setter private String dbase=null;
    @Getter private String importXLS=null;
    public boolean hasUser(){ return user!=null; }
    public boolean hasPass(){ return pass!=null; }
    public boolean hasDBase(){ return dbase!=null; }
    public boolean hasConsoleLog(){ return consoleLog; }
    public boolean hasImport(){ return importXLS!=null; }
    public boolean isOther(String ss){ return false; }
    public boolean hasNetwork(){ return network!=null; }
    public CommandStringData(int port0, String dBase0){
        port = port0;
        dbase = dBase0;
        }
    public CommandStringData(){}
    public void parse(String pars[]){
        for(String ss : pars){
            if (ss.startsWith("host:")){
                host = ss.substring(5).trim();
                }
            else
            if (ss.startsWith("user:")){
                user = ss.substring(5).trim();
                }
            else
            if (ss.startsWith("pass:")){
                pass = ss.substring(5).trim();
                }
            else
            if (ss.startsWith("port:")){
                try {
                    port = Integer.parseInt(ss.substring(5).trim());
                    } catch (Exception ee){
                        errors.addError("Недопустимое значение параметра: "+ss);
                        }
                }
            else
            if (ss.startsWith("import:")){
                importXLS = ss.substring(7).trim();
                }
            else
            if (ss.startsWith("dbase:")){
                dbase = ss.substring(6).trim();
                }
            else
            if (ss.startsWith("consolelog:")){
                try {
                    consoleLogEnable = Boolean.parseBoolean(ss.substring(11).trim());
                    consoleLog = true;
                    } catch (Exception ee){
                        errors.addError("Недопустимое значение параметра: "+ss);
                        }
                }
            else
            if (ss.startsWith("network:")){
                network = ss.substring(8).trim();
                }
            else
            if (ss.startsWith("timezone:")){
                try {
                    timeZone = Integer.parseInt(ss.substring(9).trim());
                    } catch (Exception ee){
                    errors.addError("Недопустимое значение параметра: "+ss);
                    }
                }
            else{
                if (!isOther(ss))
                    errors.addError("Недопустимый параметр: "+ss);
                }
        }
    }
}
