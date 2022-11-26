package romanow.abc.dataserver;

import romanow.abc.core.ErrorList;
import romanow.abc.core.constants.ValuesBase;

public class BackGroundOperation {
    public final static int BGFree=0;               // Нет операции
    public final static int BGInProcess=1;          // Выполняется
    public final static int BGDone=2;               // Закончена, но не опрошено
    private ErrorList answer=new ErrorList();       // Ответ фонового запроса
    private Thread waitForAnswer = new Thread();    // Поток задержки ответа
    private int state=BGFree;                       // Состояние выполнения операции
    public synchronized void finish(ErrorList ss){
        switch (state){
            case BGFree:
                break;
            case BGInProcess:
                answer = ss;
                waitForAnswer.interrupt();
                state = BGDone;
                ss.calcDuration();
                break;
            case BGDone:
                answer.addError(ss);
                ss.calcDuration();
                break;
            }
        }
    public synchronized ErrorList getAnswer(){
        if (state!=BGDone)
            return null;
        state = BGFree;
        return answer;
        }
    public synchronized int getState(){
        return state;
        }
    public synchronized ErrorList testAndSetBusy(){
        if (state==BackGroundOperation.BGDone){
            answer.addInfo("Завершение предыдущей операции: повторите новую");
            return answer;
            }
        if (state==BackGroundOperation.BGInProcess){
            return new ErrorList("Фоновая операция уже выполняется");
            }
        state = BGInProcess;
        answer.clear();
        return answer;
        }
    public void waitThread() {
        waitForAnswer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(ValuesBase.HTTPTimeOut/3*1000);
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
