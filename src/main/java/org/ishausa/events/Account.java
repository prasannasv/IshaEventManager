package org.ishausa.events;

/**
 *
 * @author psriniv
 */
public interface Account {
    String getUser();

    String getCalendarId();

    String getAccessToken();

    void setAccessToken(String accessToken);
}
