/*
 * Copyright (c) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package tw.htchien.youtube.api.live

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.util.DateTime
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.CdnSettings
import com.google.api.services.youtube.model.LiveBroadcast
import com.google.api.services.youtube.model.LiveBroadcastSnippet
import com.google.api.services.youtube.model.LiveBroadcastStatus
import com.google.api.services.youtube.model.LiveStream
import com.google.api.services.youtube.model.LiveStreamSnippet
import com.google.common.collect.Lists
import tw.htchien.youtube.api.Auth

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Use the YouTube Live Streaming API to insert a broadcast and a stream
 * and then bind them together. Use OAuth 2.0 to authorize the API requests.
 *
 * @author Ibrahim Ulukaya
 */
object CreateBroadcast {

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private var youtube: YouTube? = null

    /*
     * Prompt the user to enter a title for a broadcast.
     */
    private// Use "New Broadcast" as the default title.
    val broadcastTitle: String
        @Throws(IOException::class)
        get() {
            print("Please enter a broadcast title: ")
            val bReader = BufferedReader(InputStreamReader(System.`in`))
            var title = bReader.readLine()

            if (title.length < 1) {
                title = "New Broadcast"
            }
            return title
        }

    /*
     * Prompt the user to enter a title for a stream.
     */
    private// Use "New Stream" as the default title.
    val streamTitle: String
        @Throws(IOException::class)
        get() {
            print("Please enter a stream title: ")
            val bReader = BufferedReader(InputStreamReader(System.`in`))
            var title = bReader.readLine()

            if (title.length < 1) {
                title = "New Stream"
            }
            return title
        }

    /**
     * Create and insert a liveBroadcast resource.
     */
    @JvmStatic
    fun main(args: Array<String>) {

        // This OAuth 2.0 access scope allows for full read/write access to the
        // authenticated user's account.
        val scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube")

        try {
            // Authorize the request.
            val credential = Auth.authorize(scopes, "createbroadcast")

            // This object is used to make YouTube Data API requests.
            youtube = YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
                    .setApplicationName("youtube-cmdline-createbroadcast-sample").build()

            // Prompt the user to enter a title for the broadcast.
            var title = broadcastTitle
            println("You chose $title for broadcast title.")

            // Create a snippet with the title and scheduled start and end
            // times for the broadcast. Currently, those times are hard-coded.
            val broadcastSnippet = LiveBroadcastSnippet()
            broadcastSnippet.title = title
            broadcastSnippet.scheduledStartTime = DateTime("2024-01-30T00:00:00.000Z")
            broadcastSnippet.scheduledEndTime = DateTime("2024-01-31T00:00:00.000Z")

            // Set the broadcast's privacy status to "private". See:
            // https://developers.google.com/youtube/v3/live/docs/liveBroadcasts#status.privacyStatus
            val status = LiveBroadcastStatus()
            status.privacyStatus = "private"

            val broadcast = LiveBroadcast()
            broadcast.kind = "youtube#liveBroadcast"
            broadcast.snippet = broadcastSnippet
            broadcast.status = status

            // Construct and execute the API request to insert the broadcast.
            val liveBroadcastInsert = youtube!!.liveBroadcasts().insert("snippet,status", broadcast)
            var returnedBroadcast = liveBroadcastInsert.execute()

            // Print information from the API response.
            println("\n================== Returned Broadcast ==================\n")
            println("  - Id: " + returnedBroadcast.id)
            println("  - Title: " + returnedBroadcast.snippet.title)
            println("  - Description: " + returnedBroadcast.snippet.description)
            println("  - Published At: " + returnedBroadcast.snippet.publishedAt)
            println(
                    "  - Scheduled Start Time: " + returnedBroadcast.snippet.scheduledStartTime)
            println(
                    "  - Scheduled End Time: " + returnedBroadcast.snippet.scheduledEndTime)

            // Prompt the user to enter a title for the video stream.
            title = streamTitle
            println("You chose $title for stream title.")

            // Create a snippet with the video stream's title.
            val streamSnippet = LiveStreamSnippet()
            streamSnippet.title = title

            // Define the content distribution network settings for the
            // video stream. The settings specify the stream's format and
            // ingestion type. See:
            // https://developers.google.com/youtube/v3/live/docs/liveStreams#cdn
            val cdnSettings = CdnSettings()
            cdnSettings.format = "1080p"
            cdnSettings.ingestionType = "rtmp"

            val stream = LiveStream()
            stream.kind = "youtube#liveStream"
            stream.snippet = streamSnippet
            stream.cdn = cdnSettings

            // Construct and execute the API request to insert the stream.
            val liveStreamInsert = youtube!!.liveStreams().insert("snippet,cdn", stream)
            val returnedStream = liveStreamInsert.execute()

            // Print information from the API response.
            println("\n================== Returned Stream ==================\n")
            println("  - Id: " + returnedStream.id)
            println("  - Title: " + returnedStream.snippet.title)
            println("  - Description: " + returnedStream.snippet.description)
            println("  - Published At: " + returnedStream.snippet.publishedAt)

            // Construct and execute a request to bind the new broadcast
            // and stream.
            val liveBroadcastBind = youtube!!.liveBroadcasts().bind(returnedBroadcast.id, "id,contentDetails")
            liveBroadcastBind.streamId = returnedStream.id
            returnedBroadcast = liveBroadcastBind.execute()

            // Print information from the API response.
            println("\n================== Returned Bound Broadcast ==================\n")
            println("  - Broadcast Id: " + returnedBroadcast.id)
            println(
                    "  - Bound Stream Id: " + returnedBroadcast.contentDetails.boundStreamId)

        } catch (e: GoogleJsonResponseException) {
            System.err.println("GoogleJsonResponseException code: " + e.details.code + " : "
                    + e.details.message)
            e.printStackTrace()

        } catch (e: IOException) {
            System.err.println("IOException: " + e.message)
            e.printStackTrace()
        } catch (t: Throwable) {
            System.err.println("Throwable: " + t.message)
            t.printStackTrace()
        }

    }

}
