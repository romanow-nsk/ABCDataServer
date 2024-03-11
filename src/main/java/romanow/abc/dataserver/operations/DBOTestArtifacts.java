package romanow.abc.dataserver.operations;

import romanow.abc.core.ErrorList;
import romanow.abc.core.entity.Entity;
import romanow.abc.core.entity.EntityList;
import romanow.abc.core.entity.artifacts.Artifact;
import romanow.abc.dataserver.APIAdmin;
import romanow.abc.dataserver.DataServer;

import java.io.File;

public class DBOTestArtifacts implements I_SystemOperations{
    public DBOTestArtifacts(){}
    @Override
    public void execute(DataServer db, APIAdmin apiAdmin, ErrorList errors) {
        EntityList<Entity> zz = new EntityList<>();
        try {
            zz = db.mongoDB().getAll(new Artifact());
            for (Entity ent : zz) {
                Artifact art = (Artifact) ent;
                String dir = db.dataServerFileDir() + "/" + art.type() + "_" + art.directoryName();
                String fname = art.createArtifactFileName(-db.timeZoneHours);
                String ss = dir + "/" + fname;
                File ff = new File(ss);
                boolean fail = !ff.exists();
                if (fail){
                    errors.addError("Файл не найден: "+fname);
                    }
                art.setFileLost(fail);
                art.setFileSize(fail ? 0 : ff.length());
                db.mongoDB().update(art);
                }
            errors.addInfo("Всего " + zz.size() + " артефактов, отсутствуют " + errors.getErrCount() + " файлов");
            } catch (Exception ee){
                errors.addInfo("Всего " + zz.size() + " артефактов, отсутствуют " + errors.getErrCount() + " файлов");
                errors.addError("Ошибка модуля операции "+getClass().getSimpleName()+": "+ee.toString());
                }
        }
}
