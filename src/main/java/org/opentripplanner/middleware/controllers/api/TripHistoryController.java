package org.opentripplanner.middleware.controllers.api;

import com.beerboy.ss.SparkSwagger;
import com.beerboy.ss.rest.Endpoint;
import org.bson.conversions.Bson;
import org.eclipse.jetty.http.HttpStatus;
import org.opentripplanner.middleware.controllers.response.ResponseList;
import org.opentripplanner.middleware.models.TripRequest;
import org.opentripplanner.middleware.persistence.Persistence;
import org.opentripplanner.middleware.utils.DateTimeUtils;
import org.opentripplanner.middleware.utils.HttpUtils;
import org.opentripplanner.middleware.utils.JsonUtils;
import spark.Request;
import spark.Response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Date;

import static com.beerboy.ss.descriptor.EndpointDescriptor.endpointPath;
import static com.beerboy.ss.descriptor.MethodDescriptor.path;
import static org.opentripplanner.middleware.auth.Auth0Connection.isAuthorized;
import static org.opentripplanner.middleware.controllers.api.ApiController.DEFAULT_LIMIT;
import static org.opentripplanner.middleware.controllers.api.ApiController.DEFAULT_PAGE;
import static org.opentripplanner.middleware.controllers.api.ApiController.LIMIT;
import static org.opentripplanner.middleware.controllers.api.ApiController.LIMIT_PARAM;
import static org.opentripplanner.middleware.controllers.api.ApiController.PAGE;
import static org.opentripplanner.middleware.controllers.api.ApiController.PAGE_PARAM;
import static org.opentripplanner.middleware.persistence.TypedPersistence.filterByUserAndDateRange;
import static org.opentripplanner.middleware.utils.DateTimeUtils.YYYY_MM_DD;
import static org.opentripplanner.middleware.utils.HttpUtils.JSON_ONLY;
import static org.opentripplanner.middleware.utils.HttpUtils.getQueryParamFromRequest;
import static org.opentripplanner.middleware.utils.JsonUtils.logMessageAndHalt;

/**
 * Responsible for processing trip history related requests provided by MOD UI.
 * To provide a response to the calling MOD UI in JSON based on the passed in parameters.
 */
public class TripHistoryController implements Endpoint {
    private static final String FROM_DATE_PARAM = "fromDate";
    private static final String TO_DATE_PARAM = "toDate";
    private final String ROOT_ROUTE;

    public TripHistoryController(String apiPrefix) {
        this.ROOT_ROUTE = apiPrefix + "secure/triprequests";
    }

    /**
     * Register the API endpoint and GET resource to get trip requests
     * when spark-swagger calls this function with the target API instance.
     */
    @Override
    public void bind(final SparkSwagger restApi) {
       restApi.endpoint(
            endpointPath(ROOT_ROUTE).withDescription("Interface for retrieving trip requests."),
           HttpUtils.NO_FILTER
       ).get(
           path(ROOT_ROUTE)
                .withDescription("Gets a list of all trip requests for a user.")
                .withQueryParam()
                    .withName("userId")
                    .withRequired(true)
                    .withDescription("The OTP user for which to retrieve trip requests.").and()
               .withQueryParam(LIMIT)
               .withQueryParam(PAGE)
               .withQueryParam()
                    .withName(FROM_DATE_PARAM)
                    .withPattern(YYYY_MM_DD)
                    .withDefaultValue("The current date")
                    .withDescription(String.format(
                        "If specified, the earliest date (format %s) for which trip requests are retrieved.", YYYY_MM_DD
                    )).and()
               .withQueryParam()
               .withName(TO_DATE_PARAM)
                    .withPattern(YYYY_MM_DD)
                    .withDefaultValue("The current date")
                    .withDescription(String.format(
                        "If specified, the latest date (format %s) for which trip requests are retrieved.", YYYY_MM_DD
                    )).and()
               .withProduces(JSON_ONLY)
               // Note: unlike the name suggests, withResponseAsCollection does not generate an array
               // as the return type for this method. (It does generate the type for that class nonetheless.)
               .withResponseAsCollection(TripRequest.class),
           TripHistoryController::getTripRequests, JsonUtils::toJson);
    }

    /**
     * Return a user's trip request history based on provided parameters.
     * An authorized user (Auth0) and user id are required.
     */
    private static ResponseList<TripRequest> getTripRequests(Request request, Response response) {
        final String userId = getQueryParamFromRequest(request, "userId", false);
        // Check that the user is authorized (otherwise a halt is thrown).
        isAuthorized(userId, request);
        // Get params from request (or use defaults).
        int limit = getQueryParamFromRequest(request, LIMIT_PARAM, true, 0, DEFAULT_LIMIT, 100);
        int page = getQueryParamFromRequest(request, PAGE_PARAM, true, 0, DEFAULT_PAGE);
        String paramFromDate = getQueryParamFromRequest(request, FROM_DATE_PARAM, true);
        Date fromDate = getDate(request, FROM_DATE_PARAM, paramFromDate, LocalTime.MIDNIGHT);
        String paramToDate = getQueryParamFromRequest(request, TO_DATE_PARAM, true);
        Date toDate = getDate(request, TO_DATE_PARAM, paramToDate, LocalTime.MAX);
        // Throw halt if the date params are bad.
        if (fromDate != null && toDate != null && toDate.before(fromDate)) {
            logMessageAndHalt(request, HttpStatus.BAD_REQUEST_400,
                String.format("%s (%s) before %s (%s)", TO_DATE_PARAM, paramToDate, FROM_DATE_PARAM,
                    paramFromDate));
        }
        Bson filter = filterByUserAndDateRange(userId, fromDate, toDate);
        return Persistence.tripRequests.getResponseList(filter, page, limit);
    }

    /**
     * Get date from request parameter and convert to {@link Date} at a specific time of day. The date conversion
     * is based on the system time zone.
     */
    private static Date getDate(Request request, String paramName, String paramValue, LocalTime timeOfDay) {

        // no date value to work with
        if (paramValue == null) {
            return null;
        }

        LocalDate localDate = null;
        try {
            localDate = DateTimeUtils.getDateFromParam(paramName, paramValue, YYYY_MM_DD);
        } catch (DateTimeParseException e) {
            logMessageAndHalt(request, HttpStatus.BAD_REQUEST_400,
                String.format("%s value: %s is not a valid date. Must be in the format: %s", paramName, paramValue,
                    YYYY_MM_DD));
        }

        if (localDate == null) {
            return null;
        }

        return Date.from(localDate.atTime(timeOfDay)
            .atZone(DateTimeUtils.getSystemZoneId())
            .toInstant());
    }
}
