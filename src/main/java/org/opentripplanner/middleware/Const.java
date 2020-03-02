package org.opentripplanner.middleware;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Const {
    // Play with some HTTP requests
    public static final int numItineraries = 3;
    public static final String[] urls = new String[] {
            // Each request made individually below from OTP MOD UI
            // is in reality two requests, one with and one without realtime updates.

            "https://maps.trimet.org/otp_mod/plan?fromPlace=1610%20SW%20Clifton%20St%2C%20Portland%2C%20OR%2C%20USA%2097201%3A%3A45.51091832390635%2C-122.69433801297359&toPlace=3335%20SE%2010th%20Ave%2C%20Portland%2C%20OR%2C%20USA%2097202%3A%3A45.49912810913339%2C-122.656202229323&mode=WALK%2CBUS%2CTRAM%2CRAIL%2CGONDOLA&showIntermediateStops=true&maxWalkDistance=1207&optimize=QUICK&walkSpeed=1.34&ignoreRealtimeUpdates=true&numItineraries=" + numItineraries,
            "https://maps.trimet.org/otp_mod/plan?fromPlace=1610%20SW%20Clifton%20St%2C%20Portland%2C%20OR%2C%20USA%2097201%3A%3A45.51091832390635%2C-122.69433801297359&toPlace=3335%20SE%2010th%20Ave%2C%20Portland%2C%20OR%2C%20USA%2097202%3A%3A45.49912810913339%2C-122.656202229323&mode=BUS%2CTRAM%2CRAIL%2CGONDOLA%2CBICYCLE&showIntermediateStops=true&maxWalkDistance=4828&maxBikeDistance=4828&optimize=SAFE&bikeSpeed=3.58&ignoreRealtimeUpdates=true&numItineraries=" + numItineraries,
            "https://maps.trimet.org/otp_mod/plan?fromPlace=1610%20SW%20Clifton%20St%2C%20Portland%2C%20OR%2C%20USA%2097201%3A%3A45.51091832390635%2C-122.69433801297359&toPlace=3335%20SE%2010th%20Ave%2C%20Portland%2C%20OR%2C%20USA%2097202%3A%3A45.49912810913339%2C-122.656202229323&mode=BUS%2CTRAM%2CRAIL%2CGONDOLA%2CBICYCLE_RENT&showIntermediateStops=true&maxWalkDistance=4828&maxBikeDistance=4828&optimize=SAFE&bikeSpeed=3.58&ignoreRealtimeUpdates=true&companies=BIKETOWN&numItineraries=" + numItineraries,
            "https://maps.trimet.org/otp_mod/plan?fromPlace=1610%20SW%20Clifton%20St%2C%20Portland%2C%20OR%2C%20USA%2097201%3A%3A45.51091832390635%2C-122.69433801297359&toPlace=3335%20SE%2010th%20Ave%2C%20Portland%2C%20OR%2C%20USA%2097202%3A%3A45.49912810913339%2C-122.656202229323&mode=BUS%2CTRAM%2CRAIL%2CGONDOLA%2CMICROMOBILITY_RENT&showIntermediateStops=true&optimize=QUICK&maxWalkDistance=4828&maxEScooterDistance=4828&ignoreRealtimeUpdates=true&companies=BIRD%2CLIME%2CRAZOR%2CSHARED%2CSPIN&numItineraries=" + numItineraries,
            "https://maps.trimet.org/otp_mod/plan?fromPlace=1610%20SW%20Clifton%20St%2C%20Portland%2C%20OR%2C%20USA%2097201%3A%3A45.51091832390635%2C-122.69433801297359&toPlace=3335%20SE%2010th%20Ave%2C%20Portland%2C%20OR%2C%20USA%2097202%3A%3A45.49912810913339%2C-122.656202229323&mode=BUS%2CTRAM%2CRAIL%2CGONDOLA%2CCAR_PARK%2CWALK&showIntermediateStops=true&optimize=QUICK&ignoreRealtimeUpdates=true&numItineraries=" + numItineraries,
            "https://maps.trimet.org/otp_mod/plan?fromPlace=1610%20SW%20Clifton%20St%2C%20Portland%2C%20OR%2C%20USA%2097201%3A%3A45.51091832390635%2C-122.69433801297359&toPlace=3335%20SE%2010th%20Ave%2C%20Portland%2C%20OR%2C%20USA%2097202%3A%3A45.49912810913339%2C-122.656202229323&mode=BUS%2CTRAM%2CRAIL%2CGONDOLA%2CCAR_HAIL%2CWALK&showIntermediateStops=true&optimize=QUICK&ignoreRealtimeUpdates=true&companies=UBER&minTransitDistance=50%25&searchTimeout=10000&numItineraries=" + numItineraries,
            "https://maps.trimet.org/otp_mod/plan?fromPlace=1610%20SW%20Clifton%20St%2C%20Portland%2C%20OR%2C%20USA%2097201%3A%3A45.51091832390635%2C-122.69433801297359&toPlace=3335%20SE%2010th%20Ave%2C%20Portland%2C%20OR%2C%20USA%2097202%3A%3A45.49912810913339%2C-122.656202229323&mode=WALK&showIntermediateStops=true&walkSpeed=1.34&ignoreRealtimeUpdates=true&companies=UBER&numItineraries=" + numItineraries,
            "https://maps.trimet.org/otp_mod/plan?fromPlace=1610%20SW%20Clifton%20St%2C%20Portland%2C%20OR%2C%20USA%2097201%3A%3A45.51091832390635%2C-122.69433801297359&toPlace=3335%20SE%2010th%20Ave%2C%20Portland%2C%20OR%2C%20USA%2097202%3A%3A45.49912810913339%2C-122.656202229323&mode=BICYCLE&showIntermediateStops=true&optimize=SAFE&bikeSpeed=3.58&ignoreRealtimeUpdates=true&companies=UBER&numItineraries=" + numItineraries,
            "https://maps.trimet.org/otp_mod/plan?fromPlace=1610%20SW%20Clifton%20St%2C%20Portland%2C%20OR%2C%20USA%2097201%3A%3A45.51091832390635%2C-122.69433801297359&toPlace=3335%20SE%2010th%20Ave%2C%20Portland%2C%20OR%2C%20USA%2097202%3A%3A45.49912810913339%2C-122.656202229323&mode=BICYCLE_RENT&showIntermediateStops=true&optimize=SAFE&bikeSpeed=3.58&ignoreRealtimeUpdates=true&companies=UBER&numItineraries=" + numItineraries,
    };
}
