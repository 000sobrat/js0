/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssg.lib.jana.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import ssg.lib.http.HttpSession;
import ssg.lib.http.HttpUser;
import ssg.lib.http.base.HttpRequest;
import ssg.lib.http.rest.annotations.XMethod;
import ssg.lib.http.rest.annotations.XParameter;
import ssg.lib.http.rest.annotations.XType;
import ssg.lib.jana.api.ScheduleAPI.Room;
import ssg.lib.jana.api.ScheduleAPI.TimeEvent;
import ssg.lib.jana.api.TrainingAPI.Category;
import ssg.lib.jana.api.TrainingAPI.Course;
import ssg.lib.jana.api.TrainingAPI.Group;
import ssg.lib.jana.api.TrainingAPI.IconInfo;
import ssg.lib.jana.api.TrainingAPI.Trainer;
import ssg.lib.jana.tools.TimeTools;

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

    @XMethod(name = "eventsMeta")
    public Map<String, Object> getEventsMeta(
            @XParameter(name = "from", optional = true) Long from,
            @XParameter(name = "to", optional = true) Long to
    ) throws IOException {
        Map<String, Object> r = new LinkedHashMap<>();
        Map<String, String[]> rg = new LinkedHashMap<>();
        Map<String, String[]> rc = new LinkedHashMap<>();
        Map<String, String[]> rcc = new LinkedHashMap<>();
        Map<String, String> rt = new LinkedHashMap<>();
        Map<String, String> rm = new LinkedHashMap<>();

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
     * @param course
     * @param group
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
            @XParameter(name = "group", optional = true) String group
    ) throws IOException {
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
            }
        }

        HttpUser user = (req != null && req.getHttpSession() != null) ? req.getHttpSession().getUser() : null;
        String email = (user != null) ? user.getName() : null;
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
        boolean r = false;
        if (eventIDs != null) {
            Map<String, TimeEvent> basket = (Map) req.getHttpSession().getProperties().get("eventsBasket");
            if (basket != null) {
                for (String eventId : eventIDs) {
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
        Map<String, TimeEvent> tes = (Map) req.getHttpSession().getProperties().get("eventsBasket");
        if (tes != null) {
            HttpUser user = (req != null && req.getHttpSession() != null) ? req.getHttpSession().getUser() : null;
            String email = (user != null) ? user.getName() : null;
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
        TRAINEE_STATUS r = TRAINEE_STATUS.no_target;

        TimeEvent ts = schedule.getEvent(eventId, null, null, null);
        if (ts == null) {
            return r; //TRAINEE_STATUS.no_target;
        }

//        HttpSession sess = (req != null) ? req.getHttpSession() : null;
//        HttpUser user = (sess != null) ? sess.getUser() : null;
//        if (user != null) {
//            if (email == null) {
//                email = (String) user.getProperties().get("email");
//            }
//            if (email != null && !email.equals(user.getProperties().get(email))) {
//                List<String> roles = user.getRoles();
//                if (roles != null && !roles.contains("admin")) {
//                    throw new IOException("Non-admin can only self-apply.");
//                }
//            }
//        }
//        // guess if have trainee -> evaluate email.
//        if (email != null && !email.contains("@")) {
//            String un = um.findUser(email);
//            if (un != null) {
//                email = un;
//            } else {
//                email = null;
//            }
//        }
//        if (email == null || email.isEmpty()) {
//            email = (String) req.getContext().getProperties().get("email");
//            if (email == null || email.isEmpty()) {
//                return TRAINEE_STATUS.no_applier;
//            }
//        } else {
//            String semail = (String) req.getContext().getProperties().get("email");
//            if (semail == null || semail.isEmpty()) {
//                req.getContext().getProperties().put("email", email);
//            }
//        }
//
//        String u = um.findUser(email);
//        if (u == null) {
//            u = email;
//            um.users.put(u, u);
//            training.courseParticipants.put(u, TrainingAPI.PSTATE.verifying);
//        } else {
//            // verify if need authentication
//            String pass = um.pwds.get(u);
//            if (user == null && pass != null && !pass.isEmpty() && req.getHttpSession() != null) {
//                user = checkAuthentication(req, true);
//                if (user == null) {
//                    return r;
//                }
//                user.getProperties().put("email", email);
//                user.getProperties().put("name", user.getName());
//                user.getProperties().put("roles", user.getRoles());
//            }
//        }
//        String u = um.findUser(email);
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
            um.users.put(u, u);
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

    public TE toTE(TimeEvent te, String myEmail, boolean admin) {
        TE t = new TE(te);
        Group g = training.groups.get(te.getName());
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
}
