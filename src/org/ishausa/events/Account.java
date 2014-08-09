package org.ishausa.events;

/**
 *
 * @author psriniv
 */
public interface Account {
    String getUser();
    
    String getOAuthToken();
    
    String getOAuthRefreshToken();
    
    String getOAuthClientId();
    
    String getOAuthClientSecret();
}
