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
import com.google.api.services.youtube.model.LiveChatMessage
import com.google.api.services.youtube.model.LiveChatMessageSnippet
import com.google.api.services.youtube.model.LiveChatTextMessageDetails
import com.google.common.collect.Lists
import tw.htchien.youtube.api.Auth

import java.io.IOException

/**
 * Inserts a message into a live broadcast of the current user or a video specified by id.
 *
 * The videoId is often included in the video's url, e.g.:
 * https://www.youtube.com/watch?v=L5Xc93_ZL60
 * ^ videoId
 * The video URL may be found in the browser address bar, or by right-clicking a video and selecting
 * Copy video URL from the context menu.
 *
 * @author Jim Rogers
 */
object InsertLiveChatMessage {

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private var youtube: YouTube? = null

    /**
     * Inserts a message into a live broadcast.
     *
     * @param args The message to insert (required) followed by the videoId (optional).
     * If the videoId is given, live chat messages will be retrieved from the chat associated with
     * this video. If the videoId is not specified, the signed in user's current live broadcast will
     * be used instead.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        // Get the chat message to insert
        if (args.size == 0) {
            System.err.println("No message specified")
            System.exit(1)
        }
        val message = args[0]

        // This OAuth 2.0 access scope allows for write access to the authenticated user's account.
        val scopes = Lists.newArrayList(YouTubeScopes.YOUTUBE_FORCE_SSL)

        try {
            // Authorize the request.
            val credential = Auth.authorize(scopes, "insertlivechatmessage")

            // This object is used to make YouTube Data API requests.
            youtube = YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
                    .setApplicationName("youtube-cmdline-insertchatmessage-sample").build()

            // Get the liveChatId
            val liveChatId = if (args.size == 2)
                GetLiveChatId.getLiveChatId(youtube!!, args[1])
            else
                GetLiveChatId.getLiveChatId(youtube!!)
            if (liveChatId != null) {
                println("Live chat id: $liveChatId")
            } else {
                System.err.println("Unable to find a live chat id")
                System.exit(1)
            }

            // Insert the message into live chat
            val liveChatMessage = LiveChatMessage()
            val snippet = LiveChatMessageSnippet()
            snippet.type = "textMessageEvent"
            snippet.liveChatId = liveChatId
            val details = LiveChatTextMessageDetails()
            details.messageText = message
            snippet.textMessageDetails = details
            liveChatMessage.snippet = snippet
            val liveChatInsert = youtube!!.liveChatMessages().insert("snippet", liveChatMessage)
            val response = liveChatInsert.execute()
            println("Inserted message id " + response.id)
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
}
