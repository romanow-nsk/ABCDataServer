package romanow.abc.dataserver;

public class LockSleep{
    public final static int LockSleepTime=20;
    public LockSleep(){}
    public LockSleep(DataServer db0){
        db = db0;
        }
    private DataServer db;
    private volatile boolean busy=false;
    public void lock(int idx){
        if (db.traceMax())
            System.out.println(idx+" lock+++ ");
        while(true){
            synchronized (this){
                if (!busy){
                    busy=true;
                    if (db.traceMax())
                        System.out.println(idx+" lock--- ");
                    return;
                    }
                }
                try {
                    Thread.sleep(LockSleepTime);
                    } catch (InterruptedException e) {}
            }
        }
    public void unlock(int idx){
        if (db.traceMax())
            System.out.println(idx+" unlock");
        synchronized (this){
            busy=false;
            }
    }
}
