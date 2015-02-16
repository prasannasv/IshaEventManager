package org.ishausa.events;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import java.io.File;
import java.io.FileInputStream;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.ishausa.events.IshaEvent.EventSetting;

/**
 *
 * @author psriniv
 */
public class CalendarEntryCreator implements MailParser.EventListener {
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /** Directory to store user credentials. */
    private static final java.io.File DATA_STORE_DIR =
        new File(System.getProperty("user.home"), ".store/calendar_sample");

    private static FileDataStoreFactory dataStoreFactory;

    private Account account;
    private HttpTransport transport;
    private Calendar client;

    public CalendarEntryCreator(Account account) throws Exception {
        this.account = account;
        transport = GoogleNetHttpTransport.newTrustedTransport();
        dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
        Credential credential = authorize(transport, dataStoreFactory);
        client = new Calendar.Builder(transport, JSON_FACTORY, credential)
                .setApplicationName("IshaEventsManager")
                .build();
    }

    /** Authorizes the installed application to access user's protected data. */
    private Credential authorize(HttpTransport httpTransport, DataStoreFactory dataStoreFactory) throws Exception {
        // load client secrets
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
            new InputStreamReader(new FileInputStream("C:/Other/Personal/Prasanna/Code/Java/IshaEvents/client_secrets.json")));
            //new InputStreamReader(CalendarEntryCreator.class.getResourceAsStream("C:/Other/Personal/Prasanna/Code/Java/IshaEvents/client_secrets.json")));

        // Set up authorization code flow.
        // Ask for only the permissions you need. Asking for more permissions will
        // reduce the number of users who finish the process for giving you access
        // to their accounts. It will also increase the amount of effort you will
        // have to spend explaining to users what you are doing with their data.
        // Here we are listing all of the available scopes. You should remove scopes
        // that you are not actually using.
        Set<String> scopes = new HashSet<String>();
        scopes.add(CalendarScopes.CALENDAR);
        scopes.add(CalendarScopes.CALENDAR_READONLY);

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            httpTransport, JSON_FACTORY, clientSecrets, scopes)
            .setDataStoreFactory(dataStoreFactory)
            .build();
        // authorize
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    @Override
    public void onNewEvent(IshaEvent ishaEvent) {
        System.out.println("Received new event: " + ishaEvent);
        if (!ishaEvent.isIshaKriyaEvent || ishaEvent.eventSetting == EventSetting.PRIVATE) {
            System.out.println("Ignoring it since it is either not an IshaKriya event or is Private.");
            return;
        }
        try {
            Event event = createEvent(ishaEvent);
            Event createdEvent = client.events().insert(account.getUser(), event).execute();
            System.out.println("Event created: " + createdEvent);

            /*Events events = client.events().list(account.getUser()).execute();
            List<Event> items = events.getItems();
            for (Event item : items) {
                System.out.println("Summary: " + item.getSummary());
            }*/
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    private Event createEvent(IshaEvent ishaEvent) throws Exception {
        Event event = new Event();

        event.setSummary(ishaEvent.title);
        event.setDescription(ishaEvent.comments);
        event.setLocation(ishaEvent.location.toString());

        long startDateMillis = ishaEvent.date.getTime();
        IshaEvent.Duration eventDuration = IshaEvent.parseDuration(ishaEvent.timing);

        Date startDate = new Date(startDateMillis + TimeUnit.HOURS.toMillis(eventDuration.start.hour) + TimeUnit.MINUTES.toMillis(eventDuration.start.minute));
        DateTime start = new DateTime(startDate);
        event.setStart(new EventDateTime().setDateTime(start));

        Date endDate = new Date(startDateMillis + TimeUnit.HOURS.toMillis(eventDuration.end.hour) + TimeUnit.MINUTES.toMillis(eventDuration.end.minute));
        DateTime end = new DateTime(endDate);
        event.setEnd(new EventDateTime().setDateTime(end));

        return event;
    }
}
