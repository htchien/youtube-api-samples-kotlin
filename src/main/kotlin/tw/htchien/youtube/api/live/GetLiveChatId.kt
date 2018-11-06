/*
 * Copyright (c) 2017 Google Inc.
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
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeScopes
import com.google.common.collect.Lists
import tw.htchien.youtube.api.Auth

import java.io.IOException

/**
 * Gets a live chat id from a video id or current signed in user.
 *
 * The videoId is often included in the video's url, e.g.:
 * https://www.youtube.com/watch?v=L5Xc93_ZL60
 * ^ videoId
 * The video URL may be found in the browser address bar, or by right-clicking a video and selecting
 * Copy video URL from the context menu.
 *
 * @author Jim Rogers
 */
object GetLiveChatId {

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private var youtube: YouTube? = null

    /**
     * Poll live chat messages and SuperChat details from a live broadcast.
     *
     * @param args videoId (optional). If the videoId is given, live chat messages will be retrieved
     * from the chat associated with this video. If the videoId is not specified, the signed in
     * user's current live broadcast will be used instead.
     */
    @JvmStatic
    fun main(args: Array<String>) {

        // This OAuth 2.0 access scope allows for read-only access to the
        // authenticated user's account, but not other types of account access.
        val scopes = Lists.newArrayList(YouTubeScopes.YOUTUBE_READONLY)

        try {
            // Authorize the request.
            val credential = Auth.authorize(scopes, "getlivechatid")

            // This object is used to make YouTube Data API requests.
            youtube = YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
                    .setApplicationName("youtube-cmdline-getlivechatid-sample").build()

            // Get the liveChatId
            val liveChatId = if (args.size == 1)
                getLiveChatId(youtube!!, args[0])
            else
                getLiveChatId(youtube!!)
            if (liveChatId != null) {
                println("Live chat id: $liveChatId")
            } else {
                System.err.println("Unable to find a live chat id")
                System.exit(1)
            }
        } catch (e: GoogleJsonResponseException) {
            System.err
                    .println("GoogleJsonResponseException code: " + e.details.code + " : "
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

    /**
     * Retrieves the liveChatId from the authenticated user's live broadcast.
     *
     * @param youtube The object is used to make YouTube Data API requests.
     * @return A liveChatId, or null if not found.
     */
    @Throws(IOException::class)
    internal fun getLiveChatId(youtube: YouTube): String? {
        // Get signed in user's liveChatId
        val broadcastList = youtube
                .liveBroadcasts()
                .list("snippet")
                .setFields("items/snippet/liveChatId")
                .setBroadcastType("all")
                .setBroadcastStatus("active")
        val broadcastListResponse = broadcastList.execute()
        for (b in broadcastListResponse.items) {
            val liveChatId = b.snippet.liveChatId
            if (liveChatId != null && !liveChatId.isEmpty()) {
                return liveChatId
            }
        }

        return null
    }

    /**
     * Retrieves the liveChatId from the broadcast associated with a videoId.
     *
     * @param youtube The object is used to make YouTube Data API requests.
     * @param videoId The videoId associated with the live broadcast.
     * @return A liveChatId, or null if not found.
     */
    @Throws(IOException::class)
    internal fun getLiveChatId(youtube: YouTube, videoId: String): String? {
        // Get liveChatId from the video
        val videoList = youtube.videos()
                .list("liveStreamingDetails")
                .setFields("items/liveStreamingDetails/activeLiveChatId")
                .setId(videoId)
        val response = videoList.execute()
        for (v in response.items) {
            val liveChatId = v.liveStreamingDetails.activeLiveChatId
            if (liveChatId != null && !liveChatId.isEmpty()) {
                return liveChatId
            }
        }

        return null
    }
}
