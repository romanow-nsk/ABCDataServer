package romanow.abc.dataserver;

import romanow.abc.core.*;
import romanow.abc.core.API.RestAPIBase;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import romanow.abc.core.constants.ValuesBase;
import romanow.abc.core.export.Excel;
import romanow.abc.core.export.ExcelX;
import romanow.abc.core.export.I_Excel;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

public class ConsoleServer {
    protected I_DBTarget dbTarget;
    protected Class apiFace;
    private int lineCount=0;
    private String gblEncoding="";
    private boolean utf8;
    private DataServer dataServer = new DataServer();
    private I_ServerState serverBack = new I_ServerState() {
        @Override
        public void onStateChanged(ServerState serverState) {
            dataServer.delayInGUI(1,new Runnable() {
                public void run() {
                    System.out.println(serverState.toString());
                }
            });
        }
    };
    //---------------------------------------------------------------------
    private I_EmptyEvent asteriskBack = new I_EmptyEvent() {
        @Override
        public void onEvent() {
                System.out.println(""+dataServer.getServerState().getLastMailNumber());
        }
    };
    public ConsoleServer(){
        ValuesBase.init();
        dbTarget = new DBExample();
        apiFace = RestAPIBase.class;
        }
    public ConsoleServer(I_DBTarget target, Class apiFace0){
        apiFace = apiFace0;
        dbTarget = target;
        }
    private int port;
    public void setTarget(){
        Retrofit retrofit=null;
        RestAPIBase service=null;
        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(ValuesBase.HTTPTimeOut, TimeUnit.SECONDS)
                .connectTimeout(ValuesBase.HTTPTimeOut, TimeUnit.SECONDS)
                .build();
        retrofit = new Retrofit.Builder()
                .baseUrl("http://localhost:"+port)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        service = (RestAPIBase) retrofit.create(apiFace);
        dbTarget.createAll(service, ValuesBase.DebugTokenPass);
        }
    public void startServer(int port0,boolean init){
        port = port0;
        dataServer.startServer(port, ValuesBase.MongoDBType36, serverBack,(init));
        gblEncoding = System.getProperty("file.encoding");
        utf8 = gblEncoding.equals("UTF-8");
        asteriskBack.onEvent();
        final LogStream log = new LogStream(utf8, dataServer.getConsoleLog(), new I_String() {
            @Override
            public void onEvent(String ss) {
                dataServer.addToLog(ss);
                }
            });
        if (init){
            Thread tt = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(10);
                        } catch (InterruptedException e) {}
                    setTarget();
                    System.setOut(new PrintStream(log));
                    System.setErr(new PrintStream(log));
                    }
                });
            tt.setName("ConsoleServer");
            tt.start();
            }
        else {
            System.setOut(new PrintStream(log));
            System.setErr(new PrintStream(log));
            }
        }

    public static void main(String args[]) {
        String ss[]= {"port:4569","conf:BARS","iec61850:0"};
        if (args.length!=0)
            ss = args;
        CommandStringData data = new CommandStringData();
        data.parse(ss);
        ErrorList errors = data.getErrors();
        if (!errors.valid()){
            System.out.println("Ошибки командной строки:\n"+errors.toString());
            return;
            }
        System.out.println("Порт="+data.getPort());
        if (data.hasConf())
            System.out.println("Конфигурация="+data.getConf());
        ConsoleServer server = new ConsoleServer();
        if(data.hasUser())
            ValuesBase.env().superUser().setLoginPhone(data.getUser());
        if (data.hasPass())
            ValuesBase.env().superUser().setPassword(data.getPass());
        if (!data.hasImport())
            server.startServer(data.getPort(), data.isInit());
        else{
            server.startServer(data.getPort(), false);
            try {
                DataServer db = server.dataServer;
                String fname = data.getImportXLS();
                I_Excel xls = fname.endsWith("xlsx") ? new ExcelX() : new Excel();
                db.mongoDB.clearDB();
                fname = System.getProperty("user.dir")+"/"+fname;
                System.out.println("Импорт БД из: "+fname);
                String zz = xls.load(fname,db.mongoDB);
                System.out.println(zz);
                db.shutdown();
                try {
                    Thread.sleep(5*1000);
                } catch (InterruptedException e) {}
                server.startServer(data.getPort(), data.isInit());
            } catch (UniException e) {
                System.out.println("Ошибка импорта БД: "+e.toString());
            }
        }
    }
    }
