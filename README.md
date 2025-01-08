# Keycloak providers

## Plugins in this Bundle

### Remote Password Blacklist

This plugin takes one or more links to text files with blacklisted passwords. The lists are separated by space. The lists will be loaded on startup or when the configuration changes. An additional REST-API endpoint allows checking a password against the list.

#### Configuration

(In 20.0.3)

1. Enter the admin console and select the realm for which you want to configure the plugin.
2. Select _Authentication_ and _Policies_.
3. Add the policy _Remote Password Blacklist_.
4. Enter the link to the blacklist(s) that you want to use.
5. Save.

#### How to use

* Checking passwords against the blacklist works automatically.
* The REST-API endpoint is available at `[KEYCLOAK_ADDRESS]/realms/[MY_REALM]/remoteBlacklistRealm/check/[PASSWORD]` as GET request.

### SHA-1 hash provider and importer

This plugin allows importing existing users, that have SHA-1 based passwords. The passwords will be automatically updated to the configured default algorithm on a users first login.

#### Configuration

(In 20.0.3)

1. Enter the admin console and select the realm for which you want to configure the plugin.
2. Select _Realm roles_.
3. Create role _sha1-import_.
4. Save.
5. Add the role to the user / client that you use to import the users.

#### How to use

Import the users one by one using the provided REST-API endpoint `[KEYCLOAK_ADDRESS]/realms/[TARGET_REALM]/sha1Import/import`:

```shell
curl -i --request POST [KEYCLOAK_ADDRESS]/realms/[TARGET_REALM]/sha1Import/import --header "Authorization: Bearer $ACCESS_TOKEN" --header "Content-Type: application/json" --data '{"firstName":"[FIRST_NAME]","lastName":"[LAST_NAME]","email":"[EMAIL]","username":"[USER_NAME]","emailVerified":true,"enabled":true,"hash":"[THE_HASH]","salt":"[THE_SALT]"}';
```

### Brevo newsletter registration

This plugin allows you to create a newsletter subscription request. Maybe you want to have that option on your registration page.

#### Configuration

1. Provide the environment variable _BREVO_FORM_LINK_ which needs to be a subscription form link. To get this link:
   1. Log into Brevo.
   2. Select _Contacts_ and _Forms_.
   3. Create a new Subscription form or edit an existing one.
   4. In the step _Share_ you can find a _Quick share_ link, that's the one.
2. Enter the admin console and select the realm for which you want to configure the plugin.
3. Select _Realm settings_ and _Events_.
4. Add the event listener _brevo-newsletter-registration-event-listener_.
5. Save.

#### How to use

* Add the newsletter subscription checkbox to your _login/register.ftl_:
  ```html
  <div class="checkbox">
    <label>
      <input id="user.attributes.newsletter" name="user.attributes.newsletter" type="checkbox">${msg("subscribeToNewsletter")}
    </label>
  </div>
  ```
* Add the property _subscribeToNewsletter_ to your _login/messages/messages\_**.properties_
  ```properties
  subscribeToNewsletter=Subscribe to newsletter
  ```

### Keycloak save last login as attribute [inspired by ThoreKr](https://github.com/ThoreKr/keycloak-last-login-event-listener)

#### Configuration


1. Enter the admin console and select the realm for which you want to configure the plugin.
2. Select _Realm settings_ and _Events_.
3. Add the event listener _qupaya-lastLogin-event-listener_.
5. Save.

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

Find examples in [the official keycloak documentation](https://www.keycloak.org/docs/latest/server_development/#_providers).
