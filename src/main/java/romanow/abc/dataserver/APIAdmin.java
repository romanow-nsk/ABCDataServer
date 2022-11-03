package romanow.abc.dataserver;

import org.apache.poi.ss.usermodel.Row;
import romanow.abc.core.*;

import romanow.abc.core.constants.TableItem;
import romanow.abc.core.constants.ValuesBase;
import romanow.abc.core.entity.*;
import romanow.abc.core.entity.artifacts.Artifact;
import romanow.abc.core.entity.artifacts.ArtifactTypes;
import romanow.abc.core.entity.artifacts.ReportFile;
import romanow.abc.core.entity.base.HelpFile;
import romanow.abc.core.entity.base.StringList;
import romanow.abc.core.entity.baseentityes.*;
import romanow.abc.core.entity.users.Account;
import romanow.abc.core.entity.users.User;
import romanow.abc.core.export.Excel;
import romanow.abc.core.export.ExcelX;
import romanow.abc.core.export.I_Excel;
import romanow.abc.core.jdbc.JDBCFactory;
import romanow.abc.core.mongo.DBQueryList;
import romanow.abc.core.mongo.I_ChangeRecord;
import romanow.abc.core.mongo.I_MongoDB;
import romanow.abc.core.mongo.RequestStatistic;
import romanow.abc.core.utils.Address;
import romanow.abc.core.utils.GPSPoint;
import romanow.abc.core.utils.OwnDateTime;
import spark.Request;
import spark.Response;

import java.io.*;
import java.util.*;

public class APIAdmin extends APIBase{
    private BackGroundOperation background = new BackGroundOperation();
    public APIAdmin(DataServer db0) {
        super(db0);
        spark.Spark.get("/api/admin/exportdb", routeExportXLS);
        spark.Spark.get("/api/admin/dump", routeDump);
        spark.Spark.post("/api/admin/reboot", apiReboot);
        spark.Spark.post("/api/admin/importdb", routeImportXLS);
        spark.Spark.post("/api/admin/restore", routeRestore);
        spark.Spark.post("/api/admin/deploy", apiDeploy);
        spark.Spark.post("/api/admin/execute", apiExecute);
        spark.Spark.post("/api/admin/shutdown", apiShutdown);
        spark.Spark.post("/api/admin/asterisk/setmailcount", apiSetMailCount);
        spark.Spark.get("/api/admin/preparedb",apiPrepareDB);
        spark.Spark.get("/api/admin/testcall",apiTestCall);
        spark.Spark.get("/api/admin/longpoll",apiBackgroundAnswer);
        spark.Spark.post("/api/admin/lock",apiLock);
        spark.Spark.get("/api/admin/cleardb", apiClearDB);
        spark.Spark.get("/api/admin/cleartable", apiClearTable);
        spark.Spark.post("/api/admin/cashmode", apiSetCashMode);
        spark.Spark.post("/api/admin/logfile/reopen", apiReopenLogFile);
        spark.Spark.get("/api/admin/files/list", apiGetFileList);
        spark.Spark.post("/api/admin/clonedb", apiCloneDB);
        }
        //--------------------------------------------------------------------------------------
    RouteWrap apiLock = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            if (!db.users.isOnlyForSuperAdmin(req,res))
                return null;
            ParamBoolean on = new ParamBoolean(req,res,"on");
            if (!on.isValid())
                return null;
            boolean onVal = on.getValue();
            db.getServerState().setLocked(onVal);
            if (onVal)
                db.sessions.keepOnlySuperAdmins();     // Оставить только SA
            return new JEmpty();
        }};
    RouteWrap apiBackgroundAnswer = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            if (!db.users.isOnlyForSuperAdmin(req,res))
                return null;
            if (!background.isBusy()){
                db.createHTTPError(res, ValuesBase.HTTPRequestError, "Фоновая операция не выполняется");
                return null;
            }
            String ss = background.getAnswer();
            if (ss.length()!=0)
                return new JString(ss);
            background.waitThread();
            return new JString(background.getAnswer());
        }};
    RouteWrap apiPrepareDB = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            String out="";
            if (!db.users.isOnlyForSuperAdmin(req,res))
                return null;
            ParamInt oper = new ParamInt(req,res,"operation");
            if (!oper.isValid()) return null;
            if (!background.testAndSetBusy()){
                db.createHTTPError(res, ValuesBase.HTTPRequestError, "Фоновая операция уже выполняется");
                return null;
            }
            final int code=oper.getValue();
            System.out.println("Операция над БД: "+code);
            new Thread(new Runnable() {     // В потоке
                @Override
                public void run() {
                    String out="";
                    try {
                        String xx = systemOperation(code);
                        background.setAnswer("Завершение фоновой операции\n"+xx);
                    } catch(Exception ee){
                        background.setAnswer("Ошибка фона:"+ Utils.createFatalMessage(ee));
                    }
                }
            }){}.start();
            return new JString("Операция в фоне, ждите...");
        }};
    RouteWrap apiReboot = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            String out = "";
            if (!db.users.isOnlyForSuperAdmin(req,res))
                return null;
            db.delayInGUI(ValuesBase.ServerRebootDelay,new Runnable() {
                @Override
                public void run() {
                        db.restartServer(false);
                        }
                    });
            db.delayInGUI(ValuesBase.ServerRebootDelay/2,new Runnable() {
                @Override
                public void run() {
                            db.shutdown();
                        }
                    });
            return new JEmpty();
           }
        };

    public String printConsoleOutput(Process p,boolean isWin) throws Exception{
        String out="";
        Locale current = Locale.getDefault();
        InputStream is = p.getInputStream();
        InputStream ers = p.getErrorStream();
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(is, (isWin ? "Cp866" : "UTF8")));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(ers, (isWin ? "Cp866" : "UTF8")));
        String temp=null;
        long clock = System.currentTimeMillis();
        while (p.isAlive() && System.currentTimeMillis()-clock < ValuesBase.ConsolePrintPause*1000){
            while (is.available()!=0){
                temp = stdInput.readLine();
                if (temp!=null){
                    System.out.println(temp);
                    out += temp+"\n";
                    clock = System.currentTimeMillis();
                    }
                }
             while (ers.available()!=0){
                temp = stdError.readLine();
                if (temp!=null){
                    System.out.println(temp);
                    out += temp+"\n";
                    clock = System.currentTimeMillis();
                    }
                }
            }
        return out;
        }
    RouteWrap apiExecute = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            String out = "";
            if (!db.users.isOnlyForSuperAdmin(req,res))
                return null;
            //------------- В Windows однократное выполнение команды cmd /c dir
            ParamString cmd = new ParamString(req, res, "cmd");
            if (!cmd.isValid()) return null;
            StringTokenizer tok = new StringTokenizer(cmd.getValue()," ");
            int cc = tok.countTokens();
            String vv[] = new String[cc];
            for(int i=0;i<cc;i++)
                vv[i]=tok.nextToken();
            final boolean isWin = System.getProperty("os.name").startsWith("Windows");
            Runtime r =Runtime.getRuntime();
            Process p =null;
            try {
                p=r.exec(vv);
                String ss=  printConsoleOutput(p,isWin);
                out+=ss;
                p.destroy();
                } catch (Exception e) {
                    String zz = "Ошибка выполнения скрипта "+e.toString();
                    System.out.println(zz);
                    out+=zz+"\n";
                    if (p!=null)
                        p.destroy();
                        }
            return new JString(out);
        }
    };

    RouteWrap apiDeploy = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            String out = "";
            if (!db.users.isOnlyForSuperAdmin(req,res))
                return null;
            ParamInt  mbSize = new ParamInt(req,res,"mb",0);
            if (!mbSize.isValid())
                return null;
            int mb = mbSize.getValue();
            final boolean isWin = System.getProperty("os.name").startsWith("Windows");
            String indicatorName = db.port+"_"+ValuesBase.DeployScriptName+".lock";
            BufferedWriter bat = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(indicatorName)));
            bat.write("Индикатор обновления сервера");
            bat.newLine();
            bat.flush();
            bat.close();
            String batName = db.port+"_"+ValuesBase.DeployScriptName+(isWin ? ".bat" : ".sh");
            bat = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(batName)));
            String serverName = ValuesBase.env().applicationName(ValuesBase.AppNameServerJar);
            String className = ValuesBase.env().applicationClassName(ValuesBase.ClassNameConsoleServer);
            if (isWin) {
                bat.write("timeout /t " + ValuesBase.ServerRebootDelay);
                bat.newLine();
                bat.write("copy " + db.port + "\\" + serverName + " " + db.port + "_" + serverName);
                bat.newLine();
                bat.write("java "+(mb!=0 ? " -Xmx"+mb+"m" : "")+" -cp " + db.port + "_" + serverName + " "+className+" " + db.port+" none");
                bat.newLine();
                bat.write("del /q " + db.port+"_"+ValuesBase.DeployScriptName+".lock");
                bat.newLine();
            }
            else{
                bat.write("#!/bin/bash");   // #!/usr/bin/bash
                bat.newLine();
                bat.write("sleep " + ValuesBase.ServerRebootDelay);
                bat.newLine();
                bat.write("cp -f " + db.port + "/" + serverName + " " + db.port + "_" + serverName);
                bat.newLine();
                bat.write("nohup java "+(mb!=0 ? " -Xmx"+mb+"m" : "")+" -cp " + db.port + "_" + serverName + " "+className+" " + db.port+" none "+(mb!=0 ? " -Mxmx"+mb+"m" : "")+" &");
                bat.newLine();
                bat.write("rm -f " + db.port+"_"+ValuesBase.DeployScriptName+".lock");
                bat.newLine();
                }
            bat.flush();
            bat.close();
            db.delayInGUI(ValuesBase.ServerRebootDelay,new Runnable() {
                @Override
                public void run() {
                    Runtime r =Runtime.getRuntime();
                    Process p =null;
                    try {
                        if (isWin)
                            p=r.exec(batName);
                        else{
                            String zz[]=new String[2];
                            zz[0]=ValuesBase.bashPath+"bash";
                            zz[1]=batName;
                            System.out.println(zz[0]+" "+zz[1]);
                            p=r.exec(zz);
                            }
                        db.delayInGUI(ValuesBase.ServerRebootDelay/2,new Runnable() {
                            @Override
                            public void run() {
                                System.exit(0);
                            }
                        });
                    } catch (IOException e) {
                        System.out.println("Ошибка выполнения скрипта "+e.toString());
                    }
                }
            });
            db.delayInGUI(ValuesBase.ServerRebootDelay/2,new Runnable() {
                @Override
                public void run() {
                    db.shutdown();
                }
            });
            return new JString("Обновление");
        }
    };

    RouteWrap apiShutdown = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            String out = "";
            if (!db.users.isOnlyForSuperAdmin(req,res))
                return null;
            db.delayInGUI(ValuesBase.ServerRebootDelay,new Runnable() {
                @Override
                public void run() {
                    System.exit(0);
                }
            });
            db.delayInGUI(ValuesBase.ServerRebootDelay/2,new Runnable() {
                @Override
                public void run() {
                    db.shutdown();
                }
            });
            return new JString("Завершение");
        }
    };


    RouteWrap apiSetMailCount = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            String out = "";
            if (!db.users.isOnlyForSuperAdmin(req,res))
                return null;
            ParamInt mailCount = new ParamInt(req, res, "mailcount");
            if (!mailCount.isValid())
                return null;
            db.changeServerState(new I_ChangeRecord() {
                @Override
                public boolean changeRecord(Entity ent) {
                    ((ServerState)ent).setLastMailNumber(mailCount.getValue());
                    return true;
                }
            });
            return new JEmpty();
            }
        };

    RouteWrap routeImportXLS = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            if (!db.users.isOnlyForSuperAdmin(req,res))
                return null;
            Artifact art = (Artifact)db.common.getEntityByIdHTTP(req,res,Artifact.class);
            if (art==null)
                return null;
            I_Excel xls = art.getOriginalExt().equals("xlsx") ? new ExcelX() : new Excel();
            db.clearDB();
            String zz = xls.load(db.dataServerFileDir()+"/"+art.createArtifactServerPath(),db.mongoDB);
            db.files.deleteArtifactFile(art);
            db.mongoDB.remove(art);
            db.delayInGUI(ValuesBase.ServerRebootDelay,new Runnable() {
                @Override
                public void run() {
                    db.restartServer(false);
                }
                });
            db.delayInGUI(ValuesBase.ServerRebootDelay/2,new Runnable() {
                @Override
                public void run() {
                    db.shutdown();
                }
                });
            return new JString(zz);
        }};
    public boolean exportToExcel(I_Excel ex){
        int i=0;
        ArrayList<TableItem> olist = ValuesBase.EntityFactory().classList(true);
        for(TableItem item : olist){
            if (!item.isTable || !item.isExportXLS())
                continue;
            try {
                Entity ent = (Entity) item.clazz.newInstance();
                ArrayList<Entity> list = db.mongoDB.getAll(ent,ValuesBase.GetAllModeTotal,0);
                ex.exportHeader(ent);
                ex.exportData(list);
            } catch (Exception ee){
                System.out.println("Не могу экспортировать "+item.name+"\n"+ee.toString());
                }
            }
        return true;
        }

    public boolean exportToExcelBlocked(I_Excel ex, int blockSize){
        int i=0;
        ArrayList<TableItem> olist = ValuesBase.EntityFactory().classList(true);
        for(TableItem item : olist){
            if (!item.isTable || !item.isExportXLS())
                continue;
            try {
                Entity ent = (Entity) item.clazz.newInstance();
                long lastOid = db.mongoDB.lastOid(ent);
                long firstOid=1;
                ex.exportHeader(ent);
                System.out.println(ent.getClass().getSimpleName()+" lastOid="+lastOid);
                DBQueryList query = new DBQueryList().add("oid",lastOid);
                ArrayList<Entity> list = db.mongoDB.getAllByQuery(ent,query,0);
                ex.exportData(list);
                while(firstOid+blockSize<lastOid){
                    query = new DBQueryList().add(DBQueryList.ModeGTE,"oid",firstOid).
                            add(DBQueryList.ModeLT,"oid",firstOid+blockSize);
                    list = db.mongoDB.getAllByQuery(ent,query,0);
                    System.out.println("oid="+firstOid+" size="+list.size());
                    //list.sort(new Comparator<Entity>() {
                    //    @Override
                    //    public int compare(Entity o1, Entity o2) {
                    //        return (int)(o1.getOid()-o2.getOid());
                    //    }
                    //    });
                    ex.exportData(list);
                    firstOid+=blockSize;
                    }
                query = new DBQueryList().add(DBQueryList.ModeGTE,"oid",firstOid).
                        add(DBQueryList.ModeLT,"oid",lastOid);
                list = db.mongoDB.getAllByQuery(ent,query,0);
                System.out.println("oid="+firstOid+" size="+list.size());
                //list.sort(new Comparator<Entity>() {
                //    @Override
                //    public int compare(Entity o1, Entity o2) {
                //        return (int)(o1.getOid()-o2.getOid());
                //        }
                //    });
                ex.exportData(list);
            } catch (Exception ee){
                System.out.println("Не могу экспортировать "+item.name+"\n"+ee.toString());
            }
        }
        return true;
    }
    RouteWrap routeExportXLS = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            OwnDateTime cc = new OwnDateTime();
            ParamBoolean xlsx = new ParamBoolean(req,res,"xlsx",false);
            if (!xlsx.isValid())
                return null;
            ParamInt blockSize = new ParamInt(req,res,"blocksize",0);
            if (!blockSize.isValid())
                return null;
            Artifact art = new Artifact("mongo-"+ValuesBase.env().applicationName(ValuesBase.AppNameDBName)+db.port+"-"+ Utils.nDigits(cc.month(),2)+Utils.nDigits(cc.day(),2)+"-"+db.common.getServerState().getReleaseNumber()+
                    (xlsx.getValue() ?".xlsx" : ".xls"),0);
            art.setName("mongo:"+db.port);
            int artType = ArtifactTypes.getArtifactType(art.getOriginalExt());
            art.setType(artType);
            String dir = db.dataServerFileDir() + "/"+art.type()+"_"+art.directoryName();
            File path = new File(dir);
            if (!path.exists())
                path.mkdir();
            db.mongoDB.add(art);
            I_Excel xls = xlsx.getValue() ? new ExcelX() : new Excel();
            if (blockSize.getValue()==0)
                exportToExcel(xls);
            else
                exportToExcelBlocked(xls,blockSize.getValue());
            String zz = dir +"/"+art.createArtifactFileName();
            xls.save(zz);
            db.mongoDB.update(art);
            ServerEvent event = new ServerEvent(ValuesBase.EventSystem,ValuesBase.ELInfo,"Скачан архив БД",art.getOriginalName())
                    .setAtrifact(art.getOid());
            db.notify.sendMailNotifycation(event);
            return art;
        }};
    //--------------------------------------------------------------------------------------------------------------------
    RouteWrap routeRestore = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            if (!db.users.isOnlyForSuperAdmin(req,res))
                return null;
            final boolean isWin = System.getProperty("os.name").startsWith("Windows");
            final Artifact art = (Artifact)db.common.getEntityByIdHTTP(req,res,Artifact.class);
            if (art==null)
                return null;
            for(TableItem item : ValuesBase.EntityFactory().classList()){
                Entity proto = (Entity)item.clazz.newInstance();
                db.mongoDB.dropTable(proto);
                }
            Runtime r =Runtime.getRuntime();
            Process p =null;
            try {
                String cmd = isWin ?
                        "mongorestore  /gzip /drop /archive:"+ db.dataServerFileDir()+"/"+art.createArtifactServerPath() :
                        "mongorestore  --gzip --drop --archive="+ db.dataServerFileDir()+"/"+art.createArtifactServerPath() ;
                p=r.exec(cmd);
                printConsoleOutput(p,isWin);
                p.destroy();
                } catch (IOException e) {
                    System.out.println("Ошибка выполнения скрипта "+e.toString());
                    }
            db.delayInGUI(ValuesBase.ServerRebootDelay/2,new Runnable() {
                @Override
                public void run() {
                    db.files.deleteArtifactFile(art);
                    try {
                        db.mongoDB.remove(art);
                        } catch (UniException e) {
                            System.out.println();
                            }
                        db.shutdown();
                        }
                    });
            db.delayInGUI(ValuesBase.ServerRebootDelay*2,new Runnable() {
                @Override
                public void run() {
                    db.restartServer(false);
                    }
                });
            return new JString("Обновление");
        }};
    //------------------------------------------------------------------------------------------------------------------
    RouteWrap routeDump = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            final boolean isWin = System.getProperty("os.name").startsWith("Windows");
            OwnDateTime cc = new OwnDateTime();
            String dbName= ValuesBase.env().applicationName(ValuesBase.AppNameDBName)+db.port;
            Artifact art = new Artifact("mongo-"+dbName+"-"+ Utils.nDigits(cc.month(),2)+Utils.nDigits(cc.day(),2)+"-"+db.common.getServerState().getReleaseNumber()+".gz",0);
            art.setName("mongo:"+db.port);
            int artType = ArtifactTypes.getArtifactType(art.getOriginalExt());
            art.setType(artType);
            String dir = db.dataServerFileDir() + "/"+art.type()+"_"+art.directoryName();
            File path = new File(dir);
            if (!path.exists())
                path.mkdir();
            db.mongoDB.add(art);
            String zz = dir +"/"+art.createArtifactFileName();
            Runtime r =Runtime.getRuntime();
            Process p =null;
            try {
                String cmd = isWin ? "mongodump /db:"+dbName+" /gzip /archive:"+ db.dataServerFileDir()+"/"+art.createArtifactServerPath() :
                        "mongodump --db="+dbName+" --gzip --archive="+ db.dataServerFileDir()+"/"+art.createArtifactServerPath();
                p=r.exec(cmd);
                printConsoleOutput(p,isWin);
                p.destroy();
                db.mongoDB.update(art);
                ServerEvent event = new ServerEvent(ValuesBase.EventSystem,ValuesBase.ELInfo,"Скачан архив БД",art.getOriginalName())
                        .setAtrifact(art.getOid());
                db.notify.sendMailNotifycation(event);
                return art;
                } catch (IOException e) {
                    String ss = "Ошибка выполнения скрипта "+e.toString();
                    System.out.println(ss);
                    db.createHTTPError(res, ValuesBase.HTTPRequestError,ss);
                    return null;
                    }
            }};
    //-------------------------------------------------------------------------------------------------------------------
    RouteWrap apiClearDB = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            String out="";
            if (!db.users.isOnlyForSuperAdmin(req,res))
                return null;
            out = db.mongoDB.clearDB();
            System.out.println("Сброс БД");
            return new JString(out);
        }};


    public String additionalSystemOperations (int code) throws Exception{
        return "Нет операции";
        }
    private String systemOperation(int code) throws Exception{
        String out="Нет операции";
        switch (code) {
            case ValuesBase.DBOBackLinks:
                out = createEntityBacks();
                break;
            case ValuesBase.DBOSquezzyTables:
                out = squeezyTables();
                break;
            case ValuesBase.DBORefreshFields:
                out = refreshTables();
                break;
            case ValuesBase.DBOCollectGarbage:
                out = collectGarbage();
                break;
            case ValuesBase.DBOTestArtifacts:
                out = testArtifacts();
                break;
            case ValuesBase.DBOTestDelay:
                try {
                    Thread.sleep(1000 * 60);
                } catch (Exception ee){}
                out = "Операция завершена";
                break;
            default: return additionalSystemOperations(code);
            }
        return out;
    }

    RouteWrap apiTestCall = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamInt oper = new ParamInt(req, res, "operation");
            if (!oper.isValid()) return null;
            switch (oper.getValue()){
                case 0: return new JString("Нормальное завершение");
                case 1: Integer vv = null;
                    vv.longValue();
                    break;
                case 2:
                    db.createHTTPError(res,ValuesBase.HTTPNotFound, "Тест - не найдено");
                    return null;
                case 3:
                    ParamInt ii = new ParamInt(req,res,"xxx");
                    if (!ii.isValid())
                        return null;
                    break;
                case 4:
                    ParamInt oo = new ParamInt(req,res,"value");
                    if (!oo.isValid())
                        return null;
                    return new JString("Тест" +oo.getValue());
            }
            return new JString("Тест");
        }};

    RouteWrap apiClearTable = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            String out="";
            if (!db.users.isOnlyForSuperAdmin(req,res))
                return null;
            ParamString table = new ParamString(req,res,"table");
            if (!table.isValid()) return null;
            Class zz = ValuesBase.EntityFactory().get(table.getValue());
            if (zz==null){
                db.createHTTPError(res,ValuesBase.HTTPRequestError, "Нет класса для "+table.getValue());
                return null;
            }
            try {
                Entity ent = (Entity) zz.newInstance();
                ent.setOid(1);
                db.mongoDB.dropTable(ent);
                long id = db.mongoDB.add(ent,0,true);
                db.mongoDB.delete(ent,id,false);
            } catch (Exception ee){
                String ss = "Не могу создать "+ zz.getSimpleName();
                db.sendBug(ss,ee);
                db.createHTTPError(res,ValuesBase.HTTPException, ss+"\n"+Utils.createFatalMessage(ee));
                return null;
            }
            out = "Очистка таблицы "+table.getValue();
            System.out.println();
            return new JString(out);
        }};


    //-------------------------------------------------------------------------------------------
    private int updateEntityBack(StringBuffer mes, Entity parent, EntityBack proto, EntityLink<?> link) throws UniException {
        return updateEntityBack(mes,parent,proto,link.getOid());
        }
    private int updateEntityBack(StringBuffer mes, Entity parent, EntityBack proto, EntityLinkList<?> links) throws UniException {
        int cnt=0;
        for(EntityLink<?> link : links)
            cnt += updateEntityBack(mes,parent,proto,link.getOid());
        return cnt;
        }
    private int updateEntityBack(StringBuffer mes, Entity parent, EntityBack proto, long oid) throws UniException {
        if (oid==0)
            return 0;
        db.mongoDB.getByIdAndUpdate(proto, oid, new I_ChangeRecord() {
            @Override
            public boolean changeRecord(Entity ent) {
                EntityBack xx = (EntityBack)ent;
                xx.setParent(parent);
                return true;
            }
        });
        return 1;
    }
    private void clearBackLinks(EntityBack proto) throws UniException {
        EntityList<Entity> list = db.mongoDB.getAll(proto);
        for(Entity ent : list) {
            EntityBack xx = (EntityBack)ent;
            xx.setParentOid(0);
            xx.setParentName("");
            db.mongoDB.update(xx);
        }
    }
    private String createEntityBacks() throws UniException {
        StringBuffer rez=new StringBuffer();
        GPSPoint proto = new GPSPoint();
        Address proto3 = new Address();
        Artifact proto4 = new Artifact();
        clearBackLinks(proto);
        clearBackLinks(proto3);
        clearBackLinks(proto4);
        int countGPS=0;
        int countState=0;
        int countAddr=0;
        int countArt=0;
        //------------------------------GPS/State------------------------------------
        EntityList<Entity> list;
        list = db.mongoDB.getAll(new Address());
        for(Entity ent : list){
            Address address= (Address)ent;
            countGPS += updateEntityBack(rez,address,proto,address.getLocation());
            }
        //---------------------------- Адреса -------------------------------
        //-------------------------- АРТЕФАКТЫ ---------------------------------------
        list = db.mongoDB.getAll(new ReportFile());
        for(Entity ent : list){
            ReportFile ctr= (ReportFile) ent;
            countArt += updateEntityBack(rez,ctr,proto4,ctr.getArtifact());
            }
        list = db.mongoDB.getAll(new User());
        for(Entity ent : list){
            User ctr= (User) ent;
            countArt += updateEntityBack(rez,ctr,proto4,ctr.getPhoto());
        }
        list = db.mongoDB.getAll(new HelpFile());
        for(Entity ent : list){
            HelpFile ctr= (HelpFile) ent;
            countArt += updateEntityBack(rez,ctr,proto4,ctr.getItemFile());
            }
        return rez.toString()+"\nссылки:\nGPS="+countGPS+"\nсостояния="+countState+"\nадреса="+countAddr+"\nартефакты="+countArt+"\n";
    }
    private String prepareOidRecords(){
        Object olist[] = ValuesBase.EntityFactory().classList().toArray();
        String out="";
        TableItem item=null;
        for(int i=0;i<olist.length;i++){
            try {
                item = (TableItem)olist[i];
                Entity ent = (Entity)(item.clazz.newInstance());
                EntityList xx = db.mongoDB.getAll(ent, ValuesBase.GetAllModeTotal,0);
                long maxOid=0;
                for(Object bb : xx){
                    Entity dd = (Entity)bb;
                    long oid = (((Entity) bb).getOid());
                    if (oid > maxOid)
                        maxOid = oid;
                }
                ent.setOid(maxOid+1);
                db.mongoDB.nextOid(ent,true);
            } catch (Exception ee){
                String ss = "Не могу создать "+ValuesBase.EntityFactory().get(item.clazz.getSimpleName())+"\n"+ee.toString()+"\n";
                System.out.print(ss);
                out+=ss+"\n";
            }
        }
        return out;
    }
    private String squeezyTables(){
        Object olist[] = ValuesBase.EntityFactory().classList().toArray();
        String out="";
        TableItem item=null;
        for(int i=0;i<olist.length;i++){
            long oid=0;
            item = (TableItem)olist[i];
            String eName = item.clazz.getSimpleName();
            try {
                Entity ent = (Entity)(item.clazz.newInstance());
                EntityList<Entity> xx = db.mongoDB.getAll(ent, ValuesBase.GetAllModeDeleted,0);
                for(int j=1; j<xx.size();j++){              // ПЕРВУЮ ПРОПУСТИТЬ
                    oid = xx.get(j).getOid();
                    db.mongoDB.remove(ent,oid);
                    }
                out+=eName+" удалено "+(xx.size()-1)+"\n";
            } catch (Exception ee){
                String ss = "Ошибка удаления "+eName+" oid = "+oid+"\n"+ee.toString()+"\n";
                System.out.print(ss);
                out+=ss+"\n";
            }
        }
        return out;
    }
    private String refreshTables(){
        Object olist[] = ValuesBase.EntityFactory().classList().toArray();
        String out="";
        TableItem item=null;
        for(int i=0;i<olist.length;i++){
            long oid=0;
            Entity zz=new Entity();
            item = (TableItem)olist[i];
            String eName = item.clazz.getSimpleName();
            try {
                Entity ent = (Entity) (item.clazz.newInstance());
                EntityList<Entity> xx = db.mongoDB.getAll(ent, ValuesBase.GetAllModeActual, 0);
                out+=eName+" обновляется "+(xx.size())+"\n";
                for (int j = 0; j < xx.size(); j++) {
                    zz = xx.get(j);
                    try {
                        db.mongoDB.update(zz);
                    } catch (Exception ee) {
                        String ss = "Ошибка обновления " + eName + " oid = " + zz.getOid() + " " + zz.getTitle() + "\n" + ee.toString() + "\n";
                        System.out.print(ss);
                        out += ss + "\n";
                    }
                }
            } catch (Exception ee){
                String ss = "Ошибка таблицы "+eName+"\n"+ee.toString()+"\n";
                System.out.print(ss);
                out+=ss+"\n";
            }
        }
        return out;
    }
    /*
    private String setSendToContractorState() throws UniException {
        MaintenanceList list = db.maintenance.getMaintenanceByCondition(0,-1,ValuesBase.MMonthly,-1,-1,ValuesBase.DateNone,0,0,0,0,0);
        for(Maintenance main : list){
            main.setWorkCompletionReportState(ValuesBase.WCRSendToContractor);
            db.mongoDB.update(main);
        }
        return "Состояние акта изменено в "+list.size()+" заявках";
    }
     */
    /*
    public String changeContracts() throws UniException {
        int cnt=0;
        EntityList<Entity> list = db.mongoDB.getAll(new Contract());
        for(Entity ent : list){
            Contract contract = (Contract) ent;
            contract.getFacilities().clear();
            long facId = contract.getFacility().getOid();
            contract.getFacilities().add(facId);
            Facility fac = new Facility();
            db.mongoDB.getById(fac,facId);
            contract.getContractor().setOid(fac.getContractor().getOid());
            db.mongoDB.update(contract);
            cnt++;
            }
        int cnt1=0;
        list = db.mongoDB.getAll(new Maintenance());
        for(Entity ent : list){
            Maintenance main = (Maintenance)ent;
            if (main.getPaymentState()== ValuesBase.PINone)
                continue;
            main.setFirstInGroup(true);
            db.mongoDB.add(main);
            cnt1++;
            }
        return "Обновлено "+cnt+" договоров";
        }
    */
    public String squeezyArtifacts() throws UniException {
        int count1=0;
        int count2=0;
        EntityList<Entity> list = db.mongoDB.getAll(new Artifact());
        for(Entity e1 :list){
            Artifact art = (Artifact) e1;
            if (art.getParentOid()!=0)
                continue;
            count1++;
            if (db.files.deleteArtifactFile(art))
                count2++;
            db.mongoDB.remove(art);
            }
        return "Артефакты: удалено "+count1+" записей, "+count2+" файлов\n";
        }
    public String squeezyAddress() throws UniException {
        int count1=0;
        EntityList<Entity> list = db.mongoDB.getAll(new Address());
        for(Entity e1 :list){
            Address art = (Address) e1;
            if (art.getParentOid()!=0)
                continue;
            count1++;
            db.mongoDB.remove(art);
            }
        return "Адреса: удалено "+count1+" записей\n";
        }
    public String squeezyGPS() throws UniException {
        int count1=0;
        EntityList<Entity> list = db.mongoDB.getAll(new GPSPoint());
        for(Entity e1 :list){
            GPSPoint art = (GPSPoint) e1;
            if (art.getParentOid()!=0)
                continue;
            count1++;
            db.mongoDB.remove(art);
            }
        return "GPS: удалено "+count1+" записей\n";
        }
    public String testArtifacts() throws UniException {
        int count=0;
        EntityList<Entity> zz = db.mongoDB.getAll(new Artifact());
        for(Entity ent : zz){
            Artifact art = (Artifact)ent;
            String dir = db.dataServerFileDir() + "/"+art.type()+"_"+art.directoryName();
            String ss = dir +"/"+art.createArtifactFileName();
            File ff = new File(ss);
            boolean fail = !ff.exists();
            if (fail) count++;
            art.setFileLost(fail);
            art.setFileSize(fail ? 0 : ff.length());
            db.mongoDB.update(art);
        }
        return "Всего "+zz.size()+" артефактов, отсутствуют "+count+" файлов";
    }
    //------------------ Сбор мусора ------------------------------------------------------------
    public String removeNonArchiveArtifacts() throws UniException {
        EntityList<Entity> list = db.mongoDB.getAll(new ReportFile(),ValuesBase.GetAllModeActual,1);
        int count1=0;
        int count2=0;
        for(Entity e1 :list){
            ReportFile rf = (ReportFile)e1;
            if (rf.isArchive())
                continue;
            count1++;
            Artifact art = rf.getArtifact().getRef();
            if (db.files.deleteArtifactFile(art))
                count2++;
            db.mongoDB.remove(art);
            db.mongoDB.remove(rf);
        }
        return "Отчеты: удалено "+count1+" записей, "+count2+" файлов\n";
    }
    public String collectGarbage() throws UniException {
        String out = createEntityBacks();
        out += removeNonArchiveArtifacts();
        out += squeezyArtifacts();
        out += squeezyAddress();
        out += squeezyGPS();
        return out;
        }
    private String clearTable(String className) throws Exception{
        String ss =" Удалено "+className+" "+db.common.getEntityNumber(className)+"\n";
        db.mongoDB.clearTable(className);
        return ss;
        }
    public String copyAccounts() throws UniException {
        EntityList out = db.mongoDB.getAll(new User());
        db.mongoDB.clearTable("Account");
        for(Object ent : out){
            User  user =  (User)ent;
            Account data = new Account();
            data.load(user.getAccount());
            long id = db.mongoDB.add(data);
            user.getAccountData().setOid(id);
            db.mongoDB.update(user);
            }
        return "Скопировано аккаунтов "+out.size();
        }
    public String clearContent() throws Exception {
        String out="";
        out += clearTable("PaymentItem");
        out += clearTable("Shift");
        out += clearTable("Maintenance");
        out += clearTable("MaintenanceJob");
        out += clearTable("Problem");
        out += clearTable("StatePoint");
        out += clearTable("BKOperation");
        out += clearTable("DefectSheet");
        out += clearTable("PaymentOrder");
        out += clearTable("ReportFile");
        return out;
        }
    RouteWrap apiSetCashMode = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            if (!db.users.isOnlyForSuperAdmin(req,res))
                return null;
            ParamBoolean on = new ParamBoolean(req,res,"mode");
            if (!on.isValid())
                return null;
            db.mongoDB.setCashOn(on.getValue());
            return new JEmpty();
        }};
    RouteWrap apiReopenLogFile = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            if (!db.users.isOnlyForSuperAdmin(req,res))
                return null;
            db.openLogFile();
            return new JEmpty();
        }};
    RouteWrap apiGetFileList = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            if (!db.users.isOnlyForSuperAdmin(req,res))
                return null;
            ParamString dirName = new ParamString(req,res,"folder");
            if (!dirName.isValid())
                return null;
            File ff = new File(db.dataServerFileDir()+"/"+dirName.getValue());
            if (!ff.exists()){
                db.createHTTPError(res,ValuesBase.HTTPNotFound,"Каталог не найден "+dirName.getValue());
                return null;
                }
            if (!ff.isDirectory()){
                db.createHTTPError(res,ValuesBase.HTTPNotFound,"Это не каталог "+dirName.getValue());
                return null;
                }
            String[] files = ff.list(new FilenameFilter() {
                @Override public boolean accept(File folder, String name) {
                    return true;
                    }
                });
            StringList out = new StringList();
            for(String ss : files)
                out.add(ss);
            return out;
        }};
    //-------------------------------------------------------------------------------------------------------------------
    public ErrorList copyDB(int port){
        ErrorList errorList = new ErrorList();
        if (port== db.port){
            errorList.addError("Недопустимое копирование "+port+" -> "+port);
            return errorList;
            }
        I_MongoDB outDB = new JDBCFactory().getDriverByIndex(ValuesBase.MongoDBType36);
        try {
            if (!outDB.openDB(port)){
                errorList.addError("Ошибка открытия выходной БД");
                return errorList;
                }
            } catch (UniException e) {
                errorList.addError("Ошибка открытия выходной БД: "+e.toString());
                return errorList;
                }
        try {
            outDB.clearDB();
            ArrayList<TableItem> olist = ValuesBase.EntityFactory().classList(true);
            for(TableItem item : olist){
                if (!item.isTable)
                    continue;
                try {
                    Entity ent = (Entity) item.clazz.newInstance();
                    outDB.dropTable(ent);
                    ArrayList<Entity> list = db.mongoDB.getAll(ent,ValuesBase.GetAllModeTotal,0);
                    for(Entity entity : list)
                        outDB.add(entity,0,true);
                    } catch (Exception ee){
                        errorList.addError("Не могу экспортировать "+item.name+"\n"+ee.toString());
                        }
                    }
            } catch (UniException e) {
                String ss = "Ошибка копирования: "+e.toString();
                System.out.println(ss);
                return errorList;
                }
        errorList.addInfo("БД скопирована " +db.port+" -> " +port);
        return errorList;
        }
    //------------------------------------------------------------------------------------------------------------------
    RouteWrap apiCloneDB = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            String out = "";
            if (!db.users.isOnlyForSuperAdmin(req, res))
                return null;
            ParamInt port = new ParamInt(req,res,"port");
            if (!port.isValid())
                return null;
            ErrorList errorList = copyDB(port.getValue());
            return errorList;
            }
        };
    }
