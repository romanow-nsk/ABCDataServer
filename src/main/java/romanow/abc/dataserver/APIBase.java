package romanow.abc.dataserver;

import romanow.abc.core.ServerState;
import romanow.abc.core.UniException;
import romanow.abc.core.Utils;
import romanow.abc.core.constants.ValuesBase;
import romanow.abc.core.entity.base.BugMessage;
import romanow.abc.core.mongo.RequestStatistic;
import spark.Request;
import spark.Response;
import spark.Route;

public class APIBase<T extends DataServer> {
    protected T db;
    public APIBase(T db0){
        db = db0;
    }
    public void set(T db0) {
        db = db0;
    }
    protected abstract class RouteWrap implements Route {
        private boolean testToken=true;
        public RouteWrap(){}
        public RouteWrap(boolean testTokenMode){
            testToken = testTokenMode;
        }
        public abstract Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception;
        @Override
        public Object handle(Request req, Response res){
            if (db.traceMax())
                System.out.println("+++"+req.pathInfo());
            synchronized (db.serverLock){
            //db.serverLock.lock(-1);
            ServerState state = db.getServerState();
            try {
                long tt = db.canDo(req,res,testToken);
                if(tt==0) return res.body();
                state.incRequestNum();                    //++ операций
                RequestStatistic statistic = new RequestStatistic();
                statistic.startTime = tt;
                Object out = _handle(req,res, statistic);
                state.decRequestNum();
                //db.serverLock.unlock(-1);
                if (db.traceMax())
                    System.out.println("---"+req.pathInfo());
                return out==null ? res.body() : db.toJSON(out, req, statistic);
                } catch (Exception ee) {
                    state.decRequestNum();
                    String mes = Utils.createFatalMessage(ee) + "\n" + db.traceRequest(req);
                    System.out.println(mes);
                    db.sendBug("Сервер", mes);
                    try {
                        db.mongoDB.add(new BugMessage(mes));
                    } catch (UniException e) {}
                    db.createHTTPError(res, ValuesBase.HTTPException, mes);
                    //db.serverLock.unlock(-1);
                    if (db.traceMax())
                        System.out.println("---"+req.pathInfo());
                    }
                return res.body();
            }
        }
    }

}
