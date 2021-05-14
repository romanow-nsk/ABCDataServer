package romanow.abc.dataserver;

import retrofit2.Call;
import retrofit2.Response;
import romanow.abc.core.UniException;
import romanow.abc.core.Utils;
import romanow.abc.core.constants.ValuesBase;

import java.io.IOException;

public abstract class APICallC<T> {
    public abstract Call<T> apiFun();
    public APICallC(){
        }
    public T call()throws UniException {
        String mes="";
        Response<T> res;
        long tt;
        try {
            tt = System.currentTimeMillis();
            res = apiFun().execute();
            } catch (Exception ex) {
                throw UniException.bug(ex);
                }
        if (!res.isSuccessful()){
            if (res.code()== ValuesBase.HTTPAuthorization){
                mes =  "Сеанс закрыт " + Utils.httpError(res);
                throw UniException.io(mes);
                }
            try {
                mes = "Ошибка " + res.message() + " (" + res.code() + ")\n" + res.errorBody().string();
                }   catch (IOException ex){ mes += "Ошибка: "+ex.toString(); }
            throw UniException.io(mes);
            }
        return res.body();
    }
}
