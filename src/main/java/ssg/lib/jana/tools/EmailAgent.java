/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssg.lib.jana.tools;

import com.sun.mail.imap.IMAPFolder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.search.FlagTerm;
import javax.mail.search.MessageIDTerm;
import javax.mail.search.OrTerm;
import javax.mail.search.SearchTerm;
import ssg.lib.jana.tools.EMailTools.EmailMessageListener;
import ssg.lib.jana.tools.EMailTools.SimpleMessage;
import ssg.lib.jana.tools.EmailAgent.EMailAccount.MediatorAccount;

/**
 *
 * @author sesidoro
 */
public class EmailAgent implements Runnable, Serializable {

    private static long serialVersionUID = 1;

    String[] admins;
    MediatorAccount mediator;
    String from[]; // optional replacement "from": email, alias
    String replyTo[]; // optional replacement "replyTo": email, alias

    List<EMailListener> listeners = new ArrayList<>();
    List<SimpleMessage> outgoing = new ArrayList<>();
    Map<String, SimpleMessage> verifyOutgoing = new HashMap<>();
    boolean active = false;
    boolean stopped = false;
    long checkInterval = TimeTools.ONE_MIN * 2 + TimeTools.ONE_SEC * 45;
    long minCheckInterval = TimeTools.ONE_SEC * 15;
    long lastCycleTimestamp = 0;
    long lastCycleDuration = 0;
    float activeTime = 0;
    long sentCount = 0;
    long receivedCount = 0;
    int cyclesCount = 0;
    int runCycleErrors = 0;
    Throwable lastCheckError = null;
    Throwable lastSendError = null;
    // optional owned process (running thread)
    Thread process = null;

    public EmailAgent(MediatorAccount mediator, String... admins) {
        this.admins = admins;
        this.mediator = mediator;
    }

    @Override
    public void run() {
        if (active) {
            return;
        }
        active = true;
        stopped = false;
        while (!stopped) {
            try {
                // ensure mediator has in/out box info...
                try {
                    checkMediator(mediator);
                } catch (Throwable th) {
                    th.printStackTrace();
                }

                lastCycleTimestamp = System.currentTimeMillis();
                long startedNS = System.nanoTime();
                runCycle();
                activeTime += ((System.nanoTime() - startedNS) / 1000000f);
                lastCycleDuration = System.currentTimeMillis() - lastCycleTimestamp;
                if (stopped) {
                    break;
                }
                long wait = checkInterval - System.currentTimeMillis() - lastCycleTimestamp;
                if (wait < minCheckInterval) {
                    wait = minCheckInterval;
                }
                Thread.sleep(wait);
            } catch (InterruptedException iex) {
                break;
            } finally {
                active = false;
                stopped = false;
                if (process != null) {
                    process = null;
                }
            }
        }
    }

    public void start() {
        if (!active) {
            Thread thread = new Thread(this);
            thread.setDaemon(true);
            process = thread;
            thread.start();
        }
    }

    public void stop() {
        if (active && !stopped) {
            stopped = true;
        }
    }

    public void runCycle() {
        cyclesCount++;
        try {
            checkIncomingMessages();
        } catch (Throwable checkError) {
            runCycleErrors++;
            lastCheckError = checkError;
        }
        try {
            sendOutgoingMessages();
        } catch (Throwable sendError) {
            runCycleErrors++;
            lastSendError = sendError;
        }
    }

    public void resetCounters() {
        lastCycleTimestamp = 0;
        lastCycleDuration = 0;
        activeTime = 0;
        sentCount = 0;
        receivedCount = 0;
        cyclesCount = 0;
        runCycleErrors = 0;
        lastCheckError = null;
        lastSendError = null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("EmailAgent{");
        if (admins != null && admins.length > 0) {
            sb.append("\n  admins[" + admins.length + "]:");
            for (String admin : admins) {
                sb.append("\n    " + admin);
            }
        }
        if (mediator != null) {
            sb.append("\n  mediator=" + mediator.toString().replace("\n", "\n    "));
        }
        if (listeners != null && !listeners.isEmpty()) {
            sb.append("\n  listeners[" + listeners.size() + "]:");
            for (EMailListener l : listeners) {
                if (l != null) {
                    sb.append("\n    " + l.toString().replace("\n", "\n    "));
                }
            }
        }
        if (outgoing != null && !outgoing.isEmpty()) {
            sb.append("\n  outgoing[" + outgoing.size() + "]:");
            for (SimpleMessage sm : outgoing.toArray(new SimpleMessage[outgoing.size()])) {
                sb.append("\n    " + sm.to + "  " + sm.subject);
            }
        }
        if (verifyOutgoing != null && !verifyOutgoing.isEmpty()) {
            sb.append("\n  verifyOutgoing[" + verifyOutgoing.size() + "]:");
            for (SimpleMessage sm : verifyOutgoing.values().toArray(new SimpleMessage[verifyOutgoing.size()])) {
                sb.append("\n    " + sm.id + "  " + sm.to + "  " + sm.subject);
            }
        }
        sb.append("\n  STATUS:");
        sb.append(getStatus("\n    "));
        sb.append("\n}");
        return sb.toString();
    }

    public String getStatus(String separator) {
        if (separator == null) {
            separator = "\n  ";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(separator);
        sb.append("process=");
        sb.append(process);
        sb.append(separator);
        sb.append("active=");
        sb.append(active);
        sb.append(separator);
        sb.append("stopped=");
        sb.append(stopped);
        sb.append(separator);
        sb.append("checkInterval=");
        sb.append(checkInterval);
        sb.append(separator);
        sb.append("minCheckInterval=");
        sb.append(minCheckInterval);
        sb.append(separator);
        sb.append("lastCycleTimestamp=");
        sb.append(lastCycleTimestamp);
        sb.append(separator);
        sb.append("lastCycleDuration=");
        sb.append(lastCycleDuration);
        sb.append(separator);
        sb.append("activeTime=");
        sb.append(activeTime);
        sb.append(separator);
        sb.append("sentCount=");
        sb.append(sentCount);
        sb.append(separator);
        sb.append("receivedCount=");
        sb.append(receivedCount);
        sb.append(separator);
        sb.append("cyclesCount=");
        sb.append(cyclesCount);
        sb.append(separator);
        sb.append("runCycleErrors=");
        sb.append(runCycleErrors);
        sb.append(separator);
        sb.append("lastCheckError=");
        sb.append(lastCheckError);
        sb.append(separator);
        sb.append("lastSendError=");
        sb.append(lastSendError);
        return sb.toString();
    }

    public void checkMediator(MediatorAccount mediator) throws IOException, MessagingException {
        if (mediator.sentItems == null) {
            List<IMAPFolder> folders = EMailTools.listIMAPFolders(
                    mediator.imapHost.getHost(),
                    mediator.imapHost.getPort(),
                    mediator.email,
                    mediator.pwd
            );
            for (IMAPFolder f : folders) {
                if (f.getName().startsWith("Sent ")) {
                    mediator.sentItems = f.getFullName();
                    break;
                }
                String[] attrs = f.getAttributes();
                for (String a : attrs) {
                    if ("\\Sent".equalsIgnoreCase(a)) {
                        mediator.sentItems = f.getFullName();
                        break;
                    }
                }
            }
        }
    }

    public void checkIncomingMessages() throws IOException, MessagingException {
        if (mediator == null || mediator.imapHost == null) {
            return;
        }

        List<Message> toMove = new ArrayList<>();

        String[] folders = (mediator.sentItems != null) ? new String[]{mediator.inbox, mediator.sentItems} : new String[]{mediator.inbox};
        List<Message>[] checked = EMailTools.checkIMAPMessages(mediator.imapHost.getHost(),
                mediator.imapHost.getPort(),
                mediator.email,
                mediator.pwd,
                folders,
                null,
                new EmailMessageListener() {
            @Override
            public Message[] search(Folder folder) throws MessagingException {
                if (mediator.inbox.equals(folder.getName())) {
                    FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
                    Message messages[] = folder.search(ft);
                    return messages;
                } else if (mediator.sentItems.equalsIgnoreCase(folder.getName()) || mediator.sentItems.equalsIgnoreCase(folder.getFullName())) {
                    SearchTerm st = new OrTerm(new MessageIDTerm(""), new MessageIDTerm(""));

                    for (String msgID : verifyOutgoing.keySet()) {
                        if (st == null) {
                            st = new MessageIDTerm(msgID);
                        } else {
                            st = new OrTerm(st, new MessageIDTerm(msgID));
                        }
                    }
                    Message[] messages = folder.search(st);
//                    Message[] messages = folder.search(new SearchTerm() {
//                        @Override
//                        public boolean match(Message msg) {
//                            String[] messageID = getMessageHeaderValues(msg, "Message-ID");
//                            if (messageID != null && verifyOutgoing.containsKey(messageID[0])) {
//                                return true;
//                            } else {
//                                return false;
//                            }
//                        }
//                    });
                    return messages;
                } else {
                    return folder.getMessages();
                }
            }

            @Override
            public boolean onCheckMessage(Session session, Message msg) {
                String[] messageID = getMessageHeaderValues(msg, "Message-ID");
                String[] returnPath = getMessageHeaderValues(msg, "Return-Path");
                String[] references = getMessageHeaderValues(msg, "References");
                String[] inReplyTo = getMessageHeaderValues(msg, "In-Reply-To");
                String[] failedRecipients = getMessageHeaderValues(msg, "X-Failed-Recipients");

                if (mediator.inbox.equalsIgnoreCase(msg.getFolder().getName())) {
                    // INBOX - responses: check if response to mediator message or if mediator message delivery failed
                    String responseTo = inReplyTo != null ? inReplyTo[0] : references != null ? references[0] : null;
                    if (responseTo != null && verifyOutgoing.containsKey(responseTo)) {
                        SimpleMessage sm = verifyOutgoing.remove(msg);
                        for (EMailListener l : listeners.toArray(new EMailListener[listeners.size()])) {
                            if (failedRecipients != null) {
                                l.onFailedToSendEMail(EmailAgent.this, sm.to, sm.subject, msg);
                            } else {
                                l.onReceivedEMail(EmailAgent.this, sm.to, sm.subject, msg);
                            }
                        }
                        toMove.add(msg);
                        return true;
                    }
                } else if (mediator.sentItems != null && (mediator.sentItems.equalsIgnoreCase(msg.getFolder().getName())
                        || mediator.sentItems.equalsIgnoreCase(msg.getFolder().getFullName()))) {
                    // SENT - register successfull send's...
                    String responseTo = (messageID != null) ? messageID[0] : null;
                    if (responseTo != null && verifyOutgoing.containsKey(responseTo)) {
                        SimpleMessage sm = verifyOutgoing.remove(responseTo);
                        for (EMailListener l : listeners.toArray(new EMailListener[listeners.size()])) {
                            l.onSentEMail(EmailAgent.this, sm.to, sm.subject, msg);
                        }
                        return true;
                    }
                } else {
                    //
                    int a = 0;
                }
                return false;
            }

            @Override
            public boolean canMoveMessage(Session session, Folder toFolder, Message msg) {
                return false;
            }

            @Override
            public boolean canDeleteMessage(Session session, Message msg) {
                return false;
            }
        });

        if (!toMove.isEmpty()) {
            Calendar cal = TimeTools.getCalendar(System.currentTimeMillis());
            List<Message> moved = EMailTools.moveIMAPMessages(
                    mediator.imapHost.getHost(),
                    mediator.imapHost.getPort(),
                    mediator.email,
                    mediator.pwd,
                    folders[0],
                    "OLD-" + cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1),
                    new EmailMessageListener() {
                @Override
                public Message[] search(Folder folder) throws MessagingException {
                    return null;
                }

                @Override
                public boolean canMoveMessage(Session session, Folder toFolder, Message msg) {
                    return toMove.contains(msg);
                }

                @Override
                public boolean canDeleteMessage(Session session, Message msg) {
                    return false;
                }

                @Override
                public boolean onCheckMessage(Session session, Message msg) {
                    return false;
                }
            });
        }
    }

    public String getMessageHeaderValue(Message msg, String headerName) {
        String[] ss = getMessageHeaderValues(msg, headerName);
        if (ss != null && ss.length > 0) {
            return ss[0];
        } else {
            return null;
        }
    }

    public String[] getMessageHeaderValues(Message msg, String headerName) {
        if (headerName == null || headerName.trim().isEmpty()) {
            return null;
        }
        try {
            String[] r = msg.getHeader(headerName);
            if (r != null && r.length == 0) {
                r = null;
            }
            return r;
        } catch (MessagingException mex) {
            return null;
        }
    }

    public void sendOutgoingMessages() throws IOException, MessagingException {
        if (mediator == null || mediator.smtpHost == null) {
            return;
        }
        if (!outgoing.isEmpty()) {
            synchronized (outgoing) {
                SimpleMessage[] msgs = outgoing.toArray(new SimpleMessage[outgoing.size()]);
                outgoing.clear();
                String[] sent = EMailTools.sendSMTPMessage(
                        mediator.smtpHost.getHost(),
                        mediator.smtpHost.getPort(),
                        mediator.email,
                        mediator.pwd,
                        msgs);
                synchronized (verifyOutgoing) {
                    for (int i = 0; i < sent.length; i++) {
                        if (sent[i] != null && msgs[i].id != null) {
                            verifyOutgoing.put(msgs[i].id, msgs[i]);
                            for (EMailListener l : listeners.toArray(new EMailListener[listeners.size()])) {
                                l.onSentEMail(EmailAgent.this, msgs[i].to, msgs[i].subject, null);
                            }
                            sentCount++;
                        }
                    }
                }
            }
        }
    }

    public void addEMailListener(EMailListener... ls) {
        if (ls != null) {
            for (EMailListener l : ls) {
                if (l != null && !listeners.contains(l)) {
                    listeners.add(l);
                }
            }
        }
    }

    public void removeEMailListener(EMailListener... ls) {
        if (ls != null) {
            for (EMailListener l : ls) {
                if (l != null && listeners.contains(l)) {
                    listeners.remove(l);
                }
            }
        }
    }

    public void sendFromMediator(String toEMail, String subject, String text) throws IOException {
        synchronized (outgoing) {
            outgoing.add(new SimpleMessage(toEMail, subject, text));
        }
    }

    public static class EMailAccount implements Serializable {

        private static long serialVersionUID = 1;

        URL imapHost;
        URL smtpHost;
        String email;
        String pwd;

        public EMailAccount() {
        }

        public EMailAccount(
                URL imapHost,
                URL smtpHost,
                String email,
                String pwd
        ) {
            this.imapHost = imapHost;
            this.smtpHost = smtpHost;
            this.email = email;
            this.pwd = pwd;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{"
                    + "email=" + email
                    + ", pwd=" + ((pwd != null) ? pwd.hashCode() : "<none>")
                    + ((imapHost != null) ? ", imapHost=" + imapHost : "")
                    + ((smtpHost != null) ? ", smtpHost=" + smtpHost : "")
                    + '}';
        }

        public static class MediatorAccount extends EMailAccount {

            private static long serialVersionUID = 1;

            String inbox = "INBOX";
            String sentItems;

            public MediatorAccount() {
            }

            public MediatorAccount(URL imapHost, URL smtpHost, String email, String pwd) {
                super(imapHost, smtpHost, email, pwd);
            }

            public MediatorAccount(URL imapHost, URL smtpHost, String email, String pwd, String inbox, String sentItems) {
                super(imapHost, smtpHost, email, pwd);
                if (inbox != null) {
                    this.inbox = inbox;
                }
                this.sentItems = sentItems;
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append(super.toString());
                return sb.toString();
            }

        }
    }

    public static interface EMailListener extends Serializable {

        /**
         * Handle message sent in response to mediator message to "from" with
         * given subject.
         *
         * @param agent
         * @param from
         * @param subject
         * @param msg
         */
        void onReceivedEMail(EmailAgent agent, String from, String subject, Message msg);

        void onSentEMail(EmailAgent agent, String to, String subject, Message msg);

        void onFailedToSendEMail(EmailAgent agent, String to, String subject, Message msg);
    }

    public static void main(String... args) throws Exception {
        File agentFile = new File("target/test.agent");
        EmailAgent agent = new EmailAgent(
                new MediatorAccount(
                        new URL("http://imap.gmail.com:993"),
                        new URL("http://smtp.gmail.com:587"),
                        "001sobrat@gmail.com",
                        "T1234567.1"
                ),
                "000sobrat@gmail.com"
        );

        boolean restored = false;
        if (agentFile.exists()) {
            ObjectInputStream is = null;
            try {
                is = new ObjectInputStream(new FileInputStream(agentFile));
                agent = (EmailAgent) is.readObject();
                restored = true;
                agent.lastCheckError = null;
                agent.lastSendError = null;
                is.close();
            } catch (Throwable th) {
                th.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (Throwable th) {
                }
            }
        }

        if (!restored) {
            agent.addEMailListener(new EMailListener() {
                @Override
                public void onReceivedEMail(EmailAgent agent, String from, String subject, Message msg) {
                    System.out.println("RECIVED[" + from + "]: "
                            + subject
                            + ((msg != null) ? "  " + agent.getMessageHeaderValue(msg, "Message-ID") : "")
                    );
                }

                @Override
                public void onSentEMail(EmailAgent agent, String to, String subject, Message msg) {
                    System.out.println("SENT[" + ((msg != null) ? "OK " : "???") + "]: "
                            + to
                            + "  " + subject
                            + ((msg != null) ? "  " + agent.getMessageHeaderValue(msg, "Message-ID") : "")
                    );
                }

                @Override
                public void onFailedToSendEMail(EmailAgent agent, String to, String subject, Message msg) {
                    System.out.println("SENT[ERR]: "
                            + to
                            + "  " + subject
                            + ((msg != null) ? "  " + agent.getMessageHeaderValue(msg, "Message-ID") : "")
                    );
                }
            });
        }

        agent.checkInterval = TimeTools.ONE_SEC * 15;
        agent.minCheckInterval = agent.checkInterval;
        agent.start();
        System.out.println(agent);
        int maxMsg = 2;
        System.out.println("\n\n----------------------------------- SEND " + maxMsg + " messages...");
        for (int i = 0; i < maxMsg; i++) {
            agent.sendFromMediator("000sobrat@gmail.com", "T:" + i, "Test message " + i);
        }
        System.out.println(agent);
        System.out.println("\n\n-----------------------------------");

        long timeout = System.currentTimeMillis() + TimeTools.ONE_MIN * 10;
        while (System.currentTimeMillis() < timeout && agent.active) {
            System.out.println("\n\n-----------------------------------");
            System.out.println(agent);
            Thread.sleep(1000 * 10);
        }

        System.out.println("\n\n-----------------------------------STOP ");
        agent.stop();
        System.out.println("\n\n-----------------------------------");
        System.out.println(agent);
        while (agent.active) {
            Thread.sleep(100);
        }
        System.out.println("\n\n-----------------------------------STOPPED ");
        System.out.println("\n\n-----------------------------------");
        System.out.println(agent);

        try {
            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(agentFile));
            os.writeObject(agent);
            os.close();
        } catch (Throwable th) {
            th.printStackTrace();
        }

    }
}
