package romanow.abc.dataserver;

public interface I_DataServer {
    public long createEvent(int type,int level,String title, String comment,long artId);
    public void onClock();
    public void onStart();
    public void onShutdown();
    public String dataServerFileDir();
    }
