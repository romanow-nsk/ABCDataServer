package romanow.abc.dataserver;

import romanow.abc.core.utils.OwnDateTime;

import java.util.ArrayList;

public class ErrorCounter {
    private boolean detect=false;               // Признак обнаружения ошибки
    private ArrayList<String> mesList=new ArrayList<>();  // Список сообщений
    private OwnDateTime beginMark = null;       // Метка времени начала
    private OwnDateTime endMark = null;         // Метка времени окончания
    public void clear(){
        detect = false;
        beginMark = endMark =null;
        mesList=new ArrayList<>();
    }
    public ErrorCounter onError(String mes){
        if (detect){
            mesList.add(mes);
            endMark = new OwnDateTime();
            return null;
            }
        else{
            beginMark = new OwnDateTime();
            detect = true;
            mesList.add(mes);
            ErrorCounter out = new ErrorCounter();
            out.detect = true;
            out.beginMark = new OwnDateTime();
            out.mesList.add(mes);
            return out;
        }
    }
    public ErrorCounter onSuccess(){          // И тестовое для завершения последовательности
        if (!detect)
            return null;
        endMark = new OwnDateTime();
        detect = false;
        ErrorCounter out = new ErrorCounter();
        out.detect = false;
        out.beginMark = new OwnDateTime(beginMark.timeInMS());
        out.endMark = new OwnDateTime();
        out.mesList = mesList;
        mesList = new ArrayList<>();
        return out;
        }
    public ErrorCounter(){}
    public boolean isDetect() {
        return detect; }
    public int getCount() {
        return mesList.size(); }
    public OwnDateTime getBeginMark() {
        return beginMark; }
    public OwnDateTime getEndMark() {
        return endMark; }
    public String toString(){
        if (detect)
            return "Начало "+beginMark.timeFullToString()+"\n"+mesList.get(0);
        String out = "Окончание ["+mesList.size()+"] "+beginMark.timeFullToString()+"-"+endMark.timeFullToString();
        for(String ss : mesList)
            out+="\n"+ss;
        return out;
    }
    public static void main(String ss[]){
        ErrorCounter counter = new ErrorCounter();
        ErrorCounter two;
        two = counter.onSuccess();
        if (two!=null)
            System.out.println(two);
        two = counter.onError("222");
        if (two!=null)
            System.out.println(two);
        two = counter.onError("333");
        if (two!=null)
            System.out.println(two);
        two = counter.onSuccess();
        if (two!=null)
            System.out.println(two);
        two = counter.onError("444");
        if (two!=null)
            System.out.println(two);
        two = counter.onSuccess();
        if (two!=null)
            System.out.println(two);
        two = counter.onSuccess();
        if (two!=null)
            System.out.println(two);
    }
}
