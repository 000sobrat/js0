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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import ssg.lib.common.JSON;
import ssg.lib.common.Refl;
import ssg.lib.common.Refl.ReflImpl;
import ssg.lib.http.rest.annotations.XMethod;
import ssg.lib.http.rest.annotations.XParameter;
import ssg.lib.http.rest.annotations.XType;

/**
 *
 * @author sesidoro
 */
@XType
public class TrainingAPI implements AppItem, Exportable {

    public static enum PSTATE {
        trusted,
        verifying,
        forbidden
    }

    static final long serialVersionUID = 1L;

    private String id = "Training";
    Map<String, Trainer> trainers = new LinkedHashMap<>();
    Map<String, Pricing> prices = new LinkedHashMap<>();
    Map<String, Category> categories = new LinkedHashMap<>();
    Map<String, Course> courses = new LinkedHashMap<>();
    Map<String, Group> groups = new LinkedHashMap<>();
    Map<Course, List<Trainer>> courseTrainers = new LinkedHashMap<>();
    Map<String, PSTATE> courseParticipants = new LinkedHashMap<>();
    Map<String, IconInfo> icons = new LinkedHashMap<>();

    @Override
    public void exportTo(Writer wr) throws IOException {
        JSON.Encoder jsonEncoder = new JSON.Encoder("UTF-8", new ReflImpl());
        Map m = new LinkedHashMap();
        m.put("trainers", trainers);
        m.put("prices", prices);
        m.put("icons", icons);
        m.put("categories", categories);
        m.put("courses", courses);
        m.put("groups", groups);
        Map cts = new LinkedHashMap();
        for (Entry<Course, List<Trainer>> e : courseTrainers.entrySet()) {
            List<Trainer> ts = e.getValue();
            if (ts != null && !ts.isEmpty()) {
                List tss = new ArrayList();
                for (Trainer tr : ts) {
                    tss.add(tr.getId());
                }
                cts.put(e.getKey().getId(), tss);
            }
        }
        m.put("courseTrainers", cts);
        m.put("courseParticipants", courseParticipants);
        //System.out.println("M : "+m.toString());
        jsonEncoder.writeObject(m, wr);
    }

    @Override
    public void importFrom(Reader rdr) throws IOException {
        JSON.Decoder jsonDecoder = new JSON.Decoder("UTF-8");
        Map m = jsonDecoder.readObject(rdr, Map.class);
        Refl refl = new ReflImpl();
        Map<String, Map> mi = (Map) m.get("trainers");
        for (Entry<String, Map> e : mi.entrySet()) {
            if (trainers.containsKey(e.getKey())) {
                continue;
            }
            Trainer tr = refl.enrich(e.getValue(), Trainer.class);
            trainers.put(e.getKey(), tr);
        }
        mi = (Map) m.get("prices");
        for (Entry<String, Map> e : mi.entrySet()) {
            if (prices.containsKey(e.getKey())) {
                continue;
            }
            Pricing p = refl.enrich(e.getValue(), Pricing.class);
            prices.put(e.getKey(), p);
        }

        mi = (Map) m.get("icons");
        for (Entry<String, Map> e : mi.entrySet()) {
            if (icons.containsKey(e.getKey())) {
                continue;
            }
            IconInfo ii = refl.enrich(e.getValue(), IconInfo.class);
            icons.put(e.getKey(), ii);
        }

        mi = (Map) m.get("categories");
        for (Entry<String, Map> e : mi.entrySet()) {
            if (categories.containsKey(e.getKey())) {
                continue;
            }
            Category c = refl.enrich(e.getValue(), Category.class);
            categories.put(e.getKey(), c);
        }
        mi = (Map) m.get("courses");
        for (Entry<String, Map> e : mi.entrySet()) {
            if (courses.containsKey(e.getKey())) {
                continue;
            }
            Course c = refl.enrich(e.getValue(), Course.class);
            courses.put(e.getKey(), c);
        }
        mi = (Map) m.get("groups");
        for (Entry<String, Map> e : mi.entrySet()) {
            if (groups.containsKey(e.getKey())) {
                continue;
            }
            Group g = refl.enrich(e.getValue(), Group.class);
            groups.put(e.getKey(), g);
        }
        Map<String, List<String>> ml = (Map) m.get("courseTrainers");
        for (Entry<String, List<String>> e : ml.entrySet()) {
            Course c = courses.get(e.getKey());
            List<String> vss = e.getValue();
            List<Trainer> trs = courseTrainers.get(c);
            if (trs == null) {
                trs = new ArrayList<>();
                courseTrainers.put(c, trs);
            }
            for (String v : vss) {
                Trainer tr = trainers.get(v);
                if (tr != null) {
                    trs.add(tr);
                }
            }
        }
        Map<String, String> mp = (Map) m.get("courseParticipants");
        for (Entry<String, String> e : mp.entrySet()) {
            String p = e.getKey();
            String s = e.getValue();
            try {
                //PSTATE old=courseParticipants.get(p);
                PSTATE pst = PSTATE.valueOf(s);
                courseParticipants.put(p, pst);
            } catch (Throwable th) {
            }
        }
    }

    @XMethod(name = "addTrainer")
    public boolean addTrainer(
            @XParameter(name = "email") String email,
            @XParameter(name = "name", optional = true) String name,
            @XParameter(name = "courseNames", optional = true) String... courseNames
    ) {
        boolean changed = false;
        Trainer tr = null;
        boolean dropped = false;
        if (tr == null && email != null) {
            tr = trainers.get(email);
        }
        if (name != null) {
            if (tr != null && !name.equals(tr.name)) {
                tr = null;
                dropped = true;
            } else {
                for (Trainer tri : trainers.values()) {
                    if (tri.name != null && tri.getName().equals(name)) {
                        tr = tri;
                        break;
                    }
                }
            }
        }
        if (tr == null && !dropped && email != null) {
            tr = new Trainer();
            tr.setEmail(email);
            tr.setName(name);
            changed = true;
        }

        if (tr != null && courseNames != null && courseNames.length > 0) {
            for (String cn : courseNames) {
                Course c = courses.get(cn);
                List<Trainer> cts = courseTrainers.get(c);
                if (cts == null) {
                    cts = new ArrayList<>();
                    courseTrainers.put(c, cts);
                }
                if (!cts.contains(tr)) {
                    cts.add(tr);
                    changed = true;
                }
            }
        }
        return changed;
    }

    @XMethod(name = "addTrainer")
    public Trainer getTrainer(
            @XParameter(name = "email", optional = true) String email,
            @XParameter(name = "name", optional = true) String name
    ) {
        Trainer tr = null;
        if (tr == null && email != null) {
            tr = trainers.get(email);
        }
        if (name != null) {
            if (tr != null && !name.equals(tr.name)) {
                tr = null;
            } else {
                for (Trainer tri : trainers.values()) {
                    if (tri.name != null && tri.getName().equals(name)) {
                        tr = tri;
                        break;
                    }
                }
            }
        }
        return tr;
    }

    @XMethod(name = "findTrainers")
    public List<Trainer> findTrainers(
            @XParameter(name = "email", optional = true) String email,
            @XParameter(name = "name", optional = true) String name,
            @XParameter(name = "courses", optional = true) String... courses
    ) {
        List<Trainer> r = new ArrayList<>();
        for (Trainer tr : trainers.values()) {
            if (name == null || name.equals(tr.name)) {
                if (email == null || email.equals(tr.email)) {
                    if (courses == null || courses.length == 0 || courses.length == 1 && courses[0] == null) {
                        r.add(tr);
                    } else {
                        for (Entry<Course, List<Trainer>> cts : courseTrainers.entrySet()) {
                            if (cts.getValue() != null && cts.getValue().contains(tr)) {
                                r.add(tr);
                                break;
                            }
                        }
                    }
                }
            }
        }
        return r;
    }

    @XMethod(name = "findCourses")
    public List<Course> findCourses(
            @XParameter(name = "name", optional = true) String name,
            @XParameter(name = "trainers", optional = true) String... trainerEmails
    ) {
        List<Course> r = new ArrayList<>();
        if (name != null) {
            Course c = courses.get(name);
            if (c != null) {
                if (trainerEmails != null && trainerEmails.length > 0) {
                    List<Trainer> trs = courseTrainers.get(c);
                    if (trs != null && !trs.isEmpty()) {
                        for (String email : trainerEmails) {
                            Trainer tr = getTrainer(email, null);
                            if (tr != null && trs.contains(tr)) {
                                r.add(c);
                                break;
                            }
                        }
                    }
                } else {
                    r.add(c);
                }
            }
        } else if (trainerEmails != null && trainerEmails.length > 0) {
            for (Course c : courses.values()) {
                if (trainerEmails != null && trainerEmails.length > 0) {
                    List<Trainer> trs = courseTrainers.get(c);
                    if (trs != null && !trs.isEmpty()) {
                        for (String email : trainerEmails) {
                            Trainer tr = getTrainer(email, null);
                            if (tr != null && trs.contains(tr)) {
                                r.add(c);
                                break;
                            }
                        }
                    }
                } else {
                    r.add(c);
                }
            }
        } else {
            r.addAll(courses.values());
        }
        return r;
    }

    @XMethod(name = "findGroups")
    public List<Group> findGroups(
            @XParameter(name = "name", optional = true) String name,
            @XParameter(name = "trainer", optional = true) String trainerEmail,
            @XParameter(name = "courses", optional = true) String... courses
    ) {
        List<Group> r = new ArrayList<>();
        for (Group gr : groups.values()) {
            if (name == null || name.equals(gr.name)) {
                if (courses != null && courses.length > 0) {
                    boolean found = false;
                    for (String cn : courses) {
                        if (cn != null && cn.equals(gr.course)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        continue;
                    }
                }
                if (trainerEmail == null || trainerEmail.equals(gr.trainer)) {
                    r.add(gr);
                }
            }
        }
        return r;
    }

    public IconInfo findIcon(Category cat, Course crs, Group g) {
        String icon = null;
        if (g != null) {
            icon = g.getIcon();
        }
        if (icon == null) {
            if (crs != null || g != null && g.getCourse() != null && courses.containsKey(g.getCourse())) {
                if (crs == null) {
                    crs = courses.get(g.getCourse());
                }
                icon = crs.getIcon();
            }
        }
        if (icon == null) {
            if (cat != null || crs != null && crs.getCategory() != null && categories.containsKey(crs.getCategory())) {
                if (cat == null) {
                    cat = categories.get(crs.getCategory());
                }
                icon = cat.getIcon();
            }
        }

        return (icon != null) ? icons.get(icon) : null;
    }

    public static class Trainer implements AppItem {

        static final long serialVersionUID = 1L;

        private String email;
        private String name;

        @Override
        public String getId() {
            return getEmail();
        }

        @Override
        public void setId(String id) {
            setEmail(id);
        }

        /**
         * @return the email
         */
        public String getEmail() {
            return email;
        }

        /**
         * @param email the email to set
         */
        public void setEmail(String email) {
            this.email = email;
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
    }

    public static class Pricing implements AppItem {

        private String id = UUID.randomUUID().toString();
        private String category;
        private String course;
        private String room;

        private PriceItem first;
        private PriceItem single;
        private List<PriceItem> abonements = new ArrayList<>();

        public Pricing() {
        }

        public Pricing(
                String category,
                String course,
                String room,
                PriceItem first,
                PriceItem single,
                PriceItem... abonements
        ) {
            this.category = category;
            this.course = course;
            this.room = room;

            this.first = first;
            this.single = single;
            if (abonements != null) {
                for (PriceItem pi : abonements) {
                    if (pi != null) {
                        this.abonements.add(pi);
                    }
                }
            }
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
         * @return the category
         */
        public String getCategory() {
            return category;
        }

        /**
         * @param category the category to set
         */
        public void setCategory(String category) {
            this.category = category;
        }

        /**
         * @return the course
         */
        public String getCourse() {
            return course;
        }

        /**
         * @param course the course to set
         */
        public void setCourse(String course) {
            this.course = course;
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
         * @return the first
         */
        public PriceItem getFirst() {
            return first;
        }

        /**
         * @param first the first to set
         */
        public void setFirst(PriceItem first) {
            this.first = first;
        }

        /**
         * @return the single
         */
        public PriceItem getSingle() {
            return single;
        }

        /**
         * @param single the single to set
         */
        public void setSingle(PriceItem single) {
            this.single = single;
        }

        /**
         * @return the abonements
         */
        public List<PriceItem> getAbonements() {
            return abonements;
        }

        /**
         * @param abonements the abonements to set
         */
        public void setAbonements(List<PriceItem> abonements) {
            this.abonements = abonements;
        }

        public static class PriceItem {

            private Integer count;
            private Float price;
            private Long timeLimit;
            private String notes;

            public PriceItem() {
            }

            public PriceItem(
                    Integer count,
                    Float price,
                    Long timeLimit
            ) {
                this.count = count;
                this.price = price;
                this.timeLimit = timeLimit;
            }

            public PriceItem(
                    Integer count,
                    Float price,
                    Long timeLimit,
                    String notes
            ) {
                this.count = count;
                this.price = price;
                this.timeLimit = timeLimit;
                this.notes = notes;
            }

            /**
             * @return the count
             */
            public Integer getCount() {
                return count;
            }

            /**
             * @param count the count to set
             */
            public void setCount(Integer count) {
                this.count = count;
            }

            /**
             * @return the price
             */
            public Float getPrice() {
                return price;
            }

            /**
             * @param price the price to set
             */
            public void setPrice(Float price) {
                this.price = price;
            }

            /**
             * @return the timeLimit
             */
            public Long getTimeLimit() {
                return timeLimit;
            }

            /**
             * @param timeLimit the timeLimit to set
             */
            public void setTimeLimit(Long timeLimit) {
                this.timeLimit = timeLimit;
            }

            /**
             * @return the notes
             */
            public String getNotes() {
                return notes;
            }

            /**
             * @param notes the notes to set
             */
            public void setNotes(String notes) {
                this.notes = notes;
            }
        }
    }

    public static class Category implements AppItem {

        private String name;
        private String pricing;
        private String icon;

        public Category() {
        }

        public Category(String name, String pricing) {
            this.name = name;
            this.pricing = pricing;
        }

        public Category(String name, String pricing, String icon) {
            this.name = name;
            this.pricing = pricing;
            this.icon = icon;
        }

        @Override
        public String getId() {
            return getName();
        }

        @Override
        public void setId(String id) {
            this.setName(id);
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
         * @return the pricing
         */
        public String getPricing() {
            return pricing;
        }

        /**
         * @param pricing the pricing to set
         */
        public void setPricing(String pricing) {
            this.pricing = pricing;
        }

        /**
         * @return the icon
         */
        public String getIcon() {
            return icon;
        }

        /**
         * @param icon the icon to set
         */
        public void setIcon(String icon) {
            this.icon = icon;
        }
    }

    public static class Course implements AppItem {

        static final long serialVersionUID = 1L;

        private String category;
        private String name;
        private String description;
        private String pricing;
        private String icon;

        public Course() {
        }

        public Course(
                String category,
                String name,
                String description,
                String pricing
        ) {
            this.category = category;
            this.name = name;
            this.description = description;
            this.pricing = pricing;
        }

        public Course(
                String category,
                String name,
                String description,
                String pricing,
                String icon
        ) {
            this.category = category;
            this.name = name;
            this.description = description;
            this.pricing = pricing;
            this.icon = icon;
        }

        @Override
        public String getId() {
            return getName();
        }

        @Override
        public void setId(String id) {
            setName(id);
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
         * @return the description
         */
        public String getDescription() {
            return description;
        }

        /**
         * @param description the description to set
         */
        public void setDescription(String description) {
            this.description = description;
        }

        /**
         * @return the pricing
         */
        public String getPricing() {
            return pricing;
        }

        /**
         * @param pricing the pricing to set
         */
        public void setPricing(String pricing) {
            this.pricing = pricing;
        }

        /**
         * @return the category
         */
        public String getCategory() {
            return category;
        }

        /**
         * @param category the category to set
         */
        public void setCategory(String category) {
            this.category = category;
        }

        /**
         * @return the icon
         */
        public String getIcon() {
            return icon;
        }

        /**
         * @param icon the icon to set
         */
        public void setIcon(String icon) {
            this.icon = icon;
        }
    }

    public static class Group implements AppItem {

        static final long serialVersionUID = 1L;

        private String name;
        private String shortName;
        private String course;
        private String trainer;
        private int maxSize = 10;
        private String pricing;
        private String icon;

        public Group() {
        }

        public Group(
                String name,
                String shortName,
                String course,
                String trainer,
                Integer maxSize,
                String pricing
        ) {
            this.name = name;
            this.shortName = shortName;
            this.course = course;
            this.trainer = trainer;
            if (maxSize != null && maxSize > 0) {
                this.maxSize = maxSize;
            }
            this.pricing = pricing;
        }

        public Group(
                String name,
                String shortName,
                String course,
                String trainer,
                Integer maxSize,
                String pricing,
                String icon
        ) {
            this.name = name;
            this.shortName = shortName;
            this.course = course;
            this.trainer = trainer;
            if (maxSize != null && maxSize > 0) {
                this.maxSize = maxSize;
            }
            this.pricing = pricing;
            this.icon = icon;
        }

        @Override
        public String getId() {
            return getName();
        }

        @Override
        public void setId(String id) {
            setName(id);
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
         * @return the course
         */
        public String getCourse() {
            return course;
        }

        /**
         * @param course the course to set
         */
        public void setCourse(String course) {
            this.course = course;
        }

        /**
         * @return the trainer
         */
        public String getTrainer() {
            return trainer;
        }

        /**
         * @param trainer the trainer to set
         */
        public void setTrainer(String trainer) {
            this.trainer = trainer;
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

        /**
         * @return the pricing
         */
        public String getPricing() {
            return pricing;
        }

        /**
         * @param pricing the pricing to set
         */
        public void setPricing(String pricing) {
            this.pricing = pricing;
        }

        /**
         * @return the shortName
         */
        public String getShortName() {
            return shortName;
        }

        /**
         * @param shortName the shortName to set
         */
        public void setShortName(String shortName) {
            this.shortName = shortName;
        }

        /**
         * @return the icon
         */
        public String getIcon() {
            return icon;
        }

        /**
         * @param icon the icon to set
         */
        public void setIcon(String icon) {
            this.icon = icon;
        }
    }

    public static class IconInfo implements AppItem {

        String id;
        private Map<String, String> icons = new LinkedHashMap<>();

        public IconInfo() {
        }

        public IconInfo(String id) {
            this.id = id;
        }

        public IconInfo(String id, String icon) {
            this.id = id;
            addIcon(0, icon);
        }

        public IconInfo addIcon(int size, String icon) {
            if (size < 0) {
                size = 0;
            }
            if (icon != null) {
                this.icons.put("" + size, icon);
            } else if (this.icons.containsKey(size)) {
                this.icons.remove(size);
            }
            return this;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public void setId(String id) {
            this.id = id;
        }

        /**
         * @return the icons
         */
        public Map<String, String> getIcons() {
            return icons;
        }

        /**
         * @param icons the icons to set
         */
        public void setIcons(Map<String, String> icons) {
            this.icons = icons;
        }

        public String getIcon(Integer size) {
            String v = (size != null) ? icons.get("" + size) : null;
            if (v == null) {
                v = icons.get("0");
            }
            return v;
        }
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
}
