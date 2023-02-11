package romanow.abc.dataserver;

import lombok.Getter;
import romanow.abc.core.ErrorList;

public class CommandStringData {
    //---------------------------------- командная строка
    //  port:4569      - порт
    //  user:xxxx      - имя суперпользователя
    //  pass:xxxx      - пароль  суперпользователя
    //  conf:xxxx      - имя разворачиваемой конфигурации
    //  init           - инициализация БД
    @Getter private ErrorList errors=new ErrorList();
    @Getter private int port = 4567;
    @Getter private boolean init=false;
    @Getter private String user=null;
    @Getter private String pass=null;
    @Getter private String conf=null;
    @Getter private String importXLS=null;
    public boolean hasUser(){ return user!=null; }
    public boolean hasPass(){ return pass!=null; }
    public boolean hasConf(){ return conf!=null; }
    public boolean hasImport(){ return importXLS!=null; }
    public boolean isOther(String ss){ return false; }
    public CommandStringData(){}
    public void parse(String pars[]){
        for(String ss : pars){
            if (ss.startsWith("init:")){
                init = true;
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
            if (ss.startsWith("conf:")){
                conf = ss.substring(5).trim();
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
            else{
                if (!isOther(ss))
                    errors.addError("Недопустимый параметр: "+ss);
                }
        }
    }
}
