package org.opentripplanner.middleware.controllers.api;

import com.beerboy.ss.SparkSwagger;
import com.beerboy.ss.rest.Endpoint;
import org.eclipse.jetty.http.HttpStatus;
import org.opentripplanner.middleware.models.Model;
import org.opentripplanner.middleware.persistence.TypedPersistence;
import org.opentripplanner.middleware.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.HaltException;
import spark.Request;
import spark.Response;

import java.util.Date;
import java.util.List;

import static com.beerboy.ss.descriptor.EndpointDescriptor.endpointPath;
import static com.beerboy.ss.descriptor.MethodDescriptor.path;
import static org.opentripplanner.middleware.utils.JsonUtils.getPOJOFromRequestBody;
import static org.opentripplanner.middleware.utils.JsonUtils.logMessageAndHalt;

/**
 * Generic API controller abstract class. This class provides CRUD methods using {@link spark.Spark} HTTP request
 * methods. This will identify the MongoDB collection on which to operate based on the provided {@link Model} class.
 *
 * TODO: Add hooks so that validation can be performed on certain methods (e.g., validating fields on create/update,
 *  checking user permissions to perform certain actions, checking whether an entity can be deleted due to references
 *  that exist in other collection).
 * @param <T> One of the {@link Model} classes (extracted from {@link TypedPersistence})
 */
public abstract class ApiController<T extends Model> implements Endpoint {
    private static final String ID_PARAM = "/:id";
    private final String ROOT_ROUTE;
    private static final String SECURE = "secure/";
    private static final Logger LOG = LoggerFactory.getLogger(ApiController.class);
    private final String classToLowercase;
    private final TypedPersistence<T> persistence;
    private final Class<T> clazz;

    /**
     * @param apiPrefix string prefix to use in determining the resource location
     * @param persistence {@link TypedPersistence} persistence for the entity to set up CRUD endpoints for
     */
    public ApiController (String apiPrefix, TypedPersistence<T> persistence) {
        this.clazz = persistence.clazz;
        this.persistence = persistence;
        this.classToLowercase = persistence.clazz.getSimpleName().toLowerCase();
        this.ROOT_ROUTE = apiPrefix + SECURE + classToLowercase;
    }

    /**
     * This method is called by {@link SparkSwagger} to register endpoints and generate the docs.
     * @param restApi The object to which to attach the documentation.
     */
    @Override
    public void bind(final SparkSwagger restApi) {
        LOG.info("Registering routes and enabling docs for {}", ROOT_ROUTE);

        restApi.endpoint(endpointPath(ROOT_ROUTE)
            .withDescription("Interface for querying and managing '" + classToLowercase + "' entities."), (q, a) -> LOG.info("Received request for '{}' Rest API", classToLowercase))

            // Careful here!
            // If using lambdas with the GET method, a bug in spark-swagger
            // requires you to write path(<entire_route>).
            // If you use `new GsonRoute() {...}` with the GET method, you only need to write path(<relative_to_endpointPath>).
            // Other HTTP methods are not affected by this bug.

            // Get multiple entities.
            .get(path(ROOT_ROUTE)
                    .withDescription("Gets a list of all '" + classToLowercase + "' entities.")
                    .withResponseAsCollection(clazz),
                    this::getMany, JsonUtils::toJson
            )

            // Get one entity.
            .get(path(ROOT_ROUTE + ID_PARAM)
                    .withDescription("Returns a '" + classToLowercase + "' entity with the specified id, or 404 if not found.")
                    .withPathParam().withName("id").withDescription("The id of the entity to search.").and()
                    // .withResponses(...) // FIXME: not implemented (requires source change).
                    .withResponseType(clazz),
                    this::getOne, JsonUtils::toJson
            )

            // Options response for CORS
            .options(path(""), (req, res) -> "")

            // Create entity request
            .post(path("")
                    .withDescription("Creates a '" + classToLowercase + "' entity.")
                    .withRequestType(clazz) // FIXME: Embedded Swagger UI doesn't work for this request. (Embed or link a more recent version?)
                    .withResponseType(clazz),
                    this::createOrUpdate, JsonUtils::toJson
            )

            // Update entity request
            .put(path(ID_PARAM)
                    .withDescription("Updates and returns the '" + classToLowercase + "' entity with the specified id, or 404 if not found.")
                    .withPathParam().withName("id").withDescription("The id of the entity to update.").and()
                    // FIXME: The Swagger UI embedded in spark-swagger doesn't work for this request.
                    //  (Embed or link a more recent Swagger UI version?)
                    .withRequestType(clazz)
                    // FIXME: `withResponses` is supposed to document the expected HTTP responses (200, 403, 404)...
                    //  but that doesn't appear to be implemented in spark-swagger.
                    // .withResponses(...)
                    .withResponseType(clazz),
                    this::createOrUpdate, JsonUtils::toJson
            )

            // Delete entity request
            .delete(path(ID_PARAM)
                    .withDescription("Deletes the '" + classToLowercase + "' entity with the specified id if it exists.")
                    .withPathParam().withName("id").withDescription("The id of the entity to delete.").and()
                    .withGenericResponse(),
                    this::deleteOne, JsonUtils::toJson
            );
    }

    /**
     * HTTP endpoint to get multiple entities.
     */
    private List<T> getMany(Request req, Response res) {
        return persistence.getAll();
    }

    /**
     * HTTP endpoint to get one entity specified by ID.
     */
    private T getOne(Request req, Response res) {
        String id = getIdFromRequest(req);
        return getObjectForId(req, id);
    }

    /**
     * HTTP endpoint to delete one entity specified by ID.
     */
    private String deleteOne(Request req, Response res) {
        long startTime = System.currentTimeMillis();
        String id = getIdFromRequest(req);
        try {
            T object = getObjectForId(req, id);
            // Run pre-delete hook. If return value is false, abort.
            if (!preDeleteHook(object, req)) {
                logMessageAndHalt(req, 500, "Unknown error occurred during delete attempt.");
            }
            boolean success = persistence.removeById(id);
            int code = success ? HttpStatus.OK_200 : HttpStatus.INTERNAL_SERVER_ERROR_500;
            String message = success
                ? String.format("Successfully deleted %s.", classToLowercase)
                : String.format("Failed to delete %s", classToLowercase);
            logMessageAndHalt(req, code, message, null);
        } catch (HaltException e) {
            throw e;
        } catch (Exception e) {
            logMessageAndHalt(
                req,
                HttpStatus.INTERNAL_SERVER_ERROR_500,
                String.format("Error deleting %s", classToLowercase),
                e
            );
        } finally {
            LOG.info("Delete operation took {} msec", System.currentTimeMillis() - startTime);
        }
        return null;
    }

    /**
     * Convenience method for extracting the ID param from the HTTP request.
     */
    private T getObjectForId(Request req, String id) {
        T object = persistence.getById(id);
        if (object == null) {
            logMessageAndHalt(
                req,
                HttpStatus.NOT_FOUND_404,
                String.format("No %s with id=%s found.", classToLowercase, id),
                null
            );
        }
        return object;
    }

    /**
     * Hook called before object is created in MongoDB.
     */
    abstract T preCreateHook(T entity, Request req);

    /**
     * Hook called before object is updated in MongoDB. Validation of entity object could go here.
     */
    abstract T preUpdateHook(T entity, Request req);

    /**
     * Hook called before object is deleted in MongoDB.
     */
    abstract boolean preDeleteHook(T entity, Request req);

    /**
     * HTTP endpoint to create or update a single entity. If the ID param is supplied and the HTTP method is
     * PUT, an update operation will be applied to the specified entity using the JSON body found in the request.
     * Otherwise, a new entity will be created.
     */
    private T createOrUpdate(Request req, Response res) {
        long startTime = System.currentTimeMillis();
        // Check if an update or create operation depending on presence of id param
        // This needs to be final because it is used in a lambda operation below.
        if (req.params("id") == null && req.requestMethod().equals("PUT")) {
            logMessageAndHalt(req, HttpStatus.BAD_REQUEST_400, "Must provide id");
        }
        final boolean isCreating = req.params("id") == null;
        // Save or update to database
        try {
            // Validate fields by deserializing into POJO.
            T object = getPOJOFromRequestBody(req, clazz);
            if (isCreating) {
                // Run pre-create hook and use updated object (with potentially modified values) in create operation.
                T updatedObject = preCreateHook(object, req);
                persistence.create(updatedObject);
            } else {
                String id = getIdFromRequest(req);
                // Update last updated value.
                object.lastUpdated = new Date();
                object.dateCreated = getObjectForId(req, id).dateCreated;
                // Validate that ID in JSON body matches ID param. TODO add test
                if (!id.equals(object.id)) {
                    logMessageAndHalt(req, HttpStatus.BAD_REQUEST_400, "ID in JSON body must match ID param.");
                }
                persistence.replace(id, object);
            }
            // Return object that ultimately gets stored in database.
            return persistence.getById(object.id);
        } catch (HaltException e) {
            throw e;
        } catch (Exception e) {
            logMessageAndHalt(req, 500, "An error was encountered while trying to save to the database", e);
        } finally {
            String operation = isCreating ? "Create" : "Update";
            LOG.info("{} {} operation took {} msec", operation, classToLowercase, System.currentTimeMillis() - startTime);
        }
        return null;
    }

    /**
     * Get entity ID from request.
     */
    private String getIdFromRequest(Request req) {
        String id = req.params("id");
        if (id == null) {
            logMessageAndHalt(req, HttpStatus.BAD_REQUEST_400, "Must provide id");
        }
        return id;
    }
}
