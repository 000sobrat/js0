/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssg.lib.jana;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.List;
import ssg.lib.common.TaskExecutor;
import ssg.lib.common.buffers.BufferTools;
import ssg.lib.di.DI;
import ssg.lib.di.base.BaseDI;
import ssg.lib.http.HttpApplication;
import ssg.lib.http.HttpAuthenticator;
import ssg.lib.http.HttpService;
import ssg.lib.http.base.HttpData;
import ssg.lib.http.base.HttpRequest;
import ssg.lib.http.di.DIHttpData;
import ssg.lib.service.DF_Service;

public class Http {

    HttpAuthenticator.HttpSimpleAuth httpAuth = new HttpAuthenticator.HttpSimpleAuth();
    HttpService httpService;

    public Http() {
        init();
    }

    public Http(HttpAuthenticator.HttpSimpleAuth httpAuth) {
        this.httpAuth = httpAuth;
        init();
    }

    public void dumpStat(String text) {
        StringBuilder sb = new StringBuilder();
        Runtime rt = Runtime.getRuntime();
        sb.append("[" + System.currentTimeMillis() + "]");
        sb.append(" CPUs: " + rt.availableProcessors());
        sb.append(", RAM (f/t/m, MB): " + (rt.freeMemory() / 1024 / 1024f) + "/" + (rt.totalMemory() / 1024 / 1024f) + "/" + (rt.maxMemory() / 1024 / 1024f));
        sb.append("\n  "+("" + text).replace("\n", "\n  "));
        System.out.println(sb);
        rt.gc();
    }

    public void init() {
        httpService = new HttpService(httpAuth) {
            @Override
            public void onHttpDataCreated(Channel provider, HttpData http) {
                super.onHttpDataCreated(provider, http);
                if (http instanceof HttpRequest) {
                    HttpRequest req = (HttpRequest) http;
                    dumpStat("REQUEST CREATE   : " + req.getQuery());
                }
            }

            @Override
            public void onHttpDataCompleted(Channel provider, HttpData http) {
                super.onHttpDataCompleted(provider, http);
                    if (http instanceof HttpRequest) {
                        HttpRequest req = (HttpRequest) http;
                        dumpStat("REQUEST DONE[" + req.getResponse().getHead().getProtocolInfo()[1] + "]: " + req.getQuery());
                    }
            }

            @Override
            public void onServiceError(Channel provider, DI pd, Throwable error) throws IOException {
                    if (pd instanceof DIHttpData) {
                        DIHttpData hdi = (DIHttpData) pd;
                        HttpData http = hdi.http(provider);
                        if (http instanceof HttpRequest) {
                            HttpRequest req = (HttpRequest) http;
                            dumpStat("REQUEST ERROR    : " + req.getQuery() + "\n  " + error);
                        }
                    }
                super.onServiceError(provider, pd, error);
            }
        };
    }

    public HttpService getHttp() {
        return httpService;
    }

    public HttpAuthenticator.HttpSimpleAuth getHttpAuth() {
        return httpAuth;
    }

    public void addApp(HttpApplication app, HttpAuthenticator.HttpSimpleAuth.Domain domain) {
        if (httpAuth != null && domain != null) {
            httpAuth.addDomain(domain);
        }
        getHttp().getApplications().addItem(app);
    }

    public DI<ByteBuffer, SocketChannel> buildHandler(DF_Service<SocketChannel> service) {
        if (service == null) {
            service = new DF_Service<>(new TaskExecutor.TaskExecutorSimple());
        }
        service.getServices().addItem(httpService);
        DI<ByteBuffer, SocketChannel> httpServer = new BaseDI<ByteBuffer, SocketChannel>() {
            @Override
            public long size(Collection<ByteBuffer>... data) {
                return BufferTools.getRemaining(data);
            }

            @Override
            public void consume(SocketChannel provider, Collection<ByteBuffer>... data) throws IOException {
                if (BufferTools.hasRemaining(data)) {
                    throw new UnsupportedOperationException("Not supported: service MUST handle all data without producing unhandled bytes.");
                }
            }

            @Override
            public List<ByteBuffer> produce(SocketChannel provider) throws IOException {
                return null;
            }
        };
        httpServer.filter(service);
        return httpServer;
    }
}
