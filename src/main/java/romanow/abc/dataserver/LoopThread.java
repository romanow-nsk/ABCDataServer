package romanow.abc.dataserver;

public class LoopThread{
    private Thread loopThread=null; // Поток
    private boolean stop=false;     // Двухфазное завершение
    private  int delay;             // Интерфал цикла
    private I_LoopBack back;        // События обратного вызова
    private String name;            // Имя потока ()
    public String getName() {
        return name; }
    public void setBack(I_LoopBack back) {
        this.back = back; }
    public synchronized void shutdown(){
        stop=true;                      // Двухфазное завершение
        if (loopThread!=null)
            loopThread.interrupt();         // Прервать sleep
        }
    public LoopThread(String name0,int delayInSec, I_LoopBack loopBack){
        delay = delayInSec;
        back = loopBack;
        name = name0;
        }
    public LoopThread(String name0,int delayInSec){
        delay = delayInSec;
        name = name0;
        }
    public LoopThread(String name0,I_LoopBack loopBack){
        back = loopBack;
        name = name0;
        }
    public synchronized void start(int delayInSec){
        delay = delayInSec;
        start();
        }
    public  void start(){
        loopThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!stop){
                    try {
                        if (delay!=0)
                            Thread.sleep(delay*1000);
                        else
                            Thread.yield();         // Задержка 0 - отдать процессор
                        } catch (Exception e) {}
                    if (stop)
                        break;
                    try {
                        back.run();                 // Выполнение кода
                        } catch (Exception ee){     // Перехват исключений
                            if (back.onException(name,ee))
                                stop=true;
                            }
                }
            back.onFinish(name);                    // Уведомление о завершении
            }
        });
    loopThread.setName(name);
    loopThread.start();
    }
}
