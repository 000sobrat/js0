/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssg.lib.jana.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import ssg.lib.common.CommonTools;
import ssg.lib.common.MatchScanner;
import ssg.lib.common.Matcher;
import ssg.lib.http.HttpMatcher;
import ssg.lib.http.HttpSession;
import ssg.lib.http.HttpUser;
import ssg.lib.http.base.HttpRequest;
import ssg.lib.http.rest.annotations.XMethod;
import ssg.lib.http.rest.annotations.XParameter;
import ssg.lib.http.rest.annotations.XType;
import ssg.lib.jana.api.ScheduleAPI.Room;
import ssg.lib.jana.api.ScheduleAPI.TimeEvent;
import ssg.lib.jana.api.ScheduleAPI.TimeEventPlanner;
import ssg.lib.jana.api.TrainingAPI.Category;
import ssg.lib.jana.api.TrainingAPI.Course;
import ssg.lib.jana.api.TrainingAPI.Group;
import ssg.lib.jana.api.TrainingAPI.IconInfo;
import ssg.lib.jana.api.TrainingAPI.Trainer;
import ssg.lib.jana.api.UM_API.UM_STATE;
import ssg.lib.jana.tools.TimeTools;
import ssg.lib.jana.tools.TimeTools.SEASON;
import ssg.lib.service.DF_Service;
import ssg.lib.service.DF_Service.DF_ServiceListener;
import ssg.lib.service.DF_Service.DebuggingDF_ServiceListener;
import ssg.lib.service.ProviderStatistics;

/**
 *
 * @author sesidoro
 */
@XType
public class UI_API {

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

    public static enum ACTION {
        apply,
        remove,
        confirm
    }

    ScheduleAPI schedule = new ScheduleAPI();
    TrainingAPI training = new TrainingAPI();
    UM_API um = new UM_API();
    List<MatchScanner<String, String>> scannables = new ArrayList<>();
    DF_ServiceListener serviceListener;
    DF_Service dfs;

    public UI_API() {
    }

    public UI_API(
            ScheduleAPI schedule,
            TrainingAPI training,
            UM_API um
    ) {
        this.schedule = schedule;
        this.training = training;
        this.um = um;
    }

    public UI_API(
            ScheduleAPI schedule,
            TrainingAPI training,
            UM_API um,
            DF_ServiceListener serviceListener,
            DF_Service dfs
    ) {
        this.schedule = schedule;
        this.training = training;
        this.um = um;
        this.serviceListener = serviceListener;
        this.dfs = dfs;
    }

    public void addScannables(MatchScanner<String, String>... scannables) {
        if (scannables != null) {
            for (MatchScanner<String, String> scannable : scannables) {
                if (scannable != null && !this.scannables.contains(scannable)) {
                    this.scannables.add(scannable);
                }
            }
        }
    }

    long nextCleanUp = System.currentTimeMillis();

    public void doMaintenance() {
        if (System.currentTimeMillis() >= nextCleanUp) {
            if (dfs != null) {
                dfs.clearStatistics(nextCleanUp - 1000 * 60 * 2);
            }
            nextCleanUp = System.currentTimeMillis() + 1000 * 60 * 30;
        }
    }

    @XMethod(name = "providerStatistics")
    public Map providerStatistics() {
        doMaintenance();
        Map r = new LinkedHashMap<>();
        List<String> r1 = new ArrayList<>();
        List<String> r2 = new ArrayList<>();
        if (dfs != null) {
            Map pss = dfs.getStatistics();
            List<ProviderStatistics> p1 = (List<ProviderStatistics>) pss.get("WIP");
            List<ProviderStatistics> p2 = (List<ProviderStatistics>) pss.get("DONE");
            if (pss != null) {
                System.out.println("PSS[" + pss.size() + "]");
                for (ProviderStatistics ps : p1) {
                    if (ps != null) {
                        r1.add(ps.toString());
                        //System.out.println("  ------------------------------\n    "+ps.toString().replace("\n", "\n    "));
                    }
                }
                for (ProviderStatistics ps : p2) {
                    if (ps != null) {
                        r2.add(ps.toString());
                        //System.out.println("  ------------------------------\n    "+ps.toString().replace("\n", "\n    "));
                    }
                }
            }
            for (Object key : pss.keySet()) {
                if ("WIP".equals(key)) {
                    r.put(key, r1);
                } else if ("DONE".equals(key)) {
                    r.put(key, r2);
                } else {
                    r.put(key, pss.get(key));
                }
            }
        }
        return r;
    }

    @XMethod(name = "clearStatistics")
    public void clearStatistics() {
        List<String> r = new ArrayList<>();
        if (dfs != null) {
            dfs.clearStatistics(System.currentTimeMillis());
        }
    }

    @XMethod(name = "debug")
    public void debug(
            HttpRequest req,
            @XParameter(name = "enable", optional = true) boolean enable,
            @XParameter(name = "events", optional = true) DF_ServiceListener.SERVICE_EVENT... events) {
        doMaintenance();
        if (serviceListener == null || !(serviceListener instanceof DebuggingDF_ServiceListener)) {
            return;
        }
        if (events == null || events.length == 0 || events[0] == null) {
            events = DF_ServiceListener.SERVICE_EVENT.values();
        }
        if (enable) {
            ((DebuggingDF_ServiceListener) serviceListener).includeEvents(events);
        } else {
            ((DebuggingDF_ServiceListener) serviceListener).excludeEvents(events);
            if (((DebuggingDF_ServiceListener) serviceListener).debuggingEvents().isEmpty()) {
                ((DebuggingDF_ServiceListener) serviceListener).includeEvents(
                        DF_ServiceListener.SERVICE_EVENT.no_event
                );
            }
        }
    }

    @XMethod(name = "listDebug")
    public Collection<DF_ServiceListener.SERVICE_EVENT> listDebug() {
        doMaintenance();
        return (serviceListener instanceof DebuggingDF_ServiceListener)
                ? ((DebuggingDF_ServiceListener) serviceListener).debuggingEvents()
                : null;
    }

    @XMethod(name = "canDebug")
    public DF_ServiceListener.SERVICE_EVENT[] canDebug() {
        doMaintenance();
        return (serviceListener != null)
                ? DF_ServiceListener.SERVICE_EVENT.values()
                : null;
    }

    @XMethod(name = "getAdvResources")
    public Map<String, List<String>> getAdvResources(
            HttpRequest req,
            final @XParameter(name = "masks", optional = true) String... masks
    ) throws IOException {
        doMaintenance();
        Map<String, List<String>> r = new LinkedHashMap<>();

        if (masks != null) {
            for (final String mask : masks) {
                if (!mask.contains("adv/")) {
                    continue;
                }
                Matcher<String> matcher = new Matcher<String>() {
                    HttpMatcher hm = new HttpMatcher(mask);

                    @Override
                    public float match(String t) {
                        float f = hm.match(new HttpMatcher(t));
                        return f;
                    }

                    @Override
                    public float weight() {
                        return 1f;
                    }

                    @Override
                    public String toString() {
                        return "{mask=" + mask + ", " + "hm=" + hm + '}';
                    }
                };
                List<String> lst = new ArrayList<>();
                r.put(mask, lst);
                for (MatchScanner<String, String> sc : scannables) {
                    if (sc != null) {
                        Collection<String> m = sc.scan(matcher);
                        if (m != null) {
                            for (String s : m) {
                                if (!r.containsKey(s)) {
                                    lst.add(s);
                                }
                            }
                        }
                    }
                }
            }
        }

        return r;
    }

    @XMethod(name = "setLanguage")
    public String setLanguage(
            HttpRequest req,
            @XParameter(name = "localeLanguage", optional = true) String localeLanguage,
            @XParameter(name = "localeCountry", optional = true) String localeCountry
    ) throws IOException {
        doMaintenance();
        if (localeLanguage == null || localeLanguage.isEmpty()) {
            // nothing to do, will return current setting...
        } else {
            String loc = (localeCountry != null && !localeCountry.isEmpty())
                    ? localeLanguage + '-' + localeCountry
                    : localeLanguage;
            try {
                Locale l = Locale.forLanguageTag(loc);
                if (l != null) {
                    req.getHttpSession().setLocale(l);
                }
            } catch (Throwable th) {
            }
        }
        return req.getHttpSession().getLocale().getLanguage();
    }

    @XMethod(name = "eventsMeta")
    public Map<String, Object> getEventsMeta(
            HttpRequest req,
            @XParameter(name = "from", optional = true) Long from,
            @XParameter(name = "to", optional = true) Long to
    ) throws IOException {
        doMaintenance();
        Map<String, Object> r = new LinkedHashMap<>();
        Map<String, String[]> rg = new LinkedHashMap<>();
        Map<String, String[]> rc = new LinkedHashMap<>();
        Map<String, String[]> rcc = new LinkedHashMap<>();
        Map<String, String> rt = new LinkedHashMap<>();
        Map<String, String> rm = new LinkedHashMap<>();

        r.putAll(getEventPlannersMeta(req));

        r.put("groups", rg);
        r.put("courses", rc);
        r.put("categories", rcc);
        r.put("trainers", rt);
        r.put("rooms", rm);

        List<TimeEvent> tss = schedule.findEvent(null, from, to, null);
        r.put("ranges", schedule.getTimeRanges(tss));

        if (from == null || to == null) {
            Calendar c = TimeTools.getCalendar(null);
            long[] bounds = (long[]) ((Map) r.get("ranges")).get("bounds");
            if (from == null) {
                c.setTimeInMillis(bounds[0]);
                from = TimeTools.toStartOfWeek(c);
            }
            if (to == null) {
                c.setTimeInMillis(bounds[1]);
                to = TimeTools.toEndOfWeek(c);
            }
        }

        for (TimeEvent t : tss) {
            if (t.getRoom() != null && !rm.containsKey(t.getRoom())) {
                Room room = schedule.getRoom(t.getRoom(), null);
                if (room != null) {
                    rm.put(room.getId(), room.getAddress());
                }
            }

            Group g = training.groups.get(t.getName());

            if (g != null && !rg.containsKey(t.getName())) {
                IconInfo icon = training.findIcon(null, null, g);
                rg.put(t.getName(), new String[]{g.getShortName(), icon != null ? icon.getIcon(null) : null});
            }

            if (g != null && g.getTrainer() != null && !rt.containsKey(g.getTrainer())) {
                Trainer tr = training.getTrainer(g.getTrainer(), null);
                if (tr != null && !rt.containsKey(tr.getId())) {
                    rt.put(tr.getId(), tr.getName());
                }
            }
            if (g != null) {
                String c = g.getCourse();
                if (c != null && training.courses.containsKey(c)) {
                    Course crs = training.courses.get(c);
                    if (!rc.containsKey(c)) {
                        IconInfo icon = training.findIcon(null, crs, null);
                        rc.put(c, new String[]{c, icon != null ? icon.getIcon(null) : null});
                    }

                    if (crs.getCategory() != null && training.categories.containsKey(crs.getCategory())) {
                        Category cat = training.categories.get(crs.getCategory());
                        if (!rcc.containsKey(cat.getId())) {
                            IconInfo icon = training.findIcon(cat, null, null);
                            rcc.put(cat.getId(), new String[]{cat.getId(), icon != null ? icon.getIcon(null) : null});
                        }
                    }
                }
            }
        }
        return r;
    }

    /**
     * Generic events filter allowing training-specific filtering as well.
     *
     * @param room
     * @param name
     * @param from
     * @param to
     * @param status
     * @param trainer
     * @param participant
     * @param category
     * @param course
     * @param group
     * @param dayOfWeek
     * @return
     * @throws IOException
     */
    @XMethod(name = "events")
    public List<TE> events(
            HttpRequest req,
            @XParameter(name = "room", optional = true) String room,
            @XParameter(name = "name", optional = true) String name,
            @XParameter(name = "from", optional = true) Long from,
            @XParameter(name = "to", optional = true) Long to,
            @XParameter(name = "status", optional = true) String status,
            @XParameter(name = "trainer", optional = true) String trainer,
            @XParameter(name = "participant", optional = true) String participant,
            @XParameter(name = "category", optional = true) String category,
            @XParameter(name = "course", optional = true) String course,
            @XParameter(name = "group", optional = true) String group,
            @XParameter(name = "dayOfWeek", optional = true) Integer[] dayOfWeek
    ) throws IOException {
        doMaintenance();
        List<TE> r = new ArrayList<>();

        if (room != null && room.isEmpty()) {
            room = null;
        }
        if (name != null && name.isEmpty()) {
            name = null;
        }
        if (status != null && status.isEmpty()) {
            status = null;
        }
        if (trainer != null && trainer.isEmpty()) {
            trainer = null;
        }
        if (participant != null && participant.isEmpty()) {
            participant = null;
        }
        if (category != null && category.isEmpty()) {
            category = null;
        }
        if (course != null && course.isEmpty()) {
            course = null;
        }
        if (group != null && group.isEmpty()) {
            group = null;
        }

        // generic filter based on event properties...
        List<TimeEvent> rs = schedule.findEvent(room, from, to, status, name);
        Calendar c = TimeTools.getCalendar(null);

        // training-specific filtering, if needed
        if (!rs.isEmpty() && (trainer != null || participant != null || course != null || category != null || group != null)) {
            Iterator<TimeEvent> it = rs.iterator();
            String notParticipant = null;
            if (participant != null && participant.startsWith("!")) {
                notParticipant = participant.substring(1);
                participant = null;
            }
            while (it.hasNext()) {
                TimeEvent t = it.next();
                if (group != null && !group.equals(t.getName())) {
                    it.remove();
                    continue;
                }
                if (participant != null && !t.getParticipants().containsKey(participant)) {
                    it.remove();
                    continue;
                }
                if (notParticipant != null && t.getParticipants().containsKey(notParticipant)) {
                    it.remove();
                    continue;
                }
                List<Group> gs = training.findGroups(t.getName(), trainer);
                if (trainer != null) {
                    if (gs == null || gs.size() != 1) {
                        it.remove();
                        continue;
                    }
                }
                if (course != null) {
                    if (gs == null || gs.size() != 1) {
                        it.remove();
                        continue;
                    }
                    if (!course.equals(gs.get(0).getCourse())) {
                        it.remove();
                        continue;
                    }
                }
                if (category != null) {
                    if (gs == null || gs.size() != 1) {
                        it.remove();
                        continue;
                    }
                    Course crs = training.courses.get(gs.get(0).getCourse());
                    if (crs == null || !category.equals(crs.getCategory())) {
                        it.remove();
                        continue;
                    }
                }
                if (dayOfWeek != null && dayOfWeek.length > 0) {
                    c.setTimeInMillis(t.getStart());
                    int dow = c.get(Calendar.DAY_OF_WEEK);
                    boolean found = false;
                    for (int dw : dayOfWeek) {
                        if (dw == dow) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        continue;
                    }
                }
            }
        }

        HttpUser user = (req != null && req.getHttpSession() != null) ? req.getHttpSession().getUser() : null;
        String email = (user != null) ? user.getId() : null;
        boolean admin = (user != null && user.getRoles() != null && user.getRoles().contains("admin"));
        for (TimeEvent te : rs) {
            TE t = toTE(te, email, admin);
            r.add(t);
        }
        //r.addAll(rs);

        return r;
    }

    @XMethod(name = "addEventsToBasket")
    public boolean addEventsToBasket(HttpRequest req,
            @XParameter(name = "eventIDs") String... eventIDs
    ) throws IOException {
        doMaintenance();
        boolean r = false;
        if (eventIDs != null) {
            Map<String, TimeEvent> basket = (Map) req.getHttpSession().getProperties().get("eventsBasket");
            for (String eventId : eventIDs) {
                TimeEvent ts = schedule.getEvent(eventId, null, null, null);
                if (ts != null) {
                    if (basket == null) {
                        basket = new LinkedHashMap<>();
                        req.getHttpSession().getProperties().put("eventsBasket", basket);
                    }
                    if (!basket.containsKey(ts.getId())) {
                        basket.put(ts.getId(), ts);
                        r = true;
                    }
                }
            }
        }
        return r;
    }

    @XMethod(name = "removeEventsFromBasket")
    public boolean removeEventsFromBasket(HttpRequest req,
            @XParameter(name = "eventIDs") String... eventIDs
    ) throws IOException {
        doMaintenance();
        boolean r = false;
        if (eventIDs != null) {
            Map<String, TimeEvent> basket = (Map) req.getHttpSession().getProperties().get("eventsBasket");
            if (basket != null) {
                for (String eventId : eventIDs) {
                    if ("-1".equals(eventId)) {
                        basket.clear();
                        r = true;
                        break;
                    }
                    TimeEvent ts = schedule.getEvent(eventId, null, null, null);
                    if (ts != null && basket.containsKey(ts.getId())) {
                        basket.remove(ts.getId());
                        r = true;
                    }
                }
            }
        }
        return r;
    }

    @XMethod(name = "getEventsInBasket")
    public Map<String, TE> getEventsInBasket(HttpRequest req) throws IOException {
        doMaintenance();
        Map<String, TimeEvent> tes = (Map) req.getHttpSession().getProperties().get("eventsBasket");
        if (tes != null) {
            HttpUser user = (req != null && req.getHttpSession() != null) ? req.getHttpSession().getUser() : null;
            String email = (user != null) ? user.getId() : null;
            boolean admin = (user != null && user.getRoles() != null && user.getRoles().contains("admin"));
            Map<String, TE> r = new LinkedHashMap<>();
            for (TimeEvent te : tes.values()) {
                TE t = toTE(te, email, admin);
                if (t != null) {
                    r.put(te.getId(), t);
                }
            }
            return r;
        } else {
            return null;
        }
    }

    @XMethod(name = "applyForGroups")
    public TRAINEE_STATUS[] applyForGroups(HttpRequest req,
            @XParameter(name = "email", optional = true) String email,
            @XParameter(name = "action", optional = true) ACTION action
    ) throws IOException {
        doMaintenance();

        Map<String, TimeEvent> basket = (Map) req.getHttpSession().getProperties().get("eventsBasket");

        TRAINEE_STATUS[] r = new TRAINEE_STATUS[basket != null ? basket.size() : 0];

        String u0 = um.findUser(email);
        String u = checkUser(req, email);
        HttpUser user = req.getHttpSession().getUser();

        if (u == null) {
            for (int i = 0; i < basket.size(); i++) {
                r[i] = TRAINEE_STATUS.no_applier;
            }
            return r;
        }

        int off = 0;
        for (TimeEvent ts : basket.values()) {
            int i = off++;
            if (ts == null) {
                r[i] = TRAINEE_STATUS.no_target;
                continue;
            }

            Room room = schedule.getRoom(ts.getRoom(), null);
            Group group = training.groups.get(ts.getName());
            if (group == null) {
                r[i] = TRAINEE_STATUS.no_target;
                continue;
            }

            int max = Math.min(room.getMaxSize(), group.getMaxSize());
            if (ts.getParticipants().size() >= max) {
                for (String vs : ts.getParticipants().values()) {
                    if (!"confirmed".equals(vs)) {
                        max++;
                    }
                }
            }

            if (ts.getParticipants().size() < max || action != ACTION.apply) {
                if (ts.getParticipants().containsKey(email)) {
                    switch (action) {
                        case apply:
                            r[i] = TRAINEE_STATUS.skipped_duplicate;
                            break;
                        case remove:
                            ts.getParticipants().remove(email);
                            r[i] = TRAINEE_STATUS.cancelled;
                            break;
                        case confirm:
                            if (!"confirmed".equals(ts.getParticipants().get(email))) {
                                ts.getParticipants().put(email, "confirmed");
                                r[i] = TRAINEE_STATUS.confirmed;
                            } else {
                                r[i] = TRAINEE_STATUS.pending; // TODO: add status 'ignored'?
                            }
                            break;
                    }
                } else {
                    switch (action) {
                        case apply:
                            ts.getParticipants().put(u, (u0 != null && user != null) ? "confirmed" : "" + TrainingAPI.PSTATE.verifying + "_" + Math.round(Math.random() * 200000) + "_" + Math.round(Math.random() * 200000));
                            r[i] = TRAINEE_STATUS.added;
                            break;
                        case remove:
                            r[i] = TRAINEE_STATUS.no_applier;
                            break;
                        case confirm:
                            r[i] = TRAINEE_STATUS.no_applier;
                            break;
                    }
                }
            } else {
                r[i] = TRAINEE_STATUS.no_space_in_target;
            }
        }

        // send e-mail if needed
        if (!basket.isEmpty() && u != null && (u0 == null || user == null)) {
            List<TimeEvent> evs = new ArrayList<>();
            for (int i = 0; i < basket.size(); i++) {
                if (r[i] == TRAINEE_STATUS.added) {
                    evs.add(basket.get(i));
                }
            }
            if (!evs.isEmpty()) {
                System.out.println("TODO: send e-mail to " + u + " for confirmation of " + evs.size() + " events ...");
                if (um.emailAgent() != null) {
                    // TODO: send email...
                } else {
                    for (TimeEvent ts : evs) {
                        if (ts == null) {
                            continue;
                        }
                        String key = ts.getParticipants().get(u);
                        System.out.println("TE: " + ts.getId() + "|" + ts.getRoom() + "|" + ts.getName() + "|" + ts.getStart() + ":" + ts.getDuration() + "|" + u + " -> " + key);
                    }
                }
            }
        }

        return r;
    }

    @XMethod(name = "applyForGroup")
    public TRAINEE_STATUS applyForGroup(HttpRequest req,
            @XParameter(name = "eventId") String eventId,
            @XParameter(name = "email", optional = true) String email) throws IOException {
        doMaintenance();
        TRAINEE_STATUS r = TRAINEE_STATUS.no_target;

        TimeEvent ts = schedule.getEvent(eventId, null, null, null);
        if (ts == null) {
            return r; //TRAINEE_STATUS.no_target;
        }
        String u0 = um.findUser(email);
        String u = checkUser(req, email);

        Room room = schedule.getRoom(ts.getRoom(), null);
        Group group = training.groups.get(ts.getName());

        int max = Math.min(room.getMaxSize(), group.getMaxSize());

        if (ts.getParticipants().size() < max) {
            if (ts.getParticipants().containsKey(email)) {
                r = TRAINEE_STATUS.skipped_duplicate;
            } else {
                //ts.getParticipants().put(u, (user != null) ? "confirmed" : "" + TrainingAPI.PSTATE.verifying);
                ts.getParticipants().put(u, (u0 != null) ? "confirmed" : "" + TrainingAPI.PSTATE.verifying);
                r = TRAINEE_STATUS.added;
                if (um.emailAgent() != null) {
                    System.out.println("TODO: send e-mail to " + u + " for confirmation...");
                    // TODO: send email...
                }
            }
        } else {
            r = TRAINEE_STATUS.no_space_in_target;
        }

        return r;
    }

    public String checkUser(HttpRequest req, String email) throws IOException {
        doMaintenance();
        HttpSession sess = (req != null) ? req.getHttpSession() : null;
        HttpUser user = (sess != null) ? sess.getUser() : null;
        if (user != null) {
            if (email == null) {
                email = (String) user.getProperties().get("email");
            }
            if (email != null && !(email.equals(user.getProperties().get(email)) || UM_STATE.verified.equals(um.userStates.get(user.getId())))) {
                List<String> roles = user.getRoles();
                if (roles != null && !roles.contains("admin")) {
                    throw new IOException("Non-admin can only self-apply.");
                }
            }
        }

        // guess if have trainee -> evaluate email.
        if (email != null && !email.contains("@")) {
            String un = um.findUser(email);
            if (un != null) {
                email = un;
            } else {
                email = null;
            }
        }
        if (email == null || email.isEmpty()) {
            email = (String) req.getContext().getProperties().get("email");
            if (email == null || email.isEmpty()) {
                return null;
            }
        } else {
            String semail = (String) req.getContext().getProperties().get("email");
            if (semail == null || semail.isEmpty()) {
                req.getContext().getProperties().put("email", email);
            }
        }

        String u = um.findUser(email);
        if (u == null) {
            u = email;
            um.addUser(u, null, UM_STATE.pending);
            training.courseParticipants.put(u, TrainingAPI.PSTATE.verifying);
        } else {
            // verify if need authentication
            String pass = um.pwds.get(u);
            if (user == null && pass != null && !pass.isEmpty() && req.getHttpSession() != null) {
                user = checkAuthentication(req, true);
                if (user == null) {
                    return null;
                }
                //user.getProperties().put("email", email);
                //user.getProperties().put("name", user.getName());
                //user.getProperties().put("roles", user.getRoles());
            }
        }
        return u;
    }

    public HttpUser checkAuthentication(HttpRequest req, boolean required) throws IOException {
        doMaintenance();
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

    @XMethod(name = "checkResources")
    public List<String> getResources(HttpRequest req,
            @XParameter(name = "prefix", optional = true) String prefix,
            @XParameter(name = "anywhere", optional = true) String anywhere,
            @XParameter(name = "suffix", optional = true) String suffix
    ) throws IOException {
        doMaintenance();
        if (prefix != null && prefix.isEmpty()) {
            prefix = null;
        }
        if (anywhere != null && anywhere.isEmpty()) {
            anywhere = null;
        }
        if (suffix != null && suffix.isEmpty()) {
            suffix = null;
        }
        List<String> r = new RL(getClass().getClassLoader()).print(null);
        if (prefix != null || anywhere != null || suffix != null) {
            Iterator<String> it = r.iterator();
            while (it.hasNext()) {
                String s = it.next();
                if (prefix != null && !s.startsWith(prefix)) {
                    it.remove();
                    continue;
                }
                if (suffix != null && !s.endsWith(suffix)) {
                    it.remove();
                    continue;
                }
                if (anywhere != null && !s.contains(anywhere)) {
                    it.remove();
                    continue;
                }
            }
        }
        return r;
    }

    @XMethod(name = "eventPlannersMeta")
    public Map getEventPlannersMeta(
            HttpRequest req
    ) throws IOException {
        doMaintenance();
        Map r = new LinkedHashMap<>();

        // year-level meta
        Calendar c = TimeTools.getCalendar(null);

        int nowYear = c.get(Calendar.YEAR);
        int nowSeason = SEASON.seasonOf(c).order();
        int nowQuarter = c.get(Calendar.MONTH) / 3;

        TimeTools.toStartOfYear(c);

        //c.add(Calendar.YEAR, -1);
        int year = c.get(Calendar.YEAR);
        Map y = new LinkedHashMap();
        List ys = new ArrayList();
        List yq = new ArrayList();
        r.put("years", y);
        r.put("yearSeasons", ys);
        r.put("yearQuarters", yq);
        for (int i = year; i < (year + 2); i++) {
            Map ym = new LinkedHashMap<>();
            Map sm = new LinkedHashMap<>();
            Map qm = new LinkedHashMap<>();
            y.put("" + i, ym);
            ym.put("seasons", sm);
            ym.put("quarters", qm);
            c.set(Calendar.YEAR, i);
            TimeTools.toStartOfYear(c);
            for (SEASON s : SEASON.values()) {
                c.set(Calendar.MONTH, s.months()[1]);
                long start = TimeTools.toStartOfSeason((Calendar) c.clone());
                long end = TimeTools.toEndOfSeason((Calendar) c.clone());
                sm.put("" + s, new long[]{start, end});
                ys.add(new long[]{i << 16 | s.order(), start, end});
                if (i == nowYear && s.order() == nowSeason) {
                    r.put("yearSeason", new long[]{i << 16 | s.order(), start, end});
                }
            }
            for (int q = 0; q < 4; q++) {
                c.set(Calendar.MONTH, q * 3);
                long start = TimeTools.toStartOfQuarter((Calendar) c.clone());
                long end = TimeTools.toEndOfQuarter((Calendar) c.clone());
                qm.put("" + q, new long[]{start, end});
                yq.add(new long[]{i << 16 | q, start, end});
                if (i == nowYear && q == nowQuarter) {
                    r.put("yearQuarter", new long[]{i << 16 | q, start, end});
                }
            }
        }
        List wd = new ArrayList();
        r.put("weekDays", wd);
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.MONTH, 4);
        TimeTools.toStartOfDay(c);
        while (c.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        for (int i = 0; i < 8; i++) {
            wd.add(c.getTimeInMillis());
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        wd.set(0, wd.get(wd.size() - 1));
        wd.remove(wd.size() - 1);

        // start times: 6:00
        Map t = new LinkedHashMap();
        List ts = new ArrayList();
        List td = new ArrayList();
        r.put("times", t);
        t.put("starts", ts);
        t.put("durations", td);
        long minT = 1000 * 60 * 60 * 6; // 6:00
        long maxT = 1000 * 60 * 60 * 22; // 22:00
        long stepT = 1000 * 60 * 15; // 22:00
        long maxD = 1000 * 60 * 60 * 12; // 12:00
        t.put("min", minT);
        t.put("max", maxT);
        t.put("step", stepT);
        for (long l = minT; l <= maxT; l += stepT) {
            ts.add(l);
        }
        for (long l = 0; l <= maxD; l += stepT) {
            td.add(l);
        }
        return r;
    }

    @XMethod(name = "eventPlanners")
    public Collection<TE> findTimeEventPlanners(
            HttpRequest req,
            @XParameter(name = "room", optional = true) String room,
            @XParameter(name = "name", optional = true) String name,
            @XParameter(name = "from", optional = true) Long from,
            @XParameter(name = "to", optional = true) Long to,
            @XParameter(name = "status", optional = true) String status,
            @XParameter(name = "trainer", optional = true) String trainer,
            @XParameter(name = "participant", optional = true) String participant,
            @XParameter(name = "category", optional = true) String category,
            @XParameter(name = "course", optional = true) String course,
            @XParameter(name = "group", optional = true) String group,
            @XParameter(name = "dayOfWeek", optional = true) Integer[] dayOfWeek
    ) throws IOException {
        doMaintenance();
        List<TE> r = new ArrayList<>();

        if (room != null && room.isEmpty()) {
            room = null;
        }
        if (name != null && name.isEmpty()) {
            name = null;
        }
        if (status != null && status.isEmpty()) {
            status = null;
        }
        if (trainer != null && trainer.isEmpty()) {
            trainer = null;
        }
        if (participant != null && participant.isEmpty()) {
            participant = null;
        }
        if (category != null && category.isEmpty()) {
            category = null;
        }
        if (course != null && course.isEmpty()) {
            course = null;
        }
        if (group != null && group.isEmpty()) {
            group = null;
        }

        // generic filter based on event properties...
        List<TimeEventPlanner> rs = schedule.findEventPlanners(room, from, to, name);
        Collection<TimeEventPlanner> hidden = new HashSet<>();

        // training-specific filtering, if needed
        if (!rs.isEmpty() && (trainer != null || participant != null || course != null || category != null || group != null)) {
            Iterator<TimeEventPlanner> it = rs.iterator();
            String notParticipant = null;
            if (participant != null && participant.startsWith("!")) {
                notParticipant = participant.substring(1);
                participant = null;
            }
            while (it.hasNext()) {
                TimeEventPlanner t = it.next();
                if (group != null && !group.equals(t.getName())) {
//                    it.remove();
                    hidden.add(t);
                    continue;
                }
//                if (participant != null && !t.getParticipants().containsKey(participant)) {
//                    it.remove();
//                    continue;
//                }
//                if (notParticipant != null && t.getParticipants().containsKey(notParticipant)) {
//                    it.remove();
//                    continue;
//                }
                List<Group> gs = training.findGroups(t.getName(), trainer);
                if (trainer != null) {
                    if (gs == null || gs.size() != 1) {
//                        it.remove();
                        hidden.add(t);
                        continue;
                    }
                }
                if (course != null) {
                    if (gs == null || gs.size() != 1) {
//                        it.remove();
                        hidden.add(t);
                        continue;
                    }
                    if (!course.equals(gs.get(0).getCourse())) {
//                        it.remove();
                        hidden.add(t);
                        continue;
                    }
                }
                if (category != null) {
                    if (gs == null || gs.size() != 1) {
//                        it.remove();
                        hidden.add(t);
                        continue;
                    }
                    if (!hidden.contains(t)) {
                        Course crs = training.courses.get(gs.get(0).getCourse());
                        if (crs == null || !category.equals(crs.getCategory())) {
//                        it.remove();
                            hidden.add(t);
                            continue;
                        }
                    }
                }
            }
        }

        HttpUser user = (req != null && req.getHttpSession() != null) ? req.getHttpSession().getUser() : null;
        String email = (user != null) ? user.getId() : null;
        boolean admin = (user != null && user.getRoles() != null && user.getRoles().contains("admin"));
        for (TimeEventPlanner te : rs) {
            TEP[] t = toTEP(te, dayOfWeek);
            if (t != null) {
                for (TEP tep : t) {
                    tep.hidden = (hidden.contains(te));
                    r.add(tep);
                }
            }
        }
        //r.addAll(rs);

        return r;
    }

    /**
     * Apply event planner actions: add/modify/delete
     *
     * @param req
     * @param added
     * @param modified
     * @param deleted
     * @return
     * @throws IOException
     */
    @XMethod(name = "modifyEventPlanners")
    public Map<String, Object> modifyTimeEventPlanners(
            HttpRequest req,
            @XParameter(name = "added", optional = true) Collection<Map> added,
            @XParameter(name = "deleted", optional = true) Collection<String> deleted,
            @XParameter(name = "modified", optional = true) Map<String, Map> modified
    ) throws IOException {
        doMaintenance();
        Map<String, Object> r = new LinkedHashMap<>();
        if (deleted != null) {
            for (String id : deleted) {
                if (schedule.eventPlanners.containsKey(id)) {
                    TimeEventPlanner te = schedule.eventPlanners.remove(id);
                    r.put(id, "deleted");
                }
            }
        }
        if (added != null) {
            for (Map map : added) {
                String id = (map != null) ? "" + map.get("id") : null;
                if (id == null) {
                    continue;
                }
                try {
                    String room = (String) map.get("room");
                    String name = (String) map.get("name");
                    Long from = CommonTools.toType(map.get("from"), Long.class);
                    Long to = CommonTools.toType(map.get("to"), Long.class);
                    Long start = CommonTools.toType(map.get("start"), Long.class);
                    Long duration = CommonTools.toType(map.get("duration"), Long.class);
                    int[] weekDays = CommonTools.toType(map.get("weekDays"), int[].class);
                    TimeEventPlanner te = schedule.addEventPlanner(
                            room,
                            name,
                            from,
                            to,
                            start,
                            duration,
                            weekDays
                    );
                    TEP[] tt = toTEP(te, -1);
                    if (tt != null && tt.length == 1) {
                        r.put(id, tt[0]);
                    } else {
                        r.put(id, "failed");
                    }
                } catch (Throwable th) {
                    r.put(id, "" + th);
                }
            }
        }
        if (modified != null) {
            for (Entry<String, Map> entry : modified.entrySet()) {
                String id = entry.getKey();
                Map map = entry.getValue();
                if (id == null || map == null) {
                    continue;
                }
                TimeEventPlanner te = schedule.eventPlanners.get(id);
                if (te != null) {
                    try {
                        String room = (String) map.get("room");
                        String name = (String) map.get("name");
                        Long from = CommonTools.toType(map.get("from"), Long.class);
                        Long to = CommonTools.toType(map.get("to"), Long.class);
                        Long start = CommonTools.toType(map.get("start"), Long.class);
                        Long duration = CommonTools.toType(map.get("duration"), Long.class);
                        int[] weekDays = CommonTools.toType(map.get("weekDays"), int[].class);
                        if (room != null) {
                            te.setRoom(room);
                        }
                        if (name != null) {
                            te.setName(name);
                        }
                        if (from != null) {
                            te.setFrom(from);
                        }
                        if (to != null) {
                            te.setTo(to);
                        }
                        if (start != null) {
                            te.setStart(start);
                        }
                        if (duration != null) {
                            te.setDuration(duration);
                        }
                        if (weekDays != null) {
                            te.setWeekDays((int[]) weekDays);
                        }
                        TEP[] tt = toTEP(te, -1);
                        if (tt != null && tt.length == 1) {
                            r.put(id, tt[0]);
                        } else {
                            r.put(id, "failed");
                        }
                    } catch (Throwable th) {
                        r.put(id, "" + th);
                    }
                } else {
                    r.put(id, null);
                }
            }
        }
        return r;
    }

    //@XMethod(name = "names")
    public String[] getPropertyNames() {
        return ((Collection<String>) Collections.list(System.getProperties().propertyNames())).toArray(new String[System.getProperties().size()]);
    }

    //@XMethod(name = "names")
    public String[] getPropertyNames(@XParameter(name = "mask") String mask) {
        String[] ns = getPropertyNames();
        int c = 0;
        for (int i = 0; i < ns.length; i++) {
            if (mask != null && !ns[i].contains(mask)) {
                ns[i] = null;
            } else {
                c++;
            }
        }
        if (c < ns.length) {
            if (c == 0) {
                return new String[0];
            }
            int off = 0;
            for (int i = 0; i < ns.length; i++) {
                if (ns[i] == null) {
                } else {
                    ns[off++] = ns[i];
                }
            }
        }
        if (c < ns.length) {
            return Arrays.copyOf(ns, c);
        } else {
            return ns;
        }
    }

    //@XMethod(name = "property")
    public String getProperty(@XParameter(name = "name") String name) {
        return System.getProperty(name);
    }

    //@XMethod(name = "properties")
    public String[][] getProperties(@XParameter(name = "mask") String mask, @XParameter(name = "valueMask", optional = true) String valueMask, @XParameter(name = "skipEmpties", optional = true) Boolean skipEmpties) {
        String[] ns = getPropertyNames(mask);
        if (skipEmpties == null) {
            skipEmpties = false;
        }
        if (ns.length > 0) {
            String[][] result = new String[ns.length][2];
            int off = 0;
            for (int i = 0; i < result.length; i++) {
                String v = System.getProperty(ns[i]);
                if (v != null && valueMask != null && !v.contains(valueMask)) {
                    v = null;
                }
                if (v == null && skipEmpties) {
                    continue;
                }
                result[off][0] = ns[i];
                result[off++][1] = v;
            }
            if (off < result.length) {
                result = Arrays.copyOf(result, off);
            }
            return result;
        } else {
            return new String[0][0];
        }
    }

    public TE toTE(TimeEvent te, String myEmail, boolean admin) {
        TE t = new TE(te);
        Group g = training.groups.get(t.name);
        if (g != null) {
            t.course = g.getCourse();
            t.trainer = g.getTrainer();
            t.shortName = g.getShortName();
            t.maxSize = g.getMaxSize();

            IconInfo ii = training.findIcon(null, null, g);
            if (ii != null) {
                t.icon = ii.getIcon(null);
            }
        }
        if (te.getParticipants() != null) {
            t.size = te.getParticipants().size();
            int cc = 0;
            for (String s : te.getParticipants().values()) {
                if (s.startsWith("confirmed")) {
                    cc++;
                }
            }
            t.confSize = cc;
        }
        if (myEmail != null) {
            t.myState = te.getParticipants().get(myEmail);
        }
        if (admin && te.getParticipants() != null && !te.getParticipants().isEmpty()) {
            Map<String, String> ps = te.getParticipants();
            t.participants = new String[ps.size()][];
            int off = 0;
            for (Entry<String, String> ent : ps.entrySet()) {
                t.participants[off++] = new String[]{ent.getKey(), ent.getValue()};
            }
        }
        return t;
    }

    public TEP[] toTEP(TimeEventPlanner te, Integer... dayOfWeek) {
        TEP[] r = new TEP[te != null && te.getWeekDays() != null ? te.getWeekDays().length : 0];
        int off = 0;
        if (te != null && te.getWeekDays() != null) {
            for (int dow : te.getWeekDays()) {
                // check if mentioned day of week is valid or no day separation is needed.
                if (dayOfWeek != null && dayOfWeek.length == 1 && dayOfWeek[0] == -1) {
                    dow = -1;
                } else if (dayOfWeek != null) {
                    boolean found = false;
                    for (int dw : dayOfWeek) {
                        if (dow == dw) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        continue;
                    }
                }
                TEP t = new TEP(te, dow);
                r[off++] = t;
                Group g = training.groups.get(t.name);
                if (g != null) {
                    t.course = g.getCourse();
                    t.trainer = g.getTrainer();
                    t.shortName = g.getShortName();
                    t.maxSize = g.getMaxSize();

                    IconInfo ii = training.findIcon(null, null, g);
                    if (ii != null) {
                        t.icon = ii.getIcon(null);
                    }
                }
                if (dow == -1) {
                    break;
                }
            }
        }

        if (off < r.length) {
            r = Arrays.copyOf(r, off);
        }

        return r;
    }

    public static class TE {

        public String id;
        public String name;
        public String shortName;
        public String room;
        public long start;
        public long end;
        public long duration;
        public String course;
        public String group;
        public String trainer;
        public String[][] participants;
        public String myState;
        public int size;
        public int maxSize;
        public int confSize;
        public String icon;

        public TE() {
        }

        public TE(TimeEvent te) {
            this.id = te.getId();
            this.name = te.getName();
            this.room = te.getRoom();
            this.start = te.getStart();
            this.end = te.getEnd();
            this.duration = te.getDuration();
            this.group = te.getName();
        }
    }

    public static class TEP extends TE {

        public long from;
        public long to;
        public int[] weekDays;
        public boolean hidden = false;

        public TEP() {
        }

        public TEP(TimeEventPlanner te, int dayOfWeek) {
            Calendar c = TimeTools.getCalendar(te.getFrom());
            TimeTools.toStartOfWeek(c);
            if (dayOfWeek < 0) {
                this.start = te.getStart();
                this.duration = te.getDuration();
                this.end = this.start + this.duration;
            } else {
                int dow = c.get(Calendar.DAY_OF_WEEK);
                while (dow != dayOfWeek) {
                    c.add(Calendar.DAY_OF_YEAR, 1);
                    dow = c.get(Calendar.DAY_OF_WEEK);
                }
                c.set(Calendar.HOUR_OF_DAY, (int) (te.getStart() / (1000 * 60 * 60)));
                c.set(Calendar.MINUTE, (int) (te.getStart() % (1000 * 60)));
                this.start = c.getTimeInMillis();
                this.end = start + te.getDuration();
                this.duration = te.getDuration();
            }
            this.id = te.getId();
            this.name = te.getName();
            this.room = te.getRoom();
            this.group = te.getName();
            this.weekDays = te.getWeekDays();
            this.from = te.getFrom();
            this.to = te.getTo();
        }
    }
}
