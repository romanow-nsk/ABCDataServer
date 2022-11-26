package romanow.abc.dataserver.operations;

import romanow.abc.core.ErrorList;
import romanow.abc.core.constants.TableItem;
import romanow.abc.core.constants.ValuesBase;
import romanow.abc.core.entity.Entity;
import romanow.abc.core.entity.EntityList;
import romanow.abc.dataserver.APIAdmin;
import romanow.abc.dataserver.DataServer;

public class DBORefreshFields implements I_SystemOperations{
    @Override
    public void execute(DataServer db, APIAdmin apiAdmin, ErrorList errors) {
        Object olist[] = ValuesBase.EntityFactory().classList().toArray();
        TableItem item=null;
        for(int i=0;i<olist.length;i++){
            long oid=0;
            Entity zz=new Entity();
            item = (TableItem)olist[i];
            String eName = item.clazz.getSimpleName();
            try {
                Entity ent = (Entity) (item.clazz.newInstance());
                EntityList<Entity> xx = db.mongoDB().getAll(ent, ValuesBase.GetAllModeActual, 0);
                errors.addInfo(eName+" обновляется "+(xx.size()));
                for (int j = 0; j < xx.size(); j++) {
                    zz = xx.get(j);
                    try {
                        db.mongoDB().update(zz);
                        } catch (Exception ee) {
                            String ss = "Ошибка обновления " + eName + " oid = " + zz.getOid() + " " + zz.getTitle() + "\n" + ee.toString();
                            errors.addError(ss);
                            System.out.println(ss);
                            }
                        }
                } catch (Exception ee){
                    String ss = "Ошибка таблицы "+eName+"\n"+ee.toString();
                    System.out.println(ss);
                    errors.addError(ss);
                    }
                }
            }
        }

