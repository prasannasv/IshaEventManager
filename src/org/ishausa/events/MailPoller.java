/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ishausa.events;

import com.google.code.samples.oauth2.OAuth2Authenticator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Store;

/**
 *
 * @author psriniv
 */
public class MailPoller {
    private static final long POLLING_FREQUENCY_MINUTES = 15;
    private static final String INBOX = "inbox";
    private static final String GMAIL_IMAP_HOST = "imap.gmail.com";
    private static final int GMAIL_IMAP_PORT = 993;
    
    private Account account;
    private CountDownLatch runCountLatch;
    private CopyOnWriteArrayList<MailListener> listeners = new CopyOnWriteArrayList<MailListener>();

    private ScheduledExecutorService pollerThread = Executors.newSingleThreadScheduledExecutor();

    public static interface MailListener {
        void onNewMail(Message m);
    }

    public MailPoller(Account account, int runs) {
        this.account = account;
        runCountLatch = new CountDownLatch(runs);

        System.out.println("Initializing OAuth2 Authenticator");
        OAuth2Authenticator.initialize();
    }

    public void start() {
        System.out.println("Scheduling mail checker task");
        pollerThread.scheduleWithFixedDelay(new MailCheckTask(), 0, POLLING_FREQUENCY_MINUTES, TimeUnit.MINUTES);
    }

    public void addListener(MailListener listener) {
        listeners.add(listener);
    }

    public void removeListener(MailListener listener) {
        listeners.remove(listener);
    }

    public void waitForRuns() throws InterruptedException {
        runCountLatch.await();
    }
    
    public void shutdown() {
        pollerThread.shutdown();
        try {
            pollerThread.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //ignore during shutdown
        } finally {
            if (!pollerThread.isShutdown()) {
                pollerThread.shutdownNow();
            }
        }
    }
    
    private void notifyListeners(Message m) {
        for (MailListener l : listeners) {
            l.onNewMail(m);
        }
    }

    private class MailCheckTask implements Runnable {
        @Override
        public void run() {
            try {
                System.out.println("Beginning run of Mail Check Task");

                Store store = OAuth2Authenticator.connectToImap(GMAIL_IMAP_HOST, GMAIL_IMAP_PORT,
                        account.getUser(), account.getOAuthToken(), false);

                Folder inbox = store.getFolder(INBOX);
                inbox.open(Folder.READ_WRITE);
                System.out.println("inbox folder opened for read. inbox: " + inbox);

                int totalMessagesCount = inbox.getMessageCount();
                int unreadMessagesCount = inbox.getUnreadMessageCount();
                System.out.println("Total messages: " + totalMessagesCount + ", unread messages: " + unreadMessagesCount);

                Message[] unreadMessages = inbox.getMessages(totalMessagesCount - unreadMessagesCount + 1, totalMessagesCount);
                for (Message m : unreadMessages) {
                    notifyListeners(m);
                }

                inbox.close(false);
                store.close();
            } catch (Exception e) {
                System.err.println(e);
            } finally {
                System.out.println("Finished run");
                runCountLatch.countDown();
            }
        }
    }
}
