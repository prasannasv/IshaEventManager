package org.ishausa.events;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.Multipart;

/**
 *
 * @author psriniv
 */
public class MailParser implements MailPoller.MailListener {

    private static final String ISHA_EVENT_SUBJECT = "A New Event Has Been Generated";

    private EventParser eventParser = new IshaKriyaEventParser();

    private CopyOnWriteArrayList<EventListener> listeners = new CopyOnWriteArrayList<EventListener>();

    public static interface EventListener {
        void onNewEvent(IshaEvent e);
    }

    public void addListener(EventListener l) {
        listeners.add(l);
    }

    public void removeListener(EventListener l) {
        listeners.remove(l);
    }

    @Override
    public void onNewMail(Message m) {
        System.out.println("Received new message: " + m);
        try {
            String subject = m.getSubject();

            System.out.println("Subject: " + subject);
            if (subject.contains(ISHA_EVENT_SUBJECT)) {
                IshaEvent e = eventParser.parse(m);
                notifyListeners(e);
            }

            m.setFlag(Flags.Flag.SEEN, true);
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    private void notifyListeners(IshaEvent e) {
        for (EventListener l : listeners) {
            l.onNewEvent(e);
        }
    }

    private interface EventParser {
        IshaEvent parse(Message m) throws Exception;
    }

    public static class IshaKriyaEventParser implements EventParser {
        private static final char PROPERTY_SEPARATOR = ':';

        @Override
        public IshaEvent parse(Message m) throws Exception {
            Object messageContent = m.getContent();
            String bodyContent;
            if (messageContent instanceof Multipart) {
                Multipart content = (Multipart) m.getContent();
                BodyPart body = content.getBodyPart(0);
                bodyContent = body.getContent().toString();
            } else if (messageContent instanceof String) {
                bodyContent = (String) messageContent;
            } else {
                System.out.println("Unknown type for message content, just calling toString: " + messageContent.getClass());
                bodyContent = messageContent.toString();
            }

            System.out.println("Parsing message body into Event");

            return parse(bodyContent);
        }

        public IshaEvent parse(String bodyContent) throws Exception {
            Map<String, String> eventProperties = new HashMap<String, String>();
            String[] lines = bodyContent.split("\n");
            for (String line : lines) {
                if (line.isEmpty()) {
                    continue;
                }
                int keyEnd = line.indexOf(PROPERTY_SEPARATOR);
                if (keyEnd == -1) {
                    continue;
                }
                String key = line.substring(0, keyEnd).trim();
                String value = line.substring(keyEnd + 1).trim();
                eventProperties.put(key, value);
            }
            System.out.println("Property map: " + eventProperties);

            return new IshaEvent(eventProperties);
        }
    }
}
