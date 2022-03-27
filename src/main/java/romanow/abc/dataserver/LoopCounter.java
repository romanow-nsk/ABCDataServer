package romanow.abc.dataserver;

public abstract class LoopCounter {
    public LoopCounter(){}
    public LoopCounter(int count0){
        count = count0;
        }
    private int count=0;
    private int startCount;
    public abstract void exec();
    public void dec(int startCount){
        if (startCount==0)
            return;
        if (count--<=0){
            count = startCount;
            exec();
        }
    }
}
