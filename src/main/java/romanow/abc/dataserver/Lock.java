package romanow.abc.dataserver;

import lombok.Getter;
import lombok.Setter;

public class Lock {
    public Lock(){}
    public Lock(boolean trace0){
        trace = trace0;
    }
    @Getter @Setter private boolean trace=false;
    private volatile int synchCounter=0;
    public synchronized void lock(int idx){
        if (trace)
            System.out.println(idx+" lock+++ ");
        synchCounter++;
        if (synchCounter>1) {
            try {
                this.wait();
                if (trace)
                    System.out.println(idx+" unlock");
            } catch (InterruptedException e) {
                if (trace)
                    System.out.println(idx+" unlock+");
            }
        }
    }
    public synchronized void unlock(int idx){
        if (trace)
            System.out.println(idx+" lock---");
        synchCounter--;
        this.notify();
    }
}
