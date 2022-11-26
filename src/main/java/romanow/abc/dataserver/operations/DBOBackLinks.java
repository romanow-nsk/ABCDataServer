package romanow.abc.dataserver.operations;

import romanow.abc.core.ErrorList;
import romanow.abc.core.constants.TableItem;
import romanow.abc.core.constants.ValuesBase;
import romanow.abc.core.entity.*;
import romanow.abc.core.entity.artifacts.Artifact;
import romanow.abc.dataserver.APIAdmin;
import romanow.abc.dataserver.DataServer;

import java.util.ArrayList;

public class DBOBackLinks implements I_SystemOperations{
    int totalCount = 0;
    int notFound=0;
    EntityRefList<Artifact> artMap = new EntityRefList<>();
    private void procBackLink(long artOid, Entity parent, ErrorList errors) {
        if (artOid==0)
            return;
        totalCount++;
        Artifact cc = artMap.getById(artOid);
        if (cc==null){
            notFound++;
            errors.addError("Не найден артефакт id="+artOid+" для "+parent.getClass().getSimpleName()+" id="+parent.getOid());
            return;
            }
        cc.setParent(parent);
        }
    @Override
    public void execute(DataServer db, APIAdmin apiAdmin, ErrorList errors) {
        EntityList<Entity> artList;
        try {
            artList = db.mongoDB().getAll(new Artifact(), ValuesBase.GetAllModeActual, 0);
            } catch (Exception ee){
                errors.addError("Ошибка чтения артефактов: "+ee.toString());
                return;
                }
        for(Entity ent : artList){
            Artifact art = (Artifact)ent;
            artMap.add(art);
            art.setParentName("");
            art.setParentOid(0);
            }
        artMap.createMap();
        Object olist[] = ValuesBase.EntityFactory().classList().toArray();
        //DAOAccessFactory daoFactory = new DAOAccessFactory();
        //I_DAOAccess entityLink = daoFactory.getClassMap().get(ValuesBase.DAOEntityLink);
        //I_DAOAccess entityLinkList = daoFactory.getClassMap().get(ValuesBase.DAOEntityLinkList);
        TableItem item=null;
        for(int i=0;i<olist.length;i++){
            long oid=0;
            Entity dao=new Entity();
            item = (TableItem)olist[i];
            String eName = item.clazz.getSimpleName();
            long artOid=0;
            try {
                Entity ent = (Entity) (item.clazz.newInstance());
                ArrayList<EntityField> fields = ent.getFields();
                EntityList<Entity> xx = db.mongoDB().getAll(ent, ValuesBase.GetAllModeActual, 0);
                int count=0;
                for (int j = 0; j < xx.size(); j++) {
                    dao = xx.get(j);
                    for(EntityField ff : fields){
                        if (ff.type==ValuesBase.DAOEntityLink && ff.genericName.equals("Artifact")){
                            //entityLink.getField(ff,zz);
                            artOid =  ((EntityLink)ff.field.get(dao)).getOid();
                            procBackLink(artOid,dao,errors);
                            count++;
                            }
                        if (ff.type==ValuesBase.DAOEntityLinkList && ff.genericName.equals("Artifact")){
                            //entityLink.getField(ff,zz);
                            EntityLinkList<?> list = (EntityLinkList)ff.field.get(dao);
                            for(EntityLink<?> one : list){
                                procBackLink(one.getOid(),dao,errors);
                                count++;
                                }
                            }
                        }
                    }
                if (count!=0)
                    errors.addInfo(eName+" обновляется "+count);
                //} catch (Exception ee) {
                //    String ss = "Ошибка обновления " + eName + " oid = " + zz.getOid() + " " + zz.getTitle() + "\n" + ee.toString();
                //    errors.addError(ss);
                //    System.out.println(ss);
                //    }
                } catch (Exception ee){
                    String ss = "Ошибка таблицы "+eName+"\n"+ee.toString();
                    System.out.println(ss);
                    errors.addError(ss);
                    }
                }
            errors.addInfo("Обратных ссылок: "+totalCount+" не найдено артефактов: "+notFound);
        try {
            for(Entity ent : artList) {
                db.mongoDB().update(ent);
                }
            } catch (Exception ee){
                errors.addError("Ошибка обновления артефактов: "+ee.toString());
                return;
                }
        }
    }

