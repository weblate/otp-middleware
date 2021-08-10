package org.opentripplanner.middleware.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.opentripplanner.middleware.bugsnag.BugsnagDispatcher;
import org.opentripplanner.middleware.utils.HttpResponseValues;
import org.opentripplanner.middleware.utils.JsonUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Represents a Bugsnag event request. The class is used for both Mongo storage and JSON deserialization.
 * Information relating to this can be found here:
 * https://bugsnagapiv2.docs.apiary.io/#reference/projects/event-data-requests/create-an-event-data-request
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BugsnagEventRequest extends Model {

    /** Event data request id which is unique to this request */
    @JsonProperty("id")
    public String eventDataRequestId;

    /** The status of the event data request e.g. PREPARING, COMPLETED etc */
    public String status;

    /** The total number of events that are expected to be returned */
    public int total;

    /** How far back the event data request should go. Populates "event.since" in filter. **/
    public int daysInPast;

    /** URL for downloading the report of the requested event data */
    public String url;

    /** Event request project id. This is not provided with the event request response so must be added separately so
     * that subsequent calls can be made. */
    public String projectId;

    /** This no-arg constructor exists to make MongoDB happy. */
    public BugsnagEventRequest() {
    }

    public static BugsnagEventRequest createFromRequest(HttpResponseValues response, String projectId, int daysInPast)
        throws JsonProcessingException
    {
        BugsnagEventRequest bugsnagEventRequest = JsonUtils.getPOJOFromHttpBody(response, BugsnagEventRequest.class);
        if (bugsnagEventRequest != null) {
            // Add the project id for subsequent calls as it is not provided in the response.
            bugsnagEventRequest.projectId = projectId;
            bugsnagEventRequest.daysInPast = daysInPast;
        }
        return bugsnagEventRequest;
    }

    @JsonIgnore
    @BsonIgnore
    public long getTimeWindowEndInMillis() {
        long timeWindowMillis = TimeUnit.MILLISECONDS.convert(this.daysInPast, TimeUnit.DAYS);
        return this.dateCreated.getTime() + timeWindowMillis;
    }

    /**
     * Refresh this event data request using the requestId. This provides a convenient way to check the current status
     * of an older {@link BugsnagEventRequest}.
     */
    public BugsnagEventRequest refreshEventDataRequest() {
        return BugsnagDispatcher.checkEventDataRequest(eventDataRequestId, projectId);
    }

    /**
     * Fetch the associated list of {@link BugsnagEvent} for this data request.
     */
    @JsonIgnore
    @BsonIgnore
    public List<BugsnagEvent> getEventData() {
        return BugsnagDispatcher.getEventData(url);
    }
}
