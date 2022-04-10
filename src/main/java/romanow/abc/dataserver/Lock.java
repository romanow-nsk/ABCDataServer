package romanow.abc.dataserver;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.locks.ReentrantLock;

public class Lock extends ReentrantLock {
    public Lock(){}
    public Lock(boolean trace0){
        trace = trace0;
    }
    @Getter @Setter private boolean trace=false;
    private volatile int synchCounter=0;
    public void lock(int idx){
        //if (trace)
        //    System.out.println(idx+" lock+++ ");
        //super.lock();
        //if (trace)
        //    System.out.println(idx+" lock--- ");
        }
    public void unlock(int idx){
        //if (trace)
        //    System.out.println(idx+" unlock");
        //super.unlock();
    }
}
