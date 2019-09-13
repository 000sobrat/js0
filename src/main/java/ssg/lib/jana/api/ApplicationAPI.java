/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssg.lib.jana.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.mail.Message;
import ssg.lib.http.HttpAuthenticator.HttpSimpleAuth.Domain;
import ssg.lib.http.HttpSession;
import ssg.lib.http.HttpUser;
import ssg.lib.http.RAT;
import ssg.lib.http.base.HttpRequest;
import ssg.lib.http.base.HttpResponse;
import ssg.lib.http.rest.annotations.XAccess;
import ssg.lib.http.rest.annotations.XMethod;
import ssg.lib.http.rest.annotations.XParameter;
import ssg.lib.http.rest.annotations.XType;
import ssg.lib.jana.api.ApplicationAPI.Group.LEVEL;
import ssg.lib.jana.tools.EmailAgent;
import ssg.lib.jana.tools.EmailAgent.EMailListener;
import ssg.lib.jana.tools.TimeTools;

/**
 * Scheduling application defines data structures and base API for managing time
 * schedule for training groups.
 *
 *
 * @author sesidoro
 */
@XType
public class ApplicationAPI implements Serializable, Cloneable {

    public static final String ROLE_TRAINEE = "trainee";
    public static final String ROLE_TRAINER = "trainer";
    public static final String ROLE_ADMIN = "admin";

    static long started = System.currentTimeMillis();
    private static final long serialVersionUID = 1L;

    Scheduler scheduler;
//    Schedule schedule = new Schedule();
//    Groups groups = new Groups();
//    Trainings trainings = new Trainings();
//    Map<Trainee, String> passes = new HashMap<>();
//    List<Price> prices = new ArrayList<>();

    Domain domain;
    private EmailAgent agent;
    private EMailListener emailListener = null;

    public static enum TRAINEE_STATUS {
        added, // trainee added to trainees list for the training
        skipped_duplicate, // trainee adding is skipped for already in list
        no_target, // no training found!
        no_space_in_target, // no more trainees can be added to the training list (max group size is reached)
        no_applier, // no valid trainee detected (need email or alias associated with email
        // 
        confirmed, // trainee confirmed participation in training
        pending, // trainee confirmation is needed/expected
        cancelled, // trainee cancelled participation in training
        attended // trainee attended the training
    }

    public ApplicationAPI(Scheduler scheduler) {
        setScheduler(scheduler);
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
        domain = new Domain(getVersion());
    }

    @XMethod
    public long upTime() {
        return System.currentTimeMillis() - started;
    }

    @XMethod(name = "version")
    public String getVersion() {
        return getClass().getSimpleName()
                + "(A.S.G.T)"
                + "." + serialVersionUID
                + "." + scheduler.getSchedule().serialVersionUID
                + "." + scheduler.getGroups().serialVersionUID
                + "." + scheduler.getTrainings().serialVersionUID;
    }

    @XMethod(name = "login")
    public String login(HttpRequest req,
            @XParameter(name = "type", optional = true) String type,
            @XParameter(name = "user", optional = true) String user,
            @XParameter(name = "pwd", optional = true) String pwd,
            @XParameter(name = "force", optional = true) boolean force
    ) throws IOException {
        if (req.getHttpSession() != null && req.getHttpSession().isValid()) {
            HttpSession sess = req.getHttpSession();
            if (sess.getUser() == null || sess.getRevalidateUser() != null || force) {
                if (type != null && "Basic".equalsIgnoreCase(type) && (sess.getApplication() == null || sess.getApplication().isBasicAuthEnabled())) {
                    String vp = (sess.getRevalidateUser() != null) ? "?" : "";
                    sess.setRevalidateUser(null);
                    sess.getApplication().doAuthentication(req);
//                    HttpResponse resp = req.getResponse();
//                    resp.setResponseCode(401, "Access denied");
//                    String dn = sess.getBaseURL(); // "localhost/app/"; // domain.getName()
//                    if (dn.contains("//")) {
//                        dn = dn.substring(dn.indexOf("//") + 2);
//                    }
//                    resp.setHeader("WWW-Authenticate", "Basic realm=\"" + vp + dn + "\", charset=\"UTF-8\"");
//                    //resp.setHeader(HttpData.HH_CONTENT_LENGTH, "" + buf.length);
//                    resp.onHeaderLoaded();
//                    resp.onLoaded();
                    return "OK";
                } else if (user != null) {
                    HttpUser httpUser = domain.authenticate(null, null, user, pwd);
                    if (httpUser != null) {
                        sess.setUser(httpUser);
                        httpUser.getProperties().put("email", httpUser.getName());
                        Trainee tre = scheduler.getTrainings().findTrainee(httpUser.getName(), httpUser.getName());
                        if (tre != null) {
                            httpUser.getProperties().put("name", (tre.name != null) ? tre.name : "");
                            httpUser.getProperties().put("url", (tre.url != null) ? tre.url : "");
                        }
                        return "OK";
                    }
                }
            }
        }
        throw new IOException("no authentication available for type=" + type + ", user=" + user);
    }

    @XMethod(name = "logout")
    public String logout(HttpRequest req
    ) {
        HttpUser user = null;
        if (req.getHttpSession() != null && req.getHttpSession().isValid()) {
            user = req.getHttpSession().getUser();
            req.getHttpSession().setUser(null);
            req.getHttpSession().setRevalidateUser("revalidate");
            return "logged out " + ((user != null) ? user.getName() : "");
        } else {
            return "not logged in";
        }
    }

    @XMethod(name = "timeRanges")
    public Map getTimeRanges(@XParameter(name = "from", optional = true) Long from,
            @XParameter(name = "to", optional = true) Long to,
            @XParameter(name = "name", optional = true) String name,
            @XParameter(name = "trainee", optional = true) String trainee
    ) throws IOException {
        Calendar c = TimeTools.getCalendar(null);
        long[] bounds = TimeTools.evalBounds(scheduler.getSchedule().timeslots);
        if (from == null || to == null) {
            if (from == null) {
                c.setTimeInMillis(bounds[0]);
                from = TimeTools.toStartOfWeek(c);
            }
            if (to == null) {
                c.setTimeInMillis(bounds[1]);
                to = TimeTools.toEndOfWeek(c);
            }
        }

        if (name != null) {
            name = URLDecoder.decode(name, "UTF-8");
        }
        if (trainee != null) {
            trainee = URLDecoder.decode(trainee, "UTF-8");
        }

        List<Timeslot> tss = scheduler.getSchedule().findTimeslots(from, to);
        if (name != null) {
            Iterator<Timeslot> it = tss.iterator();
            while (it.hasNext()) {
                Timeslot t = it.next();
                if (!t.group.name.contains(name)) {
                    it.remove();
                }
            }
        }
        if (trainee != null) {
            Trainee tr = scheduler.getTrainings().findTrainee(trainee, trainee);
            if (tr == null) {
                tss.clear();
            } else {
                Iterator<Timeslot> it = tss.iterator();
                while (it.hasNext()) {
                    Timeslot t = it.next();
                    if (!t.trainees.containsKey(tr.email)) {
                        it.remove();
                    }
                }
            }
        }

        Map r = new LinkedHashMap();
        r.put("bounds", bounds);
        r.put("from", from);
        r.put("to", to);
        // [min,max]
        List<long[]> days = new ArrayList<>();
        List<long[]> weeks = new ArrayList<>();
        List<long[]> months = new ArrayList<>();
        List<long[]> years = new ArrayList<>();
        r.put("years", years);
        r.put("months", months);
        r.put("weeks", weeks);
        r.put("days", days);

        c.setTimeInMillis(from);
        TimeTools.toStartOfDay(c);
        long last = c.getTimeInMillis();
        while (c.getTimeInMillis() < to) {
            c.add(Calendar.DAY_OF_YEAR, 1);
            days.add(new long[]{last, c.getTimeInMillis() - 1});
            last = c.getTimeInMillis();
        }

        c.setTimeInMillis(from);
        TimeTools.toStartOfWeek(c);
        last = c.getTimeInMillis();
        while (c.getTimeInMillis() < to) {
            c.add(Calendar.WEEK_OF_YEAR, 1);
            weeks.add(new long[]{last, c.getTimeInMillis() - 1});
            last = c.getTimeInMillis();
        }

        c.setTimeInMillis(from);
        TimeTools.toStartOfMonth(c);
        last = c.getTimeInMillis();
        while (c.getTimeInMillis() < to) {
            c.add(Calendar.MONTH, 1);
            months.add(new long[]{last, c.getTimeInMillis() - 1});
            last = c.getTimeInMillis();
        }

        c.setTimeInMillis(from);
        TimeTools.toStartOfYear(c);
        last = c.getTimeInMillis();
        while (c.getTimeInMillis() < to) {
            c.add(Calendar.YEAR, 1);
            years.add(new long[]{last, c.getTimeInMillis() - 1});
            last = c.getTimeInMillis();
        }

        return r;
    }

    @XMethod(name = "scheduledEvents")
    public List getScheduledEvents(@XParameter(name = "from", optional = true) Long from,
            @XParameter(name = "to", optional = true) Long to,
            @XParameter(name = "trainer", optional = true) String trainer,
            @XParameter(name = "trainee", optional = true) String trainee,
            @XParameter(name = "name", optional = true) String[] names
    ) throws IOException {
        if (from == null || to == null) {
            Calendar c = TimeTools.getCalendar(null);
            long[] bounds = TimeTools.evalBounds(scheduler.getSchedule().timeslots);
            if (from == null) {
                c.setTimeInMillis(bounds[0]);
                from = TimeTools.toStartOfWeek(c);
            }
            if (to == null) {
                c.setTimeInMillis(bounds[1]);
                to = TimeTools.toEndOfWeek(c);
            }
        }

        String name = (names != null && names.length > 0) ? names[0] : null;
        if (name != null) {
            name = URLDecoder.decode(name, "UTF-8");
        }
        if (trainer != null) {
            trainer = URLDecoder.decode(trainer, "UTF-8");
        }
        if (trainee != null) {
            trainee = URLDecoder.decode(trainee, "UTF-8");
        }

        List<Timeslot> tss = scheduler.getSchedule().findTimeslots(from, to);
        if (name != null && !name.isEmpty()) {
            Iterator<Timeslot> it = tss.iterator();
            while (it.hasNext()) {
                Timeslot t = it.next();
                if (!t.group.name.contains(name)) {
                    it.remove();
                }
            }
        }
        if (trainer != null && !trainer.isEmpty()) {
            Trainer tr = scheduler.getTrainings().findTrainer(trainer, trainer);
            if (tr == null) {
                tss.clear();
            } else {
                Iterator<Timeslot> it = tss.iterator();
                while (it.hasNext()) {
                    Timeslot t = it.next();
                    if (t.trainer != tr) {
                        it.remove();
                    }
                }
            }
        }
        if (trainee != null && !trainee.isEmpty()) {
            Trainee tr = scheduler.getTrainings().findTrainee(trainee, trainee);
            if (tr == null) {
                tss.clear();
            } else {
                Iterator<Timeslot> it = tss.iterator();
                while (it.hasNext()) {
                    Timeslot t = it.next();
                    if (!t.trainees.containsKey(tr.email)) {
                        it.remove();
                    }
                }
            }
        }

        List r = new ArrayList<>();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        for (Timeslot ts : tss) {
            Map event = new LinkedHashMap<>();
            event.put("title", ts.group.name);
            event.put("start", df.format(new Date(ts.from)));
            event.put("end", df.format(new Date(ts.from + ts.duration)));
            event.put("duration", ts.duration);
            event.put("group", ts.group);
            event.put("trainees", ts.trainees);
            event.put("trainer", ts.trainer.name);
            r.add(event);
        }
        return r;
    }

    @XMethod(name = "scheduledGroups")
    public Map<String, Object> getScheduledGroups(
            @XParameter(name = "from", optional = true) Long from,
            @XParameter(name = "to", optional = true) Long to
    ) throws IOException {
        Map<String, Object> r = new LinkedHashMap<>();
        List<String> rg = new ArrayList<>();
        Map<String, String> rt = new LinkedHashMap<>();
        r.put("groups", rg);
        r.put("trainers", rt);
        r.put("ranges", getTimeRanges(from, to, null, null));

        if (from == null || to == null) {
            Calendar c = TimeTools.getCalendar(null);
            long[] bounds = TimeTools.evalBounds(scheduler.getSchedule().timeslots);
            if (from == null) {
                c.setTimeInMillis(bounds[0]);
                from = TimeTools.toStartOfWeek(c);
            }
            if (to == null) {
                c.setTimeInMillis(bounds[1]);
                to = TimeTools.toEndOfWeek(c);
            }
        }
        List<Timeslot> tss = scheduler.getSchedule().findTimeslots(from, to);
        for (Timeslot t : tss) {
            if (!rg.contains(t.group.name)) {
                rg.add(t.group.name);
            }
            if (!rt.containsKey(t.trainer.email)) {
                rt.put(t.trainer.email, t.trainer.name);
            }
        }
        return r;
    }

    @XMethod(name = "groups")
    public List<Group> getGroup(@XParameter(name = "name", optional = true) String name
    ) {

        List<Group> grs = new ArrayList<>();
        if (name != null && !name.isEmpty()) {
            Group gr = scheduler.getGroups().findGroup(name, name);
            if (gr == null) {
                for (Group g : grs) {
                    if (g.name.contains(name)) {
                        grs.add(g);
                    }
                }
            } else {
                grs.add(gr);
            }
        } else {
            grs.addAll(scheduler.getGroups().groups);
        }

        return grs;
    }

    @XMethod(name = "prices")
    public List<Price> getPrices() {
        return scheduler.getPrices();
    }

    @XMethod(name = "upload")
    @XAccess(roles = "admin")
    public void upload(@XParameter(name = "name") String name, byte[] data
    ) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
            scheduler.loadFrom(bais);
        }
    }

    @XMethod(name = "download")
    @XAccess(roles = "admin")
    public void download(HttpRequest req,
            @XParameter(name = "name") String name) throws IOException {

        byte[] data = null;//ApplicationTools.load(ApplicationAPI.class.getClassLoader().getResourceAsStream("scheduler.png"));
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            scheduler.saveTo(baos, null);
            data = baos.toByteArray();
        }
        if (req != null && req.getResponse() != null && !req.getResponse().isCompleted()) {
            HttpResponse resp = req.getResponse();
            resp.prepareDownload(
                    "application/binary",
                    "scheduler_" + System.currentTimeMillis() + ".xlsx",
                    data,
                    false);
        } else {
            throw new IOException("Cannot put data into non-empty HTTP response.");
            //return data;
        }
    }

    @XMethod(name = "registerTrainee")
    public boolean registerTrainee(HttpRequest req,
            @XParameter(name = "email") String email,
            @XParameter(name = "alias", optional = true) String alias,
            @XParameter(name = "url", optional = true) String url,
            @XParameter(name = "pwd") String pwd) throws IOException {

        Trainee tre = scheduler.getTrainings().findTrainee(alias, email);
        if (tre != null) {
            if (alias != null) {
                tre.name = alias;
            }
            if (url != null) {
                tre.url = url;
            }
            return true;
        } else {
            if (email != null && email.contains("@") && email.indexOf("@") == email.lastIndexOf("@")) {
                tre = new Trainee();
                tre.email = email;
                tre.name = alias;
                tre.url = url;
                scheduler.getTrainings().trainees.add(tre);
                if (pwd == null) {
                    pwd = ("" + email.hashCode()).replace("-", "_");
                    scheduler.getPasses().put(tre, pwd);
                } else {
                    scheduler.getPasses().put(tre, pwd);
                    pwd = null;
                }
                if (agent != null) {
                    agent.sendFromMediator(email, url, "<html>"
                            + "Registered account for:<table>"
                            + "<tr><th>EMail</th><td>" + email + "</td></tr>"
                            + "<tr><th>Name</th><td>" + alias + "</td></tr>"
                            + "<tr><th>URL</th><td>" + url + "</td></tr>"
                            + ((pwd != null) ? "<tr><th>Generated pwd</th><td>" + pwd + "</td></tr>" : "")
                            + "<tr><th>.</th><td>.</td></tr>"
                            + "<tr><th>Change password</th><td>" + req.getHttpSession().getBaseURL() + "changePwd.html?trainee=" + URLEncoder.encode(email, "UTF-8") + "</td></tr>"
                            + "</table>"
                            + "</html>"
                    );
                }
            }
        }
        return false;
    }

    @XMethod(name = "changeTraineePwd")
    public boolean changeTrainee(HttpRequest req,
            @XParameter(name = "email") String email,
            @XParameter(name = "oldPwd", optional = true) String pwd1,
            @XParameter(name = "newPwd", optional = true) String pwd2) throws IOException {
        Trainee tre = scheduler.getTrainings().findTrainee(email, email);
        if (tre != null) {
            if (scheduler.getPasses().containsKey(tre)) {
                //
                String old = scheduler.getPasses().get(tre);
                if (old != null && old.equals(pwd1) || old == null && pwd1 == null || old == null && pwd1.isEmpty()) {
                    scheduler.getPasses().put(tre, pwd2);
                    return true;
                }
            } else if (pwd1 == null || pwd1.isEmpty()) {
                scheduler.getPasses().put(tre, pwd2);
                return true;
            }
        }
        return false;
    }

    @XMethod(name = "addGroupsToBasket")
    public boolean addGroupsToBasket(HttpRequest req,
            @XParameter(name = "groupTimes") long... groupTimes
    ) throws IOException {
        boolean r = false;
        if (groupTimes != null) {
            List<Timeslot> basket = (List) req.getHttpSession().getProperties().get("groupsBasket");
            for (long time : groupTimes) {
                Timeslot ts = scheduler.getSchedule().getTimeslot(time);
                if (ts != null) {
                    if (basket == null) {
                        basket = new ArrayList<Timeslot>();
                        req.getHttpSession().getProperties().put("groupsBasket", basket);
                    }
                    if (!basket.contains(ts)) {
                        basket.add(ts);
                        r = true;
                    }
                }
            }
        }
        return r;
    }

    @XMethod(name = "removeGroupsFromBasket")
    public boolean removeGroupsFromBasket(HttpRequest req,
            @XParameter(name = "groupTimes") long... groupTimes
    ) throws IOException {
        boolean r = false;
        if (groupTimes != null) {
            List<Timeslot> basket = (List) req.getHttpSession().getProperties().get("groupsBasket");
            if (basket != null) {
                for (long time : groupTimes) {
                    Timeslot ts = scheduler.getSchedule().getTimeslot(time);
                    if (ts != null && basket.contains(ts)) {
                        basket.remove(ts);
                        r = true;
                    }
                }
            }
        }
        return r;
    }

    @XMethod(name = "getGroupsInBasket")
    public List<Timeslot> getGroupsInBasket(HttpRequest req) throws IOException {
        return (List) req.getHttpSession().getProperties().get("groupsBasket");
    }

    @XMethod(name = "applyForGroup")
    public TRAINEE_STATUS applyForGroup(HttpRequest req,
            @XParameter(name = "time") long time,
            @XParameter(name = "email", optional = true) String email) throws IOException {
        TRAINEE_STATUS r = TRAINEE_STATUS.no_target;

        HttpSession sess = (req != null) ? req.getHttpSession() : null;
        HttpUser user = (sess != null) ? sess.getUser() : null;
        if (user != null) {
            if (email == null) {
                email = (String) user.getProperties().get("email");
            }
            if (email != null && !email.equals(user.getProperties().get(email))) {
                List<String> roles = user.getRoles();
                if (roles != null && !roles.contains("admin")) {
                    throw new IOException("Non-admin can only self-apply.");
                }
            }
        }

        // guess if have trainee -> evaluate email.
        if (email != null && !email.contains("@")) {
            Trainee tre = scheduler.getTrainings().findTrainee(email, null);
            if (tre != null) {
                email = tre.email;
            } else {
                email = null;
            }
        }
        if (email == null || email.isEmpty()) {
            email = (String) req.getContext().getProperties().get("email");
            if (email == null || email.isEmpty()) {
                return TRAINEE_STATUS.no_applier;
            }
        } else {
            String semail = (String) req.getContext().getProperties().get("email");
            if (semail == null || semail.isEmpty()) {
                req.getContext().getProperties().put("email", email);
            }
        }

        Timeslot ts = scheduler.getSchedule().getTimeslot(time);
        if (ts == null) {
            return TRAINEE_STATUS.no_target;
        }

        Trainee tre = scheduler.getTrainings().findTrainee(email, email);
        if (tre == null) {
            tre = new Trainee();
            tre.email = email;
            scheduler.getTrainings().trainees.add(tre);
        } else {
            // verify if need authentication
            String pass = scheduler.getPasses().get(tre);
            if (user == null && pass != null && !pass.isEmpty() && req.getHttpSession() != null) {
                user = checkAuthentication(req, true);
                if (user == null) {
                    return r;
                }
                user.getProperties().put("email", email);
                user.getProperties().put("name", user.getName());
                user.getProperties().put("roles", user.getRoles());
            }
        }

        if (ts.trainees.size() < ts.group.training.maxSize) {
            if (ts.trainees.containsKey(email)) {
                r = TRAINEE_STATUS.skipped_duplicate;
            } else {
                TraineeInGroup tg = new TraineeInGroup();
                tg.trainee = tre;
                tg.setApplied((Long) System.currentTimeMillis());
                tg.status = TRAINEE_STATUS.added;
                ts.trainees.put(tre.email, tg);
                r = TRAINEE_STATUS.added;
                if (getEmailAgent() != null) {
                    // TODO: send email...
                }
            }
        } else {
            r = TRAINEE_STATUS.no_space_in_target;
        }

        return r;
    }

    public HttpUser checkAuthentication(HttpRequest req, boolean required) throws IOException {
        // verify if need authentication
        HttpSession sess = req.getHttpSession();
        HttpUser user = sess.getUser();
        if (user == null && required) {
            if (sess.getApplication() != null) {
                if (sess.getApplication().doAuthentication(req)) {
                    return null;
                }
            }
            return null;
        }
        return user;
    }

    public Domain getDomain() {
        if (scheduler.getTrainings() != null && scheduler.getPasses() != null && !scheduler.getPasses().isEmpty()) {
            for (Trainee tr : scheduler.getPasses().keySet()) {
                if (!(domain.hasUser(tr.email) || domain.hasUser(tr.name))) {
                    RAT rat = new RAT(new ArrayList<String>(), null, null);
                    rat.getRoles().add(ROLE_TRAINEE);
                    if (scheduler.getTrainings().findTrainer(tr.name, tr.email) != null) {
                        rat.getRoles().add(ROLE_TRAINER);
                    }
                    if (scheduler.getMeta() != null && isAdmin(tr.email)) {
                        rat.getRoles().add(ROLE_ADMIN);
                    }
                    domain.addUser(tr.email, scheduler.getPasses().get(tr), domain.getName(), rat);
                }
            }
            for (Trainer tr : scheduler.getTrainings().trainers) {
                if (!(domain.hasUser(tr.email) || domain.hasUser(tr.name))) {
                    RAT rat = new RAT(new ArrayList<String>(), null, null);
                    rat.getRoles().add(ROLE_TRAINER);
                    if (scheduler.getMeta() != null && isAdmin(tr.email)) {
                        rat.getRoles().add(ROLE_ADMIN);
                    }
                    domain.addUser(tr.email, scheduler.getPasses().get(tr), domain.getName(), rat);
                } else {
                    RAT rat = domain.getUserRAT(tr.email);
                    if (rat != null) {
                        if (rat.getRoles() != null) {
                            rat.getRoles().add(ROLE_TRAINER);
                            if (scheduler.getMeta() != null && isAdmin(tr.email)) {
                                rat.getRoles().add(ROLE_ADMIN);
                            }
                        } else {
                            rat.setRoles(new ArrayList<>());
                            rat.getRoles().add(ROLE_TRAINER);
                            if (scheduler.getMeta() != null && isAdmin(tr.email)) {
                                rat.getRoles().add(ROLE_ADMIN);
                            }
                        }
                    }
                }
            }
        }
        return domain;
    }

    public boolean isAdmin(String email) {
        if (scheduler != null && scheduler.getMeta() != null && scheduler.getMeta().get("admin") != null) {
            String[] admins = scheduler.getMeta().get("admin");
            for (String admin : admins) {
                if (admin != null && admin.equalsIgnoreCase(email)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setEmailAgent(EmailAgent agent) {
        if (this.agent != null) {
            if (emailListener != null) {
                this.agent.removeEMailListener(emailListener);
            }
        }

        this.agent = agent;
        if (agent != null) {
            if (emailListener == null) {
                emailListener = new EMailListener() {
                    @Override
                    public void onReceivedEMail(EmailAgent agent, String from, String subject, Message msg) {
                    }

                    @Override
                    public void onSentEMail(EmailAgent agent, String to, String subject, Message msg) {
                    }

                    @Override
                    public void onFailedToSendEMail(EmailAgent agent, String to, String subject, Message msg) {
                    }
                };
            }
            agent.addEMailListener(emailListener);
        }
    }

    public EmailAgent getEmailAgent() {
        return agent;
    }

    public Map getTimeslots(@XParameter(name = "from", optional = true) Long from,
            @XParameter(name = "to", optional = true) Long to,
            @XParameter(name = "trainee", optional = true) String trainee,
            @XParameter(name = "name", optional = true) String... names
    ) throws IOException {
        Calendar c = TimeTools.getCalendar(null);
        long[] bounds = TimeTools.evalBounds(scheduler.getSchedule().timeslots);
        if (from == null || to == null) {
            if (from == null) {
                c.setTimeInMillis(bounds[0]);
                from = TimeTools.toStartOfWeek(c);
            }
            if (to == null) {
                c.setTimeInMillis(bounds[1]);
                to = TimeTools.toEndOfWeek(c);
            }
        }

        if (names != null && names.length > 0) {
            for (int i = 0; i < names.length; i++) {
                if (names[i] != null) {
                    names[i] = URLDecoder.decode(names[i], "UTF-8");
                }
            }
        }
        if (trainee != null) {
            trainee = URLDecoder.decode(trainee, "UTF-8");
        }

        List<Timeslot> tss = scheduler.getSchedule().findTimeslots(from, to);
        if (names != null && names.length > 0) {
            Iterator<Timeslot> it = tss.iterator();
            while (it.hasNext()) {
                Timeslot t = it.next();
                boolean ok = false;
                for (String name : names) {
                    if (t.group.name.contains(name)) {
                        ok = true;
                    }
                }
                if (!ok) {
                    it.remove();
                }
            }
        }
        if (trainee != null) {
            Trainee tr = scheduler.getTrainings().findTrainee(trainee, trainee);
            if (tr == null) {
                tss.clear();
            } else {
                Iterator<Timeslot> it = tss.iterator();
                while (it.hasNext()) {
                    Timeslot t = it.next();
                    if (!t.trainees.containsKey(tr.email)) {
                        it.remove();
                    }
                }
            }
        }

        Map r = new LinkedHashMap();
        r.put("bounds", bounds);
        r.put("from", from);
        r.put("to", to);
        r.put("timeslots", tss);
        return r;
    }

    public static interface ApplicationItem extends Cloneable, Serializable {
    }

////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////// scheduling
////////////////////////////////////////////////////////////////////////////
    public static class Schedule implements ApplicationItem {

        static final long serialVersionUID = 1L;

        public List<Timeslot> timeslots = new ArrayList<>();

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Schedule{");
            if (!timeslots.isEmpty()) {
                sb.append("\n  timeslots=" + timeslots.size());
                for (Timeslot tr : timeslots) {
                    sb.append("\n    " + tr.toString().replace("\n", "\n    "));
                }
            }
            sb.append("\n}");
            return sb.toString();
        }

        public List<Timeslot> findTimeslots(long from, long to) {
            List<Timeslot> r = new ArrayList<>();
            for (Timeslot t : timeslots) {
                if (t.from >= from && t.from < to) {
                    r.add(t);
                }
            }
            Collections.sort(r, new Comparator<Timeslot>() {
                @Override
                public int compare(Timeslot o1, Timeslot o2) {
                    Long l = o1.from;
                    return l.compareTo(o2.from);
                }
            });
            return r;
        }

        public Timeslot getTimeslot(long time) {
            for (Timeslot t : timeslots) {
                if (t.from == time) {
                    return t;
                }
            }
            return null;
        }
    }

    public static class Timeslot implements ApplicationItem {

        public static enum TYPE {
            available, group, gap, cleaning, reserved
        }
        public TYPE type;
        public long from;
        public long duration;
        public Group group;
        public Map<String, TraineeInGroup> trainees = new LinkedHashMap<>();
        public Trainer trainer;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Timeslot{" + "type=" + type + ", from=" + from + ", duration=" + duration);
            sb.append("\n  trainer=" + ("" + trainer).replace("\n", "\n  "));
            sb.append("\n  group=" + ("" + group).replace("\n", "\n  "));
            sb.append("\n  trainees[" + trainees.size() + "]:");
            for (TraineeInGroup tr : trainees.values()) {
                sb.append("\n    " + tr.toString().replace("\n", "\n    "));
            }
            sb.append("\n}");
            return sb.toString();
        }

    }

////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////// grouping
////////////////////////////////////////////////////////////////////////////
    public static class Groups implements ApplicationItem {

        static final long serialVersionUID = 1L;

        public List<Group> groups = new ArrayList<>();

        public Group findGroup(String name, String training) {
            for (Group t : groups) {
                if (name != null && name.equals(t.name) || training != null && (training.equals(t.training.title) || training.equals(t.training.alias))) {
                    return t;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Groups{");
            if (!groups.isEmpty()) {
                sb.append("\n  groups=" + groups.size());
                for (Group tr : groups) {
                    sb.append("\n    " + tr.toString().replace("\n", "\n    "));
                }
            }
            sb.append("\n}");
            return sb.toString();
        }
    }

    public static class Group implements ApplicationItem {

        static final long serialVersionUID = 1L;

        public static enum LEVEL {
            beginner, // just starting level/beginner
            normal, // medium or just normal (e.g. if no level differentiation)
            advanced, // experienced/advanced
            info, // indicates no skills but introduction or verification activity
            needEvaluation // proper level must be evaluated (beginner/normal/advanced ?)
        }

        public String name;
        public LEVEL level;
        public Training training;

        @Override
        public String toString() {
            return "Group{" + "name=" + name + ", level=" + level + ", training=" + training + '}';
        }
    }

////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////// training
////////////////////////////////////////////////////////////////////////////
    public static class Trainings implements ApplicationItem {

        static final long serialVersionUID = 1L;

        public List<Training> trainings = new ArrayList<>();
        public List<Trainer> trainers = new ArrayList<>();
        public List<Trainee> trainees = new ArrayList<>();

        public Training findTraining(String name, String alias) {
            for (Training t : trainings) {
                if (name != null && name.equals(t.title) || alias != null && alias.equals(t.alias)) {
                    return t;
                }
            }
            return null;
        }

        public Trainer findTrainer(String name, String email) {
            for (Trainer t : trainers) {
                if (name != null && name.equals(t.name) || email != null && email.equals(t.email)) {
                    return t;
                }
            }
            return null;
        }

        public Trainee findTrainee(String name, String email) {
            for (Trainee t : trainees) {
                if (name != null && name.equals(t.email) || email != null && email.equals(t.email)) {
                    return t;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Trainings{");
            if (!trainings.isEmpty()) {
                sb.append("\n  trainings=" + trainings.size());
                for (Training tr : trainings) {
                    sb.append("\n    " + tr.toString().replace("\n", "\n    "));
                }
            }
            if (!trainers.isEmpty()) {
                sb.append("\n  trainers=" + trainers.size());
                for (Trainer tr : trainers) {
                    sb.append("\n    " + tr.toString().replace("\n", "\n    "));
                }
            }
            if (!trainees.isEmpty()) {
                sb.append("\n  trainees=" + trainees.size());
                for (Trainee tr : trainees) {
                    sb.append("\n    " + tr.toString().replace("\n", "\n    "));
                }
            }
            sb.append("\n}");
            return sb.toString();
        }

    }

    public static class Training implements ApplicationItem {

        static final long serialVersionUID = 1L;

        public URL logo;
        public String title;
        public String alias;
        public String description;
        public LEVEL[] levels = new LEVEL[]{LEVEL.normal};
        public int maxSize;

        @Override
        public String toString() {
            return "Training{" + "logo=" + logo + ", title=" + title + ", alias=" + alias + ", description=" + description + ", levels=" + ((levels != null) ? Arrays.asList(levels) : "") + ", maxSize=" + maxSize + '}';
        }

    }

    public static class Trainer implements ApplicationItem {

        static final long serialVersionUID = 1L;

        public String name;
        public String email;
        public List<Training> trainings = new ArrayList<>();

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Trainer{");
            sb.append("name=" + name + ", email=" + email);
            if (!trainings.isEmpty()) {
                sb.append("\n  trainings=" + trainings.size());
                for (Training tr : trainings) {
                    sb.append("\n    " + tr.toString().replace("\n", "\n    "));
                }
            }
            sb.append("\n}");
            return sb.toString();
        }

    }

    public static class Trainee implements ApplicationItem {

        static final long serialVersionUID = 1L;

        public String name;
        public String email;
        public String url;

        @Override
        public String toString() {
            return "Trainee{"
                    + "email=" + email
                    + ((name != null) ? ", name=" + name : "")
                    + ((url != null) ? ", url=" + url : "")
                    + '}';
        }
    }

    public static class TraineeInGroup implements ApplicationItem {

        static final long serialVersionUID = 1L;

        Trainee trainee;
        TRAINEE_STATUS status;
        private Long applied;
        private Long confirmed;
        private Long cancelled;

        @Override
        public String toString() {
            return "TraineeInGroup{"
                    + "trainee=" + trainee.name + "/" + trainee.email
                    + ", status=" + status
                    + ", applied=" + getApplied()
                    + ((getConfirmed() != null) ? ", confirmed=" + getConfirmed() : "")
                    + ((getCancelled() != null) ? ", cancelled=" + getCancelled() : "")
                    + '}';
        }

        public Map<String, Object> getInfo() {
            Map r = new LinkedHashMap<>();
            //r.put("trainee", trainee.email);
            if (trainee.name != null) {
                r.put("name", "" + trainee.name);
            }
            r.put("status", "" + status);
            if (getApplied() != null) {
                r.put("applied", TimeTools.dumpDateTime(getApplied()));
            }
            if (getConfirmed() != null) {
                r.put("confirmed", TimeTools.dumpDateTime(getConfirmed()));
            }
            if (getCancelled() != null) {
                r.put("cancelled", TimeTools.dumpDateTime(getCancelled()));
            }
            return r;
        }

        /**
         * @return the applied
         */
        public Long getApplied() {
            return applied;
        }

        /**
         * @param applied the applied to set
         */
        public void setApplied(Long applied) {
            this.applied = applied;
            if (status == null && applied != null && (confirmed == null || cancelled == null)) {
                status = TRAINEE_STATUS.pending;
            }
        }

        /**
         * @return the confirmed
         */
        public Long getConfirmed() {
            return confirmed;
        }

        /**
         * @param confirmed the confirmed to set
         */
        public void setConfirmed(Long confirmed) {
            this.confirmed = confirmed;
            if ((status == null || status != TRAINEE_STATUS.cancelled) && confirmed != null) {
                status = TRAINEE_STATUS.confirmed;
            }
        }

        /**
         * @return the cancelled
         */
        public Long getCancelled() {
            return cancelled;
        }

        /**
         * @param cancelled the cancelled to set
         */
        public void setCancelled(Long cancelled) {
            this.cancelled = cancelled;
            if (cancelled != null) {
                status = TRAINEE_STATUS.cancelled;
            }
        }
    }

    public static class Price implements ApplicationItem {

        static final long serialVersionUID = 1L;

        public String item;
        public String description;
        public Float[] prices;
    }

    public static interface Scheduler {

        String getName();

        void setName(String name);

        Schedule getSchedule();

        void setSchedule(Schedule schedule);

        Groups getGroups();

        void setGroups(Groups groups);

        Trainings getTrainings();

        void setTrainings(Trainings trainings);

        Map<Trainee, String> getPasses();

        void setPasses(Map<Trainee, String> passes);

        List<Price> getPrices();

        void setPrices(List<Price> prices);

        List<String> getFormats();

        Map<String, String[]> getMeta();

        void setMeta(Map<String, String[]> meta);

        void loadFrom(InputStream is) throws IOException;

        void saveTo(OutputStream os, String format) throws IOException;
    }
}
