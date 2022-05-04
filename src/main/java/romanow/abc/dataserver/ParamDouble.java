package romanow.abc.dataserver;

import romanow.abc.core.constants.ValuesBase;
import spark.Request;
import spark.Response;

import java.io.IOException;

public class ParamDouble {
    private boolean valid=false;
    private double value=0;
    public double getValue(){ return  value; }
    public boolean isValid(){ return valid; }
    public ParamDouble(Request req, Response res, String name) throws IOException {
        String ss = req.raw().getParameter(name);
        try{
            if (ss==null){
                DataServer.funCreateHTTPError(res, ValuesBase.HTTPRequestError, "Отсутствует параметр "+name);
                return;
                }
            value  = Double.parseDouble(ss);
            valid = true;
            } catch(Exception ee){
                DataServer.funCreateHTTPError(res,ValuesBase.HTTPRequestError, "Недопустимое значение параметра "+name+":"+ss);
        }
    }
    public ParamDouble(Request req, Response res, String name, double defValue) throws IOException {
        try{
            String ss = req.raw().getParameter(name);
            if (ss==null){
                value = defValue;
                valid = true;
                return;
                }
            value  = Double.parseDouble(ss);
            valid = true;
            } catch(Exception ee){
                DataServer.funCreateHTTPError(res,ValuesBase.HTTPRequestError, "Недопустимое значение параметра "+name);
            }
        }
}
