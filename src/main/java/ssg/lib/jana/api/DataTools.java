/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssg.lib.jana.api;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import ssg.lib.jana.api.ScheduleAPI.Room;
import ssg.lib.jana.api.ScheduleAPI.TimeEvent;
import ssg.lib.jana.api.ScheduleAPI.TimeEventPlanner;
import ssg.lib.jana.api.TrainingAPI.Category;
import ssg.lib.jana.api.TrainingAPI.Course;
import ssg.lib.jana.api.TrainingAPI.Group;
import ssg.lib.jana.api.TrainingAPI.PSTATE;
import ssg.lib.jana.api.TrainingAPI.Pricing;
import ssg.lib.jana.api.TrainingAPI.Pricing.PriceItem;
import ssg.lib.jana.api.TrainingAPI.Trainer;
import ssg.lib.jana.tools.TimeTools;

/**
 *
 * @author sesidoro
 */
public class DataTools {

    public static UM_API initUM(String prefix) {
        UM_API r = new UM_API();

        if (prefix == null) {
            prefix = "";
        }

        r.users.put(prefix + "a@a.com", "A A A");
        r.users.put(prefix + "b@a.com", "B B B");
        r.users.put(prefix + "c@a.com", "C C C");
        r.users.put(prefix + "d@a.b.c.d.e.f.g.h.com", "D D D D");
        r.users.put(prefix + "e@a.com", "Евгений");
        r.users.put(prefix + "000sobrat@gmail.com", "Sobrat 000");
        r.pwds.put(prefix + "a@a.com", "aaa");
        r.pwds.put(prefix + "b@a.com", "bbb");
        r.pwds.put(prefix + "d@a.b.c.d.e.f.g.h.com", "ddd");
        r.pwds.put(prefix + "000sobrat@gmail.com", "000");
        r.addUserRoles(prefix + "a@a.com", "admin","trainer");
        r.addUserRoles(prefix + "b@a.com", "trainer");
        r.addUserRoles(prefix + "d@a.b.c.d.e.f.g.h.com", "admin");
        r.addUserRoles(prefix + "000sobrat@gmail.com", "admin");
        return r;
    }

    public static TrainingAPI initTraining(String prefix) {
        TrainingAPI r = new TrainingAPI();

        if (prefix == null) {
            prefix = "";
        }

        Trainer trA = new Trainer();
        trA.setEmail(prefix + "a@a.com");
        trA.setName("A A A");
        Trainer trB = new Trainer();
        trB.setEmail(prefix + "b@a.com");
        trB.setName("B B B");

        r.trainers.put(trA.getId(), trA);
        r.trainers.put(trB.getId(), trB);

        Course cA1 = new Course();
        Course cA2 = new Course();
        Course cA3 = new Course();
        Course cA4 = new Course();
        Course cB1 = new Course();
        Course cB2 = new Course();
        cA1.setName("CA1");
        cA1.setDescription("Course A1");
        cA2.setName("CA2");
        cA2.setDescription("Course A2");
        cA3.setName("CA3");
        cA3.setDescription("Course A3");
        cA4.setName("CA4");
        cA4.setDescription("Course A4");
        cB1.setName("CB1");
        cB1.setDescription("Course B1");
        cB2.setName("CB2");
        cB2.setDescription("Course B2");

        for (Course c : new Course[]{cA1, cA2, cA3, cA4, cB1, cB2}) {
            r.courses.put(c.getId(), c);
        }

        Group gA11 = new Group();
        Group gA12 = new Group();
        Group gA41 = new Group();
        Group gB11 = new Group();
        Group gB23 = new Group();
        gA11.setName("A1-1");
        gA11.setCourse(cA1.getId());
        gA11.setTrainer(trA.getId());
        gA12.setName("A1-2");
        gA12.setCourse(cA2.getId());
        gA12.setTrainer(trA.getId());
        gA41.setName("A4-1");
        gA41.setCourse(cA4.getId());
        gA41.setTrainer(trA.getId());
        gB11.setName("B1-1");
        gB11.setCourse(cB1.getId());
        gB11.setTrainer(trB.getId());
        gB11.setMaxSize(8);
        gB23.setName("B2-3");
        gB23.setCourse(cB2.getId());
        gB23.setTrainer(trB.getId());

        for (Group g : new Group[]{gA11, gA12, gA41, gB11, gB23}) {
            r.groups.put(g.getId(), g);
        }

        r.courseTrainers.put(cA1, Collections.singletonList(trA));
        r.courseTrainers.put(cA2, Collections.singletonList(trA));
        r.courseTrainers.put(cA3, Collections.singletonList(trA));
        r.courseTrainers.put(cA4, Collections.singletonList(trA));
        r.courseTrainers.put(cB1, Collections.singletonList(trB));
        r.courseTrainers.put(cB2, Collections.singletonList(trB));

        r.courseParticipants.put(prefix + "a@a.com", PSTATE.trusted);
        r.courseParticipants.put(prefix + "b@a.com", PSTATE.trusted);
        r.courseParticipants.put(prefix + "c@a.com", PSTATE.trusted);
        r.courseParticipants.put(prefix + "d@a.b.c.d.e.f.g.h.com", PSTATE.forbidden);
        r.courseParticipants.put(prefix + "e@a.com", PSTATE.verifying);

        return r;
    }

    public static ScheduleAPI initSchedule(String prefix) {
        ScheduleAPI r = new ScheduleAPI();

        Room rA = new Room();
        Room rB = new Room();
        rA.setName("Room A");
        rA.setAddress("Somwhere at A1");
        rA.setMaxSize(15);
        rB.setName("Room B");
        rB.setAddress("Somwhere at A1");
        rB.setMaxSize(10);

        for (Room ri : new Room[]{rA, rB}) {
            r.rooms.add(ri);
        }

        Calendar cal = TimeTools.getCalendar(null);
        TimeTools.toStartOfMonth(cal);
        TimeTools.toStartOfWeek(cal);
        for (int i = 0; i < 35; i++) {
            long D1 = cal.getTimeInMillis();
            cal.add(Calendar.DAY_OF_WEEK, 1);
            long D2 = cal.getTimeInMillis();
            cal.add(Calendar.DAY_OF_WEEK, 1);
            long D3 = cal.getTimeInMillis();
            cal.add(Calendar.DAY_OF_WEEK, 1);
            long D4 = cal.getTimeInMillis();
            cal.add(Calendar.DAY_OF_WEEK, 1);
            long D5 = cal.getTimeInMillis();
            cal.add(Calendar.DAY_OF_WEEK, 1);
            long D6 = cal.getTimeInMillis();
            cal.add(Calendar.DAY_OF_WEEK, 1);
            long D7 = cal.getTimeInMillis();

            long t1 = TimeTools.ONE_HOUR * 16 + TimeTools.ONE_MIN * 30;
            long t2 = TimeTools.ONE_HOUR * 17 + TimeTools.ONE_MIN * 00;
            long t3 = TimeTools.ONE_HOUR * 18 + TimeTools.ONE_MIN * 00;
            long t4 = TimeTools.ONE_HOUR * 19 + TimeTools.ONE_MIN * 30;

            long d1 = TimeTools.ONE_MIN * 30;
            long d2 = TimeTools.ONE_MIN * 45;
            long d3 = TimeTools.ONE_MIN * 60;

            for (Long D : new Long[]{D1, D2, D3, D4, D5, D6}) {
                for (Object[] oo : new Object[][]{
                    {rA.getId(), D + t1, d1, "A1-1", null, new String[][]{
                        {"a@a.com", "confirmed"},
                        {"b@a.com", "confirmed"},
                        {"c@a.com", "confirmed"},
                        {"e@a.com", "cancelled"}
                    }},
                    {rA.getId(), D + t2, d3, "A1-2", null},
                    {rB.getId(), D + t3, d1 + d3, "B2-1", null},
                    {rA.getId(), D + t4, d2, "B1-1", null}
                }) {
                    TimeEvent t = new TimeEvent();
                    t.setRoom((String) oo[0]);
                    t.setStart((Long) oo[1]);
                    t.setDuration((Long) oo[2]);
                    t.setName((String) oo[3]);
                    t.setStatus((String) oo[4]);
                    r.events.put(t.getId(), t);
                    if (oo.length > 5 && oo[5] instanceof String[][]) {
                        for (String[] ss : ((String[][]) oo[5])) {
                            t.getParticipants().put(ss[0], ss[1]);
                        }
                    }
                }
            }
        }

        return r;
    }

    public static Map initJana(String prefix) throws IOException {
        Map map = new LinkedHashMap();

        {
            UM_API um = new UM_API();
            TrainingAPI training = new TrainingAPI();
            ScheduleAPI schedule = new ScheduleAPI();

            map.put("um", um);
            map.put("training", training);
            map.put("schedule", schedule);

            if (prefix == null) {
                prefix = "";
            }

            ////////////////////////////////////////////////////////////////////
            ///////////////////////////////////////////////////////////////// UM
            ////////////////////////////////////////////////////////////////////
            // basic
            um.users.put("jana@kuntajana.fi", "Jana");
            um.users.put("dancer@kuntajana.fi", "Dance trainer");
            um.users.put("b-dancer@kuntajana.fi", "Break Dance trainer");
            um.addUserRoles("jana@kuntajana.fi", "admin", "trainer");
            um.addUserRoles("dancer@kuntajana.fi", "trainer");
            um.addUserRoles("b-dancer@kuntajana.fi", "trainer");

            // demo
            um.users.put(prefix + "a@a.com", "A A A");
            um.users.put(prefix + "b@a.com", "B B B");
            um.users.put(prefix + "c@a.com", "C C C");
            um.users.put(prefix + "d@a.b.c.d.e.f.g.h.com", "D D D D");
            um.users.put(prefix + "e@a.com", "Евгений");
            um.users.put(prefix + "000sobrat@gmail.com", "Sobrat 000");
            um.pwds.put(prefix + "a@a.com", "aaa");
            um.pwds.put(prefix + "b@a.com", "bbb");
            um.pwds.put(prefix + "d@a.b.c.d.e.f.g.h.com", "ddd");
            um.pwds.put(prefix + "000sobrat@gmail.com", "000");
            um.addUserRoles(prefix + "a@a.com", "admin", "trainer");
            um.addUserRoles(prefix + "b@a.com", "trainer");
            um.addUserRoles(prefix + "d@a.b.c.d.e.f.g.h.com", "admin");
            um.addUserRoles(prefix + "000sobrat@gmail.com", "admin");

            ////////////////////////////////////////////////////////////////////
            /////////////////////////////////////////////////////////// Training
            ////////////////////////////////////////////////////////////////////
            Trainer trA = new Trainer();
            trA.setEmail("jana@kuntajana.fi");
            trA.setName("Jana");
            Trainer trB = new Trainer();
            trB.setEmail("dancer@kuntajana.fi");
            trB.setName("Dance trainer");
            Trainer trC = new Trainer();
            trC.setEmail("b-dancer@kuntajana.fi");
            trC.setName("Break Dance trainer");

            Category sport = new Category("Спорт", null);
            Category dance = new Category("Танцы", null);

            Pricing priceSport = new Pricing(
                    sport.getId(), // String category,
                    null, // String course,
                    null, // String room,
                    new PriceItem(1, 10f, null), // Первое посещение 10е
                    new PriceItem(1, 15f, 0L), // Разовая оплата 15е
                    //PriceItem... abonements
                    new PriceItem(10, 100f, TimeTools.ONE_DAY * 7 * 4 * 2), // -100е абонемент на 10 занятий (2 месяца длительность).
                    new PriceItem(10, 120f, TimeTools.ONE_DAY * 7 * 4 * 4) // -120е абонемент на 10 занятий (4 месяца длительность).
            );
            Pricing priceDance = new Pricing(
                    dance.getId(), // String category,
                    null, // String course,
                    null, // String room,
                    new PriceItem(1, 10f, null), // Первое посещение 10е
                    new PriceItem(1, 15f, 0L), // Разовая оплата 15е
                    //PriceItem... abonements
                    new PriceItem(4, 40f, TimeTools.ONE_DAY * 7 * 4, "4 раза 1 раз в неделю"), // абонемент на один месяц: 4 раза 1 раз в неделю 40е
                    new PriceItem(8, 80f, TimeTools.ONE_DAY * 7 * 4, "8 раз в месяц -80е") // 8 раз в месяц -80е
            );

            sport.setPricing(priceSport.getId());
            dance.setPricing(priceDance.getId());

            Course csport1 = new Course(sport.getId(), "Pilates", null, null);
            Course csport2 = new Course(sport.getId(), "Fly Yoga", null, null);
            Course csport3 = new Course(sport.getId(), "Stretching", null, null);
            Course csport4 = new Course(sport.getId(), "Koreagrafia ballet", null, null);
            Course csport5 = new Course(sport.getId(), "Tabatta intensiv", null, null);

            Course cdance1 = new Course(dance.getId(), "Fly Dance", null, null);
            Course cdance2 = new Course(dance.getId(), "Strip Dance", null, null);
            Course cdance3 = new Course(dance.getId(), "Break Dance, дети", null, null);

            for (Trainer t : new Trainer[]{trA, trB, trC}) {
                training.trainers.put(t.getId(), t);
            }
            for (Category c : new Category[]{sport, dance}) {
                training.categories.put(c.getId(), c);
            }
            for (Pricing p : new Pricing[]{priceSport, priceDance}) {
                training.prices.put(p.getId(), p);
            }
            for (Course c : new Course[]{csport1, csport2, csport3, csport4, csport5, cdance1, cdance2, cdance3}) {
                training.courses.put(c.getId(), c);
            }

            Group gPilates = new Group("Pilates", "Plt", csport1.getId(), trA.getId(), null, null);
            Group gKoreografia = new Group("Koreografia ballet", "Kor", csport4.getId(), trB.getId(), null, null);
            Group gPilatesBasic = new Group("Pilates basik", "Plt(b)", csport1.getId(), trA.getId(), null, null);
            Group gStretching = new Group("Stretching Split", "Str", csport3.getId(), trA.getId(), null, null);

            Group gFlyDanceAcro = new Group("Fly Dance acro 14+", "FD 14+", cdance1.getId(), trB.getId(), null, null);
            Group gFlyYogaBasic = new Group("Fly Jooga basik", "FY(b)", csport2.getId(), trA.getId(), null, null);
            Group gFrameUp = new Group("Frame-Up", "FrUp", cdance2.getId(), trB.getId(), null, null);

            Group gTabatta = new Group("Tabatta", "Tab", csport5.getId(), trA.getId(), null, null);
            Group gPilatesIntensive = new Group("Pilates intensive", "Plt(i)", csport1.getId(), trA.getId(), null, null);

            Group gFlyYogaAdv = new Group("Fly Jooga jatko", "FY(adv)", csport2.getId(), trA.getId(), null, null);
            Group gBreakDance = new Group("Break dance 7+", "Brd 7+", cdance3.getId(), trC.getId(), null, null);

            for (Group g : new Group[]{
                gPilates,
                gKoreografia,
                gPilatesBasic,
                gStretching,
                gFlyDanceAcro,
                gFlyYogaBasic,
                gFrameUp,
                gTabatta,
                gPilatesIntensive,
                gFlyYogaAdv,
                gBreakDance
            }) {
                training.groups.put(g.getId(), g);
            }

//            for (Object[] oo : new Object[][]{
//                {"sP-1", csport1, trA},
//                {"sFY-1", csport2, trA},
//                {"sST-1", csport3, trA},
//                {"sKB-1", csport4, trB},
//                {"sTI-1", csport5, trA},
//                {"dFD-1", cdance1, trB},
//                {"dSD-1", cdance2, trB},
//                {"dBD-1", cdance3, trC}
//            }) {
//                Group g = new Group(
//                        (String) oo[0], // String name,
//                        ((Course) oo[1]).getId(), // String course,
//                        ((Trainer) oo[2]).getId(), // String trainer
//                        null, //Integer maxSize,
//                        null //String pricing
//                );
//                training.groups.put(g.getId(), g);
//            }

            for (Course c : new Course[]{csport1, csport2, csport3, csport5}) {
                training.courseTrainers.put(c, Collections.singletonList(trA));
            }
            for (Course c : new Course[]{csport4, cdance1, cdance2}) {
                training.courseTrainers.put(c, Collections.singletonList(trB));
            }
            for (Course c : new Course[]{cdance3}) {
                training.courseTrainers.put(c, Collections.singletonList(trC));
            }

            training.courseParticipants.put(prefix + "a@a.com", PSTATE.trusted);
            training.courseParticipants.put(prefix + "b@a.com", PSTATE.trusted);
            training.courseParticipants.put(prefix + "c@a.com", PSTATE.trusted);
            training.courseParticipants.put(prefix + "d@a.b.c.d.e.f.g.h.com", PSTATE.forbidden);
            training.courseParticipants.put(prefix + "e@a.com", PSTATE.verifying);

            ////////////////////////////////////////////////////////////////////
            /////////////////////////////////////////////////////////// Training
            ////////////////////////////////////////////////////////////////////
            Room rA = new Room();
            Room rB = new Room();
            rA.setName("Kuparitie 1");
            rA.setAddress("Kuparitie 1, Pohjois-Haaga");
            rA.setMaxSize(15);
            rB.setName("Vesala");
            rB.setAddress("Vesala...");
            rB.setMaxSize(10);

            for (Room ri : new Room[]{rA, rB}) {
                schedule.rooms.add(ri);
            }

            // add group-based event planners:
            {
                long[] season=TimeTools.rangeOf(null, TimeTools.SEASON.autumn);
                long from = season[0];
                long to = season[1];

/*                
                gPilates,
                gKoreografia,
                gPilatesBasic,
                gStretching,
                gFlyDanceAcro,
                gFlyYogaBasic,
                gFrameUp,
                gPilatesBasicV,
                gTabatta,
                gPilatesIntensive,
                gFlyYogaAdv,
                gBreakDance
*/                
                
                for (Object[] oo : new Object[][]{
                    //
                    {"Аренда", "арендатор", TimeTools.timeHM(17, 00), TimeTools.timeHM(3, 00), new int[]{Calendar.MONDAY}},
                    //
                    {gPilates, rA, TimeTools.timeHM(10, 30), TimeTools.timeHM(1, 0), new int[]{Calendar.TUESDAY}},
                    {gKoreografia, rA, TimeTools.timeHM(17, 00), TimeTools.timeHM(1, 0), new int[]{Calendar.TUESDAY}},
                    {gPilatesBasic, rA, TimeTools.timeHM(18, 15), TimeTools.timeHM(1, 0), new int[]{Calendar.TUESDAY}},
                    {gStretching, rA, TimeTools.timeHM(19, 30), TimeTools.timeHM(1, 15), new int[]{Calendar.TUESDAY}},
                    //
                    {gFlyDanceAcro, rA, TimeTools.timeHM(17, 00), TimeTools.timeHM(1, 0), new int[]{Calendar.WEDNESDAY}},
                    {gFlyYogaBasic, rA, TimeTools.timeHM(18, 15), TimeTools.timeHM(1, 0), new int[]{Calendar.WEDNESDAY}},
                    {gFrameUp, rA, TimeTools.timeHM(19, 30), TimeTools.timeHM(1, 0), new int[]{Calendar.WEDNESDAY}},
                    {gPilatesBasic, rB, TimeTools.timeHM(19, 30), TimeTools.timeHM(1, 0), new int[]{Calendar.WEDNESDAY}},
                    //
                    {gPilates, rA, TimeTools.timeHM(10, 30), TimeTools.timeHM(1, 0), new int[]{Calendar.THURSDAY}},
                    {gTabatta, rA, TimeTools.timeHM(17, 00), TimeTools.timeHM(1, 0), new int[]{Calendar.THURSDAY}},
                    {gPilatesIntensive, rA, TimeTools.timeHM(18, 15), TimeTools.timeHM(1, 0), new int[]{Calendar.THURSDAY}},
                    {gStretching, rA, TimeTools.timeHM(19, 30), TimeTools.timeHM(1, 0), new int[]{Calendar.THURSDAY}},
                    //
                    {gFlyDanceAcro, rA, TimeTools.timeHM(17, 00), TimeTools.timeHM(1, 0), new int[]{Calendar.FRIDAY}},
                    {gFlyYogaAdv, rA, TimeTools.timeHM(18, 15), TimeTools.timeHM(1, 0), new int[]{Calendar.FRIDAY}},
                    {gFrameUp, rA, TimeTools.timeHM(19, 30), TimeTools.timeHM(1, 0), new int[]{Calendar.FRIDAY}},
                    {gPilates, rB, TimeTools.timeHM(19, 30), TimeTools.timeHM(1, 0), new int[]{Calendar.FRIDAY}},
                    //
                    {gTabatta, rA, TimeTools.timeHM(11, 00), TimeTools.timeHM(1, 0), new int[]{Calendar.SATURDAY}},
                    {gPilatesIntensive, rA, TimeTools.timeHM(12, 15), TimeTools.timeHM(1, 0), new int[]{Calendar.SATURDAY}},
                    //
                    {gPilates, rA, TimeTools.timeHM(11, 00), TimeTools.timeHM(1, 0), new int[]{Calendar.SUNDAY}},
                    {gKoreografia, rA, TimeTools.timeHM(12, 15), TimeTools.timeHM(1, 0), new int[]{Calendar.SUNDAY}},
                    {gBreakDance, rA, TimeTools.timeHM(16, 30), TimeTools.timeHM(1, 15), new int[]{Calendar.SUNDAY}},
                    null
                }) {
                    if (oo == null) {
                        continue;
                    }
                    Group g = (oo[0] instanceof Group) ? (Group) oo[0] : null;
                    String n = (oo[0] instanceof String) ? (String) oo[0] : null;
                    TimeEventPlanner tep = new TimeEventPlanner(
                            from,// Long from,
                            to,// Long to,
                            (oo[1] instanceof Room) ? ((Room) oo[1]).getId() : null, // String room,
                            (g!=null) ? g.getId() : n, // String name,
                            (Long) oo[2], // long start,
                            (Long) oo[3], // long duration,
                            null, //Map<String, String> participants,
                            (int[]) oo[4] // int... weekDays
                    );
                    schedule.eventPlanners.put(tep.getId(), tep);
                }
            }
            
//            {
//                Calendar cal = TimeTools.getCalendar(null);
//                long from = TimeTools.toStartOfMonth(cal);
//                long to = TimeTools.toEndOfYear(cal);
//
//                for (Object[] oo : new Object[][]{
//                    {"sP-1", rA, TimeTools.timeHM(16, 15), TimeTools.timeHM(0, 45), new int[]{Calendar.TUESDAY, Calendar.THURSDAY, Calendar.FRIDAY}},
//                    {"sP-1", rA, TimeTools.timeHM(15, 00), TimeTools.timeHM(0, 45), new int[]{Calendar.SUNDAY}}, //                    {"sP-1", rA, TimeTools.timeHM(16, 15), TimeTools.timeHM(0, 45), new int[]{Calendar.TUESDAY, Calendar.THURSDAY, Calendar.FRIDAY}},
//                    {"sFY-1", rA, TimeTools.timeHM(17, 45), TimeTools.timeHM(0, 60), new int[]{Calendar.THURSDAY, Calendar.FRIDAY}},
//                    {"sFY-1", rA, TimeTools.timeHM(18, 00), TimeTools.timeHM(0, 60), new int[]{Calendar.SUNDAY}},
//                    {"dFD-1", rB, TimeTools.timeHM(17, 00), TimeTools.timeHM(0, 45), new int[]{Calendar.TUESDAY, Calendar.THURSDAY, Calendar.FRIDAY}},
//                    //{"dFD-1", rB, TimeTools.timeHM(17, 15), TimeTools.timeHM(0, 45), new int[]{}},
//                    {"dFD-1", rB, TimeTools.timeHM(15, 45), TimeTools.timeHM(0, 45), new int[]{Calendar.SUNDAY}},
//                    {"dSD-1", rB, TimeTools.timeHM(15, 30), TimeTools.timeHM(0, 45), new int[]{Calendar.TUESDAY, Calendar.THURSDAY}},
//                    {"dSD-1", rB, TimeTools.timeHM(16, 30), TimeTools.timeHM(0, 45), new int[]{Calendar.SUNDAY}},
//                    //                    {"sP-1", rA, TimeTools.timeHM(16, 15), TimeTools.timeHM(0, 45), new int[]{Calendar.TUESDAY, Calendar.THURSDAY, Calendar.FRIDAY}},
//                    //                    {"sP-1", rA, TimeTools.timeHM(16, 15), TimeTools.timeHM(0, 45), new int[]{Calendar.TUESDAY, Calendar.THURSDAY, Calendar.FRIDAY}},
//                    //                    {"sP-1", rA, TimeTools.timeHM(16, 15), TimeTools.timeHM(0, 45), new int[]{Calendar.TUESDAY, Calendar.THURSDAY, Calendar.FRIDAY}},
//                    //                    {"sP-1", rA, TimeTools.timeHM(16, 15), TimeTools.timeHM(0, 45), new int[]{Calendar.TUESDAY, Calendar.THURSDAY, Calendar.FRIDAY}},
//                    //                    {"sP-1", rA, TimeTools.timeHM(16, 15), TimeTools.timeHM(0, 45), new int[]{Calendar.TUESDAY, Calendar.THURSDAY, Calendar.FRIDAY}},
//                    //                    {"sP-1", rA, TimeTools.timeHM(16, 15), TimeTools.timeHM(0, 45), new int[]{Calendar.TUESDAY, Calendar.THURSDAY, Calendar.FRIDAY}},
//                    //                    {"sP-1", rA, TimeTools.timeHM(16, 15), TimeTools.timeHM(0, 45), new int[]{Calendar.TUESDAY, Calendar.THURSDAY, Calendar.FRIDAY}},
//                    //                    {"sP-1", rA, TimeTools.timeHM(16, 15), TimeTools.timeHM(0, 45), new int[]{Calendar.TUESDAY, Calendar.THURSDAY, Calendar.FRIDAY}},
//                    //                    {"sP-1", rA, TimeTools.timeHM(16, 15), TimeTools.timeHM(0, 45), new int[]{Calendar.TUESDAY, Calendar.THURSDAY, Calendar.FRIDAY}},
//                    //                    {"sP-1", rA, TimeTools.timeHM(16, 15), TimeTools.timeHM(0, 45), new int[]{Calendar.TUESDAY, Calendar.THURSDAY, Calendar.FRIDAY}},
//                    //                    {"sP-1", rA, TimeTools.timeHM(16, 15), TimeTools.timeHM(0, 45), new int[]{Calendar.TUESDAY, Calendar.THURSDAY, Calendar.FRIDAY}},
//                    //                    {"sP-1", rA, TimeTools.timeHM(16, 15), TimeTools.timeHM(0, 45), new int[]{Calendar.TUESDAY, Calendar.THURSDAY, Calendar.FRIDAY}},
//                    null
//                }) {
//                    if (oo == null) {
//                        continue;
//                    }
//                    Group g = training.groups.get((String) oo[0]);
//                    TimeEventPlanner tep = new TimeEventPlanner(
//                            from,// Long from,
//                            to,// Long to,
//                            ((Room) oo[1]).getId(), // String room,
//                            g.getId(), // String name,
//                            (Long) oo[2], // long start,
//                            (Long) oo[3], // long duration,
//                            null, //Map<String, String> participants,
//                            (int[]) oo[4] // int... weekDays
//                    );
//                    schedule.eventPlanners.put(tep.getId(), tep);
//                }
//            }

            // generate events...
            {
                Calendar cal = TimeTools.getCalendar(null);
                long from = TimeTools.toStartOfMonth(cal);
                long to = TimeTools.toEndOfMonth(cal);
                to = TimeTools.toEndOfYear(cal);

                System.out.println("FROM: " + new Date(from));
                System.out.println("TO  : " + new Date(to));

                int plannedEvents = schedule.generateTimeEvents(from, to, Boolean.TRUE);
                if (plannedEvents > 0) {
                    schedule.generateTimeEvents(from, to, Boolean.FALSE);
                    for (TimeEvent e : schedule.events.values()) {
                        System.out.println(e);
                    }
                    int a = 0;
                }
            }

        }

        return map;
    }

    public static void main(String... args) throws Exception {
        UM_API um = initUM(null);
        UM_API um1 = initUM("_");

        TrainingAPI ta = initTraining(null);
        TrainingAPI ta1 = initTraining("_");

        ScheduleAPI sa = initSchedule(null);
        ScheduleAPI sa1 = initSchedule("_");

        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (Writer wr = new OutputStreamWriter(baos, "UTF-8")) {
                um.exportTo(wr);
            }

            UM_API um2 = new UM_API();
            um2.importFrom(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()), "UTF-8"));

            baos.reset();
            try (Writer wr = new OutputStreamWriter(baos, "UTF-8")) {
                um1.exportTo(wr);
            }
            um2.importFrom(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()), "UTF-8"));

            int a = 0;
        }

        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (Writer wr = new OutputStreamWriter(baos, "UTF-8")) {
                ta.exportTo(wr);
            }

            TrainingAPI ta2 = new TrainingAPI();
            ta2.importFrom(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()), "UTF-8"));

            baos.reset();
            try (Writer wr = new OutputStreamWriter(baos, "UTF-8")) {
                ta1.exportTo(wr);
            }
            ta2.importFrom(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()), "UTF-8"));

            int a = 0;
        }

        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (Writer wr = new OutputStreamWriter(baos, "UTF-8")) {
                sa.exportTo(wr);
            }

            ScheduleAPI sa2 = new ScheduleAPI();
            sa2.importFrom(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()), "UTF-8"));

            baos.reset();
            try (Writer wr = new OutputStreamWriter(baos, "UTF-8")) {
                sa1.exportTo(wr);
            }
            sa2.importFrom(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()), "UTF-8"));

            int a = 0;
        }

        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStream osUM = new FileOutputStream("target/um.json");
            OutputStream osTA = new FileOutputStream("target/ta.json");
            OutputStream osSA = new FileOutputStream("target/sa.json");
            try (
                    Writer wr = new OutputStreamWriter(baos, "UTF-8");
                    Writer wrUM = new OutputStreamWriter(osUM, "UTF-8");
                    Writer wrTA = new OutputStreamWriter(osTA, "UTF-8");
                    Writer wrSA = new OutputStreamWriter(osSA, "UTF-8");) {
                um.exportTo(wr);
                ta.exportTo(wr);
                sa.exportTo(wr);

                um.exportTo(wrUM);
                ta.exportTo(wrTA);
                sa.exportTo(wrSA);
            }

            try (OutputStream os = new FileOutputStream("target/all.json");) {
                os.write(baos.toByteArray());
            }

            Reader rdr = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()), "UTF-8") {
//                @Override
//                public int read() throws IOException {
//                    int training = super.read();
//                    System.out.println("R[]\t" + training + ((training >= ' ') ? "  " + ((char) training) : ""));
//                    return training;
//                }

//                @Override
//                public int read(char[] cbuf, int offset, int length) throws IOException {
//                    int training = super.read(cbuf, offset, length);
//                    if (training > 0) {
//                        System.out.println("R: " + new String(cbuf, offset, training));
//                    }
//                    return training;
//                }
            });

            UM_API um2 = new UM_API();
            um2.importFrom(rdr);

            TrainingAPI ta2 = new TrainingAPI();
            ta2.importFrom(rdr);

            ScheduleAPI sa2 = new ScheduleAPI();
            sa2.importFrom(rdr);

            int a = 0;
        }

        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Map m = initJana(null);
            UM_API jum = (UM_API) m.get("um");
            TrainingAPI jta = (TrainingAPI) m.get("training");
            ScheduleAPI jsa = (ScheduleAPI) m.get("schedule");

            try (Writer wr = new OutputStreamWriter(baos, "UTF-8");) {
                jum.exportTo(wr);
                jta.exportTo(wr);
                jsa.exportTo(wr);
            }

            try (OutputStream os = new FileOutputStream("target/jana.json");) {
                os.write(baos.toByteArray());
            }
        }
    }
}
