/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssg.lib.jana;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.net.ssl.SSLContext;
import ssg.lib.di.base.SSL_DF;
import ssg.lib.ssl.SSLTools;
import ssg.lib.ssl.SSLTools.PrivateKeyCertificateInfo;

public class SSLSupport {
    // SSL support

    SSLContext sslCtx;
    SSL_DF ssl_df_client;
    SSL_DF ssl_df_server;

    public SSLSupport(String... args) throws IOException {
        initSSL(args);
    }

    public SSLSupport(PrivateKeyCertificateInfo... pkcis) throws IOException {
        String tsLocation = System.getProperty("javax.net.ssl.trustStore");
        String tsPassword = System.getProperty("javax.net.ssl.trustStorePassword");

        if (tsLocation == null) {
            URL ts = SSLSupport.class.getClassLoader().getResource("jssecacerts");
            if (ts == null) {
                ts = SSLSupport.class.getClassLoader().getResource("cacerts");
            }
            String sep = File.separator;
            File file = new File(System.getProperty("java.home") + sep
                    + "lib" + sep + "security" + sep
                    + "cacerts");
            if (file.exists()) {
                ts = file.toURI().toURL();
            }
            if (ts != null) {
                tsLocation = ts.toString();
            }
        }

        try {

            SSLContext ctx = SSLTools.composeSSLContext(
                    (tsLocation != null) ? new URL(tsLocation) : null,
                    tsPassword,
                    "TLS",
                    pkcis);
            prepareFor(ctx);
        } catch (Throwable th) {
            prepareFor(null);
            th.printStackTrace();
        }
    }

    void prepareFor(SSLContext ctx) {
        this.sslCtx = ctx;
        if (ctx != null) {
            ssl_df_client = new SSL_DF(sslCtx, true);
            ssl_df_server = new SSL_DF(sslCtx, false);
            ssl_df_server.setNeedClientAuth(Boolean.FALSE);
            ssl_df_server.setAutodetect(true);
        } else {
            ssl_df_client = null;
            ssl_df_server = null;
        }
    }

    public void initSSL(String args[]) throws IOException {
        String ksResource = "ks/localhost__abc.p12";
        String tsResource = "ks/localhost__abc_ts.p12";
        ksResource = "keystore.p12";
        tsResource = "keystore.p12";
        String ksPwd = "passw0rd";
        String kPwd = "passw0rd";
        String tsPwd = "passw0rd";
        String protocol = "TLS";

        if (args != null) {
            for (String arg : args) {
                if (arg.startsWith("ks=")) {
                    ksResource = arg.substring(3);
                } else if (arg.startsWith("ts=")) {
                    tsResource = arg.substring(3);
                } else if (arg.startsWith("ksp=")) {
                    ksPwd = arg.substring(4);
                } else if (arg.startsWith("kp=")) {
                    kPwd = arg.substring(3);
                } else if (arg.startsWith("tsp=")) {
                    tsPwd = arg.substring(4);
                } else if (arg.startsWith("sp=")) {
                    protocol = arg.substring(3);
                }
            }
        }

        if (ksResource != null && !ksResource.trim().isEmpty()) {
            try {
                SSLContext sslCtx = SSLContext.getDefault();
                if (1 == 1) {
                    try {
                        SSLTools.SSLHelper sslh_abc = SSLTools.createSSLHelper(
                                SSLSupport.class.getClassLoader().getResource(ksResource),
                                ksPwd,
                                kPwd,
                                SSLSupport.class.getClassLoader().getResource(tsResource),
                                tsPwd
                        );
                        sslCtx = sslh_abc.createSSLContext(protocol, true);
                        //SSLTools.TrustAllTrustManager.logger = System.out;
                    } catch (Throwable th) {
                        //sslCtx = TestSSLTools.getSSLContext();
                    }

                }
                prepareFor(sslCtx);
            } catch (Throwable th) {
                prepareFor(null);
                throw (th instanceof IOException) ? (IOException) th : new IOException(th);
            }
        } else {
            prepareFor(null);
        }
    }

}
