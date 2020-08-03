package org.opentripplanner.middleware.controllers.api;

import com.amazonaws.services.apigateway.model.GetUsageResult;
import org.eclipse.jetty.http.HttpStatus;
import org.opentripplanner.middleware.auth.Auth0Connection;
import org.opentripplanner.middleware.auth.Auth0UserProfile;
import org.opentripplanner.middleware.utils.ApiGatewayUtils;
import org.opentripplanner.middleware.utils.JsonUtils;
import spark.Request;
import spark.Response;
import spark.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.opentripplanner.middleware.auth.Auth0Connection.isUserAdmin;
import static org.opentripplanner.middleware.utils.JsonUtils.logMessageAndHalt;

/**
 * Sets up HTTP endpoints for getting logging and request summary information from AWS Cloudwatch and API Gateway.
 */
public class LogController {

    /**
     * Register http endpoints with {@link spark.Spark} instance at the provided API prefix.
     */
    public static void register(Service spark, String apiPrefix) {
        spark.get(apiPrefix + "/secure/logs", LogController::getUsageLogs, JsonUtils::toJson);
    }

    /**
     * HTTP endpoint to return the usage (number of requests made/requests remaining) for the AWS API Gateway usage
     * plans. Defaults to the last 30 days for all API keys in the AWS account.
     */
    private static List<GetUsageResult> getUsageLogs(Request req, Response res) {
        // keyId param is optional (if not provided, all API keys will be included in response).
        List<String> apiKeyIds = Arrays.asList(req.queryParamOrDefault("keyId", "").split(","));
        Auth0UserProfile requestingUser = Auth0Connection.getUserFromRequest(req);
        if (!isUserAdmin(requestingUser)) {
            if (requestingUser.apiUser == null) {
                logMessageAndHalt(req, HttpStatus.FORBIDDEN_403, "Action is not permitted for user.");
            }
            apiKeyIds = requestingUser.apiUser.apiKeyIds;
        }
        LocalDateTime now = LocalDateTime.now();
        // TODO: Future work might modify this so that we accept multiple API key IDs for a single request (depends on
        //  how third party developer accounts are structured).
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String startDate = req.queryParamOrDefault("startDate", formatter.format(now.minusDays(30)));
        String endDate = req.queryParamOrDefault("endDate", formatter.format(now));
        List<GetUsageResult> usageLogsForKey = new ArrayList<>();
        try {
            if (apiKeyIds.size() == 0) {
                return ApiGatewayUtils.getUsageLogsForKey(null, startDate, endDate);
            }
            return ApiGatewayUtils.getUsageLogsForKeys(apiKeyIds, startDate, endDate);
        } catch (Exception e) {
            // Catch any issues with bad request parameters (e.g., invalid API keyId or bad date format).
            logMessageAndHalt(req, HttpStatus.BAD_REQUEST_400, "Error requesting usage results", e);
        }

        return null;
    }
}
