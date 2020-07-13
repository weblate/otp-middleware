package org.opentripplanner.middleware.controllers.api;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.apigateway.AmazonApiGateway;
import com.amazonaws.services.apigateway.AmazonApiGatewayClient;
import com.amazonaws.services.apigateway.AmazonApiGatewayClientBuilder;
import com.amazonaws.services.apigateway.model.GetUsagePlansRequest;
import com.amazonaws.services.apigateway.model.GetUsagePlansResult;
import com.amazonaws.services.apigateway.model.GetUsageRequest;
import com.amazonaws.services.apigateway.model.GetUsageResult;
import com.amazonaws.services.apigateway.model.UsagePlan;
import org.eclipse.jetty.http.HttpStatus;
import org.opentripplanner.middleware.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.opentripplanner.middleware.spark.Main.getConfigPropertyAsText;
import static org.opentripplanner.middleware.spark.Main.hasConfigProperty;
import static org.opentripplanner.middleware.spark.Main.inTestEnvironment;
import static org.opentripplanner.middleware.utils.JsonUtils.logMessageAndHalt;

/**
 * @schema LogResult
 * description: Data structure for log response. The data is based on https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/AmazonWebServiceResult.html, with items having the fields outlined in https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/apigateway/model/GetUsageResult.html.
 * type: object
 */

/**
 * Sets up HTTP endpoints for getting logging and request summary information from AWS Cloudwatch and API Gateway.
 */
public class LogController {
    private static final Logger LOG = LoggerFactory.getLogger(LogController.class);
    private static AmazonApiGateway gateway;

    /**
     * Register http endpoints with {@link spark.Spark} instance at the provided API prefix.
     */
    public static void register (Service spark, String apiPrefix) {
        AmazonApiGatewayClientBuilder gatewayBuilder = AmazonApiGatewayClient.builder();
        if (hasConfigProperty("AWS_PROFILE")) {
            gatewayBuilder.withCredentials(new ProfileCredentialsProvider(getConfigPropertyAsText("AWS_PROFILE")));
        }
        if (!inTestEnvironment) {
            // FIXME Only build API Gateway client if not testing to avoid Travis issues with AWS credentials.
            gateway = gatewayBuilder.build();
        } else {
            LOG.warn("Not building API Gateway client.");
        }

        /**
         * @api [get] /api/secure/logs
         * tags:
         * - "api/secure/logs"
         * description: "Gets a list of recent logs."
         * parameters:
         * - $ref: '#/components/parameters/AWSAuthHeaderRequired'
         * security:
         * - ApiKey: [] # Required for AWS integration.
         *   Auth0Bearer: []
         * responses:
         *   200:
         *     description: "successful operation"
         *     content:
         *       application/json:
         *         schema:
         *           type: array
         *           items:
         *              schema:
         *                $ref: "#/components/schemas/LogResult"
         *
         *     headers:
         *       Access-Control-Allow-Origin:
         *         schema:
         *           type: "string"
         * x-amazon-apigateway-integration:
         *   uri: "http://54.147.40.127/api/secure/logs"
         *   responses:
         *     default:
         *       statusCode: "200"
         *       responseParameters:
         *         method.response.header.Access-Control-Allow-Origin: "'*'"
         *   requestParameters:
         *     integration.request.header.Authorization: "method.request.header.Authorization"
         *   passthroughBehavior: "when_no_match"
         *   httpMethod: "GET"
         *   type: "http"
         *
         */

        /**
         * @api [options] /api/secure/logs
         * responses:
         *   200:
         *     $ref: "#/components/responses/AllowCORS"
         * tags:
         *  - "api/secure/logs"
         * x-amazon-apigateway-integration:
         *   uri: "http://54.147.40.127/api/secure/logs"
         *   responses:
         *     default:
         *       statusCode: 200
         *       responseParameters:
         *         method.response.header.Access-Control-Allow-Methods: "'GET,OPTIONS'"
         *         method.response.header.Access-Control-Allow-Headers: "'Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token'"
         *         method.response.header.Access-Control-Allow-Origin: "'*'"
         *   passthroughBehavior: when_no_match
         *   httpMethod: OPTIONS
         *   type: http
         */
        spark.get(apiPrefix + "/secure/logs", LogController::getUsageLogs, JsonUtils::toJson);
    }

    /**
     * HTTP endpoint to return the usage (number of requests made/requests remaining) for the AWS API Gateway usage
     * plans. Defaults to the last 30 days for all API keys in the AWS account.
     */
    public static List<GetUsageResult> getUsageLogs(Request req, Response res) {
        // keyId param is optional (if not provided, all API keys will be included in response).
        String keyId = req.queryParamOrDefault("keyId", null);
        LocalDateTime now = LocalDateTime.now();
        // TODO: Future work might modify this so that we accept multiple API key IDs for a single request (depends on
        //  how third party developer accounts are structured).
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String startDate = req.queryParamOrDefault("startDate", formatter.format(now.minusDays(30)));
        String endDate = req.queryParamOrDefault("endDate", formatter.format(now));
        GetUsagePlansRequest getUsagePlansRequest = new GetUsagePlansRequest();
        GetUsagePlansResult usagePlansResult = gateway.getUsagePlans(getUsagePlansRequest);
        List<GetUsageResult> usageResults = new ArrayList<>();
        for (UsagePlan usagePlan : usagePlansResult.getItems()) {
            GetUsageRequest getUsageRequest = new GetUsageRequest()
                // TODO: Once third party dev accounts are fleshed out, this query param might go away?
                .withKeyId(keyId)
                .withStartDate(startDate)
                .withEndDate(endDate)
                .withUsagePlanId(usagePlan.getId());
            try {
                usageResults.add(gateway.getUsage(getUsageRequest));
            } catch (Exception e) {
                // Catch any issues with bad request parameters (e.g., invalid API keyId or bad date format).
                logMessageAndHalt(req, HttpStatus.BAD_REQUEST_400, "Error requesting usage results", e);
            }
        }
        return usageResults;
    }
}
