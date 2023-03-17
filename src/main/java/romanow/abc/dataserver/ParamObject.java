package romanow.abc.dataserver;

import com.google.gson.Gson;
import romanow.abc.core.constants.ValuesBase;
import romanow.abc.core.entity.Entity;
import spark.Request;
import spark.Response;

import java.io.IOException;

public class ParamObject {
    private boolean valid=false;
    private Object value=null;
    public Object getValue(){ return  value; }
    public boolean isValid(){ return valid; }
    public long getOid(){ return ((Entity)value).getOid(); }
    public ParamObject(Request req, Response res, Class clazz) throws IOException {
        try{
            Gson gson = new Gson();
            value = gson.fromJson(req.body(),clazz);
            valid = value!=null;
            } catch(Exception ee){
            DataServer.funCreateHTTPError(res, ValuesBase.HTTPRequestError, "Ошибка формата класса: "+clazz.getSimpleName());
                }
        }
    }