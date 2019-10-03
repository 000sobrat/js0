/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssg.lib.jana.api;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import ssg.lib.common.JSON;
import ssg.lib.common.Refl;
import ssg.lib.http.rest.annotations.XMethod;
import ssg.lib.http.rest.annotations.XParameter;
import ssg.lib.http.rest.annotations.XType;
import ssg.lib.jana.tools.TimeTools;

/**
 *
 * @author sesidoro
 */
@XType
public class ScheduleAPI implements AppItem, Exportable {

    static final long serialVersionUID = 1L;

    private String id = "Schedule";

    // events
    List<Room> rooms = new ArrayList<>();
    Map<String, TimeEvent> events = new LinkedHashMap<>();

    // events generation support
    Map<String, TimeEventPlanner> eventPlanners = new LinkedHashMap<>();
    Map<String, long[]> exclusions = new LinkedHashMap<>();

    @Override
    public void exportTo(Writer wr) throws IOException {
        JSON.Encoder jsonEncoder = new JSON.Encoder("UTF-8", new Refl.ReflImpl());
        Map m = new LinkedHashMap();
        m.put("rooms", rooms);
        m.put("events", events);
        m.put("eventPlanners", eventPlanners);
        m.put("exclusions", exclusions);
        //System.out.println("M : "+m.toString());
        jsonEncoder.writeObject(m, wr);
    }

    @Override
    public void importFrom(Reader rdr) throws IOException {
        JSON.Decoder jsonDecoder = new JSON.Decoder("UTF-8");
        Map m = jsonDecoder.readObject(rdr, Map.class);
        Refl refl = new Refl.ReflImpl();
        List lst = (List) m.get("rooms");
        for (Object o : lst) {
            Room rm = refl.enrich(o, Room.class);
            if (!rooms.contains(rm)) {
                rooms.add(rm);
            }
        }
        Map<String, Object> mi = (Map) m.get("events");
        for (Entry<String, Object> e : mi.entrySet()) {
            if (events.containsKey(e.getKey())) {
                continue;
            }
            TimeEvent c = refl.enrich(e.getValue(), TimeEvent.class);
            events.put(e.getKey(), c);
        }
        mi = (Map) m.get("eventPlanners");
        if (mi != null) {
            for (Entry<String, Object> e : mi.entrySet()) {
                if (eventPlanners.containsKey(e.getKey())) {
                    continue;
                }
                TimeEventPlanner c = refl.enrich(e.getValue(), TimeEventPlanner.class);
                eventPlanners.put(e.getKey(), c);
            }
        }
        mi = (Map) m.get("exclusions");
        if (mi != null) {
            for (Entry<String, Object> e : mi.entrySet()) {
                if (exclusions.containsKey(e.getKey())) {
                    continue;
                }
                long[] c = refl.enrich(e.getValue(), long[].class);
                exclusions.put(e.getKey(), c);
            }
        }
    }

    @XMethod(name = "room")
    public Room getRoom(
            @XParameter(name = "name", optional = true) String name,
            @XParameter(name = "address", optional = true) String address) {
        Room r = null;
        for (Room room : rooms) {
            if (name == null || room.name.equals(name)) {
                if (address == null || address.equals(room.address)) {
                    r = room;
                    break;
                }
            }
        }
        return r;
    }

    @XMethod(name = "event")
    public TimeEvent getEvent(
            @XParameter(name = "id", optional = true) String id,
            @XParameter(name = "room", optional = true) String room,
            @XParameter(name = "start", optional = true) Long start,
            @XParameter(name = "name", optional = true) String name
    ) {
        TimeEvent r = (id != null) ? events.get(id) : null;
        if (r == null) {
            for (TimeEvent e : events.values()) {
                if (start == null || start == e.start) {
                    if (room == null || room.equals(e.room)) {
                        if (name == null || name.equals(e.name)) {
                            r = e;
                            break;
                        }
                    }
                }
            }
        }
        return r;
    }

    @XMethod(name = "addEvent")
    public boolean addEvent(
            @XParameter(name = "room") String room,
            @XParameter(name = "name") String name,
            @XParameter(name = "start") long start,
            @XParameter(name = "duration") long duration,
            @XParameter(name = "status", optional = true) String status
    ) {
        List<TimeEvent> es = findEvent(room, start, null, null);
        if (es.isEmpty()) {
            TimeEvent e = new TimeEvent();
            e.setRoom(room);
            e.setDuration(duration);
            e.setStart(start);
            e.setName(name);
            e.setStatus((status != null) ? status : "new");
            events.put(e.getId(), e);
            return true;
        }
        return false;
    }

    @XMethod(name = "setParticipant")
    public boolean setParticipant(
            @XParameter(name = "eventId") String eventId,
            @XParameter(name = "participant") String participant,
            @XParameter(name = "value", optional = true) String value
    ) {
        if (participant != null) {
            TimeEvent e = events.get(eventId);
            if (e != null) {
                if (value == null) {
                    if (e.getParticipants().containsKey(participant));
                    e.getParticipants().remove(participant);
                } else {
                    e.getParticipants().put(participant, value);
                }
                return true;
            }
        }
        return false;
    }

    @XMethod(name = "findRoom")
    public List<Room> findRoom(
            @XParameter(name = "name", optional = true) String name,
            @XParameter(name = "address", optional = true) String address
    ) {
        List<Room> r = new ArrayList<>();
        if (name != null) {
            for (Room room : rooms) {
                if (room.name != null && room.name.contains(name)) {
                    if (address == null || room.address != null && room.address.contains(address)) {
                        r.add(room);
                    }
                }
            }
        } else if (address != null) {
            for (Room room : rooms) {
                if (room.address != null && room.address.contains(address)) {
                    r.add(room);
                }
            }
        } else {
            r.addAll(rooms);
        }
        return r;
    }

    @XMethod(name = "findEvent")
    public List<TimeEvent> findEvent(
            @XParameter(name = "room", optional = true) String room,
            @XParameter(name = "from", optional = true) Long from,
            @XParameter(name = "to", optional = true) Long to,
            @XParameter(name = "status", optional = true) String status,
            @XParameter(name = "names", optional = true) String... names
    ) {
        List<TimeEvent> r = new ArrayList<>();
        for (TimeEvent e : events.values()) {
            if (room != null && !room.equals(e.room)) {
                continue;
            }
            if (from != null && e.start < from) {
                continue;
            }
            if (to != null && (e.start + e.duration) > to) {
                continue;
            }
            if (status != null && !status.equals(e.status)) {
                continue;
            }
            if (names != null && names.length > 0 && !(names.length == 1 && names[0] == null)) {
                boolean found = false;
                for (String name : names) {
                    if (name != null && name.equals(e.name)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    continue;
                }
            }
            r.add(e);
        }
        Collections.sort(r, new Comparator<TimeEvent>() {
            @Override
            public int compare(TimeEvent o1, TimeEvent o2) {
                Long from = o1.start;
                int c = from.compareTo(o2.start);
                if (c == 0) {
                    c = o1.room.compareTo(o2.room);
                }
                return c;
            }
        });
        return r;
    }

    @XMethod(name = "timeRanges")
    public Map getTimeRanges(
            @XParameter(name = "from", optional = true) Long from,
            @XParameter(name = "to", optional = true) Long to
    ) throws IOException {
        Calendar c = TimeTools.getCalendar(null);
        long[] bounds = TimeTools.evalTimeEventBounds(events.values());
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

        List<TimeEvent> tss = findEvent(null, from, to, null);

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

    public Map getTimeRanges(List<TimeEvent> tss) throws IOException {
        Calendar c = TimeTools.getCalendar(null);
        long[] bounds = TimeTools.evalTimeEventBounds(tss);
        c.setTimeInMillis(bounds[0]);
        long from = TimeTools.toStartOfWeek(c);

        c.setTimeInMillis(bounds[1]);
        long to = TimeTools.toEndOfWeek(c);

        Map r = new LinkedHashMap();
        r.put("bounds", bounds);
        r.put("from", from);
        r.put("to", to);
        // [value,min,max]
        List<long[]> days = new ArrayList<>();
        List<long[]> weeks = new ArrayList<>();
        List<long[]> months = new ArrayList<>();
        List<long[]> years = new ArrayList<>();
        long[] defaultRange = new long[3];
        r.put("years", years);
        r.put("months", months);
        r.put("weeks", weeks);
        r.put("days", days);
        r.put("default", defaultRange);

        c.setTimeInMillis(from);
        TimeTools.toStartOfDay(c);
        long val = c.get(Calendar.DAY_OF_YEAR);
        long last = c.getTimeInMillis();
        while (c.getTimeInMillis() < to) {
            c.add(Calendar.DAY_OF_YEAR, 1);
            days.add(new long[]{val, last, c.getTimeInMillis() - 1});
            last = c.getTimeInMillis();
            val = c.get(Calendar.DAY_OF_YEAR);
        }

        long now = System.currentTimeMillis();

        c.setTimeInMillis(from);
        TimeTools.toStartOfWeek(c);
        val = c.get(Calendar.WEEK_OF_YEAR);
        last = c.getTimeInMillis();
        while (c.getTimeInMillis() < to) {
            c.add(Calendar.WEEK_OF_YEAR, 1);
            long[] ww = new long[]{val, last, c.getTimeInMillis() - 1};
            if (now >= ww[1] && now <= ww[2]) {
                System.arraycopy(ww, 0, defaultRange, 0, Math.min(ww.length, defaultRange.length));
            }
            weeks.add(ww);
            last = c.getTimeInMillis();
            val = c.get(Calendar.WEEK_OF_YEAR);
        }

        c.setTimeInMillis(from);
        TimeTools.toStartOfMonth(c);
        val = c.get(Calendar.MONTH) + 1;
        last = c.getTimeInMillis();
        while (c.getTimeInMillis() < to) {
            c.add(Calendar.MONTH, 1);
            months.add(new long[]{val, last, c.getTimeInMillis() - 1});
            last = c.getTimeInMillis();
            val = c.get(Calendar.MONTH) + 1;
        }

        c.setTimeInMillis(from);
        TimeTools.toStartOfYear(c);
        val = c.get(Calendar.YEAR);
        last = c.getTimeInMillis();
        while (c.getTimeInMillis() < to) {
            c.add(Calendar.YEAR, 1);
            years.add(new long[]{val, last, c.getTimeInMillis() - 1});
            last = c.getTimeInMillis();
            val = c.get(Calendar.YEAR);
        }

        return r;
    }

    @XMethod(name = "generateTimeEvents")
    public int generateTimeEvents(
            @XParameter(name = "from", optional = true) Long from,
            @XParameter(name = "to", optional = true) Long to,
            @XParameter(name = "checkOnly", optional = true) Boolean checkOnly
    ) throws IOException {
        Calendar c = TimeTools.getCalendar(null);
        long[] bounds = TimeTools.evalTimeEventBounds(events.values());
        if (checkOnly == null) {
            checkOnly = true;
        }
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

        List<TimeEvent> tes = new ArrayList<>();
        for (TimeEventPlanner tep : eventPlanners.values()) {
            List<TimeEvent> teps = tep.generateEvents(from, to);
            if (teps != null) {
                tes.addAll(teps);
            }
        }

        // remove conflicting events
        Iterator<TimeEvent> it = tes.iterator();
        while (it.hasNext()) {
            TimeEvent te = it.next();
            if (te == null || events.containsKey(te.getId())) {
                it.remove();
                continue;
            }
            for (TimeEvent t : events.values()) {
                if (t.overlaps(te)) {
                    if (t.room != null && t.room.equals(te.room)) {
                        it.remove();
                        break;
                    }
                }
            }
        }

        if (!tes.isEmpty() && !checkOnly) {
            for (TimeEvent te : tes) {
                events.put(te.getId(), te);
            }
        }

        return tes.size();
    }
    
    
    @XMethod(name = "addEvent")
    public TimeEventPlanner addEventPlanner(
            @XParameter(name = "room") String room,
            @XParameter(name = "name") String name,
            @XParameter(name = "from") long from,
            @XParameter(name = "to") long to,
            @XParameter(name = "start") long start,
            @XParameter(name = "duration") long duration,
            @XParameter(name = "weekDays", optional = true) int[] weekDays
    ) {
        String r=null;
        
        List<TimeEventPlanner> es = findEventPlanners(room, from, to, name);
        if(!es.isEmpty()) {
            Iterator<TimeEventPlanner> it=es.iterator();
            while(it.hasNext()) {
                TimeEventPlanner te=it.next();
                if(te.start!=start) it.remove();
            }
        }
        if (es.isEmpty()) {
            // add
            TimeEventPlanner e = new TimeEventPlanner();
            e.setRoom(room);
            e.setFrom(from);
            e.setTo(to);
            e.setDuration(duration);
            e.setStart(start);
            e.setName(name);
            e.weekDays=weekDays;
            eventPlanners.put(e.getId(), e);
            return e;
        } else if(es.size()==1) {
            // modify: not...
            TimeEventPlanner te=es.get(0);
        }else{
            // inconsistency: more than 1 item start same time at same place...
        }
        return null;
    }
    
    public List<TimeEventPlanner> findEventPlanners(
            @XParameter(name = "room", optional = true) String room,
            @XParameter(name = "from", optional = true) Long from,
            @XParameter(name = "to", optional = true) Long to,
            @XParameter(name = "names", optional = true) String... names
    ) {
        List<ScheduleAPI.TimeEventPlanner> r = new ArrayList<>();

        if ("".equals(room)) {
            room = null;
        }

        for (ScheduleAPI.TimeEventPlanner e : eventPlanners.values()) {
            if (room != null && !room.equals(e.room)) {
                continue;
            }
            if (from != null && e.from < from) {
                continue;
            }
            if (to != null && (e.to) > to) {
                continue;
            }
            if (room != null && !room.equals(e.getRoom())) {
                continue;
            }
            if (names != null && names.length > 0 && !(names.length == 1 && names[0] == null)) {
                boolean found = false;
                for (String name : names) {
                    if (name != null && name.equals(e.name)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    continue;
                }
            }
            r.add(e);
        }
        Collections.sort(r, new Comparator<ScheduleAPI.TimeEventPlanner>() {
            @Override
            public int compare(ScheduleAPI.TimeEventPlanner o1, ScheduleAPI.TimeEventPlanner o2) {
                Long from = o1.start;
                int c = from.compareTo(o2.start);
                if (c == 0) {
                    c = o1.room.compareTo(o2.room);
                }
                return c;
            }
        });
        return r;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    public static class Room implements AppItem {

        static final long serialVersionUID = 1L;

        private String name;
        private String address;
        private int maxSize;

        @Override
        public String getId() {
            return getName();
        }

        @Override
        public void setId(String id) {
            name = id;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @param name the name to set
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @return the address
         */
        public String getAddress() {
            return address;
        }

        /**
         * @param address the address to set
         */
        public void setAddress(String address) {
            this.address = address;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 71 * hash + Objects.hashCode(this.name);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Room other = (Room) obj;
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Room{" + "name=" + name + ", address=" + address + '}';
        }

        /**
         * @return the maxSize
         */
        public int getMaxSize() {
            return maxSize;
        }

        /**
         * @param maxSize the maxSize to set
         */
        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

    }

    public static class TimeEvent implements AppItem {

        static final long serialVersionUID = 1L;

        private String id = UUID.randomUUID().toString();
        private String room;
        private String name;
        private long start;
        private long duration;
        private String status;
        private Map<String, String> participants = new LinkedHashMap<>();

        public TimeEvent() {
        }

        public TimeEvent(
                String room,
                String name,
                long start,
                long duration,
                String status
        ) {
            this.room = room;
            this.name = name;
            this.start = start;
            this.duration = duration;
            this.status = status;
        }

        /**
         * @return the room
         */
        public String getRoom() {
            return room;
        }

        /**
         * @param room the room to set
         */
        public void setRoom(String room) {
            this.room = room;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @param name the name to set
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @return the start
         */
        public long getStart() {
            return start;
        }

        /**
         * @param from the start to set
         */
        public void setStart(long from) {
            this.start = from;
        }

        /**
         * @return the end (start+duration)
         */
        public long getEnd() {
            return start + duration;
        }

        /**
         * @return the duration
         */
        public long getDuration() {
            return duration;
        }

        /**
         * @param duration the duration to set
         */
        public void setDuration(long duration) {
            this.duration = duration;
        }

        /**
         * @return the participants
         */
        public Map<String, String> getParticipants() {
            return participants;
        }

        /**
         * @param participants the participants to set
         */
        public void setParticipants(Map<String, String> participants) {
            this.participants = participants;
        }

        /**
         * @return the getId
         */
        public String getId() {
            return id;
        }

        /**
         * @param id the getId to set
         */
        public void setId(String id) {
            this.id = id;
        }

        /**
         * @return the status
         */
        public String getStatus() {
            return status;
        }

        /**
         * @param status the status to set
         */
        public void setStatus(String status) {
            this.status = status;
        }

        public boolean overlaps(TimeEvent other) {
            if (other == null) {
                return false;
            }
            if (other.start > getEnd() || other.getEnd() < start) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getClass().getSimpleName() + "{");
            sb.append("id=" + id + ", room=" + room + ", name=" + name + ", start=" + TimeTools.dumpDateTime(start) + " (" + start + "), duration=" + duration + ", status=" + status);
            if (participants != null && !participants.isEmpty()) {
                sb.append(", participants=" + participants.size());
                for (Entry<String, String> p : participants.entrySet()) {
                    sb.append("\n  " + p.getKey() + ": " + p.getValue());
                }
                sb.append("\n");
            }
            sb.append('}');
            return sb.toString();
        }
    }

    public static class TimeEventPlanner implements AppItem {

        private String id = UUID.randomUUID().toString();

        private Long from;
        private Long to;
        private String room;
        private String name;
        private long start;
        private long duration;
        private Map<String, String> participants;
        private int[] weekDays;

        public TimeEventPlanner() {
        }

        public TimeEventPlanner(
                Long from,
                Long to,
                String room,
                String name,
                long start,
                long duration,
                Map<String, String> participants,
                int... weekDays
        ) {
            this.from = from;
            this.to = to;
            this.room = room;
            this.name = name;
            this.start = start;
            this.duration = duration;
            this.participants = participants;
            this.weekDays = weekDays;
            if (this.weekDays != null) {
                Arrays.sort(this.weekDays);
            } else {
//                this.weekDays = new int[]{
//                    Calendar.MONDAY,
//                    Calendar.TUESDAY,
//                    Calendar.WEDNESDAY,
//                    Calendar.THURSDAY,
//                    Calendar.FRIDAY,
//                    Calendar.SATURDAY,
//                    Calendar.SUNDAY
//                };
            }
        }

        public List<TimeEvent> generateEvents(Long from, Long to) {
            List<TimeEvent> r = new ArrayList<TimeEvent>();

            Calendar c = TimeTools.getCalendar(null);
            if (from == null) {
                from = this.getFrom();
                if (from == null) {
                    from = System.currentTimeMillis();
                    c.setTimeInMillis(from);
                    TimeTools.toStartOfDay(c);
                }
            } else if (this.getFrom() != null && this.getFrom() > from) {
                from = this.getFrom();
            }

            if (to == null) {
                to = this.getTo();
                if (to == null) {
                    to = System.currentTimeMillis();
                    c.setTimeInMillis(to);
                    TimeTools.toEndOfMonth(c);
                }
            } else if (this.getTo() != null && this.getTo() < to) {
                to = this.getTo();
            }

            c.setTimeInMillis(from);
            while (c.getTimeInMillis() < to) {
                if (getWeekDays() != null && getWeekDays().length > 0) {
                    int wd = c.get(Calendar.DAY_OF_WEEK);
                    if (Arrays.binarySearch(getWeekDays(), wd) < 0) {
                        // next day...
                        TimeTools.toEndOfDay(c);
                        c.add(Calendar.MILLISECOND, 1);
                        continue;
                    }
                }
                TimeTools.setTimeHM(c, getStart());
                TimeEvent te = new TimeEvent(
                        getRoom(), getName(),
                        c.getTimeInMillis(), getDuration(),
                        null
                );
                if (participants != null) {
                    te.getParticipants().putAll(participants);
                }
                r.add(te);
                // next day...
                TimeTools.toEndOfDay(c);
                c.add(Calendar.MILLISECOND, 1);
            }

            return r;
        }

        /**
         * @return the id
         */
        public String getId() {
            return id;
        }

        /**
         * @param id the id to set
         */
        public void setId(String id) {
            this.id = id;
        }

        /**
         * @return the from
         */
        public Long getFrom() {
            return from;
        }

        /**
         * @param from the from to set
         */
        public void setFrom(Long from) {
            this.from = from;
        }

        /**
         * @return the to
         */
        public Long getTo() {
            return to;
        }

        /**
         * @param to the to to set
         */
        public void setTo(Long to) {
            this.to = to;
        }

        /**
         * @return the room
         */
        public String getRoom() {
            return room;
        }

        /**
         * @param room the room to set
         */
        public void setRoom(String room) {
            this.room = room;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @param name the name to set
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @return the start
         */
        public long getStart() {
            return start;
        }

        /**
         * @param start the start to set
         */
        public void setStart(long start) {
            this.start = start;
        }

        /**
         * @return the duration
         */
        public long getDuration() {
            return duration;
        }

        /**
         * @param duration the duration to set
         */
        public void setDuration(long duration) {
            this.duration = duration;
        }

        /**
         * @return the participants
         */
        public Map<String, String> getParticipants() {
            return participants;
        }

        /**
         * @param participants the participants to set
         */
        public void setParticipants(Map<String, String> participants) {
            this.participants = participants;
        }

        /**
         * @return the weekDays
         */
        public int[] getWeekDays() {
            return weekDays;
        }

        /**
         * @param weekDays the weekDays to set
         */
        public void setWeekDays(int[] weekDays) {
            this.weekDays = weekDays;
        }
    }
}
