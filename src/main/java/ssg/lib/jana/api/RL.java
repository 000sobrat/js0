/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssg.lib.jana.api;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 *
 * @author sesidoro
 */
public class RL {

    private ClassLoader classLoader;

    RL(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public List<String> print(List<String> list) {
        if (list == null) {
            list = new ArrayList<>();
        }
        try {

            List<URL> urls = (classLoader instanceof URLClassLoader)
                    ? Arrays.asList(((URLClassLoader) classLoader).getURLs())
                    : (classLoader!=null) 
                    ? Collections.list(classLoader.getResources(""))
                    : null;

            if (urls == null || urls.isEmpty()) {
                urls = new ArrayList();
                String[] paths = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
                for (String p : paths) {
                    try {
                        File f = new File(p);
                        URL u = f.toURI().toURL();
                        urls.add(u);
                    } catch (Throwable th) {
                    }
                }
            }

            for (URL url : urls) {
                printUrl(url, list);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private void printUrl(URL url, List<String> list) throws URISyntaxException, IOException {
        File file = new File(url.toURI());

        if (file.isDirectory()) {
            printDirContent(file, list);
        } else {
            printJarContent(new JarFile(file), list);
        }
    }

    private void printJarContent(JarFile jarFile, List<String> list) {
        Enumeration<JarEntry> enumeration = jarFile.entries();
        while (enumeration.hasMoreElements()) {
            list.add(enumeration.nextElement().getName());
        }
    }

    private void printDirContent(File dir, List<String> list) throws IOException {
        String[] children = dir.list();
        for (String aChildren : children) {
            visitAllDirsAndFiles(new File(dir, aChildren), list);
        }
    }

    private void visitAllDirsAndFiles(File file, List<String> list) throws IOException {
        if (file.isDirectory()) {
            printDirContent(file, list);
        } else {
            list.add(file.getCanonicalPath());
        }
    }

    public static void main(String... args) throws Exception {
        RL rl = new RL(null);//RL.class.getClassLoader());
        List<String> list = rl.print(null);
        for (String s : list) {
            System.out.println(s);
        }
    }
}
