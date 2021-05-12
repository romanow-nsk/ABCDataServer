package romanow.abc.dataserver;

import romanow.abc.core.entity.base.WorkSettingsBase;

public interface I_DataServerBack {
    public long createEvent(int type,int level,String title, String comment,long artId);
    public void onClock();
    public void onStart();
    public void onShutdown();
    }
