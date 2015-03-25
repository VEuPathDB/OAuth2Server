
EuPathDB OAuth2 Server
======================

The OAuth2Server project provides a generic OAuth2 server, which will handle
customized user authentication and identification to client applications,
allowing multiple clients to use the server to authenticate and identify their
users without needing to know the users' username and password on the OAuth2
server (i.e. the equivalent of having a "Login with Facebook" option to access a
linked account.  Developers using this project can specify a custom
Authenticator Java class, which executes the actual authentication and tailored
identification of users, and a custom login page to display your brand.

To skip to the EuPathDB implementation installation, see [here](#installation).

### How does it work? ###

A webapp using this library is deployed to a servlet container.  Approved
clients send an OAuth2 authentication request to the /&lt;webapp&gt;/authorize
endpoint.  If the user has already logged in to the OAuth server, an
authorization code is returned via redirect.  If not, a login form is displayed
and the user is asked to log in.  Once the user provides correct credentials,
he is redirected back to the client with the authorization code.

The client's server can then trade the authorization code for an access token
(but only with its client secret) via an OAuth2 token request (a POST to the
/&lt;webapp&gt;/token endpoint, returning JSON).  With an access token, a client
server can access the /&lt;webapp&gt;/user resource, which returns a JSON response
containing user information provided by the configured Authenticator class.

From the browser, the client can logout using the /&lt;webapp&gt;/logout URL, which
expires the OAuth server's session cookie.

### How to use the library ###

The OAuth2Server server module is available as a JAR file to be included in the
classpath of a (unprovided) Java webapp.  This dependency is available via Maven
as:

    <dependency>
      <groupId>org.gusdb</groupId>
      <artifactId>oauth2-server</artifactId>
    </dependency>

Creating a webapp that uses the library is relatively easy.  In your webapp's
web.xml, include the following servlet:

    <servlet>
      <servlet-name>OAuthService</servlet-name>
      <servlet-class>org.gusdb.oauth2.server.OAuthServlet</servlet-class>
      <init-param>
        <param-name>oauth.config.file</param-name>
        <param-value>${oauthConfigFile}</param-value>
      </init-param>
      <!-- optionally load application on startup -->
      <!-- <load-on-startup>0</load-on-startup> -->
    </servlet>
    <servlet-mapping>
      <servlet-name>OAuthService</servlet-name>
      <url-pattern>/*</url-pattern>
    </servlet-mapping>

The OAuth config file param should be an absolute path to the runtime file location, or a relative path from the servlet context (i.e. the directory containing WEB-INF).  It is advisable to place the file inside WEB-INF for security reasons.  The file must contain a JSON-formatted object with the following properties:

* **authenticatorClass (String, required)**: the fully qualified path of your
      org.gusdb.oauth2.AuthenticatorAuthenticator implementation

* **authenticatorConfig (Any, optional)**: arbitrary JSON configuration; this will
      be passed to your Authenticator's init() method during server
      initialization; if omitted, null will be passed

* **allowedClients (Array[Object], required)**: an array of approved clients;
      client objects have the following properties:

    - **clientId (String, required)**: client ID

    - **clientSecret (String, required)**: client secret

    - **clientDomains (Array[String], required)**: list of domains to which
            requests from this client are allowed to redirect; at least
            one domain must be provided

* **loginFormPage (String, optional)**: custom HTML page for a branded login form;
      if omitted, a default login form will be displayed (see Static Resources)

* **loginSuccessPage (String, optional)**: custom HTML page for a branded login
      success page; if omitted a default success page will be displayed; only
      relevant if allowAnonymousLogin is set to true (see Static Resources)

* **tokenExpirationSecs (Number, optional)**: how long to wait before expiring
     authentication codes and access tokens (same value used for both).  Must
     be an integer.  If omitted, 300 (5 minutes) is used.

* **allowAnonymousLogin (Boolean, optional, default: false)**: whether to allow
      user authentication without an approved client.  If allowed, a user can
      log in using the loginFormPage, then at some later time, access that
      authentication to gain access to an approved client application

* **validateDomains (Boolean, optional, default: true)**: whether to deny access
      to authentication services if the redirect URL hostname is not in the
      approved client domain list; by default, client redirect domains must
      be contained in the approved list

There is a sample config file at:
&lt;project&gt;/EuPathDB/src/main/webapp/WEB-INF/OAuthSampleConfig.json

### The Authenticator Class ###

You must provide an implementation of the org.gusdb.oauth2.Authenticator
interface which accesses your account store to authenticate users and provide
basic user information to approved clients.  This involves implementing four
methods, the details of which can be found in the Authenticator JavaDoc.

### Static Resources ###

The OAuth server provides the ability to customize the look and feel of the
login page by providing static resources served by the library.  By default, a
set of simple HTML, CSS, and JavaScript pages are provided to display the
default login form.  To provide your own, just put your resources in the Java
classpath under the /assets top-level directory.  You can then provide the name
of the custom HTML form page in the configuration file (without the "assets/"
prefix.

The only requirements are that it contain an HTML form with two input fields
(username and password) and submit via POST to the /login resource.  Optionally,
your custom resource can handle a "status" query parameter, sent to the HTML
page under various conditions.  If authentication is allowed and successful, the
page will redirect to the authorization request's redirect_uri value.  If not,
one of three status values may be returned:

* **failed**: if the submitted credentials are not valid (i.e. the configured
Authenticator's isCredentialsValid() method returned false)
* **error**: if an error occurred while authenticating
* **accessdenied**: if the form_id query string param is tampered with, or if
an anonymous login is attempted but disallowed

### The EuPathDB OAuth2 Server Implementation ###

The EuPathDB project uses this tool as the authentication mechanism across
the EuPathDB family of websites.

#### How we override the basic server ####

* **Custom Authenticator**: The org.gusdb.oauth2.wdk.UserDbAuthenticator class
    is a EuPathDB-specific implementation of the Authenticator class.  It
    accesses the USERS table and encrypts passwords in the same way as WDK, but
    does not depend on WDK.

* **Custom View**: The eupathdb-login.html page shows a custom login page used
    by EuPathDB including custom logo and message to the user.

* **web.xml**:
    1. We load the OAuthServlet on startup so we can programmatically test
        that it is running after reloading or during continuous integration
    2. We configure a custom cookie name for the OAuth server in case it shares
        a domain with any other Tomcat webapp
    3. We include the ApplicationListener class since we want to close the
        UserDB connection pool on webapp unload

* **log4j.xml**: We use the Log4J SLF4J connector and configure logging via
    log4j.xml to write to ${catalina.base}/logs/oauth/oauth.log4j

* **Dependencies**: In addition to Log4J, we depend on the FgpUtil project,
    which is downloaded and built during the installation steps below.

* **Oracle Driver**: EuPathDB uses Oracle as a user database, but Oracle
    licensing does not allow us to distribute it as part of this package.  The
    easiest solution we've found is to remove the compile-time dependency and
    simply drop the ojdbc JAR into Tomcat's /lib directory.

<a name="installation"></a>

#### Installation ####

To download and build the EuPathDB implementation:

1. Create a clean installation directory and make it your current directory.

2. Get the latest version of the project by running:

        svn co https://www.cbil.upenn.edu/svn/gus/OAuth2Server/trunk OAuth2Server

  If you do not want the build.sh script to manage dependencies (e.g. as with Jenkins jobs), 
  you will also need FgpUtil:

        svn co https://www.cbil.upenn.edu/svn/gus/FgpUtil/trunk FgpUtil

3. Decide the name and runtime location of your configuration file.  It will
    be in JSON format.  This must be an absolute path to the
    runtime/deployment location, or a relative path from the servlet context.

4. Decide if you want to configure a separate local maven repository for the
    build.  This can be handy for isolating builds and keeping all the
    dependencies required by this project separate from the main repository.

5. Run the EuPathDB OAuth2Server build script.  It has the following usage:

        OAuth2Server/EuPathDB/bin/build.sh [configFile [localMvnRepo]]

   The specification of the config file is above.  The local Maven repository
   must be a directory and should be an absolute path.  You can modify the
   script (via constants at the top of the file) to turn on unit tests or to
   tell the script to do an 'svn update' on both the OAuth2Server and FgpUtil
   projects.

6. Once you have run the build script and written your configuration file,
    simply deploy the WAR file (OAuth2Server/EuPathDB/target/oauth.war) in your servlet
    container (probably Tomcat).  Since we use a Servlet 3.0 feature (cookie
    renaming), Tomcat 7+ is required.  Don't forget to include the Oracle driver
    in Tomcat's lib directory so the UserDbAuthenticator can access the
    database.  A good test URL is
    &lt;scheme&gt;://&lt;domain&gt;:&lt;port&gt;/oauth/assets/login.html. 
