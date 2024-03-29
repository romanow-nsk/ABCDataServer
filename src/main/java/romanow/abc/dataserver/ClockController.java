package romanow.abc.dataserver;

import romanow.abc.core.utils.OwnDateTime;

public class ClockController<T extends DataServer> extends Thread{
    protected T db;                     // Головной модуль сервера данных
    protected boolean shutdown=false;   // Признак завершения работы
    private volatile boolean busy=false;
    private OwnDateTime lastDay = new OwnDateTime(false);        // Время для фиксации смены дня
    public ClockController(T db0){
        db = db0;
        lastDay.onlyDate();
        setName("ClockController");
        start();
        }
    public void run() {
        while (!shutdown){
            try {
                Thread.sleep(1000);
                clockCycle();
                } catch (InterruptedException e) {
                }
            }
        };
    public void shutdown(){
        shutdown = true;
        interrupt();
        }
    public void clockCycle(){
        if (busy)
            return;
        busy=true;
        db.onClock();
        busy=false;
        }
    private final static int hourX=19;
    private final static int minuteX=45;
    //------------------------------------------------------------------------------------------------------------------
    public static void main(String a[]){
    }
}
