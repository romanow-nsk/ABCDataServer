package romanow.abc.dataserver;

import romanow.abc.core.Utils;
import romanow.abc.core.constants.ValuesBase;
import romanow.abc.core.entity.users.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

public class SessionController extends Thread{
    private DataServer db;
    private boolean shutdown=false;
    private HashMap<String,UserContext> TokenTable = new HashMap<>();
    public SessionController(DataServer db0){
        db = db0;
        start(); }
    public void run(){                  // Удаление устаревших ключей
        while (!shutdown){
            try {
                Thread.sleep(ValuesBase.SessionCycleTime*1000);
                } catch (InterruptedException e) {}
            try {
                testAndRemoveKeys();
                // Вариант через внутреннее API
                //db.localService.sessionClock().execute();
                } catch (Exception e) { System.out.println("Внутреннее API:"+Utils.createFatalMessage(e));   }
            }
        }
    public synchronized boolean isTokenValid(String token){
        return TokenTable.get(token)!=null;
        }
    public synchronized void testAndRemoveKeys(){
            Object list[] = TokenTable.keySet().toArray().clone();
            for(Object oo : list){
                String key = (String)oo;
                UserContext ctx = TokenTable.get(key);
                if (ctx==null) continue;
                if (ctx.isOver()){
                    TokenTable.remove(key);
                    System.out.println("Удалена сессия для "+ctx.getUser().getTitle());
                }
            }
            db.getServerState().setSessionCount(TokenTable.size());
        }
    //---------------- Оставить монопольно только суперадмина -----------------------------------------
    public synchronized void keepOnlySuperAdmins(){
        Object list[] = TokenTable.keySet().toArray().clone();
        for(Object oo : list){
            String key = (String)oo;
            UserContext ctx = TokenTable.get(key);
            if (ctx.getUser().getTypeId()==ValuesBase.UserSuperAdminType)
                continue;
            System.out.println("Удалена сессия для "+TokenTable.get(key).getUser().getTitle());
            TokenTable.remove(key);
            }
        db.getServerState().setSessionCount(TokenTable.size());
        }
    public void shutdown(){
        this.interrupt();
        shutdown=true;
        }
    public void removeOldUserTokens(User user){         // Удалить старые ключт от него
        Object keys[] = TokenTable.keySet().toArray();
        for(Object oo : keys){
            String key = (String)oo;
            UserContext xx = TokenTable.get(key);
            if (xx.getUser().getOid()==user.getOid()){
                removeContext(key);
                System.out.println("Удален старый токен для "+user.getTitle()+"\n"+key);
                }
            }
        db.getServerState().setSessionCount(TokenTable.size());
        }
    public synchronized void removeContext(String Token){
        UserContext ctx = getContext(Token);
        if (ctx==null)
            System.out.println("Неизвестный ключ сессии\n"+Token);
        else{
            System.out.println("Закрыта сессия "+ctx.getUser().getTitle());
            TokenTable.remove(Token);
            }
        db.getServerState().setSessionCount(TokenTable.size());
        }
    public synchronized UserContext getContext(String Token){
        return  TokenTable.get(Token);
        }
    public synchronized String createContext(int eTime, User user){    // Создание сеанса - получение ключа
        //--------------- Удалять не надо, сами удалятся. Иначе проблема с параллельными логинами
        //removeOldUserTokens(user);
        String Token;
        if (ValuesBase.JWTSessionMode){
            Token = createJWT(""+user.getOid(),user.fullUserName(),user.getLoginPhone(),eTime*1000,ValuesBase.JWTSessionSecretKey);
            parseJWT(Token,ValuesBase.JWTSessionSecretKey);
            System.out.println("Создан контекст для "+user.getTitle()+"\n"+Token);

            TokenTable.put(Token,new UserContext(eTime,user));
            db.getServerState().setSessionCount(TokenTable.size());
            return Token;
            }
        while (true){
            Token = Utils.generateToken();
            if (TokenTable.get(Token)==null){
                TokenTable.put(Token,new UserContext(eTime,user));
                System.out.println("Создан контекст для "+user.getTitle()+"\n"+Token);
                db.getServerState().setSessionCount(TokenTable.size());
                return Token;
                }
            }
        }
    //------------------------------------------------------------------------------------------------------------------
    //Sample method to construct a JWT
    public static String createJWT(String id, String issuer, String subject, long ttlMillis, String secretKey) {
        //The JWT signature algorithm we will be using to sign the token
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        //We will sign our JWT with our ApiKey secret
        byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(secretKey);
        Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
        //Let's set the JWT Claims
        JwtBuilder builder = Jwts.builder().setId(id)
                .setIssuedAt(now)
                .setSubject(subject)
                .setIssuer(issuer)
                .signWith(signatureAlgorithm, signingKey);
        //if it has been specified, let's add the expiration
        if (ttlMillis == 0)
            ttlMillis = 10L*365*24*3600*1000;           // 10 лет для вечного ключа
        long expMillis = nowMillis + ttlMillis;
        Date exp = new Date(expMillis);
        builder.setExpiration(exp);
        //Builds the JWT and serializes it to a compact, URL-safe string
        return builder.compact();
        }
    //Sample method to validate and read the JWT
    public static Claims parseJWT(String jwt,String secretKey) {
        System.out.println("Token: "+jwt);
        //This line will throw an exception if it is not a signed JWS (as expected)
        Claims claims = Jwts.parser()
                .setSigningKey(DatatypeConverter.parseBase64Binary(secretKey))
                .parseClaimsJws(jwt).getBody();
        System.out.println("ID: " + claims.getId());
        System.out.println("Subject: " + claims.getSubject());
        System.out.println("Issuer: " + claims.getIssuer());
        System.out.println("Expiration: " + claims.getExpiration());
        return claims;
        }
    //------------------------------------------------------------------------------------------------------------------
    public static void main(String a[]){
        System.out.println(Utils.generateToken());
        String key = "pi31415926";
        String ss = createJWT("1234","sssss","666666",10*60*1000,key);
        System.out.println(ss);
        parseJWT(ss,key);
    }
}
