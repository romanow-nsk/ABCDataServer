package romanow.abc.dataserver;

import romanow.abc.core.constants.ValuesBase;
import spark.Request;
import spark.Response;

import java.io.IOException;

public class ParamBoolean {
    private boolean valid=false;
    private boolean value=false;
    public boolean getValue(){ return  value; }
    public boolean isValid(){ return valid; }
    public ParamBoolean(Request req, Response res, String name) throws IOException {
        String ss = req.raw().getParameter(name);
        try{
            if (ss==null){
                DataServer.funCreateHTTPError(res, ValuesBase.HTTPRequestError, "Отсутствует параметр "+name);
                return;
                }
            value  = Boolean.parseBoolean(ss);
            valid = true;
            } catch(Exception ee){
                DataServer.funCreateHTTPError(res,ValuesBase.HTTPRequestError, "Недопустимое значение параметра "+name+":"+ss);
        }
    }
    public ParamBoolean(Request req, Response res, String name, boolean defValue) throws IOException {
        try{
            String ss = req.raw().getParameter(name);
            if (ss==null){
                value = defValue;
                valid = true;
                return;
                }
            value  = Boolean.parseBoolean(ss);
            valid = true;
            } catch(Exception ee){
                DataServer.funCreateHTTPError(res,ValuesBase.HTTPRequestError, "Недопустимое значение параметра  "+name);
                }
        }
}
