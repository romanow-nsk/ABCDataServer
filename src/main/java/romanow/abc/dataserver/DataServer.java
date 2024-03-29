package romanow.abc.dataserver;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import romanow.abc.core.*;
import romanow.abc.core.API.RestAPIBase;
import romanow.abc.core.API.RestAPIFirstClient;
import romanow.abc.core.constants.ValuesBase;
import romanow.abc.core.dll.DLLModule;
import romanow.abc.core.entity.Entity;
import romanow.abc.core.entity.base.BugMessage;
import romanow.abc.core.entity.base.WorkSettingsBase;
import romanow.abc.core.entity.baseentityes.JInner;
import romanow.abc.core.entity.baseentityes.JString;
import romanow.abc.core.entity.users.User;
import romanow.abc.core.export.ExcelX;
import romanow.abc.core.jdbc.JDBCFactory;
import romanow.abc.core.mongo.*;
import romanow.abc.core.utils.OwnDateTime;
import romanow.abc.core.utils.Pair;
import romanow.abc.core.utils.StringFIFO;
import romanow.abc.dataserver.ftp.ServerFileAcceptor;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.ServerSocket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


// AJAX посылает post, а браузер - get
public class DataServer implements I_DataServer{
    //-------------------- Модель БД сервера --------------------------
    @Getter @Setter private boolean consoleLogEnable=true;
    private I_ServerState masterBack=null;          // Обработчик событий ServerState
    protected I_MongoDB mongoDB = new MongoDB36();            // Коннектор MongoDB
    public APIUser users = null;                    // API работы с пользователями
    public APIArtifact files = null;                // API работы с артефактами
    public APICommon common=null;                   // API общих функций сервера
    public APINotification notify=null;             // API уведомлений
    public APIAdmin admin = null;                   // API админитрирования
    private String dataServerFileDir="";            // Корневой каталог артефактов сервера
    int port;                                       // Номер порта
    private StringFIFO consoleLog = new StringFIFO(ValuesBase.ConsoleLogSize);
    private ServerFileAcceptor deployer=null;       // Приемник обновления через TCP
    SessionController sessions = null;              // Контроллер сессий
    private String debugToken = "";                 // Дежурный токен
    boolean objectTrace=false;                      // Трассировка содержимого запросов/ответов в лог
    private boolean shutDown = false;               // Признак завершения работы
    protected boolean isRun=false;                  // Признак - сервер работает
    private BufferedWriter logFile=null;            //
    private OwnDateTime logFileCreateDate;          // Время создания лог-файла
    private OwnDateTime logFileWriteDate;           // Время последней записи в лог-файл
    private DLLModule rootModule=new DLLModule();
    protected ClockController clock=null;           // Поток периодических операицй
    public ErrorCounter deviceErrors = new ErrorCounter();  // Счетчик повторных ошибок
    public int timeZoneHours=0;                     // Смещение часового пояса относительно сервера установки
    public RestAPIBase localService = null;
    public DataServer(){}
    public I_MongoDB mongoDB(){ return mongoDB; }
    public APICommon common(){ return common; }
    private CommandStringData data=null;
    private String gblEncoding="";
    private boolean utf8;
    protected LockSleep serverLock = new LockSleep(this);
    //-------------------------------------------------------------------------
    public WorkSettingsBase getWorksettings(boolean force){
        return common.getWorkSettings(force);
        }
    public StringFIFO getConsoleLog() {
        return consoleLog; }
    private void getAnswer(Process p){
        Thread tt = new Thread(){
            public void run() {
                try {
                    InputStreamReader br = new InputStreamReader(p.getInputStream(), "UTF-8");
                    while (!shutDown) {
                        int nn = br.read();
                        if (nn == -1) break;
                        System.out.print((char) nn);
                        }
                    br.close();
                    } catch (Exception e) { System.out.println("Mongo error " + e.toString()); }
                }
            };
        tt.setName("ConsoleLog");
        tt.start();
        }
    /*
    public void startMongo(){
        Runtime r =Runtime.getRuntime();
        Process p =null;
        try {
            p = r.exec(set.mongoStartCmd());
            p.waitFor();
            getAnswer(p);
            } catch(Exception ee){ System.out.println("Mongo is not started "+ee.toString());}
        }
    */
    public boolean traceMin(){
        return common.getWorkSettings().getTraceLevel()>=1;
        }
    public boolean traceMid(){
        return common.getWorkSettings().getTraceLevel()>=2;
        }
    public boolean traceMax(){
        return common.getWorkSettings().getTraceLevel()==3;
        }
    public void setMIMETypes(){
        //for(int i=0;i<ValuesBase.ArtifactExt.length;i++)
        //    spark.Spark.staticFiles.registerMimeType(ValuesBase.ArtifactExt[i],ValuesBase.ArtifactMime[i]);
        }
    public String getMongoNetwork(){
        return ValuesBase.mongoServerIP;
        }
    public String dataServerFileDir(){
        String dir =  dataServerFileDir+"/"+port;
        File path = new File(dir);
        if (!path.exists())
            path.mkdir();
        return dir;
        }
    public String rootServerFileDir(){
        String dir =  dataServerFileDir;
        File path = new File(dir);
        if (!path.exists())
            path.mkdir();
        return dir;
    }
    public I_ServerState serverBack = new I_ServerState() {        // Перехвать обратного вызова с установкой собственых
        @Override
        public void onStateChanged(ServerState serverState) {
            serverState.setServerRun(isRun);
            if (masterBack!=null)
                masterBack.onStateChanged(serverState);
            }
    };

    public boolean startServer(int port0, int mongoType, I_ServerState ss,boolean force){
        masterBack = ss;
        port = port0;
        System.out.println(System.getProperty("user.dir"));
        System.out.println(System.getProperty("os.name"));
        System.out.println("PID="+ Utils.getPID());
        mongoDB = new JDBCFactory().getDriverByIndex(mongoType);
        return restartServer(force);
        }
    public boolean startServer(CommandStringData data0,I_ServerState ss,boolean force){
        return startServer(data0,ss,force,null);
        }
    public boolean startServer(CommandStringData data0,I_ServerState ss,boolean force, Runnable finale){
        consoleLogEnable = true;
        data = data0;
        masterBack = ss;
        port = data.getPort();
        gblEncoding = System.getProperty("file.encoding");
        utf8 = gblEncoding.equals("UTF-8");
        final LogStream log = new LogStream(utf8, getConsoleLog(),new I_String() {
            @Override
            public void onEvent(String ss) {
                if (consoleLogEnable)
                    System.err.println(ss);
                addToLog(ss);
                }
            });
        System.setOut(new PrintStream(log));
        System.out.println(System.getProperty("user.dir"));
        System.out.println(System.getProperty("os.name"));
        System.out.println("PID="+ Utils.getPID());
        if (timeZoneHours!=0)
            System.out.println("Смещение часового пояса относительно сервера установки: "+timeZoneHours);
        mongoDB = null;
        String dbType = data.getDbase();
        if (dbType!=null){
            mongoDB = new JDBCFactory().getDriverByName(dbType);
            if (mongoDB==null)
                System.out.println("Не найдет тип БД: "+dbType);
            }
        if (mongoDB==null){
            mongoDB = new MongoDB36();
            }
        System.out.println("Установлен тип БД: "+mongoDB.getDriverName());
        String newtwork = data.getNetwork();
        System.out.println("Установлен тип сети: "+(newtwork==null ? "localhost" : newtwork));
        if (newtwork!=null)
            mongoDB.setNetwork(newtwork);
        boolean bb =  restartServer(force);
        if (finale!=null)
            finale.run();
        boolean dd = consoleLogEnable;
        if (data.hasConsoleLog())
            dd = data.isConsoleLogEnable();
        System.out.println("Вывод лога в консоль: "+(dd ? "разрешен" : "запрещен"));
        consoleLogEnable = dd;
        return bb;
        }
    public void setLoggers(){
        //-------------- Уровни логирования не влияют на вывод в консоль -----------------------------
        Level levelGlobal = Level.OFF;
        //-------------- Уровни логирования не влияют на вывод в консоль -----------------------------
        Logger logger =java.util.logging.Logger.getGlobal();
        logger.setLevel(levelGlobal);
        System.out.println("Level global: "+logger.getLevel());
        org.apache.logging.log4j.Logger logger2 = LogManager.getLogger(Logger.class);
        //logger2.lesetLevel(levelApache);
        System.out.println("Level apache.log4j: "+logger2.getLevel());
        logger2 = LogManager.getLogger(org.slf4j.Logger.class);
        //logger2.setLevel(levelApache);
        System.out.println("Level slf4j: "+logger2.getLevel());
        logger2 = LogManager.getLogger(org.eclipse.jetty.util.log.Logger.class);
        //logger2.setLevel(levelApache);
        System.out.println("Level jetty: "+logger2.getLevel());
        logger2 = LogManager.getRootLogger();
        //logger2.setLevel(levelApache);
        System.out.println("Root Logger: "+logger2.getName()+":"+logger2.getLevel());
        }
    public boolean restartServer(boolean force){
        //((ch.qos.logback.classic.Logger)LogManager.getLogger(ch.qos.logback.classic.Logger.class)).setLevel(WARN_INT);
        //--------------------------------------------------------------------------------------------
        try {
            if (!mongoDB.openDB(port)){
                System.out.println("MongoDB: БД не открыта");
                return false;
                }
            } catch (UniException e) {
                System.out.println("MongoDB: БД не открыта, "+e.toString());
                return false;
                }
        setMIMETypes();
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            serverSocket.close();
            } catch (Exception ee){
                System.out.println("Ошибка порта Spark: "+ee.toString());
                return false;
                }
        spark.Spark.port(port);
        spark.Spark.threadPool(ValuesBase.SparkThreadPoolSize);
        spark.Spark.staticFiles.location("/public");                            // Обязательно
        spark.Spark.notFound(new Route() {
            @Override
            public Object handle(Request request, Response response) throws Exception {
                String uri = "public"+request.raw().getRequestURI();
                System.out.println("Файл:"+uri);
                HttpServletResponse res = response.raw();
                OutputStream out = res.getOutputStream();
                InputStream in = getClass().getClassLoader().getResourceAsStream(uri);
                if (in==null){
                    res.sendError(ValuesBase.HTTPNotFound,"File "+uri+" не найден");
                    response.body("");
                    return null;
                    }
                ByteArrayOutputStream temp = new ByteArrayOutputStream();
                int cc = 0;
                while ((cc=in.read())!=-1)
                    temp.write(cc);
                temp.flush();
                byte bb[] = temp.toByteArray();
                temp.close();
                response.raw().setContentLengthLong(bb.length);
                out.write(bb);
                in.close();
                out.close();
                return null;
                }
            });
        spark.Spark.staticFiles.externalLocation("/public");            // Не понятно
        spark.Spark.staticFileLocation("/public");                            // Не понятно
        spark.Spark.externalStaticFileLocation("/public");              // Не понятно
        //https://gist.github.com/zikani03/7c82b34fbbc9a6187e9a CORS для Spark
        spark.Spark.before("/*", (request,response)->{
            // Note: this may or may not be necessary in your particular application
            response.type("application/json");
            response.header("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Headers", "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin,");
            response.header("Access-Control-Allow-Credentials", "true");
            });
         spark.Spark.options("/*",
                 (request, response) -> {
                     String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
                     if (accessControlRequestHeaders != null) {
                         response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
                        }
                     String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
                     if (accessControlRequestMethod != null) {
                         response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
                        }
                     return "OK";
                 });
         /*
        spark.Spark.options("/*", (request,response)-> {
                    // - старая рекомедация для стороннего доступа от web
                    String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
                    if (accessControlRequestHeaders != null) {
                        response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
                        }
                    String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
                    if (accessControlRequestMethod != null) {
                        response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
                        }

            response.header("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Headers", "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin,");
            response.header("Access-Control-Allow-Credentials", "true");
            response.header("Content-Type","application/json");
            return "OK";
            });
        */
        //------------------ Обработка запросов на прямое чтение файлов из каталога артефактов экземпляра сервера
        spark.Spark.get("/file/*", new Route() {
                    @Override
                    public Object handle(Request request, Response response) throws Exception {
                        System.out.println("Файл:"+request.splat()[0]);
                        HttpServletResponse res = response.raw();
                        OutputStream out = res.getOutputStream();
                        String fname = dataServerFileDir()+"/"+request.splat()[0];
                        File ff = new File(fname);
                        if (!ff.exists()){
                            res.sendError(ValuesBase.HTTPNotFound,"File "+fname+" не найден");
                            response.body("");
                            return null;
                            }
                        FileInputStream in = new FileInputStream(ff);
                        long sz = ff.length();
                        response.raw().setContentLengthLong(sz);
                        while (sz--!=0)
                            out.write(in.read());
                        in.close();
                        out.close();
                        return null;
                        }
                });
         //------------------ Обработка запросов на прямое чтение файлов из каталога ресурсов
         /*
         spark.Spark.get("/web/*", new Route() {
             @Override
             public Object handle(Request request, Response response) throws Exception {
                 String fname="public/"+request.splat()[0];
                 System.out.println("Файл:"+fname);
                 HttpServletResponse res = response.raw();
                 OutputStream out = res.getOutputStream();
                 InputStream in = getClass().getClassLoader().getResourceAsStream(fname);
                 if (in==null){
                     res.sendError(ValuesBase.HTTPNotFound,"File "+fname+" не найден");
                     response.body("");
                     return null;
                    }
                 ByteArrayOutputStream temp = new ByteArrayOutputStream();
                 int cc = 0;
                 while ((cc=in.read())!=-1)
                     temp.write(cc);
                 temp.flush();
                 byte bb[] = temp.toByteArray();
                 temp.close();
                 response.raw().setContentLengthLong(bb.length);
                 out.write(bb);
                 in.close();
                 out.close();
                 return null;
             }
         });
          */
        //-------------------------------------------------------------------------------------------------
        /*
        spark.Spark.before("/file/*", new Filter() {
            @Override
            public void handle(Request request, Response response) throws Exception {
                System.out.println("Файл:"+request.splat()[0]);
                HttpServletResponse res = response.raw();
                OutputStream out = res.getOutputStream();
                String fname = dataServerFileDir()+"/"+request.splat()[0];
                File ff = new File(fname);
                if (!ff.exists()){
                    res.sendError(ValuesBase.HTTPNotFound,"File "+fname+" не найден");
                    response.body("");
                    return;
                    }
                FileInputStream in = new FileInputStream(ff);
                long sz = ff.length();
                response.raw().setContentLengthLong(sz);
                while (sz--!=0)
                    out.write(in.read());
                in.close();
                out.close();
                }
            });
         */
        //-------------------------------------------------------------------------------------
        common = new APICommon(this);
        WorkSettingsBase ws = ValuesBase.env().currentWorkSettings();
        try {
            ws = common.getWorkSettings(true);
            dataServerFileDir = ws.isDataServerFileDirDefault() ? System.getProperty("user.dir") : ws.getDataServerFileDir();
            } catch (Exception ee){
                System.out.println("WorkSettings is not read: "+ee.toString());
                if (!force) return false;
                try {
                    mongoDB.clearTable(ws.getClass().getSimpleName());
                    mongoDB.add(ws);
                    } catch (UniException ex){
                        Utils.printFatalMessage(ex);
                        }
        }
        //--------------------------------------------------------------------------------
         sessions = new SessionController(this);
         deployer = new ServerFileAcceptor(this,dataServerFileDir(),port);
        //---------------------------------------------------------------------------------
        users = new APIUser(this);
        files = new APIArtifact(this);
        notify = new APINotification(this);
        admin = new APIAdmin(this);
        debugToken = sessions.createContext(0,ValuesBase.env().superUser());          // Вечный ключ для отладки
        isRun=true;
        try {
            changeServerState(new I_ChangeRecord() {
                @Override
                public boolean changeRecord(Entity ent) {
                    ServerState state =(ServerState)ent;
                    state.init();
                    return true;
                    }
                });
            } catch (UniException e) { System.out.println("StartServer: "+e.toString());}
        openLogFile();
        deleteOldLogFiles();
        //-------------------------------------- Загрузка корневого модуля -----------------------------
        ServerJarClassLoader loader = new ServerJarClassLoader(true,this);
        loader.loadClasses();
        Pair<String, DLLModule> list = loader.getClassesList(ValuesBase.env().applicationClassName(ValuesBase.ClassNameEnvironment));
        System.out.println("Загрузка корневого модуля");
        if (list.o1!=null)
             System.out.println(list.o1);
        System.out.println(list.o2);
        //----------------------------------------------------------------------------------------------
        createEvent(ValuesBase.EventSystem,ValuesBase.ELInfo,"Старт сервера","");
         try {
            localService = RestAPIFirstClient.startClient("localhost",""+port);
            } catch (UniException e) {
                System.out.println("Локальный клиент 1: "+e.toString());
                }
         clock = new ClockController(this);
         setLoggers();
         onStart();
         return true;
        }
    public void addToLog(String ss){
        if (!isRun)
            return;
        consoleLog.add(ss);
        writeToLogFile(ss);
        }
    public void shutdown(){
        if (!isRun) return;
        createEvent(ValuesBase.EventSystem,ValuesBase.ELInfo,"Останов сервера","");
        clock.shutdown();
        shutDown=true;
        spark.Spark.stop();
        spark.Spark.awaitStop();
        mongoDB.closeDB();
        if (deployer!=null)
            deployer.shutdown();
        if (sessions!=null)
            sessions.shutdown();
        isRun=false;
        serverBack.onStateChanged(common.getServerStateRight());
        onShutdown();
        closeLogFile();
        }
    public void setObjectTrace(boolean objectTrace) {
        this.objectTrace = objectTrace;
        }
    public String toJSON(Object ent, Request req, RequestStatistic statistic){
        String ss = req.queryString();
        if (!(ent instanceof JInner)){
            ss = "<-----"+req.ip()+req.pathInfo()+" "+req.requestMethod()+" "+(ss == null ? "" : ss);
            if (statistic.startTime!=-1){
                long dd = System.currentTimeMillis()-statistic.startTime;
                ss += " time="+(dd)+" мс";
                common.addTimeStamp(dd);
                }
            ss += " объектов "+statistic.entityCount+" ("+statistic.recurseCount+")";
            System.out.println(ss);
            }
        try {
            String out = new Gson().toJson(ent);
            if (objectTrace)
                System.out.println("--->"+ent.getClass().getSimpleName()+":"+out);
            return out;
        } catch (Exception ee){
            System.out.println("Ошибка JSON: "+ee.toString());
            return "{}";
        }
    }
    public String  traceRequest(Request req){
        String ss = "<----"+req.ip()+req.pathInfo()+" "+req.requestMethod()+" "+req.queryString()+"\n";
        Set<String> rr = req.headers();
        for (String zz : rr)
            ss += "header:"+zz+"="+req.headers(zz)+"\n";
        Map<String,String> qq = req.cookies();
        for (String zz : qq.keySet())
            ss += "qookie:"+zz+"="+qq.get(zz)+"\n";
        if (req.body().length()!=0)
            ss += req.body()+"\n";
        System.out.print(ss);
        return ss;
        }
    public long canDo(Request req, Response res) throws IOException {
        return canDo(req, res,true);
    }
    public long canDo(Request req, Response res,boolean testToken) throws IOException {
        if (!mongoDB.isOpen()){
            createHTTPError(res,ValuesBase.HTTPServiceUnavailable, "Database not open");
            return 0;
            }
        //res.header("Access-Control-Allow-Origin","*");
        //res.header("Access-Control-Allow-Methods", "GET");
        //res.type("application/json");
        //String ss = req.queryString();
        //System.out.println("<-----"+req.ip()+req.pathInfo()+" "+req.requestMethod()+" "+(ss == null ? "" : ss));
        if (objectTrace)
            traceRequest(req);
        boolean bb =  !testToken ? true : getSession(req,res)!=null;
        return bb ? System.currentTimeMillis() : 0;
        }
    //------------------- КЛючи сессий --------------------------------------------
    public boolean isAdmin(Request req,Response res) throws IOException {
        UserContext uu = getSession(req,res,false);
        if (uu==null)
            return false;
        int type = uu.getUser().getTypeId();
        if (type==ValuesBase.UserAdminType || type==ValuesBase.UserSuperAdminType)
                return true;
        return false;
        }
    public boolean isPrivilegedUser(Request req,Response res) throws IOException {
        UserContext uu = getSession(req,res,false);
        if (uu==null)
            return false;
        int type = uu.getUser().getTypeId();
        if (type==ValuesBase.UserAdminType || type==ValuesBase.UserSuperAdminType)
            return true;
        return false;
    }
    public boolean isSomeUser(Request req,Response res,User user) throws IOException {
        UserContext uu = getSession(req,res,false);
        if (uu==null)
            return false;
        int type = uu.getUser().getTypeId();
        if (type==ValuesBase.UserAdminType || type==ValuesBase.UserSuperAdminType)
            return true;
        return uu.getUser().getOid() == user.getOid();
        }
    public UserContext getSession(Request req,Response res) throws IOException {
        return getSession(req,res,true);
        }
    public UserContext getSession(Request req,Response res,boolean answer) throws IOException {
        String val = req.headers(ValuesBase.SessionHeaderName);
        if (val==null){
            if (answer)
                createHTTPError(res,ValuesBase.HTTPAuthorization, "SessionToken not send");
            return null;
            }
        UserContext ctx = sessions.getContext(val);
        if (ctx==null){
            if (answer)
                createHTTPError(res,ValuesBase.HTTPAuthorization, "SessionToken не найден (too old)");
            return null;
            }
        ctx.wasCalled();            // Отметить время обращения
        return ctx;
        }
    public String createSessionToken(User user){
        return sessions.createContext(ValuesBase.SessionSilenceTime,user);
        }
    public String getDebugToken(){ return  debugToken; }
    public void closeSession(String key){ sessions.removeContext(key);}
    //-------------------------------------------------------------------------------
    /*
    public void loadSettings(){
        try {
            set = (Settings)load(Settings.class);
            } catch (UniException e) {
                System.out.println("Настройки не загружены, сброс с исходные:" +e.toString());
                set = new Settings();
                saveSettings();
                }
        }
    public void saveSettings(){
        try {
            save(set);
            } catch (UniException e) { System.out.println("Настройки не сохранены:" +e.toString()); }
        }
    */
    public void save(Object entity) throws UniException {
        try {
            Gson gson = new Gson();
            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(entity.getClass().getSimpleName()+".json"), "UTF-8");
            gson.toJson(entity,out);
            out.flush();
            out.close();
            } catch (Exception ee){ throw UniException.io(ee); }
        }
    public Object load(Class entity) throws UniException {
        try {
            Gson gson = new Gson();
            InputStreamReader out = new InputStreamReader(new FileInputStream(entity.getSimpleName()+".json"), "UTF-8");
            Object ent = new Gson().fromJson(out,entity);
            out.close();
            return ent;
            } catch (Exception ee){ throw UniException.io(ee); }
        }
    public boolean exportToExcel(ExcelX ex) {
        return admin.exportToExcel(ex);
        }
    public String clearDB() throws UniException {
        return mongoDB.clearDB();
        }
    public ServerState getServerState(){
        return common.getServerState();
        }
    public void changeServerState(I_ChangeRecord todo) throws UniException {
        common.changeServerState(todo);
        serverBack.onStateChanged(getServerState());
        }

    public void delayInGUI(final int sec,final Runnable code){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000*sec);
                    //java.awt.EventQueue.invokeLater(code);
                    code.run();
                } catch (InterruptedException e) {}
            }
        }).start();
    }
    //----------------------------------------------------------------------------------------------------------------
    public void createHTTPError(Response res, int code, String mes){
        funCreateHTTPError(res,code,mes);
        }
    //----------------------------------------------------------------------------------------------------------------
    public static void  funCreateHTTPError(Response res, int code, String mes){
        res.status(code);
        res.raw().setCharacterEncoding("utf-8");
        res.body(mes);
        System.out.println("HTTP: "+code+" "+mes);
        }
    public boolean isTokenValid(String token){
        return sessions.isTokenValid(token);
        }
    //---------------------------------------------------------- Поддержка log-файла -----------------
    synchronized public void openLogFile(){
        if (logFile!=null){
            closeLogFile();
            }
        logFileCreateDate = new OwnDateTime();
        logFileWriteDate =  new OwnDateTime();
        logFileWriteDate.onlyDate();
        String fname = dataServerFileDir()+"/log";
        File dir = new File(fname);
        if (!dir.exists()){
            dir.mkdir();
            }
        fname+="/"+logFileCreateDate.toString2()+".log";
        try {
            logFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fname),"Windows-1251"));
            } catch (Exception e) {}
        }
    synchronized public void closeLogFile(){
        if (logFile!=null){
            try {
                logFile.flush();
                logFile.close();
                } catch (IOException e) {}
            logFile=null;
            }
        }
    synchronized public void deleteOldLogFiles(){
        OwnDateTime cData = new OwnDateTime();
        String dname = dataServerFileDir()+"/log";
        WorkSettingsBase ws = common.getWorkSettings();
        int depth = ws.getLogDepthInDay();
        if (depth==0)
            return;
        int cnt=0;
        long diff = depth * 24 * 60 * 60 *1000;
        File ff = new File(dname);
        if (!ff.isDirectory())
            return;
        String ss[] = ff.list();
        for(String fname : ss){
            File ff2 = new File(dname+"/"+fname);
            if (!ff2.isFile())
                continue;
            long vv = ff2.lastModified();
            if (vv==0)
                continue;
            OwnDateTime dd = new OwnDateTime(vv);
            if (cData.timeInMS()-vv > diff){
                System.out.println("Удаление лог-файла: "+fname);
                ff2.delete();
                cnt++;
                }
            }
        if (cnt!=0)
            System.out.println("Удалено "+cnt+" лог-файлов");
        }
    synchronized public void writeToLogFile(String ss){
        if (logFile==null)
            return;
        OwnDateTime dd = new OwnDateTime();
        dd.onlyDate();
        if (!dd.equals(logFileWriteDate)){
            closeLogFile();
            openLogFile();
            deleteOldLogFiles();
            }
        try {
            logFile.write(ss);
            logFile.newLine();
            } catch (IOException e) {}
        }
    public void sendBug(String module,Exception ex){
        try {
            String err = Utils.createFatalMessage(ex);
            System.out.println(err);
            mongoDB.add(new BugMessage(module+":\n"+err));
            createEvent(ValuesBase.EventSystem,ValuesBase.ELError,"Программная ошибка",err);
            } catch (UniException e) {
                System.out.println(Utils.createFatalMessage(e));
                }
        }
    public void sendBug(String module,String err){
        try {
            System.out.println(err);
            mongoDB.add(new BugMessage(module+":\n"+err));
            createEvent(ValuesBase.EventSystem,ValuesBase.ELError,"Программная ошибка",err);
            } catch (UniException e) {
                System.out.println(Utils.createFatalMessage(e));
                }
        }
    //---------------------------------------------------------------------------------------------------------
    public Pair<RestAPIBase,String> startOneClient(String ip, String port) throws Exception {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(ValuesBase.HTTPTimeOut, TimeUnit.SECONDS)
                .connectTimeout(ValuesBase.HTTPTimeOut, TimeUnit.SECONDS)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://"+ip+":"+port)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        RestAPIBase service = (RestAPIBase)retrofit.create(RestAPIBase.class);
        JString ss = new APICallServer<JString>(){
            @Override
            public Call<JString> apiFun() {
                return service.debugToken(ValuesBase.DebugTokenPass);
                }
            }.call(service);
        return new Pair<>(service,ss.getValue());
        }
    //----------------------------  Счетчик ошибок ПЛК ----------------------------------
    // TODO - обработка ошибок
    public long createEvent(int type,int level,String title, String comment){
        return createEvent(type,level,title,comment,0);
        }
    @Override
    public long createEvent(int type,int level,String title, String comment,long artId){
        return 0; }
    @Override
    public void onClock() {}
    @Override
    public void onStart() {}
    @Override
    public void onShutdown(){}

    public static void main(String argv[]){
        new DataServer();
        }
}
