package romanow.abc.dataserver;

import romanow.abc.core.constants.ValuesBase;
import spark.Request;
import spark.Response;

import java.io.IOException;

public class ParamString {
    private boolean valid=false;
    private String value="";
    public String getValue(){ return  value; }
    public boolean isValid(){ return valid; }
    public ParamString(Request req, Response res, String name) throws IOException {
        String ss = req.raw().getParameter(name);
        try{
            if (ss==null){
                DataServer.funCreateHTTPError(res, ValuesBase.HTTPRequestError, "Отсутствует параметр "+name);
                return;
                }
            value  = ss;
            valid = true;
            } catch(Exception ee){
                DataServer.funCreateHTTPError(res,ValuesBase.HTTPRequestError, "Недопустимое значение параметра "+name+":"+ss);
                }
        }
    public ParamString(Request req, Response res, String name, String defValue) throws IOException {
        try{
            String ss = req.raw().getParameter(name);
            if (ss==null){
                value = defValue;
                valid = true;
                return;
                }
            value  = ss;
            valid = true;
            } catch(Exception ee){
                DataServer.funCreateHTTPError(res,ValuesBase.HTTPRequestError, "Недопустимое значение параметра "+name);
            }
    }

}
