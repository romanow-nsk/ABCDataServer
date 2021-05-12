package romanow.abc.dataserver;

import romanow.abc.core.constants.ValuesBase;
import spark.Request;
import spark.Response;

import java.io.IOException;

public class ParamLong {
    private boolean valid=false;
    private long value=0;
    public long getValue(){ return  value; }
    public boolean isValid(){ return valid; }
    public ParamLong(Request req, Response res, String name) throws IOException {
        String ss = req.raw().getParameter(name);
        try{
            if (ss==null){
                DataServer.funCreateHTTPError(res, ValuesBase.HTTPRequestError, "Отсутствует параметр "+name);
                return;
                }
            value  = Long.parseLong(ss);
            valid = true;
            } catch(Exception ee){
                DataServer.funCreateHTTPError(res,ValuesBase.HTTPRequestError, "Недопустимое значение параметра "+name+":"+ss);
        }
    }
    public ParamLong(Request req, Response res, String name, long defValue) throws IOException {
        try{
            String ss = req.raw().getParameter(name);
            if (ss==null){
                value = defValue;
                valid = true;
                return;
                }
            value  = Long.parseLong(ss);
            valid = true;
            } catch(Exception ee){
                DataServer.funCreateHTTPError(res,ValuesBase.HTTPRequestError, "Недопустимое значение параметра "+name);
            }
        }
}
