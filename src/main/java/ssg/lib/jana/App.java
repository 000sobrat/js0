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
import java.util.Locale;
import ssg.lib.common.TaskExecutor;
import ssg.lib.common.buffers.ByteBufferPipeReplacement;
import ssg.lib.di.DI;
import ssg.lib.http.HttpApplication;
import ssg.lib.http.HttpSession;
import ssg.lib.http.HttpUser;
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
import ssg.lib.service.DF_Service;
import ssg.lib.service.DataProcessor;
import ssg.lib.service.Repository;

/**
 *
 * @author sesidoro
 */
public class App extends CS {
    //ByteBufferPipeReplacement.GDEBUG=true;

    // SSL support
    SSLSupport sslSupport;
    // service handlers support
    DF_Service<SocketChannel> service = new DF_Service<>(new TaskExecutor.TaskExecutorSimple());

    public App(String... args) throws IOException {
        sslSupport = new SSLSupport(args);
        init();
    }

    public App(SSLSupport sslSupport) throws IOException {
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
        App server = new App(args);
        server.start();

//        ExcelTools.GTSX gts = ExcelTools.loadGTS(new FileInputStream("./src/test/SchedulerBase.xlsx"));
//        ApplicationAPI app = new ApplicationAPI(gts);
//        if (gts.meta.containsKey("email-agent.email")) {
//            EmailAgent.EMailAccount.MediatorAccount m = new EmailAgent.EMailAccount.MediatorAccount(
//                    new URL(gts.meta.get("email-agent.imap")[0]),
//                    new URL(gts.meta.get("email-agent.smtp")[0]),
//                    gts.meta.get("email-agent.email")[0],
//                    gts.meta.get("email-agent.p")[0]
//            );
//            EmailAgent agent = new EmailAgent(m, gts.meta.get("admin")[0]);
//            agent.from = gts.meta.get("email-agent.from");
//            agent.replyTo = gts.meta.get("email-agent.replyTo");
//            app.setEmailAgent(agent);
//        }
//
//        Http http = new Http();
//        http.addApp(
//                new HttpApplication("App", "/app") {
//                    @Override
//                    public HttpUser onAuhtenticatedUser(HttpUser user) {
//                        return super.onAuhtenticatedUser(user);
//                    }
//                }
//                        .addDataProcessors(new RESTHttpDataProcessor("/app/scheduler", new MethodsProvider[]{new XMethodsProvider()}, app))
//                        .addDataProcessors(new HttpStaticDataProcessor()
//                                .add(new HttpResourceBytes(classLoader.getResourceAsStream("scheduler.png"), "/app/favicon.ico", "image/png"))
//                                .add(new HttpResourceBytes(classLoader.getResourceAsStream("scheduler.png"), "/app/logo.png", "image/png"))
//                                .add(new HttpResourceBytes(classLoader.getResourceAsStream("scheduler.html"), "/app/index.html", "text/html"))
//                                .add(new HttpResourceBytes(classLoader.getResourceAsStream("scheduler.json"), "/app/manifest.json", "application/json"))
//                                .add(new HttpResourceCollection("/app/*", "resource:app"))
//                                .noCacheing()
//                        ),
//                app.getDomain()
//        );
//
//        if (1 == 1) {
//            if (http.httpService.getDataProcessors(null, null) == null) {
//                http.httpService.setDataProcessors(new Repository<DataProcessor>());
//            }
//            http.httpService.getDataProcessors(null, null).addItem(new HttpStaticDataProcessor()
//                    .add(new HttpResourceBytes(classLoader.getResourceAsStream("scheduler.png"), "/favicon.ico", "image/png"))
//                    .add(new HttpResourceBytes(classLoader.getResourceAsStream("scheduler.png"), "/logo.png", "image/png"))
//                    .add(new HttpResourceBytes(classLoader.getResourceAsStream("scheduler.html"), "/index.html", "text/html"))
//                    .add(new HttpResourceBytes(classLoader.getResourceAsStream("scheduler.json"), "/manifest.json", "application/json"))
//            //.noCacheing()
//            );
//        }
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

        Http http = new Http();
        http.addApp(
                new HttpApplication("App", "/app") {
                    @Override
                    public HttpUser onAuhtenticatedUser(HttpSession session, HttpUser user) {
                        System.out.println("onAuhtenticatedUser: " + user);
                        if (user == null) {
                            session.getProperties().clear();
                        } else {
                            session.setLocale(Locale.forLanguageTag("ru"));
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
                        //                        .addDataProcessors(new RESTHttpDataProcessor("/app", new MethodsProvider[]{new XMethodsProvider()}, training))
                        //                        .addDataProcessors(new RESTHttpDataProcessor("/app", new MethodsProvider[]{new XMethodsProvider()}, um))
                        .addDataProcessors(new HttpStaticDataProcessor()
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
                .add(new HttpResourceBytes(classLoader.getResourceAsStream("app/images/icon.png"), "/favicon.ico", "image/png"))
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
