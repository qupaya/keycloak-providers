# Keycloak providers

## Prerequisites
* JDK 11

## Quickstart
* Clone the repository.
* Build the jar file: `./gradlew jar`.
* Copy the generated jar file into the `providers` directory (might not exist yet) in the keycloak installation. 

## REST API endpoints:
* Create a user that has the admin role for the realm.
* Use this user to get an access token from Keycloak, before making any API calls to services from this package.
* Use the token to authorize your calls to the REST API.

Check the `test-auth.sh` file for details.

## How to add a new provider
* Create the provider factory class.
* Create the provider class.
* Register the provider factory by referencing it in `META-INF/services`

Find examples in [the official keycloak documentation](https://www.keycloak.org/docs/latest/server_development/#_providers) and [the example code in the keycloak repo](https://github.com/keycloak/keycloak/tree/main/examples).