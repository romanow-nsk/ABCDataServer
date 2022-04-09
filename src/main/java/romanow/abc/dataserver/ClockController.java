package romanow.abc.dataserver;

import org.apache.poi.ss.formula.functions.T;
import romanow.abc.core.Utils;
import romanow.abc.core.utils.OwnDateTime;

import java.util.Timer;
import java.util.TimerTask;

public class ClockController<T extends DataServer> extends Thread{
    protected T db;                     // Головной модуль сервера данных
    protected boolean shutdown=false;   // Признак завершения работы
    private volatile boolean busy=false;
    private OwnDateTime lastDay = new OwnDateTime(false);        // Время для фиксации смены дня
    public ClockController(T db0){
        db = db0;
        lastDay.onlyDate();
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
