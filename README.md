# otp-middleware
The otp-middleware application proxies requests from OpenTripPlanner UI to API, 
enhancing [OpenTripPlanner](https://www.opentripplanner.org) (OTP) with user
storage, real-time trip monitoring, and more!

## Background
OTP provides multi-modal journey planning, combining transit, biking, walking,
and various car options (e.g., Park & Ride) into complete passenger itineraries.

Transportation agencies might want to extend the functionality of their
trip planner to improve the overall user experience for their OTP implementation.
otp-middleware is a potential way for agencies to fill in those gaps. It provides:

- user management and storage,
- trip monitoring,
- and other features (TODO).

## Deployment
TODO - One-time deployment.

OTP Middleware is also suitable for continuous deployment implementations.

## Development
To run otp-middleware in a local, development environment:

```bash
# Clone the repo.
git clone https://github.com/ibi-group/otp-middleware.git
cd otp-middleware
# Copy the config template, then make any changes to the file to 
# match your local configuration (e.g., database name).
cp configurations/default/env.yml.tmp configurations/default/env.yml
# At this point, you can either package the source code into an 
# executable jar, or import and run the code in an interactive 
# development environment. 
mvn package
java -jar target/otp-middleware.jar configurations/default/env.yml
```

## Configuration

### Auth0

TODO: Add Auth0 setup instructions.

### OTP Server Proxy Setup
The follow parameters are used to interact with an OTP server.

| Parameter | Description | Example |
| --- | --- | --- |
| OTP_API_ROOT | This is the address of the OTP server, including the root path to the OTP API, to which all OTP related requests will be sent to. | http://otp-server.example.com/otp | 
| OTP_PLAN_ENDPOINT | This defines the plan endpoint part of the requesting URL. If a request is made to this, the assumption is that a plan request has been made and that the response should be processed accordingly. | /plan |

### Bugsnag

Bugsnag is used to report error events that occur within the otp-middleware application or 
OpenTripPlanner components that it is monitoring. 

#### Bugsnag Configuration Parameters

These values can used as defined here (were applicable), or commented out so the default values are used. Parameters 
that don't have default values (N/A) can be obtained my following the steps in the next section.

| Parameter | Default | Description |
| --- | --- | --- |
| BUGSNAG_API_KEY | N/A | Used to authenticate against Bugsnag's API. |
| BUGSNAG_EVENT_JOB_DELAY_IN_MINUTES | 1 | Frequency in minutes to obtain events. |
| BUGSNAG_EVENT_REQUEST_JOB_DELAY_IN_MINUTES | 5 | Frequency in minutes to trigger event requests. |
| BUGSNAG_ORGANIZATION | N/A | The name of the organization defined within Bugsnag. This is used as a starting point with the Bugsnag API to obtain the organization ID which in-turn is use with most API calls. | 
| BUGSNAG_PROJECT_NOTIFIER_API_KEY | N/A | Used to report project errors to Bugsnag. |
| BUGSNAG_REPORTING_WINDOW_IN_DAYS | 14 | The number of days in the past to start retrieving event information. |  


#### Bugsnag Setup
Where default parameters cannot be used, these steps describe how to obtain each compulsory parameter.

##### BUGSNAG_API_KEY
A bugsnag API key is a key that is unique to an individual Bugsnag user. This key can be obtained by logging into 
Bugsnag (https://app.bugsnag.com), clicking on settings (top right hand corner) then `My account settings`. From here 
select `Personal auth tokens` and then `Generate new token`.

##### BUGSNAG_ORGANIZATION
A bugsnag organization contains all projects which errors/events will be reported on. The organization name and more 
specifically its ID is the starting point for most Bugsnag API requests. The organization name can be obtained by 
logging into Bugsnag (https://app.bugsnag.com), clicking on settings (top right hand corner) then `Organization settings`. 
From here, click on the organization name and then copy the name from the pop-up window.

##### BUGSNAG_PROJECT_NOTIFIER_API_KEY
A Bugsnag project identifier key is unique to a Bugsnag project and allows errors to be saved against it. This key can 
be obtained by logging into Bugsnag (https://app.bugsnag.com), clicking on Projects (left side menu) and selecting the 
required project. Once selected, the notifier API key is presented.

## Testing

### End-to-end (E2E)

In order to run E2E tests, specific configuration and environment variables must be used.

#### Auth0
The standard Auth0 configuration can be used for the server settings. However, some additional settings must be applied
in order for the server to get an oath token from Auth0 for creating authenticated requests. A private application
should be created for use in Auth0 following these instructions: https://auth0.com/docs/flows/call-your-api-using-resource-owner-password-flow

One special note is that the default directory must be set at the Account/tenant level. Otherwise, authorization will not
be successful. See this StackOverflow answer for more info: https://stackoverflow.com/a/43835563/915811

The special E2E client settings should be defined in `env.yml`:

| Parameter | Default | Description |
| --- | --- | --- |
| AUTH0_CLIENT_ID | N/A | Special E2E application client ID. |
| AUTH0_CLIENT_SECRET | N/A | Special E2E application client secret. |

**Note:** Just to reiterate, these are different from the server application settings and are only needed for E2E testing.