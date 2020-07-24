package org.opentripplanner.middleware.models;

import org.opentripplanner.middleware.otp.response.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks the state of a {@link MonitoredTrip} while it is actively being monitored.
 */
public class JourneyState extends Model {

    public JourneyState(MonitoredTrip monitoredTrip) {
        monitoredTripId = monitoredTrip.id;
    }

    public final String monitoredTripId;

    /**
     * Timestamp checking the last time a journey was checked.
     */
    public long lastChecked;

    /**
     *
     */
    public List<String> alertIds = new ArrayList<>();

    public List<Response> responses;


}
