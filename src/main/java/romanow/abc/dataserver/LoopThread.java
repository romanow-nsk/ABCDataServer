package romanow.abc.dataserver;

import lombok.Getter;

public class LoopThread{
    private Thread loopThread=null; // Поток
    private boolean stop=false;     // Двухфазное завершение
    private  int delay;             // Интерфал цикла
    private I_LoopBack back;        // События обратного вызова
    @Getter private String name;    // Имя потока ()
    public void shutdown(){
        stop=true;                  // Двухфазное завершение
        loopThread.interrupt();     // Прервать sleep
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
