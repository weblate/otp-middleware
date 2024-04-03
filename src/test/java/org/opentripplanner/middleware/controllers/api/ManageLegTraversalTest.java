package org.opentripplanner.middleware.controllers.api;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.middleware.models.TrackedJourney;
import org.opentripplanner.middleware.otp.response.Itinerary;
import org.opentripplanner.middleware.otp.response.Leg;
import org.opentripplanner.middleware.otp.response.Step;
import org.opentripplanner.middleware.testutils.CommonTestUtils;
import org.opentripplanner.middleware.triptracker.AlignedStep;
import org.opentripplanner.middleware.triptracker.LegSegment;
import org.opentripplanner.middleware.triptracker.ManageLegTraversal;
import org.opentripplanner.middleware.triptracker.StepSegment;
import org.opentripplanner.middleware.triptracker.TrackingLocation;
import org.opentripplanner.middleware.triptracker.TravelerPosition;
import org.opentripplanner.middleware.triptracker.TripInstruction;
import org.opentripplanner.middleware.triptracker.TripStatus;
import org.opentripplanner.middleware.utils.Coordinates;
import org.opentripplanner.middleware.utils.DateTimeUtils;
import org.opentripplanner.middleware.utils.JsonUtils;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.middleware.triptracker.ManageLegTraversal.getSecondsToMilliseconds;
import static org.opentripplanner.middleware.triptracker.ManageLegTraversal.interpolatePoints;
import static org.opentripplanner.middleware.triptracker.TripInstruction.NO_INSTRUCTION;
import static org.opentripplanner.middleware.triptracker.TripInstruction.alignPositionToStep;
import static org.opentripplanner.middleware.triptracker.TripInstruction.buildInstruction;
import static org.opentripplanner.middleware.triptracker.TripInstruction.getStepSegments;
import static org.opentripplanner.middleware.triptracker.TripStatus.getSegmentTimeInterval;
import static org.opentripplanner.middleware.utils.GeometryUtils.calculateBearing;
import static org.opentripplanner.middleware.utils.GeometryUtils.createDestinationPoint;

public class ManageLegTraversalTest {

    private static Itinerary busStopToJusticeCenterItinerary;

    @BeforeAll
    public static void setUp() throws IOException {
        busStopToJusticeCenterItinerary = JsonUtils.getPOJOFromJSON(
            CommonTestUtils.getTestResourceAsString("controllers/api/bus-stop-justice-center-trip.json"),
            Itinerary.class
        );
    }

    @ParameterizedTest
    @MethodSource("createTrace")
    void canTrackTrip(String time, double lat, double lon, TripStatus expected) {
        TrackedJourney trackedJourney = new TrackedJourney();
        TrackingLocation trackingLocation = new TrackingLocation(time, lat, lon);
        trackedJourney.locations = List.of(trackingLocation);
        TravelerPosition travelerPosition = new TravelerPosition(trackedJourney, busStopToJusticeCenterItinerary);
        TripStatus tripStatus = TripStatus.getTripStatus(travelerPosition);
        assertEquals(expected, tripStatus);
    }

    private static Stream<Arguments> createTrace() {
        Date startTime = busStopToJusticeCenterItinerary.startTime;
        List<LegSegment> legSegments = createSegmentsForLeg();
        LegSegment before = legSegments.get(8);
        LegSegment current = legSegments.get(10);
        LegSegment after = legSegments.get(12);
        return Stream.of(
            Arguments.of(
                getDateTimeAsString(startTime, getSegmentTimeInterval(before)),
                current.start.lat,
                current.start.lon,
                TripStatus.AHEAD_OF_SCHEDULE
            ),
            Arguments.of(
                getDateTimeAsString(startTime, current.cumulativeTime),
                current.start.lat,
                current.start.lon,
                TripStatus.ON_SCHEDULE
            ),
            Arguments.of(
                getDateTimeAsString(startTime, after.cumulativeTime),
                current.start.lat,
                current.start.lon,
                TripStatus.BEHIND_SCHEDULE
            ),
            // Slight deviation on time.
            Arguments.of(
                getDateTimeAsString(startTime, current.cumulativeTime - 4),
                current.start.lat,
                current.start.lon,
                TripStatus.ON_SCHEDULE
            ),
            // Slight deviation on time.
            Arguments.of(
                getDateTimeAsString(startTime, (current.cumulativeTime - current.timeInSegment) + 4),
                current.start.lat,
                current.start.lon,
                TripStatus.ON_SCHEDULE
            ),
            // Slight deviation on lat/lon.
            Arguments.of(
                getDateTimeAsString(startTime, current.cumulativeTime),
                current.start.lat + 0.00001,
                current.start.lon + 0.00001,
                TripStatus.ON_SCHEDULE
            ),
            // Time which can not be attributed to a trip leg.
            Arguments.of(
                getDateTimeAsString(busStopToJusticeCenterItinerary.endTime, 1),
                current.start.lat,
                current.start.lon,
                TripStatus.NO_STATUS
            ),
            // Arbitrary lat/lon values which aren't on the trip.
            Arguments.of(
                getDateTimeAsString(startTime, 0),
                33.95029,
                -83.99,
                TripStatus.DEVIATED
            )
        );
    }

    @ParameterizedTest
    @MethodSource("createTurnByTurnTrace")
    void canTrackTurnByTurn(LegSegment activeLegSegment, String expectedInstruction) {
        TravelerPosition travelerPosition = new TravelerPosition();
        travelerPosition.expectedLeg = busStopToJusticeCenterItinerary.legs.get(0);
        travelerPosition.legSegmentFromTime = activeLegSegment;
        travelerPosition.currentPosition = new Coordinates(busStopToJusticeCenterItinerary.legs.get(0).from);
        assertEquals(expectedInstruction, buildInstruction(alignPositionToStep(travelerPosition)));
    }

    private static Stream<Arguments> createTurnByTurnTrace() {
        Leg walkLeg = busStopToJusticeCenterItinerary.legs.get(0);
        List<Step> walkSteps = walkLeg.steps;
        Step lastStep = walkSteps.get(walkSteps.size()-1);
        List<StepSegment> stepSegments = getStepSegments(walkLeg);
        StepSegment firstStepSegment = stepSegments.get(0);
        StepSegment secondStep = stepSegments.get(1);
        StepSegment thirdStep = stepSegments.get(2);
        StepSegment fourthStep = stepSegments.get(3);
        StepSegment lastStepSegment = new StepSegment(
            new Coordinates(lastStep),
            new Coordinates(lastStep),
            -1
        );
        double firstStepBearing = calculateBearing(firstStepSegment.start, firstStepSegment.end);
        double secondStepBearing = calculateBearing(secondStep.start, secondStep.end);
        double fourthStepBearing = calculateBearing(fourthStep.start, fourthStep.end);
        return Stream.of(
            // On step change.
            Arguments.of(new LegSegment(
                firstStepSegment.start, firstStepSegment.end),
                TripInstruction.buildInstruction(new AlignedStep(0, walkSteps.get(1))
            )),
            Arguments.of(new LegSegment(
                secondStep.start, secondStep.end),
                TripInstruction.buildInstruction(new AlignedStep(0, walkSteps.get(2))
            )),
            Arguments.of(new LegSegment(
                thirdStep.start, thirdStep.end),
                TripInstruction.buildInstruction(new AlignedStep(0, walkSteps.get(3))
            )),
            Arguments.of(new LegSegment(
                fourthStep.start, fourthStep.end),
                TripInstruction.buildInstruction(new AlignedStep(0, walkSteps.get(4))
            )),
            // At start of trip.
            Arguments.of(createLegSegment(
                firstStepSegment.start, 0, firstStepBearing, true),
                TripInstruction.buildInstruction(new AlignedStep(1, walkSteps.get(0))
            )),
            // Pass first step.
            Arguments.of(createLegSegment(
                firstStepSegment.end, 1, firstStepBearing, false),
                TripInstruction.buildInstruction(new AlignedStep(9, walkSteps.get(2))
            )),
            // Pass last step.
            Arguments.of(createLegSegment(
                lastStepSegment.start, 10, 315, false),
                NO_INSTRUCTION
            ),
            // Approaching last step (at 90 degrees to reduce confidence).
            Arguments.of(createLegSegment(
                fourthStep.end, 2, fourthStepBearing + 90, true),
                TripInstruction.buildInstruction(new AlignedStep(8, walkSteps.get(4))
            )),
            // Approaching nearest step.
            Arguments.of(createLegSegment(
                secondStep.end, 3, secondStepBearing, true),
                TripInstruction.buildInstruction(new AlignedStep(2, walkSteps.get(1))
            ))
        );
    }

    private static LegSegment createLegSegment(
        Coordinates point,
        double distanceInMeters,
        double bearing,
        boolean oppositeDirection
    ) {
        if (oppositeDirection) {
            bearing = bearing - 180;
        }
        Coordinates end = createDestinationPoint(point, distanceInMeters, bearing);
        Coordinates start = createDestinationPoint(point, distanceInMeters + 5, bearing);
        return new LegSegment(start, end);
    }

    @Test
    void canAccumulateCorrectStartAndEndCoordinates() {
        List<LegSegment> legSegments = createSegmentsForLeg();
        for (int i = 0; i < legSegments.size()-1; i++) {
            LegSegment legSegmentOne = legSegments.get(i);
            LegSegment legSegmentTwo = legSegments.get(i+1);
            assertEquals(legSegmentOne.end.lat, legSegmentTwo.start.lat);
        }
    }

    @Test
    void canTrackLegWithoutDeviating() {
        for (int legIndex = 0; legIndex < busStopToJusticeCenterItinerary.legs.size(); legIndex++) {
            List<LegSegment> legSegments = createSegmentsForLeg();
            TrackedJourney trackedJourney = new TrackedJourney();
            ZonedDateTime startOfTrip = ZonedDateTime.ofInstant(
                busStopToJusticeCenterItinerary.legs.get(legIndex).startTime.toInstant(),
                DateTimeUtils.getOtpZoneId()
            );

            ZonedDateTime currentTime = startOfTrip;
            double cumulativeTravelTime = 0;
            for (LegSegment legSegment : legSegments) {
                trackedJourney.locations = List.of(
                    new TrackingLocation(
                        legSegment.start.lat,
                        legSegment.start.lon,
                        new Date(currentTime.toInstant().toEpochMilli())
                    )
                );
                TravelerPosition travelerPosition = new TravelerPosition(trackedJourney, busStopToJusticeCenterItinerary);
                assertEquals(
                    TripStatus.getTripStatus(travelerPosition).name(),
                    TripStatus.ON_SCHEDULE.name()
                );
                cumulativeTravelTime += legSegment.timeInSegment;
                currentTime = startOfTrip.plus(getSecondsToMilliseconds(cumulativeTravelTime), ChronoUnit.MILLIS);
            }
        }
    }

    @Test
    void cumulativeSegmentTimeMatchesWalkLegDuration() {
        List<LegSegment> legSegments = createSegmentsForLeg();
        double cumulative = 0;
        for (LegSegment legSegment : legSegments) {
            cumulative += legSegment.timeInSegment;
        }
        assertEquals(busStopToJusticeCenterItinerary.legs.get(0).duration, cumulative, 0.01f);
    }

    @ParameterizedTest
    @MethodSource("createTravelerPositions")
    void canReturnTheCorrectSegmentCoordinates(TravellerPosition segmentPosition) {
        LegSegment legSegment = ManageLegTraversal.getSegmentFromTime(
            segmentPosition.start,
            segmentPosition.currentTime,
            segmentPosition.legSegments
        );
        assertNotNull(legSegment);
        assertEquals(segmentPosition.coordinates, legSegment.start);
    }

    private static Stream<TravellerPosition> createTravelerPositions() {
        Instant segmentStartTime = ZonedDateTime.now().toInstant();
        List<LegSegment> legSegments = createSegmentsForLeg();

        return Stream.of(
            new TravellerPosition(
                legSegments.get(0).start,
                segmentStartTime,
                segmentStartTime.plusSeconds(5),
                legSegments
            ),
            new TravellerPosition(
                legSegments.get(1).start,
                segmentStartTime,
                segmentStartTime.plusSeconds(15),
                legSegments
            ),
            new TravellerPosition(
                legSegments.get(2).start,
                segmentStartTime,
                segmentStartTime.plusSeconds(25),
                legSegments
            ),
            new TravellerPosition(
                legSegments.get(3).start,
                segmentStartTime,
                segmentStartTime.plusSeconds(35),
                legSegments
            )
        );
    }

    private static class TravellerPosition {

        public Coordinates coordinates;

        public Instant start;

        public Instant currentTime;

        List<LegSegment> legSegments;

        public TravellerPosition(
            Coordinates coordinates,
            Instant start,
            Instant currentTime,
            List<LegSegment> legSegments
        ) {
            this.coordinates = coordinates;
            this.start = start;
            this.currentTime = currentTime;
            this.legSegments = legSegments;
        }
    }

    private static List<LegSegment> createSegmentsForLeg() {
        return interpolatePoints(busStopToJusticeCenterItinerary.legs.get(0));
    }

    private static String getDateTimeAsString(Date date, double offset) {
        Instant dateTime = date.toInstant().plusSeconds((long) offset);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.systemDefault());;
        return formatter.format(dateTime);
    }
}
