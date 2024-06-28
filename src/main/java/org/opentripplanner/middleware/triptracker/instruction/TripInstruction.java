package org.opentripplanner.middleware.triptracker.instruction;

import org.opentripplanner.middleware.otp.response.Place;
import org.opentripplanner.middleware.otp.response.Step;

import java.time.Instant;
import java.util.Locale;

import static org.opentripplanner.middleware.utils.ConfigUtils.getConfigPropertyAsInt;

public class TripInstruction {

    public enum TripInstructionType { ON_TRACK }

    /** The radius in meters under which an immediate instruction is given. */
    public static final int TRIP_INSTRUCTION_IMMEDIATE_RADIUS
        = getConfigPropertyAsInt("TRIP_INSTRUCTION_IMMEDIATE_RADIUS", 2);

    /** The radius in meters under which an upcoming instruction is given. */
    public static final int TRIP_INSTRUCTION_UPCOMING_RADIUS
        = getConfigPropertyAsInt("TRIP_INSTRUCTION_UPCOMING_RADIUS", 10);

    /** The prefix to use when at a street location with an instruction. */
    public static final String TRIP_INSTRUCTION_IMMEDIATE_PREFIX = "IMMEDIATE: ";

    /** The prefix to use when nearing a street location with an instruction. */
    public static final String TRIP_INSTRUCTION_UPCOMING_PREFIX = "UPCOMING: ";

    /** The prefix to use when arrived at the destination. */
    public static final String TRIP_INSTRUCTION_ARRIVED_PREFIX = "ARRIVED: ";

    public static final String NO_INSTRUCTION = "NO_INSTRUCTION";

    /** Distance in meters to step instruction or destination. */
    public double distance;

    /** Step aligned with traveler's position. */
    public Step legStep;

    /** Stop/place aligned with traveler's position. */
    public Place place;

    /** Instruction prefix. */
    public String prefix;

    /** Name of final destination or street. */
    public String locationName;

    /** The time provided by the traveler */
    public Instant currentTime;

    /** The type of instruction to be provided to the traveler. */
    private TripInstructionType tripInstructionType;

    /** The traveler's locale. */
    protected Locale locale;

    protected TripInstruction() {
        // For use by subclasses.
    }

    public TripInstruction(boolean isDestination, double distance, Locale locale) {
        this.distance = distance;
        this.tripInstructionType = TripInstructionType.ON_TRACK;
        this.locale = locale;
        setPrefix(isDestination);
    }

    /**
     * If the traveler is within the upcoming radius an instruction will be provided.
     */
    public boolean hasInstruction() {
        return distance <= TRIP_INSTRUCTION_UPCOMING_RADIUS;
    }

    /**
     * On track instruction to step.
     */
    public TripInstruction(double distance, Step legStep, Locale locale) {
        this(false, distance, locale);
        this.legStep = legStep;
    }

    /**
     * On track instruction to destination.
     */
    public TripInstruction(double distance, String locationName, Locale locale) {
        this(true, distance, locale);
        this.locationName = locationName;
    }

    /**
     * The prefix is defined depending on the traveler either approaching a step or destination and the predefined
     * distances from these points.
     */
    private void setPrefix(boolean isDestination) {
        if (distance <= TRIP_INSTRUCTION_IMMEDIATE_RADIUS) {
            prefix = (isDestination) ? TRIP_INSTRUCTION_ARRIVED_PREFIX : TRIP_INSTRUCTION_IMMEDIATE_PREFIX;
        } else if (distance <= TRIP_INSTRUCTION_UPCOMING_RADIUS) {
            prefix = TRIP_INSTRUCTION_UPCOMING_PREFIX;
        }
    }

    /**
     * Build instruction based on the traveler's location.
     */
    public String build() {
        switch (tripInstructionType) {
            case ON_TRACK:
                return buildOnTrackInstruction();
            default:
                return NO_INSTRUCTION;
        }
    }

    /**
     * Build on track instruction based on step instructions and location. e.g.
     * <p>
     * "UPCOMING: CONTINUE on Langley Drive"
     * "IMMEDIATE: RIGHT on service road"
     * "ARRIVED: Gwinnett Justice Center (Central)"
     * <p>
     * TODO: Internationalization and refinements to these generated instructions with input from the mobile app team.
     */

    private String buildOnTrackInstruction() {
        if (hasInstruction()) {
            if (legStep != null) {
                String relativeDirection = (legStep.relativeDirection.equals("DEPART"))
                    ? "Head " + legStep.absoluteDirection
                    : legStep.relativeDirection;
                return String.format("%s%s on %s", prefix, relativeDirection, legStep.streetName);
            } else if (locationName != null) {
                return String.format("%s%s", prefix, locationName);
            }
        }
        return NO_INSTRUCTION;
    }

    /**
     * Get the number of minutes to wait for a bus. If the wait is zero (or less than zero!) return empty string.
     */
    protected String getReadableMinutes(long waitInMinutes) {
        if (waitInMinutes == 1) {
            return String.format(" %s minute", waitInMinutes);
        } else if (waitInMinutes > 1) {
            return String.format(" %s minutes", waitInMinutes);
        }
        return "";
    }
}
