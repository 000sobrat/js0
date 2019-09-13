/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssg.lib.jana.tools;

import com.sun.mail.imap.IMAPFolder;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.mail.Message;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;

/**
 *
 * @author sesidoro
 */
public class EMailTools {

    public static List<Message> checkPOP3Messages(String host, int port, String user, String pwd, String folder, Long startFrom, EmailMessageListener callback) throws IOException, MessagingException {
        List<Message> r = new ArrayList<>();
        //create properties field
        Properties properties = new Properties();

        properties.put("mail.pop3.host", host);
        properties.put("mail.pop3.port", (port != 0) ? "" + port : "995");
        properties.put("mail.pop3.starttls.enable", "true");
        Session emailSession = Session.getDefaultInstance(properties);

        //create the POP3 store object and connect with the pop server
        Store store = emailSession.getStore("pop3s");

        try {
            store.connect(host, user, pwd);

            //create the folder object and open it
            Folder emailFolder = store.getFolder(folder != null ? folder : "INBOX");
            emailFolder.open(Folder.READ_ONLY);

            // retrieve the messages from the folder in an array and print it
            Message[] messages = emailFolder.getMessages();
            System.out.println("messages.length---" + messages.length);

            for (int i = 0, n = messages.length; i < n; i++) {
                Message message = messages[i];
                Date dt = message.getReceivedDate();
                if (startFrom == null || dt.getTime() >= startFrom) {
                    if (callback != null) {
                        callback.onCheckMessage(emailSession, message);
                    } else {
                        System.out.println("---------------------------------");
                        System.out.println("Email Number " + (i + 1));
                        System.out.println("Subject: " + message.getSubject());
                        System.out.println("From: " + message.getFrom()[0]);
                        System.out.println("Text: " + message.getContent().toString());
                        r.add(message);
                    }
                }
            }

            //close the store and folder objects
            emailFolder.close(false);
        } finally {
            store.close();
        }
        return r;
    }

    /**
     * Wrapper to send single simple message.
     *
     * @param host
     * @param port
     * @param user
     * @param pwd
     * @param toEmail
     * @param subject
     * @param body
     * @return
     * @throws IOException
     * @throws MessagingException
     */
    public static String sendSMTPMessage(String host, int port, String user, String pwd, String toEmail, String subject, String body) throws IOException, MessagingException {
        return sendSMTPMessage(host, port, user, pwd, new SimpleMessage(toEmail, subject, body))[0];
    }

    /**
     * Send simple messages (to,subject,body)
     *
     * @param host
     * @param port
     * @param user
     * @param pwd
     * @param msgs
     * @return
     * @throws IOException
     * @throws MessagingException
     */
    public static String[] sendSMTPMessage(String host, int port, String user, String pwd, SimpleMessage... msgs) throws IOException, MessagingException {
        String[] r = new String[msgs.length];

        Properties props = new Properties();
        props.put("mail.smtp.host", host);//"smtp.gmail.com"); //SMTP Host
        props.put("mail.smtp.port", (port != 0) ? "" + port : "587"); //TLS Port
        props.put("mail.smtp.auth", "true"); //enable authentication
        props.put("mail.smtp.starttls.enable", "true"); //enable STARTTLS

        //create Authenticator object to pass in Session.getInstance argument
        Authenticator auth = new Authenticator() {
            //override the getPasswordAuthentication method
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pwd);
            }
        };
        Session session = Session.getInstance(props, auth);

        for (int i = 0; i < msgs.length; i++) {
            if (msgs[i] == null || msgs[i].to == null) {
                continue;
            }
            String toEmail = msgs[i].to;
            String subject = msgs[i].subject;
            String body = msgs[i].body;

            MimeMessage msg = new MimeMessage(session);
            //set message headers
            msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
            msg.addHeader("format", "flowed");
            msg.addHeader("Content-Transfer-Encoding", "8bit");

            if (msgs[i].from != null && msgs[i].from.length > 0) {
                msg.setFrom(new InternetAddress(msgs[i].from[0], msgs[i].from[1]));
            } else {
                msg.setFrom(new InternetAddress("no_reply@example.com", "NoReply"));
            }
            if (msgs[i].replyTo != null && msgs[i].replyTo.length > 0) {
                msg.setReplyTo(new InternetAddress[]{
                    new InternetAddress(msgs[i].replyTo[0], msgs[i].replyTo[1])
                });
            } else {
                msg.setReplyTo(InternetAddress.parse("no_reply@example.com", false));
            }
            msg.setSubject(subject, "UTF-8");
            msg.setText(body, "UTF-8");
            msg.setSentDate(new Date());
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));

            Transport.send(msg);
            msgs[i].id = msg.getMessageID();
            r[i] = msg.getMessageID();
        }
        return r;
    }

    public static interface EmailMessageListener extends Serializable {

        Message[] search(Folder folder) throws MessagingException;

        boolean onCheckMessage(Session session, Message msg);

        boolean canMoveMessage(Session session, Folder toFolder, Message msg);

        boolean canDeleteMessage(Session session, Message msg);
    }

    public static List<Message> checkIMAPMessages(String host, int port, String user, String pwd, String folder, Long startFrom, EmailMessageListener callback) throws IOException, MessagingException {
        return EMailTools.checkIMAPMessages(host, port, user, pwd, new String[]{folder}, startFrom, callback)[0];
    }

    public static List<Message>[] checkIMAPMessages(String host, int port, String user, String pwd, String[] folders, Long startFrom, EmailMessageListener callback) throws IOException, MessagingException {
        List<Message>[] rr = new List[folders.length];
        //create properties field
        Properties properties = new Properties();

        properties.put("mail.imap.host", host);
        properties.put("mail.imap.port", (port != 0) ? "" + port : "993");
        properties.put("mail.imap.starttls.enable", "true");
        Session emailSession = Session.getDefaultInstance(properties);

        //create the POP3 store object and connect with the pop server
        Store store = emailSession.getStore("imaps");

        try {
            store.connect(host, user, pwd);

//            Folder[] mailFolders = store.getDefaultFolder().list("*");
//            for (Folder ff : mailFolders) {
//                System.out.println("Folder[" + ff.exists() + "]: " + ff.getName() + " = " + ff.getFullName());
//            }
//            Folder sentItemsFolder = store.getFolder("Sent Items");
//            System.out.println("Folder[" + sentItemsFolder.exists() + "]: " + sentItemsFolder.getName() + " = " + sentItemsFolder.getFullName());
            for (int fi = 0; fi < folders.length; fi++) {
                String folder = folders[fi];

                List<Message> r = new ArrayList<>();
                //create the folder object and open it
                Folder emailFolder = store.getFolder(folder != null ? folder : "INBOX");
                if (!emailFolder.exists()) {
                    // ignore non-existing folders...
                    continue;
                }
                emailFolder.open(Folder.READ_ONLY);

                // retrieve the messages from the folder in an array and print it
                Message[] messages = null;

                if (callback != null) {
                    messages = callback.search(emailFolder);
                }
                if (messages == null) {
                    messages = emailFolder.getMessages();
                }
                if (messages == null) {
                    continue;
                }
                //System.out.println("messages.length---" + messages.length);

                for (int i = 0, n = messages.length; i < n; i++) {
                    Message message = messages[i];
                    Date dt = message.getReceivedDate();
                    if (startFrom == null || dt.getTime() >= startFrom) {
                        if (callback != null) {
                            if (callback.onCheckMessage(emailSession, message)) {
                                r.add(message);
                            }
                        } else {
                            System.out.println("---------------------------------");
                            System.out.println("Email Number " + (i + 1));
                            System.out.println("Subject: " + message.getSubject());
                            System.out.println("From: " + message.getFrom()[0]);
                            System.out.println("Text: " + message.getContent().toString());
                            for (Header h : Collections.list(message.getAllHeaders())) {
                                System.out.println("  H:" + h.getName() + "=" + h.getValue());
                            }
                            r.add(message);
                        }
                    }
                }

                //close the store and folder objects
                emailFolder.close(false);
            }
        } finally {
            store.close();
        }
        return rr;
    }

    /**
     * Moves messages from "fromFolder" (default - INBOX) to "toFolder"
     * (defqault - OLD, created if missing) that are allowed for move by
     * "callback".
     *
     * @param host
     * @param port
     * @param user
     * @param pwd
     * @param fromFolder
     * @param toFolder
     * @param callback
     * @return
     * @throws IOException
     * @throws MessagingException
     */
    public static List<Message> moveIMAPMessages(String host, int port, String user, String pwd, String fromFolder, String toFolder, EmailMessageListener callback) throws IOException, MessagingException {
        List<Message> r = new ArrayList<>();
        if (callback == null) {
            return r;
        }

        //create properties field
        Properties properties = new Properties();

        properties.put("mail.imap.host", host);
        properties.put("mail.imap.port", (port != 0) ? "" + port : "993");
        properties.put("mail.imap.starttls.enable", "true");
        Session emailSession = Session.getDefaultInstance(properties);

        //create the POP3 store object and connect with the pop server
        Store store = emailSession.getStore("imaps");

        try {
            store.connect(host, user, pwd);

            //create the folder object and open it
            Folder sourceFolder = store.getFolder(fromFolder != null ? fromFolder : "INBOX");
            sourceFolder.open(Folder.READ_WRITE);
            Folder targetFolder = store.getFolder(toFolder != null ? toFolder : "OLD");
            if (!targetFolder.exists()) {
                targetFolder.create(Folder.HOLDS_MESSAGES);
                targetFolder.setSubscribed(true);
            }
            targetFolder.open(Folder.READ_WRITE);

            // retrieve the messages from the folder in an array and print it
            Message[] messages = sourceFolder.getMessages();

            for (int i = 0, n = messages.length; i < n; i++) {
                Message message = messages[i];
                if (callback.canMoveMessage(emailSession, targetFolder, message)) {
                    r.add(message);
                }
            }

            if (!r.isEmpty()) {
                targetFolder.appendMessages(r.toArray(new Message[r.size()]));
                for (Message m : r) {
                    m.setFlag(Flags.Flag.DELETED, true);
                }
                sourceFolder.expunge();
            }

            //close the store and folder objects
            sourceFolder.close(false);
            targetFolder.close(false);
        } finally {
            store.close();
        }
        return r;
    }

    public static List<Message> deleteIMAPMessages(String host, int port, String user, String pwd, String[] folders, boolean purge, EmailMessageListener callback) throws IOException, MessagingException {
        List<Message> r = new ArrayList<>();
        if (callback == null) {
            return r;
        }

        //create properties field
        Properties properties = new Properties();

        properties.put("mail.imap.host", host);
        properties.put("mail.imap.port", (port != 0) ? "" + port : "993");
        properties.put("mail.imap.starttls.enable", "true");
        Session emailSession = Session.getDefaultInstance(properties);

        //create the IMAP store object and connect with the pop server
        Store store = emailSession.getStore("imaps");

        try {
            store.connect(host, user, pwd);

            for (String folder : folders) {
                if (folder == null || folder.trim().isEmpty()) {
                    continue;
                }
                //open the folder
                Folder emailFolder = store.getFolder(folder);
                if (!emailFolder.exists()) {
                    continue;
                }
                emailFolder.open(Folder.READ_WRITE);

                // retrieve the messages from the folder in an array and check if deletable
                Message[] messages = emailFolder.getMessages();

                for (int i = 0, n = messages.length; i < n; i++) {
                    Message message = messages[i];
                    if (callback.canDeleteMessage(emailSession, message)) {
                        r.add(message);
                    }
                }

                if (!r.isEmpty()) {
                    for (Message m : r) {
                        m.setFlag(Flags.Flag.DELETED, true);
                    }
                    if (purge) {
                        emailFolder.expunge();
                    }
                }

                //close folder object
                emailFolder.close(false);
            }
        } finally {
            store.close();
        }
        return r;
    }

    public static List<IMAPFolder> listIMAPFolders(String host, int port, String user, String pwd) throws IOException, MessagingException {
        List<IMAPFolder> r = new ArrayList<>();
        //create properties field
        Properties properties = new Properties();

        properties.put("mail.imap.host", host);
        properties.put("mail.imap.port", (port != 0) ? "" + port : "993");
        properties.put("mail.imap.starttls.enable", "true");
        Session emailSession = Session.getDefaultInstance(properties);

        //create the POP3 store object and connect with the pop server
        Store store = emailSession.getStore("imaps");

        try {
            store.connect(host, user, pwd);

            Folder[] mailFolders = store.getDefaultFolder().list("*");
            for (Folder ff : mailFolders) {
                //System.out.println("Folder["+ff.exists()+"]: "+ff.getName()+" = "+ff.getFullName());
                if (ff instanceof IMAPFolder) {
                    r.add((IMAPFolder) ff);
                }
            }
        } finally {
            store.close();
        }
        return r;
    }

//    public static class EmailUtil {
//
//        /**
//         * Utility method to send simple HTML email
//         *
//         * @param session
//         * @param toEmail
//         * @param subject
//         * @param body
//         */
//        public static void sendEmail(Session session, String toEmail, String subject, String body) {
//            try {
//                MimeMessage msg = new MimeMessage(session);
//                //set message headers
//                msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
//                msg.addHeader("format", "flowed");
//                msg.addHeader("Content-Transfer-Encoding", "8bit");
//
//                msg.setFrom(new InternetAddress("no_reply@example.com", "NoReply-JD"));
//
//                msg.setReplyTo(InternetAddress.parse("no_reply@example.com", false));
//
//                msg.setSubject(subject, "UTF-8");
//
//                msg.setText(body, "UTF-8");
//
//                msg.setSentDate(new Date());
//
//                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
//                System.out.println("Message is ready");
//                Transport.send(msg);
//
//                System.out.println("EMail Sent Successfully!!");
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        /**
//         * Utility method to send email with attachment
//         *
//         * @param session
//         * @param toEmail
//         * @param subject
//         * @param body
//         */
//        public static void sendAttachmentEmail(Session session, String toEmail, String subject, String body) {
//            try {
//                MimeMessage msg = new MimeMessage(session);
//                msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
//                msg.addHeader("format", "flowed");
//                msg.addHeader("Content-Transfer-Encoding", "8bit");
//
//                msg.setFrom(new InternetAddress("no_reply@example.com", "NoReply-JD"));
//
//                msg.setReplyTo(InternetAddress.parse("no_reply@example.com", false));
//
//                msg.setSubject(subject, "UTF-8");
//
//                msg.setSentDate(new Date());
//
//                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
//
//                // Create the message body part
//                BodyPart messageBodyPart = new MimeBodyPart();
//
//                // Fill the message
//                messageBodyPart.setText(body);
//
//                // Create a multipart message for attachment
//                Multipart multipart = new MimeMultipart();
//
//                // Set text message part
//                multipart.addBodyPart(messageBodyPart);
//
//                // Second part is attachment
//                messageBodyPart = new MimeBodyPart();
//                String filename = "abc.txt";
//                DataSource source = new FileDataSource(filename);
//                messageBodyPart.setDataHandler(new DataHandler(source));
//                messageBodyPart.setFileName(filename);
//                multipart.addBodyPart(messageBodyPart);
//
//                // Send the complete message parts
//                msg.setContent(multipart);
//
//                // Send message
//                Transport.send(msg);
//                System.out.println("EMail Sent Successfully with attachment!!");
//            } catch (MessagingException e) {
//                e.printStackTrace();
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }
//        }
//
//        /**
//         * Utility method to send image in email body
//         *
//         * @param session
//         * @param toEmail
//         * @param subject
//         * @param body
//         */
//        public static void sendImageEmail(Session session, String toEmail, String subject, String body) {
//            try {
//                MimeMessage msg = new MimeMessage(session);
//                msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
//                msg.addHeader("format", "flowed");
//                msg.addHeader("Content-Transfer-Encoding", "8bit");
//
//                msg.setFrom(new InternetAddress("no_reply@example.com", "NoReply-JD"));
//
//                msg.setReplyTo(InternetAddress.parse("no_reply@example.com", false));
//
//                msg.setSubject(subject, "UTF-8");
//
//                msg.setSentDate(new Date());
//
//                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
//
//                // Create the message body part
//                BodyPart messageBodyPart = new MimeBodyPart();
//
//                messageBodyPart.setText(body);
//
//                // Create a multipart message for attachment
//                Multipart multipart = new MimeMultipart();
//
//                // Set text message part
//                multipart.addBodyPart(messageBodyPart);
//
//                // Second part is image attachment
//                messageBodyPart = new MimeBodyPart();
//                String filename = "image.png";
//                DataSource source = new FileDataSource(filename);
//                messageBodyPart.setDataHandler(new DataHandler(source));
//                messageBodyPart.setFileName(filename);
//                //Trick is to add the content-id header here
//                messageBodyPart.setHeader("Content-ID", "image_id");
//                multipart.addBodyPart(messageBodyPart);
//
//                //third part for displaying image in the email body
//                messageBodyPart = new MimeBodyPart();
//                messageBodyPart.setContent("<h1>Attached Image</h1>"
//                        + "<img src='cid:image_id'>", "text/html");
//                multipart.addBodyPart(messageBodyPart);
//
//                //Set the multipart message to the email message
//                msg.setContent(multipart);
//
//                // Send message
//                Transport.send(msg);
//                System.out.println("EMail Sent Successfully with image!!");
//            } catch (MessagingException e) {
//                e.printStackTrace();
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    public static class TLSEmail {
//
//        /**
//         * Outgoing Mail (SMTP) Server requires TLS or SSL: smtp.gmail.com (use
//         * authentication) Use Authentication: Yes Port for TLS/STARTTLS: 587
//         */
//        public static void main(String[] args) {
//            final String fromEmail = "myemailid@gmail.com"; //requires valid gmail id
//            final String password = "mypassword"; // correct password for gmail id
//            final String toEmail = "myemail@yahoo.com"; // can be any email id 
//
//            System.out.println("TLSEmail Start");
//            Properties props = new Properties();
//            props.put("mail.smtp.host", "smtp.gmail.com"); //SMTP Host
//            props.put("mail.smtp.port", "587"); //TLS Port
//            props.put("mail.smtp.auth", "true"); //enable authentication
//            props.put("mail.smtp.starttls.enable", "true"); //enable STARTTLS
//
//            //create Authenticator object to pass in Session.getInstance argument
//            Authenticator auth = new Authenticator() {
//                //override the getPasswordAuthentication method
//                protected PasswordAuthentication getPasswordAuthentication() {
//                    return new PasswordAuthentication(fromEmail, password);
//                }
//            };
//            Session session = Session.getInstance(props, auth);
//
//            EmailUtil.sendEmail(session, toEmail, "TLSEmail Testing Subject", "TLSEmail Testing Body");
//
//        }
//
//    }
//
//    public static class SSLEmail {
//
//        /**
//         * Outgoing Mail (SMTP) Server requires TLS or SSL: smtp.gmail.com (use
//         * authentication) Use Authentication: Yes Port for SSL: 465
//         */
//        public static void main(String[] args) {
//            final String fromEmail = "myemailid@gmail.com"; //requires valid gmail id
//            final String password = "mypassword"; // correct password for gmail id
//            final String toEmail = "myemail@yahoo.com"; // can be any email id 
//
//            System.out.println("SSLEmail Start");
//            Properties props = new Properties();
//            props.put("mail.smtp.host", "smtp.gmail.com"); //SMTP Host
//            props.put("mail.smtp.socketFactory.port", "465"); //SSL Port
//            props.put("mail.smtp.socketFactory.class",
//                    "javax.net.ssl.SSLSocketFactory"); //SSL Factory Class
//            props.put("mail.smtp.auth", "true"); //Enabling SMTP Authentication
//            props.put("mail.smtp.port", "465"); //SMTP Port
//
//            Authenticator auth = new Authenticator() {
//                //override the getPasswordAuthentication method
//                protected PasswordAuthentication getPasswordAuthentication() {
//                    return new PasswordAuthentication(fromEmail, password);
//                }
//            };
//
//            Session session = Session.getDefaultInstance(props, auth);
//            System.out.println("Session created");
//            EmailUtil.sendEmail(session, toEmail, "SSLEmail Testing Subject", "SSLEmail Testing Body");
//
//            EmailUtil.sendAttachmentEmail(session, toEmail, "SSLEmail Testing Subject with Attachment", "SSLEmail Testing Body with Attachment");
//
//            EmailUtil.sendImageEmail(session, toEmail, "SSLEmail Testing Subject with Image", "SSLEmail Testing Body with Image");
//
//        }
//    }
//
//    public static class CheckEMail {
//
//        public static void check(String host, String storeType, String user,
//                String password) {
//            try {
//
//                //create properties field
//                Properties properties = new Properties();
//
//                properties.put("mail.pop3.host", host);
//                properties.put("mail.pop3.port", "995");
//                properties.put("mail.pop3.starttls.enable", "true");
//                Session emailSession = Session.getDefaultInstance(properties);
//
//                //create the POP3 store object and connect with the pop server
//                Store store = emailSession.getStore("pop3s");
//
//                store.connect(host, user, password);
//
//                //create the folder object and open it
//                Folder sourceFolder = store.getFolder("INBOX");
//                sourceFolder.open(Folder.READ_ONLY);
//
//                // retrieve the messages from the folder in an array and print it
//                Message[] messages = sourceFolder.getMessages();
//                System.out.println("messages.length---" + messages.length);
//
//                for (int i = 0, n = messages.length; i < n; i++) {
//                    Message message = messages[i];
//                    System.out.println("---------------------------------");
//                    System.out.println("Email Number " + (i + 1));
//                    System.out.println("Subject: " + message.getSubject());
//                    System.out.println("From: " + message.getFrom()[0]);
//                    System.out.println("Text: " + message.getContent().toString());
//
//                }
//
//                //close the store and folder objects
//                sourceFolder.close(false);
//                store.close();
//
//            } catch (NoSuchProviderException e) {
//                e.printStackTrace();
//            } catch (MessagingException e) {
//                e.printStackTrace();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        public static void main(String[] args) {
//
//            String host = "pop.gmail.com";// change accordingly
//            String mailStoreType = "pop3";
//            String username = "yourmail@gmail.com";// change accordingly
//            String password = "*****";// change accordingly
//
//            check(host, mailStoreType, username, password);
//
//        }
//
//    }
    /**
     * Structure to pass email, subject, body and return message id or error.
     */
    public static class SimpleMessage implements Serializable {

        private static long serialVersionUID = 1;

        long toTimestamp = System.currentTimeMillis();
        //
        public String[] from; // email, alias
        public String[] replyTo; // email, alias
        public String to;
        public String subject;
        public String body;
        //
        long idTimestamp;
        public String id;
        //
        public long sentTimestamp;
        public String error;

        public SimpleMessage(
                String to,
                String subject,
                String body
        ) {
            this.to = to;
            this.subject = subject;
            this.body = body;
        }

        public SimpleMessage(
                String[] from,
                String[] replyTo,
                String to,
                String subject,
                String body
        ) {
            this.to = to;
            this.subject = subject;
            this.body = body;
        }

        public void setId(String id) {
            idTimestamp = System.currentTimeMillis();
            this.id = id;
        }

        public void setSent(String error) {
            sentTimestamp = System.currentTimeMillis();
            this.error = error;
        }

        @Override
        public String toString() {
            return "SimpleMessage{"
                    + "\n  toTimestamp=" + toTimestamp
                    + ((from != null && from.length > 0) ? "\n  from=" + from[1] + " <" + from[0] + ">" : "")
                    + ((replyTo != null && replyTo.length > 0) ? "\n  replyTo=" + replyTo[1] + " <" + replyTo[0] + ">" : "")
                    + "\n  to=" + to
                    + "\n  subject=" + subject
                    + "\n  body=" + body
                    + ((idTimestamp > 0)
                            ? "\n  idTimestamp=" + idTimestamp + "\n  id=" + id
                            : "")
                    + ((sentTimestamp > 0)
                            ? "\n  sentTimestamp=" + sentTimestamp
                            + ((error != null) ? "\n  error=" + error : "") : "")
                    + "\n}";
        }

    }

    public static void main(String... args) throws Exception {
        List<IMAPFolder> folders0 = EMailTools.listIMAPFolders("imap.gmail.com", 0, "000sobrat@gmail.com", "T1234567.0");
        List<IMAPFolder> folders1 = EMailTools.listIMAPFolders("imap.gmail.com", 0, "001sobrat@gmail.com", "T1234567.1");

        for (List<IMAPFolder> folders : new List[]{folders0, folders1}) {
            System.out.println("FOLDERS[" + folders.size() + "]:");
            for (IMAPFolder f : folders) {
                String[] attrs = f.getAttributes();
                System.out.print("  F: " + f.getName() + "  " + f.getFullName() + "; ");
                for (String a : attrs) {
                    System.out.print(a + " ");
                }
                System.out.println();
            }
        }

        final List<Message> messages0 = EMailTools.checkIMAPMessages("imap.gmail.com", 0, "000sobrat@gmail.com", "T1234567.0", (String) null, null, null);
        final List<Message> messages1 = EMailTools.checkIMAPMessages("imap.gmail.com", 0, "001sobrat@gmail.com", "T1234567.1", (String) null, null, null);

        if (1 == 0) {
            EMailTools.sendSMTPMessage("smtp.gmail.com", 0, "001sobrat@gmail.com", "T1234567.1", "000sobrat@gmail.com", "Test at " + System.currentTimeMillis() + " / " + System.nanoTime(), "This is mail from <href mailto: 001sobrat@gmail.com>T1</href>.");
            EMailTools.sendSMTPMessage("smtp.gmail.com", 0, "001sobrat@gmail.com", "T1234567.1", "002sobrat@gmail.com", "Test at " + System.currentTimeMillis() + " / " + System.nanoTime(), "This is mail from <href mailto: 001sobrat@gmail.com>T1</href>.");
            Thread.sleep(1000);
            EMailTools.sendSMTPMessage("smtp.gmail.com", 0, "001sobrat@gmail.com", "T1234567.1", "000sobrat@gmail.com", "Test at " + System.currentTimeMillis() + " / " + System.nanoTime(), "This is mail from <href mailto: 001sobrat@gmail.com>T1</href>.");
            Thread.sleep(1000 * 10);
            List<Message> messages01 = EMailTools.checkIMAPMessages("imap.gmail.com", 0, "000sobrat@gmail.com", "T1234567.0", (String) null, null, null);
            List<Message> messages11 = EMailTools.checkIMAPMessages("imap.gmail.com", 0, "001sobrat@gmail.com", "T1234567.1", (String) null, null, null);
        }

        if (1 == 0) {
            if (!messages0.isEmpty()) {
                System.out.println("MOVE " + messages0.size() + " messages to TEST_OLD folder");
                for (Message m : messages0) {
                    System.out.println("  " + m.getFolder().getFullName() + "[" + m.getMessageNumber() + "]: " + m.getSubject());
                }
                // move all to TEST_OLD folder
                System.out.println("MOVE: Start");
                List<Message> messages02 = EMailTools.moveIMAPMessages("imap.gmail.com", 0, "000sobrat@gmail.com", "T1234567.0", (String) null, "TEST_OLD", new EmailMessageListener() {
                    @Override
                    public Message[] search(Folder folder) throws MessagingException {
                        return folder.getMessages();
                    }

                    @Override
                    public boolean onCheckMessage(Session session, Message msg) {
                        return true;
                    }

                    @Override
                    public boolean canMoveMessage(Session session, Folder toFolder, Message msg) {
                        for (Message m : messages0) {
                            if (m.getMessageNumber() == msg.getMessageNumber()) {
                                System.out.print("MOVE: enable " + "  " + msg.getFolder().getFullName() + "[" + msg.getMessageNumber() + "]: ");
                                try {
                                    System.out.println(msg.getSubject());
                                } catch (MessagingException mex) {
                                    System.out.println(mex);
                                }
                                return true;
                            }
                        }
                        System.out.print("MOVE: disable " + "  " + msg.getFolder().getFullName() + "[" + msg.getMessageNumber() + "]: ");
                        try {
                            System.out.println(msg.getSubject());
                        } catch (MessagingException mex) {
                            System.out.println(mex);
                        }
                        return false;
                    }

                    @Override
                    public boolean canDeleteMessage(Session session, Message msg) {
                        return false;
                    }
                });
                System.out.println("MOVE: Done: moved " + messages02.size());
                for (Message m : messages0) {
                    System.out.println("  " + m.getFolder().getFullName() + "[" + m.getMessageNumber() + "]: " + m.getSubject());
                }
            }
        }

        int a = 0;
    }
}
