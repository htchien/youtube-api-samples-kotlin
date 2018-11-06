/*
 * Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tw.htchien.youtube.api.data

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.Playlist
import com.google.api.services.youtube.model.PlaylistItem
import com.google.api.services.youtube.model.PlaylistItemSnippet
import com.google.api.services.youtube.model.PlaylistSnippet
import com.google.api.services.youtube.model.PlaylistStatus
import com.google.api.services.youtube.model.ResourceId
import com.google.common.collect.Lists
import tw.htchien.youtube.api.Auth

import java.io.IOException
import java.util.Calendar

/**
 * Creates a new, private playlist in the authorized user's channel and add
 * a video to that new playlist.
 *
 * @author Jeremy Walker
 */
object PlaylistUpdates {

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private var youtube: YouTube? = null

    /**
     * Define a global variable that identifies the video that will be added
     * to the new playlist.
     */
    private val VIDEO_ID = "SZj6rAYkYOg"

    /**
     * Authorize the user, create a playlist, and add an item to the playlist.
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
            val credential = Auth.authorize(scopes, "playlistupdates")

            // This object is used to make YouTube Data API requests.
            youtube = YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
                    .setApplicationName("youtube-cmdline-playlistupdates-sample")
                    .build()

            // Create a new, private playlist in the authorized user's channel.
            val playlistId = insertPlaylist()

            // If a valid playlist was created, add a video to that playlist.
            insertPlaylistItem(playlistId, VIDEO_ID)

        } catch (e: GoogleJsonResponseException) {
            System.err.println("There was a service error: " + e.details.code + " : " + e.details.message)
            e.printStackTrace()
        } catch (e: IOException) {
            System.err.println("IOException: " + e.message)
            e.printStackTrace()
        } catch (t: Throwable) {
            System.err.println("Throwable: " + t.message)
            t.printStackTrace()
        }

    }

    /**
     * Create a playlist and add it to the authorized account.
     */
    @Throws(IOException::class)
    private fun insertPlaylist(): String {

        // This code constructs the playlist resource that is being inserted.
        // It defines the playlist's title, description, and privacy status.
        val playlistSnippet = PlaylistSnippet()
        playlistSnippet.title = "Test Playlist " + Calendar.getInstance().time
        playlistSnippet.description = "A private playlist created with the YouTube API v3"
        val playlistStatus = PlaylistStatus()
        playlistStatus.privacyStatus = "private"

        val youTubePlaylist = Playlist()
        youTubePlaylist.snippet = playlistSnippet
        youTubePlaylist.status = playlistStatus

        // Call the API to insert the new playlist. In the API call, the first
        // argument identifies the resource parts that the API response should
        // contain, and the second argument is the playlist being inserted.
        val playlistInsertCommand = youtube!!.playlists().insert("snippet,status", youTubePlaylist)
        val playlistInserted = playlistInsertCommand.execute()

        // Print data from the API response and return the new playlist's
        // unique playlist ID.
        println("New Playlist name: " + playlistInserted.snippet.title)
        println(" - Privacy: " + playlistInserted.status.privacyStatus)
        println(" - Description: " + playlistInserted.snippet.description)
        println(" - Posted: " + playlistInserted.snippet.publishedAt)
        println(" - Channel: " + playlistInserted.snippet.channelId + "\n")
        return playlistInserted.id

    }

    /**
     * Create a playlist item with the specified video ID and add it to the
     * specified playlist.
     *
     * @param playlistId assign to newly created playlistitem
     * @param videoId    YouTube video id to add to playlistitem
     */
    @Throws(IOException::class)
    private fun insertPlaylistItem(playlistId: String, videoId: String): String {

        // Define a resourceId that identifies the video being added to the
        // playlist.
        val resourceId = ResourceId()
        resourceId.kind = "youtube#video"
        resourceId.videoId = videoId

        // Set fields included in the playlistItem resource's "snippet" part.
        val playlistItemSnippet = PlaylistItemSnippet()
        playlistItemSnippet.title = "First video in the test playlist"
        playlistItemSnippet.playlistId = playlistId
        playlistItemSnippet.resourceId = resourceId

        // Create the playlistItem resource and set its snippet to the
        // object created above.
        val playlistItem = PlaylistItem()
        playlistItem.snippet = playlistItemSnippet

        // Call the API to add the playlist item to the specified playlist.
        // In the API call, the first argument identifies the resource parts
        // that the API response should contain, and the second argument is
        // the playlist item being inserted.
        val playlistItemsInsertCommand = youtube!!.playlistItems().insert("snippet,contentDetails", playlistItem)
        val returnedPlaylistItem = playlistItemsInsertCommand.execute()

        // Print data from the API response and return the new playlist
        // item's unique playlistItem ID.

        println("New PlaylistItem name: " + returnedPlaylistItem.snippet.title)
        println(" - Video id: " + returnedPlaylistItem.snippet.resourceId.videoId)
        println(" - Posted: " + returnedPlaylistItem.snippet.publishedAt)
        println(" - Channel: " + returnedPlaylistItem.snippet.channelId)
        return returnedPlaylistItem.id

    }
}
