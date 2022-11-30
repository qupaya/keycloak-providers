# Keycloak providers

## Prerequisites
* JDK 11

## Quickstart
* Clone the repository.
* Build the jar file: `./gradlew jar`.
* Copy the generated jar file into the `providers` directory (might not exist yet) in the keycloak installation.

## How to add a new provider
* Create the provider factory class.
* Create the provider class.
* Register the provider factory by referencing it in `META-INF/services`

Find examples in [the official keycloak documentation](https://www.keycloak.org/docs/latest/server_development/#_providers).