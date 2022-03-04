package romanow.abc.dataserver;

public abstract class LoopCounter {
    public LoopCounter(){}
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
