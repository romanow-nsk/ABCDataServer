package romanow.abc.dataserver;

import romanow.abc.core.API.RestAPIBase;
import romanow.abc.core.Utils;
import romanow.abc.core.constants.ValuesBase;
import romanow.abc.core.entity.EntityList;
import romanow.abc.core.entity.artifacts.Artifact;
import romanow.abc.core.entity.baseentityes.JString;
import romanow.abc.core.entity.users.Account;
import romanow.abc.core.entity.users.User;
import romanow.abc.core.settings.Settings;
import okhttp3.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;

public class RestAPIClientExample {
    //--------------------------------------------------------------------
    public static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain,  String authType) throws CertificateException {}
                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException { }
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[0];
                    }
                }};
            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            OkHttpClient okHttpClient = new OkHttpClient();
            okHttpClient = okHttpClient.newBuilder()
                    .sslSocketFactory(sslSocketFactory)
                    //.hostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
                    .build();
            return okHttpClient;
            } catch (Exception e) { throw new RuntimeException(e); }
        }
        //--------------------------------------------------------------------
            public static void main(String argv[]) throws IOException {
                Callback<User> back = new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        if (response.isSuccessful()){
                            System.out.println("Асинхронный запрос "+response.body());
                            }
                        else{
                            System.out.println("Асинхронный запрос "+ Utils.httpError(response));
                            }
                        }
                    @Override
                    public void onFailure(Call<User> call, Throwable throwable) {
                        System.out.println("Асинхронный запрос "+throwable.getMessage());
                        }
                    };
                //----------------------------------------------------------------------------
                Settings set = new Settings();
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl("http://"+set.dataServerIP()+":"+set.dataServerPort())
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                Retrofit retrofitSSL = new Retrofit.Builder()
                        .baseUrl("https://"+set.dataServerIP()+":"+set.dataServerPort())
                        .addConverterFactory(GsonConverterFactory.create())
                        .client(getUnsafeOkHttpClient())                            //SSL
                        .build();
                RestAPIBase service = retrofit.create(RestAPIBase.class);
                RestAPIBase serviceSSL = retrofitSSL.create(RestAPIBase.class);
                Account good = new Account("", "9139449081", "1234");
                Account bad = new Account("", "9139449081", "5555");
                String key  = service.debugToken(ValuesBase.DebugTokenPass).execute().body().getValue();
                // Синхронный запрос с объектом в body
                Response<User> res2 = service.login(good).execute();
                System.out.println("Синхронный запрос с объектом в body "+res2.body());
                // Синхронный запрос с объектом в body
                res2 = service.login(bad).execute();
                if (res2.isSuccessful())
                    System.out.println("Синхронный запрос с объектом в body "+res2.body());
                else
                    System.out.println(res2.message());
                // Асинхронный запрос с объектом в body
                service.login(good).enqueue(back);
                service.login(bad).enqueue(back);
                // Асинхронный запрос с параметрами в заголовке
                service.login("9139449081","1234").enqueue(back);
                // Cинхронный запрос с параметрами в заголовке
                Response<User> res3 = service.login("9139449081","1234").execute();
                System.out.println("Cинхронный запрос с параметрами в заголовке "+res3.body());
                //Response<Technician> res4 = service.getTechnicianById(res3.body().getOid()).execute();
                //System.out.println("Cинхронный запрос по id "+res4.body());
                Response<EntityList<User>> res7 = service.getUserList(key, ValuesBase.GetAllModeActual,0).execute();
                System.out.println("Список пользователей  "+res7.body());
                //--------------------------- Передача файла multipart ----------------------------------
                File file = new File("C:/solus_rex.jpg");
                // create RequestBody instance from file
                RequestBody requestFile = RequestBody.create(MediaType.parse("text"), file);
                // MultipartBody.Part is used to send also the actual file name
                MultipartBody.Part body =
                        MultipartBody.Part.createFormData("file", file.getName(), requestFile);
                // add another part within the multipart request
                //String descriptionString = "Это файл для тестирования";
                //RequestBody description = RequestBody.create(okhttp3.MultipartBody.FORM, descriptionString);
                // finally, execute the request
                //Call<Artifact> call = service.upload(description,body);
                Call<Artifact> call = service.upload(key,"Тестовая строка",file.getName(),body);
                call.enqueue(new Callback<Artifact>() {
                    @Override
                    public void onResponse(Call<Artifact> call, Response<Artifact> response) {
                        if (response.isSuccessful()){
                            System.out.println("Асинхронный запрос записи файла "+response.body());
                            }
                        else{
                            System.out.println("Асинхронный запрос записи файла  "+Utils.httpError(response));
                            }
                    }
                    @Override
                    public void onFailure(Call<Artifact> call, Throwable t) {
                        System.out.println(t.getMessage());
                    }
                });
                //--------------------------- Чтение файла ----------------------------------
                String fname = "solus_rex.jpg";
                Call<ResponseBody> call2 = service.downLoad2(key,new JString(fname));
                call2.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            ResponseBody body = response.body();
                            long fileSize = body.contentLength();
                            InputStream in = body.byteStream();
                            try {
                                FileOutputStream out = new FileOutputStream("D:/temp/_" + fname);
                                while (fileSize-- != 0)
                                    out.write(in.read());
                                in.close();
                                out.close();
                            } catch (IOException ee) {
                                System.out.println(ee.getMessage());
                            }
                        }
                        else{
                            System.out.println(Utils.httpError(response));
                        }
                    }
                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        System.out.println(t.getMessage());
                    }
                });
            }
        }
