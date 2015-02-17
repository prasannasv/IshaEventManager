package org.ishausa.events;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Country: USA
 * State: MI
 * City: Canton
 * Address: 46000 Summit Pwy
 * Zipcode: 48188
 * IshaEvent: Teacher/Ishaanga Led Session
 * IshaEvent Setting: Public
 * CME: No
 * Host: Summit
 * Is Isha Kriya part of the event? : Yes
 * Date: 2014-02-06
 * Center: dtrc
 * Category: Other
 * Title: isha kriya
 * Timings: 7-8
 * Comments:
 * Full Name: Shanthi Balakrishnan
 * Email Id: smanicka@hotmail.com
 *
 * @author psriniv
 */
public class IshaEvent {
    private static final Logger log = Logger.getLogger(IshaEvent.class.getName());
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static enum EventProperty {
        COUNTRY("Country"),
        STATE("State"),
        CITY("City"),
        ADDRESS("Address"),
        ZIPCODE("Zipcode"),
        EVENT("Event"),
        EVENT_SETTING("Event Setting"),
        CME("CME"),
        HOST("Host"),
        ISHA_KRIYA_EVENT("Is Isha Kriya part of the event?"),
        DATE("Date"),
        CENTER("Center"),
        CATEGORY("Category"),
        TITLE("Title"),
        TIMINGS("Timings"),
        COMMENTS("Comments"),
        ORGANIZER_NAME("Full Name"),
        ORGANIZER_EMAIL("Email Id");

        EventProperty(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        private String key;
    }

    public static enum EventSetting {
        PUBLIC,
        PRIVATE;
    }

    public IshaEvent(Map<String, String> eventProperties) throws Exception {
        title = eventProperties.get(EventProperty.TITLE.getKey());

        String eventSettingValue = eventProperties.get(EventProperty.EVENT_SETTING.getKey());
        eventSetting = EventSetting.PUBLIC.toString().equalsIgnoreCase(eventSettingValue) ? EventSetting.PUBLIC : EventSetting.PRIVATE;

        date = DATE_FORMAT.parse(eventProperties.get(EventProperty.DATE.getKey()));
        timing = eventProperties.get(EventProperty.TIMINGS.getKey());

        location = new Address();
        location.state = eventProperties.get(EventProperty.STATE.getKey());
        location.city = eventProperties.get(EventProperty.CITY.getKey());
        location.firstLine = eventProperties.get(EventProperty.ADDRESS.getKey());
        location.zipcode = eventProperties.get(EventProperty.ZIPCODE.getKey());

        isIshaKriyaEvent = "Yes".equalsIgnoreCase(eventProperties.get(EventProperty.ISHA_KRIYA_EVENT.getKey()));
        host = eventProperties.get(EventProperty.HOST.getKey());
        ishaCenter = eventProperties.get(EventProperty.CENTER.getKey());
        comments = eventProperties.get(EventProperty.COMMENTS.getKey());
    }

    public static class Timing {
        int hour;
        int minute;
        String meridian = "";

        public void to24HourFormat() {
            if ("pm".equals(meridian) && hour < 12) {
                hour += 12;
            }
        }

        @Override
        public String toString() {
            return String.format("%02d%02d", hour, minute);
        }
    }

    public static class Duration {
        private static final int EARLY_MORNING_THRESHOLD = 8;

        Timing start;
        Timing end;

        /**
         * 11 - 12 pm => 11 am To 12 pm
         * 11 - 1 pm => 11 am To 1 pm
         * 12 - 1 pm => doesn't matter if it is am or pm
         * 3 - 4 pm => 3 pm To 4 pm
         * 7 am - 9 => 7 am To 9 am
         * 11 am - 12 => doesn't matter
         * 11 am - 1 => 11 am To 1 pm
         * 8 am - 7 pm => 8 am To 7 pm
         * 5 - 6 => 5 pm To 6 pm
         * 8 - 9 => 8 am To 9 am
         * 12 - 1 => 12 pm To 1 pm
         */
        private void inferMeridianIfUnavailable() {
            String startMeridian = start.meridian;
            String endMeridian = end.meridian;
            if (!startMeridian.isEmpty() && !endMeridian.isEmpty()) {
                return;
            }
            String inferredMeridian = "am";
            if (startMeridian.isEmpty() && endMeridian.isEmpty()) {
                if (start.hour < EARLY_MORNING_THRESHOLD && end.hour < 12) {
                    inferredMeridian = "pm";
                } else if (start.hour > end.hour) {
                    inferredMeridian = "pm";
                }
            } else if (startMeridian.isEmpty()) {
                if ("pm".equals(end.meridian) && end.hour - start.hour >= 0 && end.hour < 12) {
                    inferredMeridian = "pm";
                }
            } else if (endMeridian.isEmpty()) {
                if (end.hour < start.hour || "pm".equals(startMeridian)) {
                    inferredMeridian = "pm";
                }
            }

            if (startMeridian.isEmpty()) {
                start.meridian = inferredMeridian;
            }
            if (endMeridian.isEmpty()) {
                end.meridian = inferredMeridian;
            }
            log.info("startMeridian: " + startMeridian + ", endMeridian: " + endMeridian + ", inferredMeridian: " + inferredMeridian);
            log.info("duration: " + this);
        }

        public void to24HourFormat() {
            inferMeridianIfUnavailable();
            start.to24HourFormat();
            end.to24HourFormat();
        }

        @Override
        public String toString() {
            return start.toString() + " To " + end.toString();
        }
    }

    // Timings: 6:30-7:30 pm
    // Timings: 5:30-7:00
    // Timings: 5:00 pm to 6:30 pm
    // Timings: 11am - 4p
    // Timings: 2:00-3:00 PM
    // Timings: 0730_1700
    // Timings: 9am - 2pm
    // Timings: 10am - 5:30pm
    // Timings: 5PM-7PM
    // Timings: 1-2PM
    // Timings: 11:30 - 12:30 PM
    // Timings: 6:30pm
    // Timings: 6pm to 8p,
    // Timings: 11am - noon
    public static Duration parseDuration(String timing) {
        // start and end time splitter (optional) [-, _, to, ]
        // possible time formats
        // digit(s)[:digit(s)] [am|pm|a|p]
        // 24 hour format
        Duration duration = new Duration();
        String[] timeTokens = null;
        if (timing.contains("_")) {
            timeTokens = timing.split("_");
        } else if (timing.contains("-")) {
            timeTokens = timing.split("-");
        } else if (timing.contains("to")) {
            timeTokens = timing.split("to");
        }

        if (timeTokens == null) {
            duration.start = parseTiming(timing);
            duration.end = new Timing();
            duration.end.hour = duration.start.hour + 1;
            duration.end.minute = duration.start.minute;
            duration.end.meridian = duration.start.meridian;
        } else {
            duration.start = parseTiming(timeTokens[0]);
            duration.end = parseTiming(timeTokens[1]);
        }
        duration.to24HourFormat();

        return duration;
    }

    private static enum ParseState {
        HOUR,
        HOUR_MINUTE_SEPARATION,
        MINUTE,
        DONE;
    }

    // We are expecting the string to be of this form: digit(s)[:][digits(s)][space][am|pm|a|p]
    public static Timing parseTiming(String time) {
        Timing timing = new Timing();
        time = time.toLowerCase().replaceAll(" ", "");
        if (time.contains("am")) {
            timing.meridian = "am";
            time = time.replaceAll("am", "");
        } else if (time.contains("a")) {
            timing.meridian = "am";
            time = time.replaceAll("a", "");
        } else if (time.contains("pm")) {
            timing.meridian = "pm";
            time = time.replaceAll("pm", "");
        } else if (time.contains("p")) {
            timing.meridian = "pm";
            time = time.replaceAll("p", "");
        } else if ("noon".equalsIgnoreCase(time.trim())) {
            timing.hour = 12;
            timing.minute = 0;
            timing.meridian = "pm";
            return timing;
        }

        time = time.trim();
        log.info("Time after inferring meridian: " + time);
        ParseState state = ParseState.HOUR;
        int value = 0;
        for (int i = 0; i < time.length(); ) {
            char c = time.charAt(i);
            switch (state) {
                case HOUR:
                    if (i > 1) {
                        timing.hour = value;
                        value = 0;
                        state = ParseState.HOUR_MINUTE_SEPARATION;
                    } else if (Character.isDigit(c)) {
                        value = value * 10 + (c - '0');
                        ++i;
                    } else if (c == ':') {
                        timing.hour = value;
                        value = 0;
                        state = ParseState.HOUR_MINUTE_SEPARATION;
                        ++i;
                    } else if (c == ' ') {
                        timing.hour = value;
                        value = 0;
                        state = ParseState.HOUR_MINUTE_SEPARATION;
                        ++i;
                    } else {
                        timing.hour = value;
                        value = 0;
                        state = ParseState.HOUR_MINUTE_SEPARATION;
                        ++i;
                    }
                    break;
                case HOUR_MINUTE_SEPARATION:
                    if (Character.isDigit(c)) {
                        state = ParseState.MINUTE;
                    } else {
                        ++i;
                    }
                    break;
                case MINUTE:
                    if (Character.isDigit(c)) {
                        value = value * 10 + (c - '0');
                        state = ParseState.MINUTE;
                        ++i;
                    } else {
                        timing.minute = value;
                        value = 0;
                        state = ParseState.DONE;
                        ++i;
                    }
                    break;
            }
        }
        if (state == ParseState.HOUR) {
            timing.hour = value;
        } else if (state == ParseState.MINUTE) {
            timing.minute = value;
        }

        return timing;
    }

    String title;
    EventSetting eventSetting;
    Date date;
    Date start;
    Date end;
    String timing;
    Address location;
    boolean isIshaKriyaEvent;
    String host;
    String ishaCenter;
    String comments;

    @Override
    public String toString() {
        return "Title: " + title + ", eventSetting: " + eventSetting + ", date: " + date +
                ", timing: " + timing + ", location: " + location;
    }
}
