/*
 * Copyright (c) 2012 Google Inc.
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

package tw.htchien.youtube.api.data

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.Activity
import com.google.api.services.youtube.model.ActivityContentDetails
import com.google.api.services.youtube.model.ActivityContentDetailsBulletin
import com.google.api.services.youtube.model.ActivitySnippet
import com.google.api.services.youtube.model.ResourceId
import com.google.common.collect.Lists
import tw.htchien.youtube.api.Auth

import java.util.Calendar

/**
 * Create a video bulletin that is posted to the user's channel feed.
 *
 * @author Jeremy Walker
 */
object ChannelBulletin {

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private var youtube: YouTube? = null

    /*
     * Define a global instance of the video ID that will be posted as a
     * bulletin into the user's channel feed. In practice, you will probably
     * retrieve this value from a search or your app.
     */
    private val VIDEO_ID = "L-oNKK1CrnU"


    /**
     * Authorize the user, call the youtube.channels.list method to retrieve
     * information about the user's YouTube channel, and post a bulletin with
     * a video ID to that channel.
     *
     * @param args command line args (not used).
     */
    @JvmStatic
    fun main(args: Array<String>) {

        // This OAuth 2.0 access scope allows for full read/write access to the
        // authenticated user's account.
        val scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube")

        try {
            // Authorize the request.
            val credential = Auth.authorize(scopes, "channelbulletin")

            // This object is used to make YouTube Data API requests.
            youtube = YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential).setApplicationName(
                    "youtube-cmdline-channelbulletin-sample").build()

            // Construct a request to retrieve the current user's channel ID.
            // See https://developers.google.com/youtube/v3/docs/channels/list
            val channelRequest = youtube!!.channels().list("contentDetails")
            channelRequest.mine = true

            // In the API response, only include channel information needed
            // for this use case.
            channelRequest.fields = "items/contentDetails"
            val channelResult = channelRequest.execute()

            val channelsList = channelResult.items

            if (channelsList != null) {
                // The user's default channel is the first item in the list.
                val channelId = channelsList[0].id

                // Create the snippet for the activity resource that
                // represents the channel bulletin. Set its channel ID
                // and description.
                val snippet = ActivitySnippet()
                snippet.channelId = channelId
                val cal = Calendar.getInstance()
                snippet.description = "Bulletin test video via YouTube API on " + cal.time

                // Create a resourceId that identifies the video ID. You could
                // set the kind to "youtube#playlist" and use a playlist ID
                // instead of a video ID.
                val resource = ResourceId()
                resource.kind = "youtube#video"
                resource.videoId = VIDEO_ID

                val bulletin = ActivityContentDetailsBulletin()
                bulletin.resourceId = resource

                // Construct the ActivityContentDetails object for the request.
                val contentDetails = ActivityContentDetails()
                contentDetails.bulletin = bulletin

                // Construct the resource, including the snippet and content
                // details, to send in the activities.insert
                val activity = Activity()
                activity.snippet = snippet
                activity.contentDetails = contentDetails

                // The API request identifies the resource parts that are being
                // written (contentDetails and snippet). The API response will
                // also include those parts.
                val insertActivities = youtube!!.activities().insert("contentDetails,snippet", activity)
                // Return the newly created activity resource.
                val newActivityInserted = insertActivities.execute()

                if (newActivityInserted != null) {
                    println(
                            "New Activity inserted of type " + newActivityInserted.snippet.type)
                    println(" - Video id " + newActivityInserted.contentDetails.bulletin.resourceId.videoId)
                    println(
                            " - Description: " + newActivityInserted.snippet.description)
                    println(" - Posted on " + newActivityInserted.snippet.publishedAt)
                } else {
                    println("Activity failed.")
                }

            } else {
                println("No channels are assigned to this user.")
            }
        } catch (e: GoogleJsonResponseException) {
            e.printStackTrace()
            System.err.println("There was a service error: " + e.details.code + " : "
                    + e.details.message)

        } catch (t: Throwable) {
            t.printStackTrace()
        }

    }
}
