package romanow.abc.dataserver.ftp;

import romanow.abc.core.constants.ValuesBase;

import java.io.*;
import java.net.Socket;

public class ServerFileWriter extends Thread{
    private String fileDir="";
    private Socket sk=null;
    private DataInputStream is=null;
    private FileOutputStream out=null;
    private ServerFileAcceptor parent;
    private String fname="";
    public String getFileName(){ return fname; }
    public ServerFileWriter(String fileDir0, Socket sk0, ServerFileAcceptor par){
        fileDir = fileDir0;
        sk = sk0;
        parent = par;
        start();
        }
    public void close(){
        try {
            is.close();
            sk.close();
            if (out!=null)
                out.close();
            is=null;
            sk=null;
            out=null;
            synchronized (parent){
                parent.readers.remove(this);
                }
            } catch (IOException e) { parent.getServer().sendBug("ServerFileWriter",e);}
        }
    @Override
    public void run() {
            try {
            is = new DataInputStream(sk.getInputStream());
            String pass = is.readUTF();
            //if (!parent.getServer().isTokenValid(pass)){
            if (!pass.equals(ValuesBase.superUser.getPassword())){
                close();
                return;
                }
            String fname = is.readUTF();
            long fileSize = is.readLong();
            long sz0 = fileSize;
            String path = fileDir + "/"+fname;
            File ff = new File(path);
            out = new FileOutputStream(ff);
            System.out.println("Выгрузка файла "+fname+" ["+fileSize+"]");
            InputStream iss = sk.getInputStream();
            int sz = ValuesBase.FileBufferSize;
            byte bb[] = new byte[sz];
            int oldProc=0;
                while (fileSize != 0){
                    int cnt = iss.available();
                    if (cnt > sz){
                        iss.read(bb);
                        out.write(bb);
                        fileSize-=sz;
                        }
                    else{
                        iss.read(bb,0,cnt);
                        out.write(bb,0,cnt);
                        fileSize-=cnt;
                        }
                    int proc = (int)((sz0-fileSize)*100/sz0);
                    if (proc >= oldProc+10){
                        System.out.println(proc+" %");
                        oldProc = proc;
                        }
                    }
                out.flush();
                close();
                System.out.println("Файл выгружен "+fname+" ["+sz0+"]");
            } catch(Exception ee){
                parent.getServer().sendBug("ServerFileWriter",ee);
                close();
                }
        }
}
