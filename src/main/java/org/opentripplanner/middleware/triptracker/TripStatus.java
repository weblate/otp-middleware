package org.opentripplanner.middleware.triptracker;

import org.opentripplanner.middleware.utils.Coordinates;

import java.time.Instant;

import static org.opentripplanner.middleware.utils.ConfigUtils.getConfigPropertyAsInt;
import static org.opentripplanner.middleware.utils.GeometryUtils.getDistanceFromLine;


/**
 * Instructions to be provided to the user depending on where they are on their journey.
 */
public enum TripStatus {

    /**
     * Within the expect position boundary.
     */
    ON_SCHEDULE,

    /**
     * Traveler's position is behind expected.
     */
    BEHIND_SCHEDULE,

    /**
     * Traveler's position is ahead of expected.
     */
    AHEAD_OF_SCHEDULE,

    /**
     * The traveler has completed their trip.
     **/
    ENDED,

    /**
     * The traveler has deviated from the trip route.
     **/
    DEVIATED,

    /**
     * Unable to ascertain the traveler's position.
     **/
    NO_STATUS;

    public static final int TRIP_TRACKING_WALK_BOUNDARY
        = getConfigPropertyAsInt("TRIP_TRACKING_WALK_BOUNDARY", 5);

    public static final int TRIP_TRACKING_BICYCLE_BOUNDARY
        = getConfigPropertyAsInt("TRIP_TRACKING_BICYCLE_BOUNDARY", 10);

    public static final int TRIP_TRACKING_BUS_BOUNDARY
        = getConfigPropertyAsInt("TRIP_TRACKING_BUS_BOUNDARY", 20);

    public static final int TRIP_TRACKING_SUBWAY_BOUNDARY
        = getConfigPropertyAsInt("TRIP_TRACKING_SUBWAY_BOUNDARY", 100);

    public static final int TRIP_TRACKING_TRAM_BOUNDARY
        = getConfigPropertyAsInt("TRIP_TRACKING_TRAM_BOUNDARY", 100);

    public static final int TRIP_TRACKING_RAIL_BOUNDARY
        = getConfigPropertyAsInt("TRIP_TRACKING_RAIL_BOUNDARY", 200);

    /**
     * Define the trip status based on the traveler's current position compared to expected and nearest points on the trip.
     */
    public static TripStatus getTripStatus(TravelerPosition travelerPosition) {
        if (travelerPosition.expectedLeg != null) {
            if (travelerPosition.legSegmentFromPosition != null &&
                isWithinModeBoundary(travelerPosition.currentPosition, travelerPosition.legSegmentFromPosition)
            ) {
                Instant segmentStartTime = travelerPosition
                    .expectedLeg
                    .startTime
                    .toInstant()
                    .plusSeconds((long) getSegmentStartTime(travelerPosition.legSegmentFromPosition));
                Instant segmentEndTime = travelerPosition
                    .expectedLeg
                    .startTime
                    .toInstant()
                    .plusSeconds((long) travelerPosition.legSegmentFromPosition.cumulativeTime);
                if (travelerPosition.currentTime.isBefore(segmentStartTime)) {
                    return TripStatus.AHEAD_OF_SCHEDULE;
                } else if (travelerPosition.currentTime.isAfter(segmentEndTime)) {
                    return TripStatus.BEHIND_SCHEDULE;
                } else {
                    return TripStatus.ON_SCHEDULE;
                }
            }
            if (travelerPosition.legSegmentFromTime != null &&
                isWithinModeBoundary(travelerPosition.currentPosition, travelerPosition.legSegmentFromTime)
            ) {
                return TripStatus.ON_SCHEDULE;
            }
            return TripStatus.DEVIATED;
        }
        return TripStatus.NO_STATUS;
    }

    /**
     * Get the cumulative time at the center of a segment.
     */
    public static double getSegmentTimeInterval(LegSegment legSegmentFromPosition) {
        return legSegmentFromPosition.cumulativeTime - (legSegmentFromPosition.timeInSegment / 2);
    }

    public static double getSegmentStartTime(LegSegment legSegmentFromPosition) {
        return legSegmentFromPosition.cumulativeTime - legSegmentFromPosition.timeInSegment;
    }

    /**
     * Checks if the traveler's position is with an acceptable distance of the mode type.
     */
    private static boolean isWithinModeBoundary(Coordinates currentPosition, LegSegment legSegment) {
        double distanceFromExpected = getDistanceFromLine(legSegment.start, legSegment.end, currentPosition);
        double modeBoundary = getModeBoundary(legSegment.mode);
        return distanceFromExpected <= modeBoundary;
    }

    /**
     * Get the acceptable 'on track' boundary in meters for mode.
     */
    public static double getModeBoundary(String mode) {
        // TODO: Replace these arbitrary values with something more concrete.
        switch (mode.toUpperCase()) {
            case "WALK":
                return TRIP_TRACKING_WALK_BOUNDARY;
            case "BICYCLE":
                return TRIP_TRACKING_BICYCLE_BOUNDARY;
            case "BUS":
                return TRIP_TRACKING_BUS_BOUNDARY;
            case "SUBWAY":
                return TRIP_TRACKING_SUBWAY_BOUNDARY;
            case "TRAM":
                return TRIP_TRACKING_TRAM_BOUNDARY;
            case "RAIL":
                return TRIP_TRACKING_RAIL_BOUNDARY;
            default:
                throw new UnsupportedOperationException("Unknown mode: " + mode);
        }
    }
}
