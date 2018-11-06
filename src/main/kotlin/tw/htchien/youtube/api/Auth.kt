package tw.htchien.youtube.api

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory

import java.io.File
import java.io.IOException
import java.io.InputStreamReader

/**
 * Shared class used by every sample. Contains methods for authorizing a user and caching credentials.
 */
object Auth {

    /**
     * Define a global instance of the HTTP transport.
     */
    val HTTP_TRANSPORT: HttpTransport = NetHttpTransport()

    /**
     * Define a global instance of the JSON factory.
     */
    val JSON_FACTORY: JsonFactory = JacksonFactory()

    /**
     * This is the directory that will be used under the user's home directory where OAuth tokens will be stored.
     */
    private val CREDENTIALS_DIRECTORY = ".oauth-credentials"

    /**
     * Authorizes the installed application to access user's protected data.
     *
     * @param scopes              list of scopes needed to run youtube upload.
     * @param credentialDatastore name of the credential datastore to cache OAuth tokens
     */
    @Throws(IOException::class)
    fun authorize(scopes: List<String>, credentialDatastore: String): Credential {

        // Load client secrets.
        val clientSecretReader = InputStreamReader(Auth::class.java.getResourceAsStream("/client_secrets.json"))
        val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, clientSecretReader)

        // Checks that the defaults have been replaced (Default = "Enter X here").
        if (clientSecrets.details.clientId.startsWith("Enter") || clientSecrets.details.clientSecret.startsWith("Enter ")) {
            println("Enter Client ID and Secret from https://console.developers.google.com/project/_/apiui/credential " + "into src/main/resources/client_secrets.json")
            System.exit(1)
        }

        // This creates the credentials datastore at ~/.oauth-credentials/${credentialDatastore}
        val fileDataStoreFactory = FileDataStoreFactory(File(System.getProperty("user.home") + "/" + CREDENTIALS_DIRECTORY))
        val datastore = fileDataStoreFactory.getDataStore<StoredCredential>(credentialDatastore)

        val flow = GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, scopes)
                .setCredentialDataStore(datastore)
                .build()

        // Build the local server and bind it to port 8080
        val localReceiver = LocalServerReceiver.Builder().setPort(8080).build()

        // Authorize.
        return AuthorizationCodeInstalledApp(flow, localReceiver).authorize("user")
    }
}
