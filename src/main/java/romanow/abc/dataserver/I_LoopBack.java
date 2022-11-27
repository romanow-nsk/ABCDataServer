package romanow.abc.dataserver;

public interface I_LoopBack extends Runnable{
    public boolean onException(String ss,Exception ee);
    public void onFinish(String ss);
}
