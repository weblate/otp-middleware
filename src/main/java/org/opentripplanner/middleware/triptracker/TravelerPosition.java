package org.opentripplanner.middleware.triptracker;

import org.opentripplanner.middleware.auth.RequestingUser;
import org.opentripplanner.middleware.models.OtpUser;
import org.opentripplanner.middleware.models.TrackedJourney;
import org.opentripplanner.middleware.otp.response.Itinerary;
import org.opentripplanner.middleware.otp.response.Leg;
import org.opentripplanner.middleware.utils.Coordinates;
import org.opentripplanner.middleware.utils.I18nUtils;

import java.time.Instant;
import java.util.Locale;

import static org.opentripplanner.middleware.triptracker.ManageLegTraversal.getExpectedLeg;
import static org.opentripplanner.middleware.triptracker.ManageLegTraversal.getNextLeg;
import static org.opentripplanner.middleware.triptracker.ManageLegTraversal.getSegmentFromPosition;

public class TravelerPosition {

    /** The leg the traveler is expected to be on. */
    public Leg expectedLeg;

    /** The expected traveler position based on position. */
    public LegSegment legSegmentFromPosition;

    /** Traveler current coordinates. */
    public Coordinates currentPosition;

    /** Traveler current time. */
    public Instant currentTime;

    /** Information held about the traveler's journey. */
    public TrackedJourney trackedJourney;

    /** The leg, if available, after the expected leg. */
    public Leg nextLeg;

    /** Traveler mobility information which is passed on to bus operators. */
    public String mobilityMode;

    /** The traveler's locale. */
    public Locale locale;

    public TravelerPosition(TrackedJourney trackedJourney, Itinerary itinerary, OtpUser otpUser) {
        TrackingLocation lastLocation = trackedJourney.locations.get(trackedJourney.locations.size() - 1);
        currentTime = lastLocation.timestamp.toInstant();
        currentPosition = new Coordinates(lastLocation);
        expectedLeg = getExpectedLeg(currentPosition, itinerary);
        if (expectedLeg != null) {
            nextLeg = getNextLeg(expectedLeg, itinerary);
        }
        legSegmentFromPosition = getSegmentFromPosition(expectedLeg, currentPosition);
        this.trackedJourney = trackedJourney;
        if (otpUser != null) {
            if (otpUser.mobilityProfile != null) {
                mobilityMode = otpUser.mobilityProfile.mobilityMode;
            }
            this.locale = I18nUtils.getOtpUserLocale(otpUser);
        }
    }

    /** Used for unit testing. */
    public TravelerPosition(Leg expectedLeg, Coordinates currentPosition) {
        this.expectedLeg = expectedLeg;
        this.currentPosition = currentPosition;
        legSegmentFromPosition = getSegmentFromPosition(expectedLeg, currentPosition);
    }
}
