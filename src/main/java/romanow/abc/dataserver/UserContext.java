package romanow.abc.dataserver;

import romanow.abc.core.entity.users.User;
import romanow.abc.core.utils.OwnDateTime;

public class UserContext {
    private User user;
    private OwnDateTime lastCallTime = new OwnDateTime();
    private int elapsedTime=0;          // Установленное время паузы для хранения для контекста в секундах, 0 - вечно
    public UserContext(int eTime,User user0){
        user = user0;
        elapsedTime = eTime;
        }
    public User getUser() {
        return user; }
    public void setUser(User user) {
        this.user = user; }
    public boolean isOver(){
        if (elapsedTime==0)
            return false;
        return (new OwnDateTime().timeInMS()-lastCallTime.timeInMS())/1000 > elapsedTime;
        }
    public void wasCalled(){
        lastCallTime = new OwnDateTime();
        }
}
