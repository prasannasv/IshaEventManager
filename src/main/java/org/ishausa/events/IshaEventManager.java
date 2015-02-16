package org.ishausa.events;

/**
 * This is the starting point for execution.
 *
 * @author psriniv
 */
public class IshaEventManager {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            System.out.println("args[0]: " + args[0]);
        }
        // Start thread to poll for mail
        // on new mail, hand it over to parser
        // if parsed as an event, create a calendar entry
        Account account = new IshaEventsOAuthTokenProvider();
        CalendarEntryCreator eventLogger = new CalendarEntryCreator(account);

        MailParser mailParser = new MailParser();
        mailParser.addListener(eventLogger);

        MailPoller poller = new MailPoller(account, 1);
        poller.addListener(mailParser);

        poller.start();
        poller.waitForRuns();
        poller.shutdown();
    }
}
