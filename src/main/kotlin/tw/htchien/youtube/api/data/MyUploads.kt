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
import com.google.api.services.youtube.model.PlaylistItem
import com.google.common.collect.Lists
import tw.htchien.youtube.api.Auth

import java.util.ArrayList

/**
 * Print a list of videos uploaded to the authenticated user's YouTube channel.
 *
 * @author Jeremy Walker
 */
object MyUploads {

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private var youtube: YouTube? = null

    /**
     * Authorize the user, call the youtube.channels.list method to retrieve
     * the playlist ID for the list of videos uploaded to the user's channel,
     * and then call the youtube.playlistItems.list method to retrieve the
     * list of videos in that playlist.
     *
     * @param args command line args (not used).
     */
    @JvmStatic
    fun main(args: Array<String>) {

        // This OAuth 2.0 access scope allows for read-only access to the
        // authenticated user's account, but not other types of account access.
        val scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube.readonly")

        try {
            // Authorize the request.
            val credential = Auth.authorize(scopes, "myuploads")

            // This object is used to make YouTube Data API requests.
            youtube = YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential).setApplicationName(
                    "youtube-cmdline-myuploads-sample").build()

            // Call the API's channels.list method to retrieve the
            // resource that represents the authenticated user's channel.
            // In the API response, only include channel information needed for
            // this use case. The channel's contentDetails part contains
            // playlist IDs relevant to the channel, including the ID for the
            // list that contains videos uploaded to the channel.
            val channelRequest = youtube!!.channels().list("contentDetails")
            channelRequest.mine = true
            channelRequest.fields = "items/contentDetails,nextPageToken,pageInfo"
            val channelResult = channelRequest.execute()

            val channelsList = channelResult.items

            if (channelsList != null) {
                // The user's default channel is the first item in the list.
                // Extract the playlist ID for the channel's videos from the
                // API response.
                val uploadPlaylistId = channelsList[0].contentDetails.relatedPlaylists.uploads

                // Define a list to store items in the list of uploaded videos.
                val playlistItemList = ArrayList<PlaylistItem>()

                // Retrieve the playlist of the channel's uploaded videos.
                val playlistItemRequest = youtube!!.playlistItems().list("id,contentDetails,snippet")
                playlistItemRequest.playlistId = uploadPlaylistId

                // Only retrieve data used in this application, thereby making
                // the application more efficient. See:
                // https://developers.google.com/youtube/v3/getting-started#partial
                playlistItemRequest.fields = "items(contentDetails/videoId,snippet/title,snippet/publishedAt),nextPageToken,pageInfo"

                var nextToken: String? = ""

                // Call the API one or more times to retrieve all items in the
                // list. As long as the API response returns a nextPageToken,
                // there are still more items to retrieve.
                do {
                    playlistItemRequest.pageToken = nextToken
                    val playlistItemResult = playlistItemRequest.execute()

                    playlistItemList.addAll(playlistItemResult.items)

                    nextToken = playlistItemResult.nextPageToken
                } while (nextToken != null)

                // Prints information about the results.
                prettyPrint(playlistItemList.size, playlistItemList.iterator())
            }

        } catch (e: GoogleJsonResponseException) {
            e.printStackTrace()
            System.err.println("There was a service error: " + e.details.code + " : "
                    + e.details.message)

        } catch (t: Throwable) {
            t.printStackTrace()
        }

    }

    /*
     * Print information about all of the items in the playlist.
     *
     * @param size size of list
     *
     * @param iterator of Playlist Items from uploaded Playlist
     */
    private fun prettyPrint(size: Int, playlistEntries: Iterator<PlaylistItem>) {
        println("=============================================================")
        println("\t\tTotal Videos Uploaded: $size")
        println("=============================================================\n")

        while (playlistEntries.hasNext()) {
            val playlistItem = playlistEntries.next()
            println(" video name  = " + playlistItem.snippet.title)
            println(" video id    = " + playlistItem.contentDetails.videoId)
            println(" upload date = " + playlistItem.snippet.publishedAt)
            println("\n-------------------------------------------------------------\n")
        }
    }
}
