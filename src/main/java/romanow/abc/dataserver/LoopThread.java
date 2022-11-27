package romanow.abc.dataserver;

import lombok.Getter;

public class LoopThread{
    private Thread loopThread=null;
    private boolean stop=false;
    private  int delay;
    private I_LoopBack back;
    @Getter private String name;
    public void shutdown(){
        stop=true;
        loopThread.interrupt();
        }
    public LoopThread(String name0,int delayInSec, I_LoopBack loopBack){
        delay = delayInSec;
        back = loopBack;
        name = name0;
        }
    public  void start(){
        loopThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!stop){
                    try {
                        Thread.sleep(delay*1000);
                        } catch (InterruptedException e) {}
                    if (stop)
                        break;
                    try {
                        back.run();
                        } catch (Exception ee){
                            if (back.onException(name,ee))
                                stop=true;
                            }
                }
            back.onFinish(name);
            }
        });
    loopThread.start();
    }
}
