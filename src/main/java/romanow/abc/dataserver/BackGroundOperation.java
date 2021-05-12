package romanow.abc.dataserver;

import romanow.abc.core.constants.ValuesBase;

public class BackGroundOperation {
    private String answer="";                       // Ответ фонового запроса
    private Thread waitForAnswer = new Thread();    // Поток задержки ответа
    private boolean busy=false;                     // Признак выполнения фоновой операции
    public synchronized void setAnswer(String ss){
        answer = ss;
        waitForAnswer.interrupt();
        setNoBusy();
        }
    public synchronized String getAnswer(){
        String ss = answer;
        answer="";
        return ss;
        }
    public synchronized boolean isBusy(){
        return busy;
        }
    public synchronized void setNoBusy(){
        busy=false;
        }
    public synchronized boolean testAndSetBusy(){
        if (busy) return false;
        busy = true;
        return true;
        }
    public void waitThread() {
        waitForAnswer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(ValuesBase.HTTPTimeOut/2*1000);
                    } catch (InterruptedException e) {}
                synchronized (waitForAnswer){
                    waitForAnswer.notifyAll();          // Разбудить ЗАПРОС
                    }
            }
        });
        waitForAnswer.start();
        synchronized (waitForAnswer){
            try {
                waitForAnswer.wait();
                } catch (InterruptedException e) {}
        }
    }
}
