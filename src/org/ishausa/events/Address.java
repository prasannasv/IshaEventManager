package org.ishausa.events;

/**
 *
 * @author psriniv
 */
public class Address {
    String firstLine;
    String city;
    String state;
    String zipcode;

    @Override
    public String toString() {
        return firstLine + ", " + city + ", " + state + ", " + zipcode;
    }
}
