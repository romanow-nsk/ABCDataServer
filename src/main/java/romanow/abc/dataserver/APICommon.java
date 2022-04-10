package romanow.abc.dataserver;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;

import romanow.abc.core.DBRequest;
import romanow.abc.core.ServerState;
import romanow.abc.core.UniException;
import romanow.abc.core.constants.ConstList;
import romanow.abc.core.constants.ValuesBase;
import romanow.abc.core.entity.*;
import romanow.abc.core.entity.artifacts.Artifact;
import romanow.abc.core.entity.base.BugMessage;
import romanow.abc.core.entity.base.HelpFile;
import romanow.abc.core.entity.base.StringList;
import romanow.abc.core.entity.base.WorkSettingsBase;
import romanow.abc.core.entity.baseentityes.*;
import romanow.abc.core.entity.users.User;
import romanow.abc.core.mongo.*;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class APICommon extends APIBase {
    private WorkSettingsBase workSettings = null;
    private ServerState serverState = null;     // Ленивый ServerState

    public boolean addUser(User tt) throws UniException {
        if (!db.mongoDB.isOpen())
            return false;
        db.mongoDB.add(tt);
        return true;
    }

    public APICommon(DataServer db0) {
        super(db0);
        //------------------------------------------------- Таблица API ------------------------------------------------
        // localhost:4567/user/login?phone=9139449081
        Spark.get("/api/entity/list", routeEntityList);
        Spark.get("/api/entity/list/last", routeEntityListLast);
        Spark.post("/api/entity/add", routeEntityAdd);
        Spark.get("/api/entity/get", routeEntityGet);
        Spark.post("/api/entity/update", routeEntityUpdate);
        Spark.post("/api/entity/update/field", routeEntityUpdateField);
        Spark.post("/api/entity/update/object/field", routeEntityUpdateField);
        Spark.post("/api/entity/remove", routeEntityRemove);
        Spark.get("/api/entity/number", routeEntityNumber);
        Spark.get("/api/entity/get/withpaths", routeEntityGet);
        Spark.get("/api/entity/list/withpaths", routeEntityList);
        Spark.get("/api/entity/list/query", routeEntityListByQuery);
        //----------------------------------------------------------------------------------------------------------
        spark.Spark.get("/api/debug/ping", apiPing);
        spark.Spark.get("/api/keepalive", apiKeepAlive);
        spark.Spark.post("/api/bug/add", apiSendBug);
        spark.Spark.get("/api/bug/list", apiBugList);
        spark.Spark.get("/api/bug/get", apiGetBug);
        spark.Spark.get("/api/version", apiVersion);
        spark.Spark.get("/api/serverstate", apiServerState);
        spark.Spark.get("/api/worksettings", apiWorkSettings);
        spark.Spark.post("/api/worksettings/update", apiWorkSettingsUpdate);
        spark.Spark.post("/api/entity/delete", apiDeleteById);
        spark.Spark.post("/api/entity/undelete", apiUndeleteById);
        spark.Spark.get("/api/debug/token", apiDebugToken);
        spark.Spark.get("/api/debug/consolelog", routeGetConsoleLog);
        spark.Spark.get("/api/const/all", apiConstAll);
        spark.Spark.get("/api/const/bygroups", apiConstByGroups);
        spark.Spark.get("/api/names/get", routeNamesByPattern);
        spark.Spark.get("/api/helpfile/list", apiHelpFileList);
        Spark.post("/api/entity/artifactlist/add", routeArtifactToList);
        Spark.post("/api/entity/artifactlist/remove", routeArtifactFromList);
        Spark.post("/api/entity/artifact/replace", routeArtifactReplace);
        spark.Spark.get("/api/worksettings/get/int", apiWorkSettingsGetInt);
        spark.Spark.get("/api/worksettings/get/string", apiWorkSettingsGetString);
        spark.Spark.get("/api/worksettings/get/boolean", apiWorkSettingsGetBoolean);
        spark.Spark.post("/api/worksettings/update/int", apiWorkSettingsUpdateInt);
        spark.Spark.post("/api/worksettings/update/string", apiWorkSettingsUpdateString);
        spark.Spark.post("/api/worksettings/update/boolean", apiWorkSettingsUpdateBoolean);
        }
    //----------------------------- образцы 4 операций --------------------------------------------
    RouteWrap routeEntityAdd = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamInt plevel = new ParamInt(req,res,"level",0);
            int level = plevel.isValid() ?  plevel.getValue() : 0;
            ParamBody qq = new ParamBody(req, res, DBRequest.class);
            if (!qq.isValid()) return null;
            DBRequest dbReq = (DBRequest)qq.getValue();
            long oid = db.mongoDB.add(dbReq.get(new Gson()),level);
            return new JLong(oid);
        }};
    RouteWrap routeEntityList = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamInt fl = new ParamInt(req,res,"mode", ValuesBase.GetAllModeActual);
            ParamInt plevel = new ParamInt(req,res,"level",0);
            ParamString cname = new ParamString(req,res,"classname","");
            String className = cname.isValid() ?  cname.getValue() : "";
            Class cc = ValuesBase.EntityFactory().getClassForSimpleName(className);
            if (cc==null){
                db.createHTTPError(res,ValuesBase.HTTPRequestError, "Недопустимый класс сущности "+className);
                return null;
                }
            ParamString paths = new ParamString(req,res,"paths","");
            String pathsList = paths.isValid() ?  paths.getValue() : "";
            EntityList<Entity> xx;
            xx = (EntityList<Entity>)db.mongoDB.getAll((Entity) cc.newInstance(),fl.getValue(),plevel.getValue(),pathsList,statistic);
            ArrayList<DBRequest> out = new ArrayList<>();
            Gson gson = new Gson();
            for(Entity ent : xx)
                out.add(new DBRequest(ent,gson));
            return out;
        }};
    RouteWrap routeEntityListByQuery = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamInt plevel = new ParamInt(req,res,"level",0);
            ParamString cname = new ParamString(req,res,"classname","");
            String className = cname.isValid() ?  cname.getValue() : "";
            Class cc = ValuesBase.EntityFactory().getClassForSimpleName(className);
            if (cc==null){
                db.createHTTPError(res,ValuesBase.HTTPRequestError, "Недопустимый класс сущности "+className);
                return null;
                }
            ParamString xmlquery = new ParamString(req,res,"xmlquery");
            if (!xmlquery.isValid())
                return null;
            DBXStream xStream = new DBXStream();
            DBQueryList query = (DBQueryList)xStream.fromXML(xmlquery.getValue());
            EntityList<Entity> xx;
            xx = (EntityList<Entity>)db.mongoDB.getAllByQuery((Entity) cc.newInstance(),query,plevel.getValue());
            ArrayList<DBRequest> out = new ArrayList<>();
            Gson gson = new Gson();
            for(Entity ent : xx)
                out.add(new DBRequest(ent,gson));
            return out;
        }};
    RouteWrap routeEntityListLast = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamInt number = new ParamInt(req,res,"number");
            if (!number.isValid())
                return null;
            ParamInt plevel = new ParamInt(req,res,"level",0);
            ParamString cname = new ParamString(req,res,"classname","");
            String className = cname.isValid() ?  cname.getValue() : "";
            Class cc = ValuesBase.EntityFactory().getClassForSimpleName(className);
            if (cc==null){
                db.createHTTPError(res,ValuesBase.HTTPRequestError, "Недопустимый класс сущности "+className);
                return null;
                }
            ParamString paths = new ParamString(req,res,"paths","");
            String pathsList = paths.isValid() ?  paths.getValue() : "";
            Entity entity = (Entity) cc.newInstance();
            long lastId = db.mongoDB.lastOid(entity);       // Номер первой свободной
            long firstId = lastId - number.getValue();
            if (lastId<0) lastId=1;
            EntityList<Entity> xx;
            List<BasicDBObject> obj = new ArrayList<BasicDBObject>();
            obj.add(new BasicDBObject(new BasicDBObject("oid", new BasicDBObject("$gte", firstId))));
            obj.add(new BasicDBObject("valid",true));
            BasicDBObject query = new BasicDBObject();
            query.put("$and", obj);
            xx = (EntityList<Entity>)db.mongoDB.getAllByQuery(entity,query,plevel.getValue(),pathsList,statistic);
            ArrayList<DBRequest> out = new ArrayList<>();
            Gson gson = new Gson();
            int size = xx.size();
            for(Entity ent : xx) {
                if (ent.getOid()==lastId)
                    continue;
                out.add(new DBRequest(ent, gson));
                }
            return out;
        }};
    RouteWrap routeEntityUpdate = new RouteWrap() {      // Нельзя менять ТИП КИ
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            if (db.users.isReadOnly(req,res)) return null;
            ParamInt plevel = new ParamInt(req,res,"level",0);
            int level = plevel.isValid() ?  plevel.getValue() : 0;
            ParamBody qq = new ParamBody(req, res, DBRequest.class);
            if (!qq.isValid()) return null;
            DBRequest dbReq = (DBRequest)qq.getValue();
            Entity ent = dbReq.get(new Gson());
            db.mongoDB.update(ent,level);
            return new JEmpty();
        }};
    RouteWrap routeEntityGet = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamLong id = new ParamLong(req,res,"id");
            if (!id.isValid()) return null;
            ParamInt plevel = new ParamInt(req,res,"level",0);
            int level = plevel.isValid() ?  plevel.getValue() : 0;
            ParamString cname = new ParamString(req,res,"classname","");
            String className = cname.isValid() ?  cname.getValue() : "";
            Class cc = ValuesBase.EntityFactory().getClassForSimpleName(className);
            if (cc==null){
                db.createHTTPError(res,ValuesBase.HTTPRequestError, "Недопустимый класс сущности "+className);
                return null;
                }
            ParamString paths = new ParamString(req,res,"paths","");
            String pathsList = paths.isValid() ?  paths.getValue() : "";
            Entity uu = (Entity) cc.newInstance();
            if (!db.mongoDB.getById(uu,id.getValue(),level,pathsList)){
                db.createHTTPError(res,ValuesBase.HTTPNotFound, className+" не найден id="+id.getValue());
                return null;
            }
            return new DBRequest(uu, new Gson());
            }
    };
    RouteWrap routeEntityRemove = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            if (db.users.isReadOnly(req,res)) return null;
            ParamLong id = new ParamLong(req,res,"id");
            if (!id.isValid()) return null;
            ParamString cname = new ParamString(req,res,"classname","");
            String className = cname.isValid() ?  cname.getValue() : "";
            Class cc = ValuesBase.EntityFactory().getClassForSimpleName(className);
            if (cc==null){
                db.createHTTPError(res,ValuesBase.HTTPRequestError, "Недопустимый класс сущности "+className);
                return null;
                }
            Entity uu = (Entity) cc.newInstance();
            boolean bb = db.mongoDB.delete(uu,id.getValue(),ValuesBase.DeleteMode);
            return new JBoolean(bb);
            }
        };
    public int getEntityNumber(String className) throws Exception {
        Class cc = ValuesBase.EntityFactory().getClassForSimpleName(className);
        if (cc==null)
            return -1;
        Entity uu = (Entity) cc.newInstance();
        BasicDBObject query = new BasicDBObject();
        query.put("valid", true);
        int num = db.mongoDB.getCountByQuery(uu,query);
        return num;
        }
    RouteWrap routeEntityNumber = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamString cname = new ParamString(req,res,"classname","");
            String className = cname.isValid() ?  cname.getValue() : "";
            int num = getEntityNumber(className);
            if (num==-1){
                db.createHTTPError(res,ValuesBase.HTTPRequestError, "Недопустимый класс сущности "+className);
                return null;
                }
            return new JInt(num);
            }
        };
    //------------------------------------------------------------------------------------- 639
    RouteWrap routeEntityUpdateField = new RouteWrap() {      // Нельзя менять ТИП КИ
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            if (db.users.isReadOnly(req,res)) return null;
            ParamString pname = new ParamString(req,res,"name");
            if (!pname.isValid())
                return null;
            ParamString pref = new ParamString(req,res,"prefix","");
            if (!pref.isValid())
                return null;
            ParamBody qq = new ParamBody(req, res, DBRequest.class);
            if (!qq.isValid()) return null;
            DBRequest dbReq = (DBRequest)qq.getValue();
            Entity ent = dbReq.get(new Gson());
            String prefix = pref.getValue();
            if (prefix.length()==0){                                                // Явный префикс отсутствует
                String key = ent.getClass().getSimpleName()+"."+pname.getValue();   // Найти в таблице префиксов
                String zz = ValuesBase.PrefixMap().get(key);
                if (zz!=null)
                    prefix = zz+"_";
            }
            if (!db.mongoDB.updateField(ent,pname.getValue(),prefix)){
                db.createHTTPError(res,ValuesBase.HTTPNotFound, "Не найден объект "+ent.getClass().getSimpleName()+"["+ent.getOid()+"]");
                return null;
            }
            return new JEmpty();
        }};
    //-----------------------------------------------------------------------------------------------------------------
    RouteWrap routeNamesByPattern = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamString entity = new ParamString(req, res, "entity");
            if (!entity.isValid()) return null;
            ParamString pattern = new ParamString(req, res, "pattern");
            if (!pattern.isValid()) return null;
            Class zz = ValuesBase.EntityFactory().get(entity.getValue());
            if (zz==null){
                db.createHTTPError(res,ValuesBase.HTTPRequestError, "Недопустимый класс сущности "+entity.getValue());
                return null;
                }
            Entity cc = (Entity) zz.newInstance();
            EntityList<EntityNamed> out = db.mongoDB.getListForPattern(cc,pattern.getValue());
            return out;
            }
        };
    RouteWrap apiPing = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            //db.createHTTPError(res,ValuesBase.HTTPRequestError, "Это ответ по-русски");
            return new JEmpty();
        }};
    RouteWrap apiWorkSettingsUpdate = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            if (db.users.isReadOnly(req,res)) return null;
            ParamBody qq = new ParamBody(req,res, DBRequest.class);
            if (!qq.isValid()) return null;
            synchronized (this){        // Синхронизация при обновлении объектов
                DBRequest dbReq = (DBRequest)qq.getValue();
                WorkSettingsBase ent = (WorkSettingsBase) dbReq.get(new Gson());
                db.mongoDB.update(ent);
                System.out.println("Обновлены рабочие настройки");
                workSettings = ent;
                }
            return new JEmpty();
        }};

    RouteWrap apiConstAll = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            return ValuesBase.constMap().getConstAll();
        }};
    RouteWrap apiConstByGroups = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            return ValuesBase.constMap().getConstByGroups();
        }};
    RouteWrap apiDebugToken= new RouteWrap(false) {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            System.out.println("Отладочный токен сервера "+db.getDebugToken());
            return new JString(db.getDebugToken());
        }};
    RouteWrap apiVersion = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            WorkSettingsBase ws = getWorkSettings();
            if (ws==null) return null;
            System.out.println("Текущая версия клиента "+ws.getMKVersion());
            return new JString(ws.getMKVersion());
        }};
    public synchronized void updateWorkSettingsField(String fld) throws UniException {
        getWorkSettings();
        db.mongoDB.updateField(workSettings,fld);
        }
    public synchronized void updateWorkSettingsInt(String fld,int value) throws UniException {
        getWorkSettings();
        workSettings.setFieldValueInt(fld,value);
        db.mongoDB.update(workSettings);
        }
    public synchronized void updateWorkSettingsBoolean(String fld,boolean value) throws UniException {
        getWorkSettings();
        workSettings.setFieldValueBoolean(fld,value);
        db.mongoDB.update(workSettings);
    }
    public synchronized void updateWorkSettingsString(String fld, String value) throws UniException {
        getWorkSettings();
        workSettings.setFieldValueString(fld,value);
        db.mongoDB.update(workSettings);
    }
    public synchronized WorkSettingsBase getWorkSettings() throws UniException {
        if (workSettings!=null)
            return workSettings;
        workSettings = ValuesBase.env().currentWorkSettings();
        ArrayList<Entity> list = db.mongoDB.getAll(workSettings,ValuesBase.GetAllModeActual,1);
        if (list.size()==0){
            db.mongoDB.clearTable("WorkSettings");
            db.mongoDB.add(workSettings);
            System.out.println("Созданы рабочие настройки");
            return workSettings;
            }
        else{
            System.out.println("Прочитаны рабочие настройки");
            workSettings = (WorkSettingsBase) list.get(0);
            return workSettings;
            }

        }

    public synchronized void changeServerState(I_ChangeRecord todo) throws UniException {
        if (serverState==null)
            serverState = getServerState();
        if (todo.changeRecord(serverState))
            db.mongoDB.update(serverState);
            }
    public synchronized void addTimeStamp(long clock) {
        if (serverState==null)
        serverState = getServerState();
        serverState.addTimeStamp(clock);
        }

    public synchronized ServerState getServerStateRight() {
        return serverState==null ? new ServerState() : serverState;
        }
    public synchronized ServerState getServerState() {
        if (serverState==null) {
            serverState = new ServerState();
            serverState.setPid();
            try {
                ArrayList<Entity> list = db.mongoDB.getAll(new ServerState(), ValuesBase.GetAllModeActual, 1);
                if (list.size() == 0) {
                    db.mongoDB.add(serverState);
                    System.out.println("Созданы параметры сервера");
                } else {
                    serverState = (ServerState) list.get(0);
                    serverState.setPid();
                    }
                } catch (UniException ee){}
            }
        return serverState;
        }
    RouteWrap apiWorkSettings = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            WorkSettingsBase ws = getWorkSettings();
            return new DBRequest(ws,new Gson());
        }};
    RouteWrap apiWorkSettingsGetInt = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamString field = new ParamString(req,res,"field");
            if (!field.isValid()) return null;
            WorkSettingsBase ws = getWorkSettings();
            return new JInt(ws.getFieldValueInt(field.getValue()));
        }};
    RouteWrap apiWorkSettingsGetBoolean = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamString field = new ParamString(req,res,"field");
            if (!field.isValid()) return null;
            WorkSettingsBase ws = getWorkSettings();
            return new JBoolean(ws.getFieldValueBoolean(field.getValue()));
        }};
    RouteWrap apiWorkSettingsGetString = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamString field = new ParamString(req,res,"field");
            if (!field.isValid()) return null;
            WorkSettingsBase ws = getWorkSettings();
            return new JString(ws.getFieldValueString(field.getValue()));
        }};
    RouteWrap apiWorkSettingsUpdateInt = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamString field = new ParamString(req,res,"field");
            if (!field.isValid()) return null;
            ParamInt value = new ParamInt(req,res,"value");
            if (!value.isValid()) return null;
            updateWorkSettingsInt(field.getValue(),value.getValue());
            return new JEmpty();
        }};
    RouteWrap apiWorkSettingsUpdateBoolean = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamString field = new ParamString(req,res,"field");
            if (!field.isValid()) return null;
            ParamBoolean value = new ParamBoolean(req,res,"value");
            if (!value.isValid()) return null;
            updateWorkSettingsBoolean(field.getValue(),value.getValue());
            return new JEmpty();
        }};
    RouteWrap apiWorkSettingsUpdateString = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamString field = new ParamString(req,res,"field");
            if (!field.isValid()) return null;
            ParamString value = new ParamString(req,res,"value");
            if (!value.isValid()) return null;
            updateWorkSettingsString(field.getValue(),value.getValue());
            return new JEmpty();
        }};
    RouteWrap apiServerState = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ServerState ws = getServerState();
            ws.setСashEnabled(db.mongoDB.isCashOn());
            ws.setTotalGetCount(db.mongoDB.getTotalGetCount());
            ws.setCashGetCount(db.mongoDB.getCashGetCount());       // Добавить, ТЕКУЩЕЕ
            return ws;
        }};
    RouteWrap apiSendBug = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            if (db.users.isReadOnly(req,res)) return null;
            Entity ent = addEntityHTTP(req,res,BugMessage.class);
            if (ent==null) return null;
            System.out.println("Передан баг: "+ent.toString());
            return new JLong(ent.getOid());
            }};
    public Object keepAlive(Request req, Response res,boolean timeStamp) throws Exception {
        int count=0;
        String val = req.headers(ValuesBase.SessionHeaderName);
        if (val==null)
            return new JInt(0);       // Вернуть количество уведомлений, не прочитанных
        //db.serverLock.lock(5);
        UserContext ctx = db.sessions.getContext(val);
        if (ctx==null){
            System.out.println("KeepAlive: no user context");
        //    db.serverLock.unlock(5);
            }
        else {
            User uu = ctx.getUser();
            if (uu == null) {
                System.out.println("KeepAlive: no user");
            } else {
                int type = uu.getTypeId();
                count = db.notify.getNotificationCount(type, ValuesBase.NSSend, uu.getOid());
                System.out.println("KeepAlive: user " + uu.getTitle() + " [" + count + "]");
                if (timeStamp)
                    ctx.wasCalled();
                }
            }
        db.serverLock.unlock(5);
        return new JInt(count);
        }
    RouteWrap apiKeepAlive = new RouteWrap(false) {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            return keepAlive(req,res,true);
            }};
    RouteWrap apiBugList = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            EntityList out = getEntityListHTTP(req,res, BugMessage.class);
            return out;
        }};
    RouteWrap routeBugGet = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            BugMessage out = (BugMessage) getEntityByIdHTTP(req,res,BugMessage.class);
            if (out==null)
                return null;
            if (out.getUserId().getOid()!=0) {
                if (!getEntityById(req,res, Artifact.class,out.getUserId(),0))
                    return null;
                }
            return out;
        }
    };

    RouteWrap apiDeleteById = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            if (db.users.isReadOnly(req,res)) return null;
            return deleteById(req,res,ValuesBase.DeleteMode);
            }};
    RouteWrap apiUndeleteById = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            if (db.users.isReadOnly(req,res)) return null;
            return deleteById(req,res,ValuesBase.UndeleteMode);
        }};

    public Object deleteById(Request req, Response res, boolean mode) throws Exception {
        ParamString entity = new ParamString(req,res,"entity");
        if (!entity.isValid()) return null;
        ParamLong id = new ParamLong(req,res,"id");
        if (!id.isValid()) return null;
        Entity cc=null;
        try {
            //-------------- По имени сущности ---------------------------------------------------------------------
            //cc = (Entity) Class.forName(entity.getValue()).newInstance();
            Class zz = ValuesBase.EntityFactory().get(entity.getValue());
            if (zz==null){
                db.createHTTPError(res,ValuesBase.HTTPRequestError, "Недопустимый класс сущности "+entity.getValue());
                }
            cc = (Entity) zz.newInstance();
            boolean bb = db.mongoDB.delete(cc,id.getValue(),mode);
            System.out.println((mode ? "Восстановлен" : "Удален")+" id="+id.getValue()+" "+bb);
            return new JBoolean(bb);
            } catch(Exception ee){
                db.createHTTPError(res,ValuesBase.HTTPRequestError, "Не могу создать объект "+entity+" "+ee.toString());
                return null;
                }
        }
    RouteWrap routeGetConsoleLog = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamInt count = new ParamInt(req,res,"count");
            if (!count.isValid()) return null;
            StringList list =  db.getConsoleLog().getStrings(count.getValue());
            return list;
        }};
    //------------------- Общий код для операций list/get/add/update----------------------------
    public boolean getEntityListById(Request req, Response res, Class proto, EntityLinkList links) throws Exception {
        return getEntityListById(req,res,proto,links,0);
        }
    public boolean getEntityListById(Request req, Response res, Class proto, EntityLinkList links, int level) throws Exception {
        for(int i=0;i<links.size();i++){
            if (!db.common.getEntityById(req,res, proto,(EntityLink) links.get(i),level))
                return false;
            }
        return true;
        }
    public boolean getEntityById(Request req, Response res, Class proto, EntityLink link) throws Exception {
        return getEntityById(req,res,proto,link,0);
        }
    public boolean getEntityById(Request req, Response res, Class proto, EntityLink link, int level) throws Exception {
        Entity ent = getEntityById(req,res,proto,link.getOid(),level);
        if (ent==null)
            return false;
        link.setRef(ent);
        return true;
        }
    public int getLevel(Request req,Response res) throws Exception {
        ParamInt plevel = new ParamInt(req,res,"level",0);
        int level = plevel.isValid() ?  plevel.getValue() : 0;
        return level;
        }
    public Entity getEntityByIdHTTP(Request req, Response res, Class proto) throws Exception {
        ParamLong id = new ParamLong(req,res,"id");
        if (!id.isValid()) return null;
        ParamInt plevel = new ParamInt(req,res,"level",0);
        int level = plevel.isValid() ?  plevel.getValue() : 0;
        return getEntityById(req,res,proto, id.getValue(),level);
        }
    public Entity getEntityByIdHTTP(Request req, Response res, Class proto,int level) throws Exception {
        ParamLong id = new ParamLong(req,res,"id");
        if (!id.isValid()) return null;
        return getEntityById(req,res,proto, id.getValue(),level);
    }
    public Entity getEntityById(Request req, Response res, Class proto, long id) throws Exception {
        return getEntityById(req,res,proto,id,0);
        }
    public Entity getEntityByIdWithLevel(Request req, Response res, Class proto, long id) throws Exception {
        ParamInt plevel = new ParamInt(req,res,"level",0);
        int level = plevel.isValid() ?  plevel.getValue() : 0;
        return getEntityById(req,res,proto,id,level);
        }
    public Entity getEntityById(Request req, Response res, Class proto, int level) throws Exception {
        ParamLong id = new ParamLong(req,res,"id");
        if (!id.isValid()) return null;
        Entity uu = (Entity) proto.newInstance();
        if (!db.mongoDB.getById(uu,id.getValue(),level)){
            db.createHTTPError(res,ValuesBase.HTTPNotFound, proto.getSimpleName()+" не найден id="+id);
            return null;
            }
        return uu;
        }
    public Entity getEntityById(Request req, Response res, Class proto, long id,int level) throws Exception {
        Entity uu = (Entity) proto.newInstance();
        if (!db.mongoDB.getById(uu,id,level)){
            db.createHTTPError(res,ValuesBase.HTTPNotFound, proto.getSimpleName()+" не найден id="+id);
            return null;
            }
        return uu;
        }
    public Entity addEntityHTTP(Request req, Response res, Class proto) throws Exception {
        return addEntityHTTP(req,res,proto,null);
        }
    public Entity addEntityHTTP(Request req, Response res, Class proto,BeforeAction before) throws Exception {
        ParamInt plevel = new ParamInt(req,res,"level",0);
        int level = plevel.isValid() ?  plevel.getValue() : 0;
        ParamBody qq = new ParamBody(req, res, proto);
        if (!qq.isValid()) return null;
        Entity ent = (Entity) qq.getValue();
        if (before!=null)
            before.daAction(ent);
        db.mongoDB.add(ent,level);
        return ent;
        }
    public EntityList getEntityListHTTP(Request req, Response res, Class proto) throws Exception {
        ParamInt fl = new ParamInt(req,res,"mode",ValuesBase.GetAllModeActual);
        ParamInt plevel = new ParamInt(req,res,"level",0);
        int level = plevel.isValid() ?  plevel.getValue() : 0;
        EntityList<Entity> xx = (EntityList<Entity>)db.mongoDB.getAll((Entity) proto.newInstance(),fl.getValue(),level);
        return xx;
        }
    public JEmpty updateEntityHTTP(Request req, Response res, Class proto) throws Exception {
        return updateEntityHTTP(req,res,proto,null);
        }
    public JEmpty updateEntityHTTP(Request req, Response res, Class proto, BeforeAction beforeUpdate) throws Exception {
        long tt = System.currentTimeMillis();
        ParamInt plevel = new ParamInt(req,res,"level",0);
        int level = plevel.isValid() ?  plevel.getValue() : 0;
        ParamBody qq = new ParamBody(req,res, proto);
        if (!qq.isValid())
            return null;
        if (getEntityById(req,res,proto,qq.getOid(),0)==null)
            return null;
        Entity ent = (Entity) qq.getValue();
        if (beforeUpdate!=null)
            beforeUpdate.daAction(ent);
        db.mongoDB.update(ent,level);
        return new JEmpty();
        }
    RouteWrap apiGetBug = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            BugMessage out = (BugMessage) db.common.getEntityByIdHTTP(req,res,BugMessage.class);
            if (out==null)
                return null;
            if (out.getUserId().getOid()!=0) {
                if (!db.common.getEntityById(req,res, User.class,out.getUserId(),0))
                    return null;
                }
            return out;
            }
        };
    //-------------------------------------------------------------------------------------------
    RouteWrap apiHelpFileList = new RouteWrap(false) {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamString question = new ParamString(req,res,"question");
            if (!question.isValid()) return null;
            String qList = question.getValue();
            EntityList<Entity> helps = db.mongoDB.getAll(new HelpFile(), ValuesBase.GetAllModeActual,1);
            EntityList<Entity> out = new EntityList<>();
            for (Entity ent : helps){
                HelpFile helpFile = (HelpFile)ent;
                if (helpFile.isAllTagsPresent(qList))
                    out.add(ent);
                }
            return out;
        }};
    //---------------------------------------------------------------------------------------------
    class ArtifactResult{
        final Entity ent;
        final Artifact art;
        final Field field;
        public ArtifactResult(Entity ent, Artifact art, Field field) {
            this.ent = ent;
            this.art = art;
            this.field = field;
        }
    }
    private ArtifactResult common(Request req, Response res, boolean linkList) throws Exception{
        ParamLong id = new ParamLong(req,res,"id");
        if (!id.isValid())
            return null;
        ParamLong artid = new ParamLong(req,res,"artifactid");
        if (!artid.isValid())
            return null;
        ParamString className = new ParamString(req,res,"classname");
        if (!className.isValid())
            return null;
        ParamString fieldName = new ParamString(req,res,"fieldname");
        if (!fieldName.isValid())
            return null;
        Class cc = ValuesBase.EntityFactory().getClassForSimpleName(className.getValue());
        if (cc==null){
            db.createHTTPError(res,ValuesBase.HTTPRequestError, "Недопустимый класс сущности "+className);
            return null;
        }
        Entity uu = (Entity) cc.newInstance();
        if (!db.mongoDB.getById(uu,id.getValue(),1)){
            db.createHTTPError(res,ValuesBase.HTTPNotFound, className+" не найден id="+id);
            return null;
        }
        Field field = uu.getField(fieldName.getValue(), linkList ? DAO.dbLinkList : DAO.dbLink);
        if (field==null){
            db.createHTTPError(res,ValuesBase.HTTPNotFound, fieldName+" не найдено");
            return null;
        }
        Artifact art = new Artifact();
        if (!db.mongoDB.getById(art,artid.getValue())){
            db.createHTTPError(res,ValuesBase.HTTPNotFound, "Артефакт не найден id="+id);
            return null;
        }
        return new ArtifactResult(uu,art,field);
    }
    RouteWrap routeArtifactToList = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            if (db.users.isReadOnly(req,res)) return null;
            ArtifactResult pair = common(req,res,true);
            if (pair==null)
                return null;
            Artifact art = pair.art;
            Entity ent = pair.ent;
            art.setParent(ent);
            db.mongoDB.update(art);
            EntityLinkList list = (EntityLinkList)pair.field.get(ent);
            list.add(art.getOid());
            db.mongoDB.update(ent);
            return new JEmpty();
        }};
    RouteWrap routeArtifactFromList = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ArtifactResult pair = common(req,res,true);
            if (pair==null)
                return null;
            Artifact art = pair.art;
            Entity ent = pair.ent;
            art.setParent(ent);
            db.mongoDB.update(art);
            EntityLinkList list = (EntityLinkList)pair.field.get(ent);
            if (!list.removeById(art.getOid())) {
                db.createHTTPError(res,ValuesBase.HTTPNotFound, "Артефакт отсутствует в списке id="+art.getOid());
                return null;
            }
            db.mongoDB.update(ent);
            db.files.deleteArtifactFile(art);
            db.mongoDB.remove(art);
            return new JEmpty();
        }};
    RouteWrap routeArtifactReplace = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            if (db.users.isReadOnly(req,res)) return null;
            ArtifactResult pair = common(req,res,false);
            if (pair==null)
                return null;
            Artifact art = pair.art;
            Entity ent = pair.ent;
            EntityLink list = (EntityLink)pair.field.get(ent);
            if (list.getOid()!=0){
                System.out.println("Replace artifact "+list.getOid()+"->"+art.getOid());
                Artifact old = new Artifact();
                db.mongoDB.getById(old,list.getOid());
                db.files.deleteArtifactFile(old);
                db.mongoDB.remove(old);             // Удалить старый артефакт и файл
                }
            art.setParent(ent);
            db.mongoDB.update(art);
            list.setOid(art.getOid());
            db.mongoDB.update(ent);
            return new JEmpty();
        }};

}
