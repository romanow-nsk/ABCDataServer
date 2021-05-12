package romanow.abc.dataserver;

import romanow.abc.core.API.RestAPIBase;
import romanow.abc.core.UniException;
import romanow.abc.core.Utils;
import romanow.abc.core.constants.ValuesBase;
import romanow.abc.core.entity.*;
import romanow.abc.core.entity.artifacts.Artifact;
import romanow.abc.core.entity.artifacts.ArtifactTypes;

import romanow.abc.core.mongo.DBQueryList;
import romanow.abc.core.mongo.I_DBQuery;
import romanow.abc.core.utils.OwnDateTime;
import spark.utils.IOUtils;

import java.io.*;
import java.util.ArrayList;

public class ClockController<T extends DataServer> extends Thread{
    protected T db;                     // Головной модуль сервера данных
    protected boolean shutdown=false;   // Признак завершения работы
    private int dataLoop=0;             // Счетчик интервала короткого цикла ПД
    private int dataLongLoop=0;         // Счетчик интервала длинного цикла ПД
    private int failureLoop=0;          // Счетчик интервала цикла аварий
    private int otherDataLoop=0;        // Счетчик интервала цикла
    private int fileScanLoop=0;         // Счетчик интервала цикла источников файлов
    private int mainServerLoop=0;       // Счетчик интервала цикла сервера СУ АГЭУ
    private OwnDateTime lastDay = new OwnDateTime(false);        // Время для фиксации смены дня
    public ClockController(T db0){
        db = db0;
        lastDay.onlyDate();
        start();
        }
    public void setShutdown(){
        shutdown=true;
        interrupt();
        }
    public void run(){
        while (!shutdown){
            try {
                Thread.sleep(1000);                 // Часики 1 сек
                } catch (InterruptedException e) {
                    return;
                    }
            try {
                clockCycle();
                } catch (Exception e) {
                    System.out.println("Внутреннее API:"+Utils.createFatalMessage(e));
                    db.sendBug("ClockController",e);
                    }
            }
        }
    public void shutdown(){
        this.interrupt();
        shutdown=true;
        }
    public void clockCycle(){
        db.getDataServerBack().onClock();
        }
    private final static int hourX=19;
    private final static int minuteX=45;
    //------------------------------------------------------------------------------------------------------------------
    public static void main(String a[]){
    }
}
