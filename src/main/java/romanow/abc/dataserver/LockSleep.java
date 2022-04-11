package romanow.abc.dataserver;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.locks.ReentrantLock;

public class LockSleep{
    public final static int LockSleepTime=20;
    public LockSleep(){}
    public LockSleep(boolean trace0){
        trace = trace0;
    }
    @Getter @Setter private boolean trace=false;
    private volatile boolean busy=false;
    public void lock(int idx){
        if (trace)
            System.out.println(idx+" lock+++ ");
        while(true){
            synchronized (this){
                if (!busy){
                    busy=true;
                    if (trace)
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
        if (trace)
            System.out.println(idx+" unlock");
        synchronized (this){
            busy=false;
            }
    }
}
