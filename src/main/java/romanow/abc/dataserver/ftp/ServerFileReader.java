package romanow.abc.dataserver.ftp;

import romanow.abc.core.constants.ValuesBase;
import romanow.abc.dataserver.DataServer;

import java.io.*;
import java.net.Socket;

public class ServerFileReader extends Thread{
    private String fileDir="";
    private Socket sk;
    private DataInputStream is;
    private DataOutputStream os;
    private ServerFileAcceptor parent;
    private DataServer db;
    private String fname="";
    public String getFileName(){ return fname; }
    public  ServerFileReader(DataServer db0,String fileDir0, Socket sk0,ServerFileAcceptor par){
        fileDir = fileDir0;
        sk = sk0;
        parent = par;
        db = db0;
        start();
        }
    public void close(){
        try {
            os.close();
            is.close();
            sk.close();
            synchronized (parent){
                parent.readers.remove(this);
                }
            } catch (IOException e) {}
        }
    @Override
    public void run() {
            try {
            is = new DataInputStream(sk.getInputStream());
            os = new DataOutputStream(sk.getOutputStream());
            fname = is.readUTF();
            String path = fileDir + "/"+fname;
            File ff = new File(path);
            if (!ff.exists()){
                os.writeInt(ValuesBase.FTPFileNotFound);
                os.flush();
                close();
                return;
                }
            FileInputStream in = new FileInputStream(ff);
            int fileSize = (int)ff.length();
            long sz0 = fileSize;
            os.writeInt((int)sz0);
            System.out.println("Загрузка файла "+fname+" ["+fileSize+"]");
            int sz = ValuesBase.FileBufferSize;
            byte bb[] = new byte[sz];
            int oldProc=0;
                //IOUtils.copy(in, out);
                while (fileSize != 0){
                    int sz2 = fileSize > sz ? sz : fileSize;
                    fileSize -= sz2;
                    in.read(bb,0,(int)sz2);
                    os.write(bb,0,(int)sz2);
                    int proc = (int)((sz0-fileSize)*100/sz0);
                    if (proc >= oldProc+10){
                        if (db.traceMid())
                            System.out.println(proc+" %");
                        oldProc = proc;
                        }
                    }
                os.flush();
                in.close();
                close();
                System.out.println("Файл загружен "+fname+" ["+fileSize+"]");
            } catch(Exception ee){ close(); }
        }
}
