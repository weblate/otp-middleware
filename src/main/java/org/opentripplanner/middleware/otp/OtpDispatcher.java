package org.opentripplanner.middleware.otp;

import org.eclipse.jetty.http.HttpMethod;
import org.opentripplanner.middleware.utils.HttpResponseValues;
import org.opentripplanner.middleware.utils.HttpUtils;
import org.opentripplanner.middleware.utils.ItineraryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static org.opentripplanner.middleware.utils.ConfigUtils.getConfigPropertyAsText;

/**
 * Responsible for constructing requests to an elected OTP server endpoint using original query parameters provided by
 * the requester. To provide the response from the OTP server in the form of status code and body so the correct
 * response can be provided to the requester.
 */
public class OtpDispatcher {

    public enum OtpVersion {
        OTP1("OTP_API_ROOT", "OTP 1"),
        OTP2("OTP2_API_ROOT", "OTP 2");

        private final String uri;
        private final String name;

        OtpVersion(String configName, String name) {
            uri = getConfigPropertyAsText(configName);
            this.name = name;
        }
        /**
         * URI location of the OpenTripPlanner API (e.g., https://otp-server.com/otp). Requests sent to this URI should
         * return OTP version info.
         */
        public String uri() {
            return uri;
        }
        public String toString() { return name; }
    }

    private static final Logger LOG = LoggerFactory.getLogger(OtpDispatcher.class);

    /**
     * Location of the OTP plan endpoint (e.g. /routers/default/plan).
     */
    public static final String OTP_PLAN_ENDPOINT = getConfigPropertyAsText("OTP_PLAN_ENDPOINT", "/routers/default/plan");

    private static final int OTP_SERVER_REQUEST_TIMEOUT_IN_SECONDS = 10;

    /**
     * Provides a response from the OTP server target service based on the query parameters provided.
     */
    public static OtpDispatcherResponse sendOtpRequest(OtpVersion version, String query, String path) {
        LOG.debug("Original query string: {}", query);
        return sendOtpRequest(buildOtpUri(version, query, path));
    }

    /**
     * Provides a response from the OTP server target service based on the query parameters provided.
     */
    public static OtpDispatcherResponse sendOtpPlanRequest(OtpVersion version, String query) {
        LOG.debug("Original query string: {}", query);
        return sendOtpRequest(buildOtpUri(version, query, OTP_PLAN_ENDPOINT));
    }

    /**
     * Provides a response from the OTP server target service based on the input {@link OtpRequest}.
     */
    public static OtpDispatcherResponse sendOtpPlanRequest(OtpVersion version, OtpRequest otpRequest) {
        return sendOtpPlanRequest(version, ItineraryUtils.toQueryString(otpRequest.requestParameters));
    }

    /**
     * Provides a response from the OTP server target service based on the query parameters provided. This is used only
     * during testing.
     */
    public static OtpDispatcherResponse sendOtpPlanRequest(OtpVersion version, String from, String to, String time) {
        return sendOtpPlanRequest(version, String.format("fromPlace=%s&toPlace=%s&time=%s", from, to, time));
    }

    /**
     * Constructs a URL based on the otp server URL and the requester's target service (e.g. plan) and query
     * parameters.
     */
    private static URI buildOtpUri(OtpVersion version, String params, String path) {
        UriBuilder uriBuilder = UriBuilder.fromUri(version.uri())
            .path(path)
            .replaceQuery(params);
        URI uri = URI.create(uriBuilder.toString());
        LOG.debug("Constructed URI: {}", uri);
        return uri;
    }

    /**
     * Makes a call to the OTP server end point. The original response and status are wrapped in a single object and
     * returned. It will fail if a connection is not made.
     */
    private static OtpDispatcherResponse sendOtpRequest(URI uri) {
        LOG.info("Sending request to OTP: {}", uri.toString());
        HttpResponseValues otpResponse =
            HttpUtils.httpRequestRawResponse(
                uri,
                OTP_SERVER_REQUEST_TIMEOUT_IN_SECONDS,
                HttpMethod.GET,
                null,
                null);
        return new OtpDispatcherResponse(otpResponse);
    }
}
