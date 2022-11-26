package romanow.abc.dataserver.operations;

import romanow.abc.core.ErrorList;
import romanow.abc.core.constants.TableItem;
import romanow.abc.core.constants.ValuesBase;
import romanow.abc.core.entity.Entity;
import romanow.abc.core.entity.EntityList;
import romanow.abc.dataserver.APIAdmin;
import romanow.abc.dataserver.DataServer;

public class DBOSquezzyTables implements I_SystemOperations{
    @Override
    public void execute(DataServer db, APIAdmin apiAdmin, ErrorList errors) {
        int totalCount=0;
        int count=0;
        try {
            Object olist[] = ValuesBase.EntityFactory().classList().toArray();
            TableItem item=null;
            for(int i=0;i<olist.length;i++){
                long oid=0;
                item = (TableItem)olist[i];
                String eName = item.clazz.getSimpleName();
                count=0;
                try {
                    Entity ent = (Entity)(item.clazz.newInstance());
                    EntityList<Entity> xx = db.mongoDB().getAll(ent, ValuesBase.GetAllModeDeleted,0);
                    for(int j=1; j<xx.size();j++){              // ПЕРВУЮ ПРОПУСТИТЬ
                        oid = xx.get(j).getOid();
                        db.mongoDB().remove(ent,oid);
                        totalCount++;
                        count++;
                        }
                    errors.addInfo(eName+" удалено "+count);
                    } catch (Exception ee){
                        errors.addInfo(eName+" удалено "+count);
                        errors.addError("Ошибка удаления "+eName+" oid = "+oid+"\n"+ee.toString());
                        }
                    }
                } catch (Exception e2){
                    errors.addError("Всего удалено" + totalCount + " записей, ошибка: " + e2.toString());
                    }
            }
}
