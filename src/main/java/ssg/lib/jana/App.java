/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssg.lib.jana;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ssg.lib.common.TaskExecutor;
import ssg.lib.di.DI;
import ssg.lib.http.HttpApplication;
import ssg.lib.http.HttpMatcher;
import ssg.lib.http.HttpSession;
import ssg.lib.http.HttpUser;
import ssg.lib.http.base.HttpData;
import ssg.lib.http.base.HttpRequest;
import ssg.lib.http.dp.HttpResourceBytes;
import ssg.lib.http.dp.HttpResourceCollection;
import ssg.lib.http.dp.HttpStaticDataProcessor;
import ssg.lib.http.rest.MethodsProvider;
import ssg.lib.http.rest.RESTHttpDataProcessor;
import ssg.lib.http.rest.XMethodsProvider;
import ssg.lib.jana.api.ScheduleAPI;
import ssg.lib.jana.api.TrainingAPI;
import ssg.lib.jana.api.UI_API;
import ssg.lib.jana.api.UM_API;
import ssg.lib.net.CS;
import ssg.lib.net.CSListener;
import ssg.lib.net.TCPHandler;
import ssg.lib.oauth.impl.OAuthClientFB;
import ssg.lib.oauth.impl.OAuthClientGoolge;
import ssg.lib.oauth.impl.OAuthClientInstagram;
import ssg.lib.oauth.impl.OAuthClientMSO;
import ssg.lib.oauth.impl.OAuthClientVK;
import ssg.lib.oauth.impl.OAuthHttpDataProcessor;
import ssg.lib.oauth.impl.UserOAuthVerifier;
import ssg.lib.service.DF_Service;
import ssg.lib.service.DataProcessor;
import ssg.lib.service.Repository;
import ssg.lib.ssl.SSLTools;
import ssg.lib.ssl.SSLTools.PrivateKeyCertificateInfo;

/**
 *
 * @author sesidoro
 */
public class App extends CS {

    // SSL support
    SSLSupport sslSupport;
    // service handlers support
    DF_Service<SocketChannel> service = new DF_Service<>(new TaskExecutor.TaskExecutorSimple());

    public App(String... args) throws IOException {
        System.out.println("init App for String[]");
        sslSupport = new SSLSupport(args);
        init();
    }

    public App(SSLTools.PrivateKeyCertificateInfo... pkcis) throws IOException {
        System.out.println("init App for PrivateKeyCertificateInfo[]");
        sslSupport = new SSLSupport(pkcis);
        init();
    }

    public App(SSLSupport sslSupport) throws IOException {
        System.out.println("init App for SSLSupport");
        this.sslSupport = sslSupport;
        init();
    }

    public void init() {
        if (sslSupport != null && sslSupport.ssl_df_server != null) {
            service.filter(sslSupport.ssl_df_server);
        }
        addCSListener(new CSListener.DebuggingCSListener(System.out, CSListener.DebuggingCSListener.DO_STRUCTURAL));
    }

    public DF_Service<SocketChannel> getDefaultService() {
        return service;
    }

    public static void main(String... args) throws Exception {
        ClassLoader classLoader = App.class.getClassLoader();

        PrivateKeyCertificateInfo pkci = new PrivateKeyCertificateInfo(
                "",
                classLoader.getResource("ssl/js.ca.crt"),
                classLoader.getResource("ssl/js.crt"),
                classLoader.getResource("ssl/js.pk")
        );

        App server = (pkci.checkCert() && pkci.checkPK()) ? new App(pkci) : new App(args);
        server.start();

        final ScheduleAPI schedule = new ScheduleAPI();
        final TrainingAPI training = new TrainingAPI();
        final UM_API um = new UM_API();
        final UI_API ui = new UI_API(schedule, training, um);

        try (InputStream is = new FileInputStream("target/jana.json");) {
            Reader rdr = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            um.importFrom(rdr);
            training.importFrom(rdr);
            schedule.importFrom(rdr);

            int a = 0;

        } catch (Throwable th) {
            th.printStackTrace();

            try (InputStream is = App.class.getClassLoader().getResourceAsStream("conf/jana.json");) {
                Reader rdr = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                um.importFrom(rdr);
                training.importFrom(rdr);
                schedule.importFrom(rdr);

                int a = 0;

            } catch (Throwable th1) {
                th.printStackTrace();
            }

        }

        OAuthHttpDataProcessor oahttp = new OAuthHttpDataProcessor(new HttpMatcher("/app/oauth/*"))
                .addOAuth("google", new OAuthClientGoolge(
                        "1019090063722-00ibplc6sv0fu1j5ma0g6fsq527c1qcv.apps.googleusercontent.com",
                        "2ZvOfhPpyaC7OKkuiwpzll6D")
                )
                .addOAuth("fb", new OAuthClientFB(
                        "512657122859011",
                        "246fa8201bdedeb046c2f4fd4c6937e4")
                )
                .addOAuth("instagram", new OAuthClientInstagram(
                        "512657122859011",
                        "246fa8201bdedeb046c2f4fd4c6937e4")
                )
                .addOAuth("mso", new OAuthClientMSO(
                        "392b54ed-67ab-4b08-bad6-3fb4f66bb406",
                        "ifgxAVEB306~ifbWCZ46!^{",
                        null)
                )
                .addOAuth("vk", new OAuthClientVK(
                        "7145014",
                        "qjc8oqqft1TxlORtDTOg")
                );
        oahttp.setHttpAuhtenticator(um.getDomain());
        UserOAuthVerifier oav = new UserOAuthVerifier(oahttp);
        um.getDomain().getUserStore().verifiers().add(oav);

        final List<String[]> oauthLinks = oahttp.getAuthLinks();
        final Map<String, String[]> oauthImages = new LinkedHashMap<>();
        for (String[] oal : oauthLinks) {
            String link = oal[0];
            String img = oal[1];
            String title = oal[2];
            if (img != null && !"none".equals(img)) {
                oauthImages.put(link, new String[]{"icons8-" + img + "-144.png", "#{" + title + "}"});
            }
        }
        um.getAuthVariants().putAll(oauthImages);

        Http http = new Http();
        http.addApp(
                new HttpApplication("App", "/app") {
                    @Override
                    public HttpUser onAuhtenticatedUser(HttpSession session, HttpUser user) {
                        System.out.println("onAuhtenticatedUser: " + user);
                        if (user == null) {
                            session.getProperties().clear();
                        } else {
                            //session.setLocale(Locale.forLanguageTag("ru"));
                        }
                        return super.onAuhtenticatedUser(session, user);
                    }
                }
                        .addDataProcessors(new RESTHttpDataProcessor(
                                "/app/api",
                                new MethodsProvider[]{new XMethodsProvider()},
                                ui,
                                schedule,
                                training,
                                um)
                        )
                        .addDataProcessors(oahttp)
                        .addDataProcessors(new HttpStaticDataProcessor() {
                            @Override
                            public void onHeaderLoaded(HttpData data) throws IOException {
                                System.out.println("STATIC RESOURCE: " + ((HttpRequest) data).getQuery());
                                super.onHeaderLoaded(data);
                            }
                        }
                                .add(new HttpResourceBytes(classLoader.getResourceAsStream("app/images/icon.png"), "/app/favicon.ico", "image/png"))
                                //                                .add(new HttpResourceBytes(classLoader.getResourceAsStream("scheduler.png"), "/app/logo.png", "image/png"))
                                //                                .add(new HttpResourceBytes(classLoader.getResourceAsStream("scheduler.html"), "/app/index.html", "text/html"))
                                //                                .add(new HttpResourceBytes(classLoader.getResourceAsStream("scheduler.json"), "/app/manifest.json", "application/json"))
                                .add(new HttpResourceCollection("/app/*", "resource:app"))
                                .resourceBundle("i18n.jana")
                        .noCacheing()
                        ),
                um.getDomain()
        );

        ////////// global resources...
        if (http.httpService.getDataProcessors(null, null) == null) {
            http.httpService.setDataProcessors(new Repository<DataProcessor>());
        }
        http.httpService.getDataProcessors(null, null).addItem(new HttpStaticDataProcessor()
                .add(new HttpResourceBytes(classLoader.getResourceAsStream("app/images/kuntajana_124.png"), "/favicon.ico", "image/png"))
                .add(new HttpResourceCollection("/.well-known/acme-challenge/", "/tmp/*"))
        );

        DI<ByteBuffer, SocketChannel> httpDI = http.buildHandler(server.getDefaultService());

        int httpPort = 18123;
        try {
            server.add(new TCPHandler(
                    new InetSocketAddress(InetAddress.getByAddress(new byte[]{0, 0, 0, 0}), httpPort))
                    .defaultHandler(httpDI)
            );
        } catch (Throwable th) {
            System.out.println("Failed to set listener at " + httpPort);
        }

        try {
            server.add(new TCPHandler(
                    new InetSocketAddress(InetAddress.getByAddress(new byte[]{0, 0, 0, 0}), 80))
                    .defaultHandler(httpDI)
            );
        } catch (Throwable th) {
            System.out.println("Failed to set listener at " + 80);
        }
        try {
            server.add(new TCPHandler(
                    new InetSocketAddress(InetAddress.getByAddress(new byte[]{0, 0, 0, 0}), 443))
                    .defaultHandler(httpDI)
            );
        } catch (Throwable th) {
            System.out.println("Failed to set listener at " + 443);
        }
        System.out.println(server);

        long timeout = System.currentTimeMillis() + 1000 * 60 * 15;
        while (System.currentTimeMillis() < timeout) {
            Thread.sleep(100);
        }

        try {
            while (true) {
                Thread.sleep(100);
            }
        } finally {
            server.stop();
        }
    }

}
