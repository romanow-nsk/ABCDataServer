package romanow.abc.dataserver.ftp;

import romanow.abc.dataserver.DataServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerFileAcceptor extends Thread{
    ArrayList<ServerFileReader> readers=new ArrayList<>();
    private ServerSocket sk;
    private int port;
    private String fileDir="";
    private boolean shutdown=false;
    private DataServer db;
    public ArrayList<String> gerFileList(){
        ArrayList<String> out = new ArrayList<>();
        for (ServerFileReader ss : readers)
            out.add(ss.getFileName());
        return out;
        }
    public DataServer getServer(){ return db; }
    public ServerFileAcceptor(DataServer db0, String fileDir0, int port0) {
        fileDir = fileDir0;
        port = port0;
        db=db0;
        start();
        }
    public void run(){
        try {
            sk = new ServerSocket(port+1);
            while(!shutdown){
                Socket ss = sk.accept();
                System.out.println("ServerFileAcceptor: connect from "+sk.getLocalSocketAddress().toString());
                new ServerFileWriter(fileDir, ss,this);
                }
            }catch (Exception ee){
                db.sendBug("ServerFileAcceptor",ee);
                }
        }
    public void shutdown(){
        try {
            ArrayList<ServerFileReader> tmp = new ArrayList<>();
            for(ServerFileReader ss : readers)
                tmp.add(ss);
            for(ServerFileReader ss : tmp)
                ss.close();
                sk.close();
            } catch (IOException e) {}
    }

}
