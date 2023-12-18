package romanow.abc.dataserver;;

import romanow.abc.core.UniException;
import romanow.abc.core.constants.ValuesBase;
import romanow.abc.core.entity.Entity;
import romanow.abc.core.entity.EntityLink;
import romanow.abc.core.entity.EntityList;
import romanow.abc.core.entity.artifacts.Artifact;
import romanow.abc.core.entity.artifacts.ArtifactList;
import romanow.abc.core.entity.artifacts.ArtifactTypes;
import romanow.abc.core.entity.baseentityes.JEmpty;
import romanow.abc.core.entity.baseentityes.JString;
import romanow.abc.core.mongo.DBQueryList;
import romanow.abc.core.mongo.I_DBQuery;
import romanow.abc.core.mongo.RequestStatistic;
import romanow.abc.core.utils.FileNameExt;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import romanow.abc.core.utils.OwnDateTime;
import spark.Request;
import spark.Response;
import spark.Spark;
import spark.utils.IOUtils;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.*;
import java.util.ArrayList;

public class APIArtifact extends APIBase{
    public APIArtifact(DataServer db0){
        super(db0);
        Spark.post("/api/file/upload", "multipart/form-data", routeUpload);
        Spark.post("/api/file/update", "multipart/form-data", routeUpdate);
        Spark.post("/api/file/uploadByName", "multipart/form-data", routeUploadByName);
        Spark.get("/api/file/load2", routeLoad2);
        Spark.get("/api/file/loadByName", routeLoadByName);
        Spark.get("/api/file/load", routeLoad);
        Spark.get("/api/artifact/get", routeGetById);
        Spark.get("/api/artifact/list",apiArtifactList);
        Spark.post("/api/artifact/setname", routeSetName);
        Spark.post("/api/artifact/remove", routeArtifactRemove);
        Spark.get("/api/artifact/condition/list",routeArtifactConditionList);
        Spark.post("/api/artifact/create", routeArtifactCreate);
        Spark.post("/api/artifact/convert", routeConvertArtifact);
        Spark.post("/api/artifact/fromstring", routeCreateArtifactFromString);
        Spark.post("/api/artifact/fromstring2", routeCreateArtifactFromString2);
        }
    RouteWrap apiArtifactList = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamInt fl = new ParamInt(req,res,"mode");
            if (!fl.isValid()) return null;
            ArrayList<Entity> xx = db.mongoDB.getAll(new Artifact(),fl.getValue(),0);
            ArtifactList out = new ArtifactList();
            for(Entity zz : xx)
                out.add((Artifact) zz);
            return out;
        }};
    public boolean getArtifact(Request req, Response res, EntityLink<Artifact> main) throws Exception {
        main.setRef(new Artifact());
        if (!db.mongoDB.getById(main.getRef(),main.getOid())){
            db.createHTTPError(res, ValuesBase.HTTPNotFound,"Артефакт не найден, id="+main.getOid());
            return false;
            }
        return true;
        }
    RouteWrap routeArtifactRemove = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            if (db.users.isReadOnly(req,res)) return null;
            ParamLong qq = new ParamLong(req,res,"id");
            if (!qq.isValid()) return null;
            long id = qq.getValue();
            Artifact art = new Artifact();
            if (!db.mongoDB.getById(art,id)){
                db.createHTTPError(res,ValuesBase.HTTPNotFound,"Артефакт не найден, id="+id);
                return null;
                }
            deleteArtifactFile(art);
            db.mongoDB.remove(art);             // Удалить старый артефакт и файл
            return new JEmpty();
            }};
    RouteWrap routeGetById = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamLong qq = new ParamLong(req,res,"id");
            if (!qq.isValid()) return null;
            long id = qq.getValue();
            Artifact art = new Artifact();
            if (!db.mongoDB.getById(art,id)){
                db.createHTTPError(res,ValuesBase.HTTPNotFound,"Артефакт не найден, id="+id);
                return null;
                }
            return art;
            }};
    RouteWrap routeSetName = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            if (db.users.isReadOnly(req,res)) return null;
            ParamLong qq = new ParamLong(req,res,"id");
            if (!qq.isValid()) return null;
            long id = qq.getValue();
            ParamString nm = new ParamString(req,res,"name");
            if (!nm.isValid()) return null;
            String name = nm.getValue();
            Artifact out = new Artifact();
            if (!db.mongoDB.getById(out,id)){
                db.createHTTPError(res,ValuesBase.HTTPNotFound, "Артефакт не найден, id="+id);
                return null;
                }
            else{
                out.setName(name);
                db.mongoDB.update(out);
                return out;
                }
            }};
    RouteWrap routeCreateArtifactFromString = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamString fName = new ParamString(req, res, "fileName");
            if (!fName.isValid()) return null;
            ParamString text = new ParamString(req, res, "text");
            if (!text.isValid()) return null;
            Artifact art = new Artifact(fName.getValue(), 0);
            art.setName("loaded as text");
            int artType = ArtifactTypes.getArtifactType(art.getOriginalExt());
            art.setType(artType);
            db.mongoDB.add(art);
            String dir = db.dataServerFileDir() + "/" + art.type() + "_" + art.directoryName();
            File path = new File(dir);
            if (!path.exists())
                path.mkdir();
            String fullName = dir + "/" + art.createArtifactFileName();
            OutputStream outputStream = new FileOutputStream(fullName);
            OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
            writer.write(text.getValue());
            writer.flush();
            writer.close();
            File ff = new File(fullName);
            art.setFileSize(ff.length());
            db.mongoDB.update(art);
            System.out.println("Артефакт " + art);
            return art;
            }
        };
    RouteWrap routeCreateArtifactFromString2 = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamString fName = new ParamString(req, res, "fileName");
            if (!fName.isValid()) return null;
            ParamBody text = new ParamBody(req, res, JString.class);
            if (!text.isValid()) return null;
            Artifact art = new Artifact(fName.getValue(), 0);
            art.setName("loaded as text");
            int artType = ArtifactTypes.getArtifactType(art.getOriginalExt());
            art.setType(artType);
            db.mongoDB.add(art);
            String dir = db.dataServerFileDir() + "/" + art.type() + "_" + art.directoryName();
            File path = new File(dir);
            if (!path.exists())
                path.mkdir();
            String fullName = dir + "/" + art.createArtifactFileName();
            OutputStream outputStream = new FileOutputStream(fullName);
            OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
            writer.write(((JString)text.getValue()).getValue());
            writer.flush();
            writer.close();
            File ff = new File(fullName);
            art.setFileSize(ff.length());
            db.mongoDB.update(art);
            System.out.println("Артефакт " + art);
            return art;
        }
    };
    public boolean deleteArtifactFile(Artifact art){
        String dir = db.dataServerFileDir() + "/"+art.type()+"_"+art.directoryName()
                +"/"+art.createArtifactFileName();
        File file = new File(dir);
        if (!file.exists())
            return false;
        return file.delete();
        }
    /** Загрузить текст из потока */
    String loadText(InputStream in) throws Exception {
        InputStreamReader inn = new InputStreamReader(in,"UTF-8");
        String ss="";
        while (true)  {
            int cc = inn.read();
            if (cc==-1)
            { inn.close(); return ss; }
            ss +=(char)cc;
            }
        }
    public void convertArtifactJar(String dir,Artifact art) throws Exception {
        if (!db.common.getWorkSettings().isConvertAtrifact())
            return;
        FileNameExt src = art.getOriginal();
        String inExt = src.getExt();
        String outExt = ValuesBase.ConvertList.get(src.getExt());
        if (outExt==null)
            return;
        String s1 = dir +"/"+art.createArtifactFileName();
        src.setExt(outExt);
        String s2 = dir +"/"+art.createArtifactFileName();
            FFmpeg ffmpeg = new FFmpeg(db.rootServerFileDir());
            FFprobe ffprobe = new FFprobe(dir);
            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(s1)                       // Filename, or a FFmpegProbeResult
                    .overrideOutputFiles(true)          // Override the output if it exists
                    .addOutput(s2)   // Filename for the destination
                    //.setFormat("mp4")                   // Format is inferred from filename, or can be set
                    .setTargetSize(250_000)             // Aim for a 250KB file
                    .disableSubtitle()                  // No subtiles
                    .setAudioChannels(1)                // Mono audio
                    .setAudioCodec("aac")               // using the aac codec
                    .setAudioSampleRate(48_000)         // at 48KHz
                    .setAudioBitRate(32768)             // at 32 kbit/s
                    .setVideoCodec("libx264")           // Video using x264
                    .setVideoFrameRate(24, 1)         // at 24 frames per second
                    .setVideoResolution(640, 480)   // at 640x480 resolution
                    .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL) // Allow FFmpeg to use experimental specs
                    .done();
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
            // Run a one-pass encode
            executor.createJob(builder).run();
            // Or run a two-pass encode (which is better quality at the cost of being slower)
            //executor.createTwoPassJob(builder).run();
            }
    public String convertArtifact(Artifact art) throws UniException {
        if (!db.common.getWorkSettings().isConvertAtrifact())
            return "Конвертация не разрешена";
        FileNameExt src = art.getOriginal();
        String inExt = src.getExt();
        String outExt = ValuesBase.ConvertList.get(src.getExt());
        if (outExt==null)
            return "Файл "+inExt+" не конвертируется";
        String dir = db.dataServerFileDir() + "/"+art.type()+"_"+art.directoryName();
        String s1 = dir +"/"+art.createArtifactFileName();
        src.setExt(outExt);
        String s2 = dir +"/"+art.createArtifactFileName();
        String cmd = "ffmpeg -i "+ s1 +" -ab 64k -ar 44100 "+s2;
        //String cmd = "ffmpeg -i "+s1+" "+s2+" -r 25 -vcodec mpeg1";
        System.out.println(cmd);
        Runtime r =Runtime.getRuntime();
        Process p =null;
        try {
            p=r.exec(cmd);
            db.mongoDB.update(art);
        } catch (IOException e) {
            String ss= "Ошибка вызова перекодировщика "+e.toString();
            System.out.println(ss);
            src.setExt(inExt);
            return ss;
        }
        return null;
    }
    /** Общий код загрузки артефакта */
    public void update(Artifact art, Request request, Response response) throws Exception {
        request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(db.dataServerFileDir()));
        Part filePart = request.raw().getPart("file");
        InputStream inputStream = filePart.getInputStream();
        String dir = db.dataServerFileDir() + "/"+art.type()+"_"+art.directoryName();
        File path = new File(dir);
        if (!path.exists())
            path.mkdir();
        deleteArtifactFile(art);
        art.setFileSize(filePart.getSize());
        art.setDate(new OwnDateTime());             // Обновить время
        OutputStream outputStream = new FileOutputStream(dir +"/"+art.createArtifactFileName());
        IOUtils.copy(inputStream, outputStream);
        filePart.delete();                      // Удалить временный после закачки
        outputStream.close();
        db.mongoDB.update(art);
        System.out.println("Артефакт "+art);
        }
    /** Общий код загрузки артефакта */
    public Artifact upload(Request request, Response response) throws Exception {
            request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(db.dataServerFileDir()));
            ParamString nm = new ParamString(request,response,"description");
            if (!nm.isValid()) return null;
            String description = nm.getValue();
            ParamString oname = new ParamString(request,response,"origname");
            if (!nm.isValid()) return null;
            String origName = oname.getValue();
            Part filePart = request.raw().getPart("file");
            InputStream inputStream = filePart.getInputStream();
            Artifact art = new Artifact(origName,filePart.getSize());
            art.setName(description);
            int artType = ArtifactTypes.getArtifactType(art.getOriginalExt());
            art.setType(artType);
            db.mongoDB.add(art);
            String dir = db.dataServerFileDir() + "/"+art.type()+"_"+art.directoryName();
            File path = new File(dir);
            if (!path.exists())
                path.mkdir();
            OutputStream outputStream = new FileOutputStream(dir +"/"+art.createArtifactFileName());
            IOUtils.copy(inputStream, outputStream);
            filePart.delete();                      // Удалить временный после закачки
            outputStream.close();
            convertArtifact(art);
            System.out.println("Артефакт "+art);
            return art;
            }
    RouteWrap routeUpload = new RouteWrap() {
        @Override
        public Object _handle(Request request, Response response, RequestStatistic statistic) throws Exception {
            if (db.users.isReadOnly(request,response)) return null;
            Artifact art = upload(request,response);
            return art;
            }
        };
    RouteWrap routeUpdate = new RouteWrap() {
        @Override
        public Object _handle(Request request, Response response, RequestStatistic statistic) throws Exception {
            if (db.users.isReadOnly(request,response)) return null;
            ParamLong artId = new ParamLong(request,response,"artId");
            if (!artId.isValid())
                return null;
            Artifact art = new Artifact();
            if (!db.mongoDB.getById(art,artId.getValue())){
                db.createHTTPError(response,ValuesBase.HTTPRequestError,"Ошибка сервера данных: файл не найден id="+artId.isValid());
                return null;
                }
            update(art,request,response);
            return art;
            }
        };
    RouteWrap routeUploadByName = new RouteWrap() {
        @Override
        public Object _handle(Request request, Response response, RequestStatistic statistic) throws Exception {
            if (db.users.isReadOnly(request,response)) return null;
            request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(db.dataServerFileDir()));
                ParamString nm = new ParamString(request,response,"fname");
                if (!nm.isValid()) return response.body();
                String fname = nm.getValue();
                Part filePart = request.raw().getPart("file");
                InputStream inputStream = filePart.getInputStream();
                ParamBoolean root = new ParamBoolean(request,response,"root");
                if (!root.isValid()) return null;
                String dir = root.getValue() ? db.rootServerFileDir() : db.dataServerFileDir();
                File path = new File(dir);
                if (!path.exists())
                    path.mkdir();
                File zz = new File(dir +"/"+fname);
                zz.delete();
                OutputStream outputStream = new FileOutputStream(dir +"/"+fname);
                IOUtils.copy(inputStream, outputStream);
                filePart.delete();                      // Удалить временный после закачки
                outputStream.close();
                System.out.println("Файл добавлен "+fname);
                return new JEmpty();
            }
        };
    public boolean loadFile(String fname,Response res) throws Exception {
        //System.out.println("Файл: ["+fname.length()+"] "+fname);
        File ff = new File(fname);
        if (!ff.exists()){
            db.createHTTPError(res,ValuesBase.HTTPNotFound,"Ошибка сервера данных: файл не найден "+fname);
            return false;
            }
        else{
            long fileSize = ff.length();
            long sz0 = fileSize;
            res.raw().setContentLengthLong(fileSize);
            System.out.println("Загрузка файла "+fname+" ["+fileSize+"]");
            OutputStream out = res.raw().getOutputStream();
            FileInputStream in = new FileInputStream(ff);
            int sz = ValuesBase.FileBufferSize;
            byte bb[] = new byte[sz];
            int oldProc=0;
            //IOUtils.copy(in, out);
            while (fileSize != 0){
                long sz2 = fileSize > sz ? sz : fileSize;
                fileSize -= sz2;
                in.read(bb,0,(int)sz2);
                out.write(bb,0,(int)sz2);
                int proc = (int)((sz0-fileSize)*100/sz0);
                if (proc >= oldProc+10){
                    if (db.traceMid())
                        System.out.println(proc+" %");
                    oldProc = proc;
                    }
                }
            in.close();
            //out.flush();
            }
        return true;
        }
    RouteWrap routeLoad2 = new RouteWrap(){
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamBody bb = new ParamBody(req,res, JString.class);
            if (!bb.isValid()) return null;
            JString pp = (JString) bb.getValue();
            return loadFile(db.dataServerFileDir() + "/"+pp,res) ? new JEmpty() : null;
        }};
    RouteWrap routeLoad = new RouteWrap(){
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamLong qq = new ParamLong(req,res,"id");
            if (!qq.isValid()) return null;
            long id = qq.getValue();
            Artifact art = new Artifact();
            if (!db.mongoDB.getById(art,id)){
                db.createHTTPError(res,ValuesBase.HTTPNotFound, "Артефакт не найден, id="+id);
                return null;
                }
            String dir = db.dataServerFileDir() + "/"+art.type()+"_"+art.directoryName();
            String fullname = dir +"/"+art.createArtifactFileName();
            return loadFile(fullname,res)  ? new JEmpty() : null;
        }};
    RouteWrap routeLoadByName = new RouteWrap(){
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamString nm = new ParamString(req,res,"fname");
            if (!nm.isValid()) return null;
            String fname = nm.getValue();
            ParamBoolean root = new ParamBoolean(req,res,"root",true);
            if (!root.isValid()) return null;
            String dir = root.getValue() ? db.rootServerFileDir() : db.dataServerFileDir();
            String fullname = dir + "/"+fname;
            return loadFile(fullname,res)  ? new JEmpty() : null;
        }};

    RouteWrap routeArtifactConditionList = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamInt intVal = new ParamInt(req,res,"type",0);
            int fileType= intVal.getValue();
            ParamString stringVal = new ParamString(req,res,"owner","");
            String owner = stringVal.getValue();
            stringVal = new ParamString(req,res,"namemask","");
            String nameNask = stringVal.getValue();
            stringVal = new ParamString(req,res,"filenamemask","");
            String fileNameMask = stringVal.getValue();
            ParamLong date = new ParamLong(req,res,"dateInMS1",0);
            long dateInMS1 = date.isValid() ?  date.getValue() : 0;
            date = new ParamLong(req,res,"dateInMS2",0);
            long dateInMS2 = date.isValid() ?  date.getValue() : 0;
            date = new ParamLong(req,res,"size1",0);
            long size1 = date.getValue();
            date = new ParamLong(req,res,"size2",0);
            long size2 = date.getValue();
            EntityList<Entity> zz;
            if (fileType==0 && dateInMS1==0 && dateInMS2==0 && size1==0 && size2==0){
                 zz = db.mongoDB.getAll(new Artifact());
                 }
            else {
                DBQueryList condition = new DBQueryList();
                if (fileType != 0)
                    condition.add("type", fileType);
                if (dateInMS1 != 0 && dateInMS2 != 0) {
                    condition.add(I_DBQuery.ModeGTE,"d_timeInMS",dateInMS1);
                    condition.add(I_DBQuery.ModeLTE,"d_timeInMS",dateInMS2);
                    //obj.add(new BasicDBObject(new BasicDBObject("d_timeInMS", new BasicDBObject("$gte", dateInMS1))));
                    //obj.add(new BasicDBObject(new BasicDBObject("d_timeInMS", new BasicDBObject("$lte", dateInMS2))));
                    }
                if (size1 != 0 && size2 != 0) {
                    condition.add(I_DBQuery.ModeGTE,"fileSize",size1);
                    condition.add(I_DBQuery.ModeLTE,"fileSize",size2);
                    //obj.add(new BasicDBObject(new BasicDBObject("fileSize", new BasicDBObject("$gte", size1))));
                    //obj.add(new BasicDBObject(new BasicDBObject("fileSize", new BasicDBObject("$lte", size2))));
                    }
                //BasicDBObject query = new BasicDBObject();
                //query.put("$and", obj);
                zz = db.mongoDB.getAllByQuery(new Artifact(), condition, 0);
                }
            ArtifactList out = new ArtifactList();
            for(Entity ent : zz){
                Artifact ctr = (Artifact) ent;
                if (nameNask.length()!=0 && !ctr.getName().contains(nameNask))
                    continue;
                if (fileNameMask.length()!=0 && !ctr.getOriginal().fileName().contains(fileNameMask))
                    continue;
                if (owner.length()!=0 && !ctr.getParentName().equals(owner))
                    continue;
                out.add(ctr);
            }
            return out;
        }};
    RouteWrap routeArtifactCreate = new RouteWrap() {
        @Override
        public Object _handle(Request request, Response response, RequestStatistic statistic) throws Exception {
            if (db.users.isReadOnly(request,response)) return null;
            ParamString nm = new ParamString(request,response,"description");
            if (!nm.isValid()) return null;
            String description = nm.getValue();
            ParamString oname = new ParamString(request,response,"origname");
            if (!nm.isValid()) return null;
            String origName = oname.getValue();
            ParamLong fsize = new ParamLong(request,response,"filesize");
            if (!fsize.isValid()) return null;
            Artifact art = new Artifact(origName,fsize.getValue());
            art.setName(description);
            int artType = ArtifactTypes.getArtifactType(art.getOriginalExt());
            art.setType(artType);
            db.mongoDB.add(art);
            String dir = db.dataServerFileDir() + "/"+art.type()+"_"+art.directoryName();
            File path = new File(dir);
            if (!path.exists())
                path.mkdir();
            System.out.println("Артефакт (объект) "+art);
            return art;
        }
    };
    //--------------------------------------------------------------------------------------
    RouteWrap routeConvertArtifact = new RouteWrap(){
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            Artifact art = (Artifact) db.common.getEntityByIdHTTP(req,res,Artifact.class);
            if (art == null)
                return null;
            ParamLong id = new ParamLong(req,res,"id");
            if (!id.isValid())
                return null;
            String ss = convertArtifact(art);
            if (ss!=null){
                db.createHTTPError(res,ValuesBase.HTTPRequestError, ss);
                return null;
            }
            return new JEmpty();
        }
    };
}
