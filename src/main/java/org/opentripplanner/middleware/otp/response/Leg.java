package org.opentripplanner.middleware.otp.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.opentripplanner.middleware.utils.DateTimeUtils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Plan response, itinerary leg information. Produced using http://www.jsonschema2pojo.org/
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Leg implements Cloneable {

    public Date startTime;
    public Date endTime;
    public Integer departureDelay;
    public Integer arrivalDelay;
    public Boolean realTime;
    public Double distance;
    public Boolean pathway;
    public String mode;
    public String route;
    public Boolean interlineWithPreviousLeg;
    public Place from;
    public Place to;
    public EncodedPolyline legGeometry;
    public Boolean rentedBike;
    public Boolean rentedCar;
    public Boolean rentedVehicle;
    public Boolean hailedCar;
    public Boolean transitLeg;
    public Double duration;
    public List<Place> intermediateStops = null;
    public List<Step> steps = null;
    public String agencyName;
    public String agencyUrl;
    public Integer routeType;
    public String routeId;
    public String agencyId;
    public String tripId;
    public String serviceDate;
    public List<EncodedPolyline> interStopGeometry = null;
    public String routeShortName;
    public String routeLongName;
    public List<LocalizedAlert> alerts = null;
    public String headsign;

    @JsonIgnore
    @BsonIgnore
    public ZonedDateTime getScheduledStartTime() {
        return ZonedDateTime.ofInstant(
            startTime.toInstant().minusSeconds(departureDelay),
            DateTimeUtils.getOtpZoneId()
        );
    }

    @JsonIgnore
    @BsonIgnore
    public ZonedDateTime getScheduledEndTime() {
        return ZonedDateTime.ofInstant(
            endTime.toInstant().minusSeconds(arrivalDelay),
            DateTimeUtils.getOtpZoneId()
        );
    }

    @Override
    protected Leg clone() throws CloneNotSupportedException {
        Leg cloned = (Leg) super.clone();
        cloned.from = this.from.clone();
        cloned.to = this.to.clone();
        cloned.steps = new ArrayList<>();
        for (Step step : this.steps) {
            cloned.steps.add(step.clone());
        }
        cloned.legGeometry = this.legGeometry.clone();
        return cloned;
    }
}
