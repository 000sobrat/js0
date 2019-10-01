/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssg.lib.jana.tools;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import ssg.lib.jana.api.ApplicationAPI;
import ssg.lib.jana.api.ScheduleAPI.TimeEvent;

/**
 *
 * @author sesidoro
 */
public class TimeTools {

    public static final long ONE_SEC = 1000;
    public static final long ONE_MIN = ONE_SEC * 60;
    public static final long ONE_HOUR = ONE_MIN * 60;
    public static final long ONE_DAY = ONE_HOUR * 24;

    public static Locale defaultLocale = Locale.forLanguageTag("ru");
    public static TimeZone defaultTimeZone = TimeZone.getTimeZone("EET");
    public static Integer defaultFirstWeekDay = Calendar.MONDAY;

    public static enum SEASON {
        spring, summer, autumn, winter;

        public int[] months() {
            switch (this) {
                case spring:
                    return new int[]{2, 3, 4};
                case summer:
                    return new int[]{5, 6, 7};
                case autumn:
                    return new int[]{8, 9, 10};
                case winter:
                default:
                    return new int[]{11, 0, 1};
            }
        }

        public static SEASON seasonOf(Calendar c) {
            if (c == null) {
                return null;
            }
            switch (c.get(Calendar.MONTH)) {
                case 0:
                case 1:
                case 11:
                    return winter;
                case 2:
                case 3:
                case 4:
                    return spring;
                case 5:
                case 6:
                case 7:
                    return summer;
                default:
                    return autumn;
            }
        }

        public int order() {
            return order(this);
        }

        public static int order(SEASON s) {
            if (s == null) {
                return -1;
            }
            switch (s) {
                case spring:
                    return 0;
                case summer:
                    return 1;
                case autumn:
                    return 2;
                case winter:
                    return 3;
                default:
                    return -1;
            }
        }
    }

    public static Calendar getCalendar(Long time) {
        Calendar c = (defaultTimeZone != null && defaultLocale != null) ? Calendar.getInstance(defaultTimeZone, defaultLocale) : Calendar.getInstance();
        if (defaultFirstWeekDay != null) {
            c.setFirstDayOfWeek(defaultFirstWeekDay);
        }
        //c.setTimeZone(TimeZone.getTimeZone("UTC"));
        if (time != null) {
            c.setTimeInMillis(time);
        }
        return c;
    }

    public static long getTimeM(Calendar c) {
        return 0L + c.get(Calendar.MINUTE) * ONE_MIN;
    }

    public static long getTimeHM(Calendar c) {
        return 0L + c.get(Calendar.HOUR_OF_DAY) * ONE_HOUR + c.get(Calendar.MINUTE) * ONE_MIN;
    }

    public static long getTimeHMS(Calendar c) {
        return 0L + c.get(Calendar.HOUR_OF_DAY) * ONE_HOUR + c.get(Calendar.MINUTE) * ONE_MIN + c.get(Calendar.SECOND) * ONE_SEC;
    }

    public static long timeHM(int hour, int min) {
        return timeHMS(hour, min, 0);
    }

    public static long timeHMS(int hour, int min, int sec) {
        return 0L + hour * ONE_HOUR + min * ONE_MIN + sec * ONE_SEC;
    }

    public static Calendar setTimeHM(Calendar c, long timeHM) {
        c.set(Calendar.HOUR_OF_DAY, (int) ((timeHM % ONE_DAY) / ONE_HOUR));
        c.set(Calendar.MINUTE, (int) ((timeHM % ONE_HOUR) / ONE_MIN));
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    public static Calendar addTimeHM(Calendar c, long timeHM) {
        int sign = timeHM < 0 ? -1 : 1;
        timeHM = Math.abs(timeHM);
        c.add(Calendar.HOUR_OF_DAY, sign * (int) ((timeHM % ONE_DAY) / ONE_HOUR));
        c.add(Calendar.MINUTE, sign * (int) ((timeHM % ONE_HOUR) / ONE_MIN));
        return c;
    }

    public static Calendar setTimeHMS(Calendar c, long timeHMS) {
        setTimeHM(c, timeHMS);
        c.set(Calendar.SECOND, (int) ((timeHMS % ONE_MIN) / ONE_SEC));
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    public static Calendar addTimeHMS(Calendar c, long timeHMS) {
        setTimeHM(c, timeHMS);
        int sign = timeHMS < 0 ? -1 : 1;
        timeHMS = Math.abs(timeHMS);
        c.add(Calendar.SECOND, sign * (int) ((timeHMS % ONE_MIN) / ONE_SEC));
        return c;
    }

    /**
     *
     * @param from
     * @param to
     * @param cts
     * @return modified to
     */
    public static Calendar copyFrom(Calendar from, Calendar to, int... cts) {
        if (to == null) {
            to = getCalendar(0L);
        }
        for (int ct : cts) {
            to.set(ct, from.get(ct));
        }
        return to;
    }

    /**
     * Clears specified values to 0 or 1 for month day or 1st day of week for
     * week day.
     *
     * @param c
     * @param cts
     */
    public static Calendar resetCalendar(Calendar c, int... cts) {
        for (int ct : cts) {
            if (ct == Calendar.DAY_OF_MONTH) {
                c.set(ct, 1);
            } else if (ct == Calendar.DAY_OF_WEEK) {
                c.set(ct, c.getFirstDayOfWeek());
            } else {
                c.set(ct, 0);
            }
        }
        return c;
    }

    /**
     * Returns array of long[] {minDay,maxDay,minTime,maxTime,timeInterval}
     *
     * Where min/max day represents 0-time days range, min/max time represent
     * time range over all days, and timeInterval - minimal needed duration
     * needed to represent all time slots (e.g. if all timeslots boundaries are
     * at sharp hours - 1h, if there's half hour : 30min, if quarter - 15 min,
     * if similar to 5min - 5 min
     *
     * @param tss
     * @return
     */
    public static long[] evalBounds(List<ApplicationAPI.Timeslot> tss) {
        Calendar c = getCalendar(null);
        long minD = Long.MAX_VALUE;
        long maxD = Long.MIN_VALUE;
        long minT = Long.MAX_VALUE;
        long maxT = Long.MIN_VALUE;
        long minDur = 1000 * 60 * 60; // 1 hour as min time granularity
        for (ApplicationAPI.Timeslot ts : tss) {
            c.setTimeInMillis(ts.from);
//            if (c.get(Calendar.MINUTE) != 0) {
//                minDur = Math.min(minDur, c.get(Calendar.MINUTE));
//            }
            if (ts.duration > 0) {
                if (ts.duration % minDur != 0 || minDur % ts.duration != 0) {
                    long m0 = ts.duration % minDur;
                    long m1 = minDur % ts.duration;
                    minDur = Math.min(m0 != 0 ? m0 : minDur, m1 != 0 ? m1 : minDur);
                }
                minDur = Math.min(minDur, ts.duration);
            }

            long tMin = getTimeHM(c);
            c.set(Calendar.MILLISECOND, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.HOUR_OF_DAY, 0);
            minD = Math.min(minD, c.getTimeInMillis());
            maxD = Math.max(maxD, c.getTimeInMillis() + ts.duration);
            minT = Math.min(minT, tMin);
            maxT = Math.max(maxT, tMin + ts.duration);

            if (minT % minDur != 0 || maxT % minDur != 0 || tMin % minDur != 0) {
                if (minT > 0 && minT % minDur != 0) {
                    minDur = minT % minDur;
                }
                if (maxT > 0 && maxT % minDur != 0) {
                    minDur = maxT % minDur;
                }
                if (tMin > 0 && tMin % minDur != 0) {
                    minDur = tMin % minDur;
                }
            }

            if (minDur > ONE_HOUR && minDur % ONE_HOUR > 0) {
                minDur = minDur % ONE_HOUR;
            }
            if (minT % ONE_HOUR > 0) {
                minDur = Math.min(minDur, minT % ONE_HOUR);
            }
            if (maxT % ONE_HOUR > 0) {
                minDur = Math.min(minDur, maxT % ONE_HOUR);
            }

            //String bs = TimeTools.dumpBounds(new long[]{minD, maxD, minT, maxT, minDur});
            //System.out.println("--->\n" + bs);
            int a = 0;
        }
        return new long[]{minD, maxD, minT, maxT, minDur};
    }

    /**
     * Returns array of long[] {minDay,maxDay,minTime,maxTime,timeInterval}
     *
     * Where min/max day represents 0-time days range, min/max time represent
     * time range over all days, and timeInterval - minimal needed duration
     * needed to represent all time slots (e.g. if all timeslots boundaries are
     * at sharp hours - 1h, if there's half hour : 30min, if quarter - 15 min,
     * if similar to 5min - 5 min
     *
     * @param tss
     * @return
     */
    public static long[] evalTimeEventBounds(Collection<TimeEvent> tss) {
        Calendar c = getCalendar(null);
        long minD = Long.MAX_VALUE;
        long maxD = Long.MIN_VALUE;
        long minT = Long.MAX_VALUE;
        long maxT = Long.MIN_VALUE;
        long minDur = 1000 * 60 * 60; // 1 hour as min time granularity
        for (TimeEvent ts : tss) {
            c.setTimeInMillis(ts.getStart());
//            if (c.get(Calendar.MINUTE) != 0) {
//                minDur = Math.min(minDur, c.get(Calendar.MINUTE));
//            }
            if (ts.getDuration() > 0) {
                if (ts.getDuration() % minDur != 0 || minDur % ts.getDuration() != 0) {
                    long m0 = ts.getDuration() % minDur;
                    long m1 = minDur % ts.getDuration();
                    minDur = Math.min(m0 != 0 ? m0 : minDur, m1 != 0 ? m1 : minDur);
                }
                minDur = Math.min(minDur, ts.getDuration());
            }

            long tMin = getTimeHM(c);
            c.set(Calendar.MILLISECOND, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.HOUR_OF_DAY, 0);
            minD = Math.min(minD, c.getTimeInMillis());
            maxD = Math.max(maxD, c.getTimeInMillis() + ts.getDuration());
            minT = Math.min(minT, tMin);
            maxT = Math.max(maxT, tMin + ts.getDuration());

            if (minT % minDur != 0 || maxT % minDur != 0 || tMin % minDur != 0) {
                if (minT > 0 && minT % minDur != 0) {
                    minDur = minT % minDur;
                }
                if (maxT > 0 && maxT % minDur != 0) {
                    minDur = maxT % minDur;
                }
                if (tMin > 0 && tMin % minDur != 0) {
                    minDur = tMin % minDur;
                }
            }

            if (minDur > ONE_HOUR && minDur % ONE_HOUR > 0) {
                minDur = minDur % ONE_HOUR;
            }
            if (minT % ONE_HOUR > 0) {
                minDur = Math.min(minDur, minT % ONE_HOUR);
            }
            if (maxT % ONE_HOUR > 0) {
                minDur = Math.min(minDur, maxT % ONE_HOUR);
            }

            //String bs = TimeTools.dumpBounds(new long[]{minD, maxD, minT, maxT, minDur});
            //System.out.println("--->\n" + bs);
            int a = 0;
        }
        return new long[]{minD, maxD, minT, maxT, minDur};
    }

    public static String dumpBounds(long[] bounds) {
        Calendar cal = TimeTools.getCalendar(null);
        StringBuilder sb = new StringBuilder();
        int off = 0;
        sb.append("bounds[" + bounds.length + "]");
        for (String n : new String[]{"minD", "maxD"}) {
            if (off >= bounds.length) {
                break;
            }
            cal.setTimeInMillis(bounds[off++]);
            sb.append("\n" + n + "\t" + cal.getTime());
        }
        for (String n : new String[]{"minT", "maxT", "minDur"}) {
            if (off >= bounds.length) {
                break;
            }
            long timeHM = bounds[off++];
            //ucal.setTimeInMillis(bounds[off++]);
            sb.append("\n" + n + "\t" + dumpTimeHM(timeHM));
        }
        return sb.toString();
    }

    public static String dumpDateTime(Long time) {
        if (time == null || time <= 0) {
            return "<none>";
        }
        Calendar cal = getCalendar(time);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.sss z");
        return sdf.format(cal.getTime());
    }

    public static String dumpTimeHM(long timeHM) {
        int h = (int) ((timeHM % ONE_DAY) / ONE_HOUR);
        int m = (int) ((timeHM % ONE_HOUR) / ONE_MIN);
        return ((h < 10) ? "0" : "") + h + ":" + ((m < 10) ? "0" : "") + m;
    }

    public static String dumpTimeHMS(long timeHMS) {
        int s = (int) ((timeHMS % ONE_MIN) / ONE_SEC);
        return dumpTimeHM(timeHMS) + ":" + ((s < 10) ? "0" : "") + s;
    }

    public List<long[]> getWeekIntervals(List<ApplicationAPI.Timeslot> tts) {
        List<long[]> r = new ArrayList<>();
        long[] bounds = evalBounds(tts);
        Calendar c = getCalendar(null);
        long t = bounds[0];
        c.setTimeInMillis(t);
        int lastW = c.getWeekYear();
        while (t <= bounds[1]) {
            c.setTimeInMillis(t);
            int w = c.getWeekYear();
            if (w != lastW) {
                long[] rr = new long[]{w, 0, 0};
                toStartOfWeek(c);
                rr[1] = c.getTimeInMillis();
                toEndOfWeek(c);
                rr[2] = c.getTimeInMillis();
            }
            lastW = w;
        }
        long[] rr = new long[]{c.get(Calendar.WEEK_OF_YEAR), 0, 0};
        toStartOfWeek(c);
        rr[1] = c.getTimeInMillis();
        toEndOfWeek(c);
        rr[2] = c.getTimeInMillis();
        return r;
    }

    public static long toStartOfDay(Calendar c) {
        c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.HOUR_OF_DAY, 0);
        return c.getTimeInMillis();
    }

    public static long toEndOfDay(Calendar c) {
        c.add(Calendar.DAY_OF_YEAR, 1);
        toStartOfDay(c);
        c.add(Calendar.MILLISECOND, -1);
        return c.getTimeInMillis();
    }

    public static long toStartOfWeek(Calendar c) {
        c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.HOUR_OF_DAY, 0);
        int wd = c.get(Calendar.DAY_OF_WEEK);
        while (wd != c.getFirstDayOfWeek()) {
            c.add(Calendar.DAY_OF_WEEK, -1);
            wd = c.get(Calendar.DAY_OF_WEEK);
        }
        return c.getTimeInMillis();
    }

    public static long toEndOfWeek(Calendar c) {
        c.add(Calendar.WEEK_OF_YEAR, 1);
        toStartOfWeek(c);
        c.add(Calendar.MILLISECOND, -1);
        return c.getTimeInMillis();
    }

    public static long toStartOfMonth(Calendar c) {
        c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis();
    }

    public static long toEndOfMonth(Calendar c) {
        c.add(Calendar.MONTH, 1);
        toStartOfMonth(c);
        c.add(Calendar.MILLISECOND, -1);
        return c.getTimeInMillis();
    }

    public SEASON getSeason(Calendar c) {
        return SEASON.seasonOf(c);
    }

    public static long toStartOfSeason(Calendar c) {
        c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.DAY_OF_MONTH, 1);
        SEASON s = SEASON.seasonOf(c);
        int m = c.get(Calendar.MONTH);
        int[] ms = s.months();
        if (ms[0] > 1) {
            c.set(Calendar.MONTH, ms[0]);
        } else {
            if (m != 11) {
                c.add(Calendar.YEAR, -1);
            }
            c.set(Calendar.MONTH, ms[0]);
        }
        return c.getTimeInMillis();
    }

    public static long toEndOfSeason(Calendar c) {
        c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.DAY_OF_MONTH, 1);
        SEASON s = SEASON.seasonOf(c);
        int m = c.get(Calendar.MONTH);
        int[] ms = s.months();
        if (ms[0] == 11) {
            c.add(Calendar.YEAR, 1);
            c.set(Calendar.MONTH, ms[2]);
        } else {
            c.set(Calendar.MONTH, ms[2]);
        }
        return toEndOfMonth(c);
    }

    public static long toStartOfQuarter(Calendar c) {
        c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.DAY_OF_MONTH, 1);
        int m = (c.get(Calendar.MONTH) / 3)*3;
        c.set(Calendar.MONTH, m);
        return c.getTimeInMillis();
    }

    public static long toEndOfQuarter(Calendar c) {
        c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.DAY_OF_MONTH, 1);
        int m = (c.get(Calendar.MONTH) / 3)*3;
        c.set(Calendar.MONTH, m + 2);
        return toEndOfMonth(c);
    }

    public static long toStartOfYear(Calendar c) {
        c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.MONTH, 0);
        return c.getTimeInMillis();
    }

    public static long toEndOfYear(Calendar c) {
        c.add(Calendar.YEAR, 1);
        toStartOfYear(c);
        c.add(Calendar.MILLISECOND, -1);
        return c.getTimeInMillis();
    }

    /**
     * Returns season time range in ms for calendar year (or current year if
     * null). Year seasons start with spring and end with winter.
     *
     * @param calendar
     * @param season
     * @return
     */
    public static long[] rangeOf(Calendar calendar, SEASON season) {
        long[] r = new long[2];
        Calendar c = getCalendar((calendar != null) ? calendar.getTimeInMillis() : null);
        if (season != null) {
            switch (season) {
                case spring:
                    c.set(Calendar.MONTH, 2);
                    r[0] = toStartOfMonth(c);
                    c.set(Calendar.MONTH, 4);
                    r[1] = toEndOfMonth(c);
                    break;
                case summer:
                    c.set(Calendar.MONTH, 5);
                    r[0] = toStartOfMonth(c);
                    c.set(Calendar.MONTH, 7);
                    r[1] = toEndOfMonth(c);
                    break;
                case autumn:
                    c.set(Calendar.MONTH, 8);
                    r[0] = toStartOfMonth(c);
                    c.set(Calendar.MONTH, 10);
                    r[1] = toEndOfMonth(c);
                    break;
                case winter:
                    c.set(Calendar.MONTH, 11);
                    r[0] = toStartOfMonth(c);
                    c.add(Calendar.YEAR, 1);
                    c.set(Calendar.MONTH, 1);
                    r[1] = toEndOfMonth(c);
                    break;
                default:
                    break;
            }
        }
        return r;
    }

    /**
     * Returns quarter time range in ms for calendar year (or current year if
     * null)
     *
     * @param calendar
     * @param quarter
     * @return
     */
    public static long[] rangeOf(Calendar calendar, int quarter) {
        long[] r = new long[2];
        Calendar c = getCalendar((calendar != null) ? calendar.getTimeInMillis() : null);
        if (quarter > 0 && quarter < 5) {
            switch (quarter) {
                case 1:
                    c.set(Calendar.MONTH, 0);
                    r[0] = toStartOfMonth(c);
                    c.set(Calendar.MONTH, 2);
                    r[1] = toEndOfMonth(c);
                    break;
                case 2:
                    c.set(Calendar.MONTH, 3);
                    r[0] = toStartOfMonth(c);
                    c.set(Calendar.MONTH, 5);
                    r[1] = toEndOfMonth(c);
                    break;
                case 3:
                    c.set(Calendar.MONTH, 6);
                    r[0] = toStartOfMonth(c);
                    c.set(Calendar.MONTH, 8);
                    r[1] = toEndOfMonth(c);
                    break;
                case 4:
                    c.set(Calendar.MONTH, 9);
                    r[0] = toStartOfMonth(c);
                    c.set(Calendar.MONTH, 11);
                    r[1] = toEndOfMonth(c);
                    break;
                default:
                    break;
            }
        }
        return r;
    }
}
