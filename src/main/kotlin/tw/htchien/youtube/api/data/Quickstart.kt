package tw.htchien.youtube.api.data

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeScopes

import java.io.IOException
import java.io.InputStreamReader
import java.util.Arrays


object Quickstart {

    /** Application name.  */
    private val APPLICATION_NAME = "API Sample"

    /** Directory to store user credentials for this application.  */
    private val DATA_STORE_DIR = java.io.File(
            System.getProperty("user.home"), ".credentials/youtube-java-quickstart")

    /** Global instance of the [FileDataStoreFactory].  */
    private var DATA_STORE_FACTORY: FileDataStoreFactory? = null

    /** Global instance of the JSON factory.  */
    private val JSON_FACTORY = JacksonFactory.getDefaultInstance()

    /** Global instance of the HTTP transport.  */
    private var HTTP_TRANSPORT: HttpTransport? = null

    /** Global instance of the scopes required by this quickstart.
     *
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/drive-java-quickstart
     */
    private val SCOPES = Arrays.asList(YouTubeScopes.YOUTUBE_READONLY)

    /**
     * Build and return an authorized API client service, such as a YouTube
     * Data API client service.
     * @return an authorized API client service
     * @throws IOException
     */
    val youTubeService: YouTube
        @Throws(IOException::class)
        get() {
            val credential = authorize()
            return YouTube.Builder(HTTP_TRANSPORT!!, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build()
        }

    init {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
            DATA_STORE_FACTORY = FileDataStoreFactory(DATA_STORE_DIR)
        } catch (t: Throwable) {
            t.printStackTrace()
            System.exit(1)
        }

    }

    /**
     * Create an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun authorize(): Credential {
        // Load client secrets.
        val `in` = Quickstart::class.java.getResourceAsStream("/client_secret.json")
        val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(`in`))

        // Build flow and trigger user authorization request.
        val flow = GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(DATA_STORE_FACTORY!!)
                .setAccessType("offline")
                .build()
        return AuthorizationCodeInstalledApp(
                flow, LocalServerReceiver()).authorize("user")
    }

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val youtube = youTubeService
        try {
            val channelsListByUsernameRequest = youtube.channels().list("snippet,contentDetails,statistics")
            channelsListByUsernameRequest.forUsername = "GoogleDevelopers"

            val response = channelsListByUsernameRequest.execute()
            val channel = response.items[0]
            System.out.printf(
                    "This channel's ID is %s. Its title is '%s', and it has %s views.\n",
                    channel.id,
                    channel.snippet.title,
                    channel.statistics.viewCount)
        } catch (e: GoogleJsonResponseException) {
            e.printStackTrace()
            System.err.println("There was a service error: " +
                    e.details.code + " : " + e.details.message)
        } catch (t: Throwable) {
            t.printStackTrace()
        }

    }
}
