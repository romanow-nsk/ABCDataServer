package romanow.abc.dataserver;

import romanow.abc.core.UniException;
import romanow.abc.core.Utils;
import romanow.abc.core.constants.ValuesBase;
import romanow.abc.core.entity.Entity;
import romanow.abc.core.entity.EntityList;
import romanow.abc.core.entity.artifacts.Artifact;
import romanow.abc.core.entity.base.WorkSettingsBase;
import romanow.abc.core.entity.baseentityes.JBoolean;
import romanow.abc.core.entity.baseentityes.JEmpty;
import romanow.abc.core.entity.baseentityes.JInt;
import romanow.abc.core.entity.baseentityes.JLong;
import romanow.abc.core.entity.notifications.NTList;
import romanow.abc.core.entity.notifications.NTMessage;
import romanow.abc.core.entity.users.User;
import romanow.abc.core.mongo.RequestStatistic;
import spark.Request;
import spark.Response;
import spark.Spark;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.util.Properties;

public class APINotification extends APIBase {
    private NTList list = new NTList();
    private long noteOid=1;
    public APINotification(DataServer db0) {
        super(db0);
        //-----------------------------------------------------------------------
        Spark.post("/api/notification/add", routeAddNotification);
        Spark.post("/api/notification/add/broadcast", routeAddNotificationBroadcast);
        Spark.get("/api/notification/get", routeGetNotification);
        Spark.post("/api/notification/setstate", routeSetNotificationState);
        Spark.get("/api/notification/user/list", routeNotificationUserList);
        Spark.get("/api/notification/list", routeNotificationList);
        Spark.get("/api/notification/count", routeNotificationCount);
        Spark.post("/api/notification/update", routeUpdateNotification);
        Spark.post("/api/notification/remove", routeRemoveNotification);
        }
    RouteWrap routeNotificationCount = new RouteWrap(false) {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            return db.common.keepAlive(req,res,false);
        }};

    public void addNotification(int type0, int sndType0, int recType0, long userId, String head, String mes0,long param){
        NTMessage mes = new NTMessage(type0,sndType0,recType0,userId,head,mes0);
        mes.setParam(param);
        mes.setOid(noteOid++);
        list.add(mes);
        }
    //=====================================================================================================
    public void procExtension(Artifact art) throws UniException {
        if (art == null)
            return;
        if (!db.common.getWorkSettings().isConvertAtrifact())
            return;
        String ext2 = ValuesBase.ConvertList.get(art.getOriginalExt());
        if (ext2 != null)
            art.setOriginalExt(ext2);
        }
    RouteWrap routeAddNotification = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamBody qq = new ParamBody(req, res, NTMessage.class);
            if (!qq.isValid()) return null;
            NTMessage mes = (NTMessage)qq.getValue();
            procExtension(mes.getArtifact().getRef());
            mes.setOid(noteOid++);
            list.add(mes);
            return new JLong(mes.getOid());
            }
        };
    RouteWrap routeAddNotificationBroadcast = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamBody qq = new ParamBody(req, res, NTMessage.class);
            if (!qq.isValid()) return null;
            NTMessage mes = (NTMessage)qq.getValue();
            procExtension(mes.getArtifact().getRef());
            EntityList<Entity> userList = db.mongoDB.getAll(new User());
            for(Entity uu : userList){
                NTMessage copy = new NTMessage(mes);
                copy.getUser().setOid(uu.getOid());
                copy.setOid(noteOid++);
                list.add(copy);
                }
            return new JInt(userList.size());
        }
    };
    RouteWrap routeSetNotificationState = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
             ParamLong notificationid = new ParamLong(req, res, "id");
             if (!notificationid.isValid()) return null;
             ParamInt state = new ParamInt(req, res, "state");
             if (!state.isValid()) return null;
             NTMessage mes = list.get(notificationid.getValue());
             if (mes==null){
                 db.createHTTPError(res, ValuesBase.HTTPNotFound, "Уведомление не найдено, id=: "+notificationid.getValue());
                 return null;
                 }
             mes.setState(state.getValue());
             System.out.println("Уведомление id=" + notificationid.getValue() + " " + ValuesBase.title("NotificationState",state.getValue()));
             return new JEmpty();
             }
        };
    RouteWrap routeGetNotification = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamLong notificationid = new ParamLong(req, res, "id");
            if (!notificationid.isValid()) return null;
            NTMessage mes = list.get(notificationid.getValue());
            if (mes==null){
                db.createHTTPError(res,ValuesBase.HTTPNotFound, "Уведомление не найдено, id=: "+notificationid.getValue());
                return null;
                }
            return mes;
            }
        };
    RouteWrap routeRemoveNotification = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamLong notificationid = new ParamLong(req, res, "id");
            if (!notificationid.isValid()) return null;
            System.out.println("Удаление уведомления id="+notificationid.getValue());
            boolean bb = list.remove(notificationid.getValue());
            return new JBoolean(bb);
            }
        };
    RouteWrap routeUpdateNotification = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamBody qq = new ParamBody(req, res, NTMessage.class);
            if (!qq.isValid()) return null;
            NTMessage mes = (NTMessage)qq.getValue();
            if (!list.update(mes)){
                db.createHTTPError(res,ValuesBase.HTTPNotFound, "Уведомление не найдено, id=: "+mes.getOid());
                return null;
                }
            return new JEmpty();
            }
        };

    RouteWrap routeNotificationUserList = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            ParamInt utype = new ParamInt(req, res, "usertype");
            if (!utype.isValid()) return null;
            int type = utype.getValue();
            ParamInt st = new ParamInt(req, res, "state");
            if (!st.isValid()) return null;
            int state = st.getValue();
            ParamInt uid = new ParamInt(req, res, "userid");
            if (!uid.isValid()) return null;
            long useriD = uid.getValue();
            return list.getListByQuery(utype.getValue(),st.getValue(),uid.getValue());
            }
        };
    public int getNotificationCount(int type, int state, long userId) throws UniException {
        int count =  list.getCountByQuery(type,state,userId);
        if (count != 0)
            System.out.println(count + " уведомлений [" + ValuesBase.title("NotificationState",state) + "] для польз(id)=" + userId + " сост:" + state + " тип польз.:" + type);
        return count;
        }
    //----------------------- Стандартные --------------------------------------
    RouteWrap routeNotificationList = new RouteWrap() {
        @Override
        public Object _handle(Request req, Response res, RequestStatistic statistic) throws Exception {
            return list.getData();
        }
    };
    //---------------------------------------------------------------------------------
    public void sendMailNotifycation(ServerEvent event){
        try {
            WorkSettingsBase ws = db.common.getWorkSettings();
            if (!ws.isMailNotifycation() || ws.getMailToSend().length()==0)
                return;
            String from = ws.getMailBox();
            String username = from.substring(0,from.indexOf("@")-1);
            String host = ws.getMailHost();
            int port = ws.getMailPort();
            final String pass = ws.getMailPass();
            //------------------------------------------------------------
            Properties props = System.getProperties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.host", host);
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.socketFactory.port", ""+port); //143
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.starttls.enable", "true");
            //Security.addProvider(new Provider())
            Session session = Session.getDefaultInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    //return new PasswordAuthentication("romanow", "streichholz");
                    return new PasswordAuthentication(from, pass);
                }
            });
            MimeMessage message = new MimeMessage(session); // email message
            message.setFrom(new InternetAddress(from)); // setting header fields
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(ws.getMailToSend()));
            message.setSubject("Событие СНЭ: "+event.getTitle()); // subject line
            String text = event.getArrivalTime().toString2();
            text += " Сообщение от "+ ws.getNodeName();
            text+="\n"+event.toString();
            MimeMultipart multipart = new MimeMultipart();
            //Первый кусочек - текст письма
            MimeBodyPart part1 = new MimeBodyPart();
            part1.addHeader("Content-Type", "text/plain; charset=UTF-8");
            part1.setDataHandler(new DataHandler(text, "text/plain; charset=\"utf-8\""));
            multipart.addBodyPart(part1);
            if (event.getArtifact().getOid()==0){
                Artifact art = new Artifact();
                db.mongoDB.getById(art,event.getArtifact().getOid());
                MimeBodyPart messageBodyPart = new MimeBodyPart();
                String dir = db.dataServerFileDir() + "/"+art.type()+"_"+art.directoryName();
                String file = dir +"/"+art.createArtifactFileName(-db.timeZoneHours);
                file = file.replace("\\","/");
                System.out.println(file);
                String fileName = art.getOriginalName();
                DataSource source = new FileDataSource(new File(file));
                messageBodyPart.setDataHandler(new DataHandler(source));
                messageBodyPart.setFileName(MimeUtility.encodeWord(fileName));
                multipart.addBodyPart(messageBodyPart);
            }
            message.setContent(multipart);
            message.setText(text);
            message.setSentDate(new java.util.Date());
            Transport.send(message);
            System.out.println("Отправлено " +ws.getMailToSend()+": "+ event.getTitle());
            } catch (Exception mex){
                Utils.printFatalMessage(mex);
                }
    }


}
