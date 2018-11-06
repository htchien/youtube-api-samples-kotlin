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
 * Delets a message from a live broadcast, using OAuth 2.0 to authorize API requests.
 *
 * @author Jim Rogers
 */
object DeleteLiveChatMessage {

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private var youtube: YouTube? = null

    /**
     * Deletes a message from a live broadcast.
     *
     * @param args The message id to delete (required) followed by the videoId (optional). If the
     * videoId is given, live chat messages will be retrieved from the chat associated with this
     * video. If the videoId is not specified, the signed in user's current live broadcast will be
     * used instead.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        // Get the message id to delete
        if (args.size == 0) {
            System.err.println("No message id specified")
            System.exit(1)
        }
        val messageId = args[0]

        // This OAuth 2.0 access scope allows for write access to the authenticated user's account.
        val scopes = Lists.newArrayList(YouTubeScopes.YOUTUBE_FORCE_SSL)

        try {
            // Authorize the request.
            val credential = Auth.authorize(scopes, "deletelivechatmessage")

            // This object is used to make YouTube Data API requests.
            youtube = YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
                    .setApplicationName("youtube-cmdline-deletechatmessages-sample").build()

            // Delete the message from live chat
            val liveChatDelete = youtube!!.liveChatMessages().delete(messageId)
            liveChatDelete.execute()
            println("Deleted message id $messageId")
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
