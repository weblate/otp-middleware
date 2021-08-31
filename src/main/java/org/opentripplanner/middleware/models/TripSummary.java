package org.opentripplanner.middleware.models;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.opentripplanner.middleware.cdp.AnonymizedTripSummary;
import org.opentripplanner.middleware.otp.response.Itinerary;
import org.opentripplanner.middleware.otp.response.Place;
import org.opentripplanner.middleware.otp.response.PlannerError;
import org.opentripplanner.middleware.otp.response.TripPlan;
import org.opentripplanner.middleware.persistence.Persistence;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.opentripplanner.middleware.persistence.TypedPersistence.filterByUserId;

/**
 * A trip summary represents the parts of an OTP plan response which are required for trip monitoring purposes
 */
public class TripSummary extends Model {
    private static final long serialVersionUID = 1L;
    public Place fromPlace;

    public Place toPlace;

    public PlannerError error;

    public List<Itinerary> itineraries;

    public String tripRequestId;

    /** This no-arg constructor exists to make MongoDB happy. */
    public TripSummary() {
    }

    public TripSummary(TripPlan tripPlan, PlannerError error, String tripRequestId) {
        if (tripPlan != null) {
            this.fromPlace = tripPlan.from;
            this.toPlace = tripPlan.to;
            this.itineraries = tripPlan.itineraries;
        }
        this.error = error;
        this.tripRequestId = tripRequestId;
    }

    public TripSummary(List<Itinerary> itineraries) {
        this.itineraries = itineraries;
    }

    private AnonymizedTripSummary getAnonimized() {
        // TODO: More work is needed in this area to define required parameters.
        return new AnonymizedTripSummary(itineraries);
    }

    /**
     * Get all trip summaries between two dates.
     */
    private static FindIterable<TripSummary> getTripSummaries(Date start, Date end) {
        return Persistence.tripSummaries.getFiltered(
            Filters.and(
                Filters.gte("dateCreated", start),
                Filters.lte("dateCreated", end)
            ),
            Sorts.descending("dateCreated")
        );
    }

    /**
     * Get all trip summaries between two dates, extract qualifying anonymous data and return.
     */
    public static List<AnonymizedTripSummary> getAnonymizedTripSummaries(Date start, Date end) {
        List<AnonymizedTripSummary> anonymizedTripSummaries = new ArrayList<>();
        for (TripSummary tripSummary : getTripSummaries(start, end)) {
            anonymizedTripSummaries.add(tripSummary.getAnonimized());
        }
        return anonymizedTripSummaries;
    }

    @Override
    public String toString() {
        return "TripSummary{" +
            "fromPlace=" + fromPlace +
            ", toPlace=" + toPlace +
            ", error=" + error +
            ", itineraries=" + itineraries +
            ", tripRequestId='" + tripRequestId + '\'' +
            ", id='" + id + '\'' +
            ", lastUpdated=" + lastUpdated +
            ", dateCreated=" + dateCreated +
            '}';
    }
}
