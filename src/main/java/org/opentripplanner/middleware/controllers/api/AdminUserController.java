package org.opentripplanner.middleware.controllers.api;

import com.auth0.exception.Auth0Exception;
import com.auth0.json.mgmt.users.User;
import com.beerboy.ss.ApiEndpoint;
import org.eclipse.jetty.http.HttpStatus;
import org.opentripplanner.middleware.auth.Auth0Connection;
import org.opentripplanner.middleware.auth.Auth0UserProfile;
import org.opentripplanner.middleware.models.AdminUser;
import org.opentripplanner.middleware.models.OtpUser;
import org.opentripplanner.middleware.persistence.Persistence;
import org.opentripplanner.middleware.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import static com.beerboy.ss.descriptor.MethodDescriptor.path;
import static org.opentripplanner.middleware.auth.Auth0Users.createNewAuth0User;
import static org.opentripplanner.middleware.auth.Auth0Users.deleteAuth0User;
import static org.opentripplanner.middleware.auth.Auth0Users.updateAuthFieldsForUser;
import static org.opentripplanner.middleware.auth.Auth0Users.validateExistingUser;
import static org.opentripplanner.middleware.utils.JsonUtils.logMessageAndHalt;

/**
 * @api [get] /api/admin/user
 * tags:
 * - "api/admin/user"
 * description: "Gets a list of all AdminUser entities."
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
 *             $ref: "#/components/schemas/AdminUser"
 *     headers:
 *       Access-Control-Allow-Origin:
 *         schema:
 *           type: "string"
 * x-amazon-apigateway-integration:
 *   uri: "http://54.147.40.127/api/admin/user"
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
 * @api [options] /api/admin/user
 * responses:
 *   200:
 *     $ref: "#/components/responses/AllowCORS"
 * tags:
 *  - "api/admin/user"
 * x-amazon-apigateway-integration:
 *   uri: "http://54.147.40.127/api/admin/user"
 *   responses:
 *     default:
 *       statusCode: 200
 *       responseParameters:
 *         method.response.header.Access-Control-Allow-Methods: "'GET,POST,OPTIONS'"
 *         method.response.header.Access-Control-Allow-Headers: "'Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token'"
 *         method.response.header.Access-Control-Allow-Origin: "'*'"
 *   passthroughBehavior: when_no_match
 *   httpMethod: OPTIONS
 *   type: http
 */

/**
 * @api [post] /api/admin/user
 * tags:
 * - "api/admin/user"
 * description: "Creates a new AdminUser entity."
 * parameters:
 * - $ref: '#/components/parameters/AWSAuthHeaderRequired'
 * security:
 * - ApiKey: [] # Required for AWS integration.
 *   Auth0Bearer: []
 * requestBody:
 *   $ref: "#/components/requestBodies/AdminUser"
 * responses:
 *   200:
 *     $ref: "#/components/responses/AdminUser"
 *   400:
 *     description: "400 response"
 *     headers:
 *       Access-Control-Allow-Origin:
 *         schema:
 *           type: "string"
 *   500:
 *     description: "500 response"
 *     headers:
 *       Access-Control-Allow-Origin:
 *         schema:
 *           type: "string"
 *   401:
 *     description: "401 response"
 *     headers:
 *       Access-Control-Allow-Origin:
 *         schema:
 *           type: "string"
 * x-amazon-apigateway-integration:
 *   uri: "http://54.147.40.127/api/admin/user"
 *   responses:
 *     200:
 *       statusCode: "200"
 *       responseParameters:
 *         method.response.header.Access-Control-Allow-Origin: "'*'"
 *     401:
 *       statusCode: "401"
 *       responseParameters:
 *         method.response.header.Access-Control-Allow-Origin: "'*'"
 *     5\d{2}:
 *       statusCode: "500"
 *       responseParameters:
 *         method.response.header.Access-Control-Allow-Origin: "'*'"
 *   requestParameters:
 *     integration.request.header.Authorization: "method.request.header.Authorization"
 *   passthroughBehavior: "when_no_match"
 *   httpMethod: "POST"
 *   type: "http"
 *
 */

// fromtoken route
/**
 * @api [get] /api/admin/user/fromtoken
 * tags:
 * - "api/admin/user"
 * description: "Retrieve a user from the Auth0 token."
 * parameters:
 * - $ref: '#/components/parameters/AWSAuthHeaderRequired'
 * security:
 * - ApiKey: [] # Required for AWS integration.
 *   Auth0Bearer: []
 * responses:
 *   200:
 *     $ref: "#/components/responses/AdminUser"
 * x-amazon-apigateway-integration:
 *   uri: "http://54.147.40.127/api/admin/user/fromtoken"
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
 * @api [options] /api/admin/user/fromtoken
 * responses:
 *   200:
 *     $ref: "#/components/responses/AllowCORS"
 * tags:
 *  - "api/admin/user"
 * x-amazon-apigateway-integration:
 *   uri: "http://54.147.40.127/api/admin/user/fromtoken"
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

// {id} methods
/**
 * @api [get] /api/admin/user/{id}
 * tags:
 * - "api/admin/user"
 * description: "Returns an AdminUser entity with the specified id, or 404 if not found."
 * parameters:
 * - $ref: '#/components/parameters/AWSAuthHeaderRequired'
 * - $ref: '#/components/parameters/IDParameter'
 * security:
 * - ApiKey: [] # Required for AWS integration.
 *   Auth0Bearer: []
 * responses:
 *   200:
 *     $ref: "#/components/responses/AdminUser"
 * x-amazon-apigateway-integration:
 *   uri: "http://54.147.40.127/api/admin/user/{id}"
 *   responses:
 *     default:
 *       statusCode: "200"
 *       responseParameters:
 *         method.response.header.Access-Control-Allow-Origin: "'*'"
 *   requestParameters:
 *     integration.request.header.Authorization: "method.request.header.Authorization"
 *     integration.request.path.id: "method.request.path.id"
 *   passthroughBehavior: "when_no_match"
 *   httpMethod: "GET"
 *   type: "http"
 */
/**
 * @api [put] /api/admin/user/{id}
 * tags:
 * - "api/admin/user"
 * description: "Updates and returns the AdminUser entity with the specified id, or\
 *   \ 404 if not found."
 * parameters:
 * - $ref: '#/components/parameters/AWSAuthHeaderRequired'
 * - $ref: '#/components/parameters/IDParameter'
 * security:
 * - ApiKey: [] # Required for AWS integration.
 *   Auth0Bearer: []
 * requestBody:
 *   $ref: "#/components/requestBodies/AdminUser"
 * responses:
 *   200:
 *     $ref: "#/components/responses/AdminUser"
 * x-amazon-apigateway-integration:
 *   uri: "http://54.147.40.127/api/admin/user/{id}"
 *   responses:
 *     default:
 *       statusCode: "200"
 *       responseParameters:
 *         method.response.header.Access-Control-Allow-Origin: "'*'"
 *   requestParameters:
 *     integration.request.header.Authorization: "method.request.header.Authorization"
 *     integration.request.path.id: "method.request.path.id"
 *   passthroughBehavior: "when_no_match"
 *   httpMethod: "PUT"
 *   type: "http"
 */
/**
 * @api [delete] /api/admin/user/{id}
 * tags:
 * - "api/admin/user"
 * description: "Deletes the AdminUser entity with the specified id if it exists."
 * parameters:
 * - $ref: '#/components/parameters/AWSAuthHeaderRequired'
 * - $ref: '#/components/parameters/IDParameter'
 * security:
 * - ApiKey: [] # Required for AWS integration.
 *   Auth0Bearer: []
 * responses:
 *   200:
 *     description: "successful operation"
 *     headers:
 *       Access-Control-Allow-Origin:
 *         schema:
 *           type: "string"
 *   400:
 *     description: "400 response"
 *   401:
 *     description: "401 response"
 * x-amazon-apigateway-integration:
 *   uri: "http://54.147.40.127/api/admin/user/{id}"
 *   responses:
 *     200:
 *       statusCode: "200"
 *       responseParameters:
 *         method.response.header.Access-Control-Allow-Origin: "'*'"
 *     400:
 *       statusCode: "400"
 *     401:
 *       statusCode: "401"
 *   requestParameters:
 *     integration.request.header.Authorization: "method.request.header.Authorization"
 *     integration.request.path.id: "method.request.path.id"
 *   passthroughBehavior: "when_no_match"
 *   httpMethod: "DELETE"
 *   type: "http"
 */
/**
 * @api [options] /api/admin/user/{id}
 * tags:
 * - "api/admin/user"
 * parameters:
 * - $ref: '#/components/parameters/IDParameter'
 * responses:
 *   200:
 *     $ref: "#/components/responses/AllowCORS"
 * x-amazon-apigateway-integration:
 *   uri: "http://54.147.40.127/api/admin/user/{id}"
 *   responses:
 *     default:
 *       statusCode: "200"
 *       responseParameters:
 *         method.response.header.Access-Control-Allow-Methods: "'DELETE,GET,OPTIONS,PUT'"
 *         method.response.header.Access-Control-Allow-Headers: "'Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token'"
 *         method.response.header.Access-Control-Allow-Origin: "'*'"
 *   requestParameters:
 *     integration.request.path.id: "method.request.path.id"
 *   passthroughBehavior: "when_no_match"
 *   httpMethod: "OPTIONS"
 *   type: "http"
 *
 */

/**
 * Implementation of the {@link ApiController} abstract class for managing users. This controller connects with Auth0
 * services using the hooks provided by {@link ApiController}.
 */
public class AdminUserController extends ApiController<AdminUser> {
    private static final Logger LOG = LoggerFactory.getLogger(AdminUserController.class);
    static final String NO_USER_WITH_AUTH0_ID_MESSAGE = "No user with auth0UserID=%s found.";
    private static final String TOKEN_PATH = "/fromtoken";

    /**
     * Instantiate the {@link AdminUser} endpoints. Note: this controller must sit behind the /admin path. This ensures
     * that the requesting user is checked for admin authorization (handled by
     * {@link org.opentripplanner.middleware.auth.Auth0Connection#checkUserIsAdmin}).
     */
    public AdminUserController(String apiPrefix){
        super(apiPrefix, Persistence.adminUsers, "admin/user");
    }

    @Override
    protected void buildEndpoint(ApiEndpoint baseEndpoint) {
        LOG.info("Registering path {}.", ROOT_ROUTE + TOKEN_PATH);

        // Add the user token route BEFORE the regular CRUD methods
        // (otherwise, /fromtoken requests would be considered
        // by spark as 'GET user with id "fromtoken"', which we don't want).
        ApiEndpoint modifiedEndpoint = baseEndpoint
            // Get user from token.
            .get(path(ROOT_ROUTE + TOKEN_PATH)
                    .withDescription("Retrieves an AdminUser entity using an Auth0 access token passed in an Authorization header.")
                    .withResponseType(persistence.clazz),
                this::getUserFromRequest, JsonUtils::toJson
            )

            // Options response for CORS for the token path
            .options(path(TOKEN_PATH), (req, res) -> "");

        // Add the regular CRUD methods after defining the /fromtoken route.
        super.buildEndpoint(modifiedEndpoint);
    }

    /**
     * HTTP endpoint to get the {@link AdminUser} entity, if it exists, from an {@link Auth0UserProfile} attribute
     * available from a {@link Request} (this is the case for '/api/admin/' endpoints).
     */
    private AdminUser getUserFromRequest(Request req, Response res) {
        Auth0UserProfile profile = Auth0Connection.getUserFromRequest(req);
        AdminUser user = profile.adminUser;

        // If the AdminUser object is null (i.e. not found), return 404.
        if (user == null) {
            logMessageAndHalt(req, HttpStatus.NOT_FOUND_404, String.format(NO_USER_WITH_AUTH0_ID_MESSAGE, profile.auth0UserId), null);
        }
        return user;
    }

    /**
     * Before creating/storing a user in MongoDB, create the user in Auth0 and update the {@link AdminUser#auth0UserId}
     * with the value from Auth0.
     */
    @Override
    AdminUser preCreateHook(AdminUser user, Request req) {
        User auth0UserProfile = createNewAuth0User(user, req, this.persistence);
        return updateAuthFieldsForUser(user, auth0UserProfile);
    }

    @Override
    AdminUser preUpdateHook(AdminUser user, AdminUser preExistingUser, Request req) {
        validateExistingUser(user, preExistingUser, req, this.persistence);
        return user;
    }

    /**
     * Before deleting the user in MongoDB, attempt to delete the user in Auth0.
     */
    @Override
    boolean preDeleteHook(AdminUser user, Request req) {
        try {
            deleteAuth0User(user.auth0UserId);
        } catch (Auth0Exception e) {
            logMessageAndHalt(req, 500, "Error deleting user.", e);
            return false;
        }
        return true;
    }
}
