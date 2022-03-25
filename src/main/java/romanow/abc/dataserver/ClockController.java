package romanow.abc.dataserver;

import org.apache.poi.ss.formula.functions.T;
import romanow.abc.core.Utils;
import romanow.abc.core.utils.OwnDateTime;

import java.util.Timer;
import java.util.TimerTask;

public class ClockController<T extends DataServer>{
    protected T db;                     // Головной модуль сервера данных
    protected boolean shutdown=false;   // Признак завершения работы
    private OwnDateTime lastDay = new OwnDateTime(false);        // Время для фиксации смены дня
    Timer timer = new Timer();
    public ClockController(T db0){
        db = db0;
        lastDay.onlyDate();
        timer.schedule(task,1000,1000);
        }
    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            clockCycle();
            }
        };
    public void shutdown(){
        timer.cancel();
        }
    public void clockCycle(){
        db.onClock();
        }
    private final static int hourX=19;
    private final static int minuteX=45;
    //------------------------------------------------------------------------------------------------------------------
    public static void main(String a[]){
    }
}
