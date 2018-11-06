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
import com.google.api.services.youtube.model.LiveChatMessageAuthorDetails
import com.google.api.services.youtube.model.LiveChatSuperChatDetails
import com.google.common.collect.Lists
import tw.htchien.youtube.api.Auth

import java.io.IOException
import java.util.ArrayList
import java.util.Timer
import java.util.TimerTask

/**
 * Lists live chat messages and SuperChat details from a live broadcast.
 *
 * The videoId is often included in the video's url, e.g.:
 * https://www.youtube.com/watch?v=L5Xc93_ZL60
 * ^ videoId
 * The video URL may be found in the browser address bar, or by right-clicking a video and selecting
 * Copy video URL from the context menu.
 *
 * @author Jim Rogers
 */
object ListLiveChatMessages {

    /**
     * Common fields to retrieve for chat messages
     */
    private val LIVE_CHAT_FIELDS = (
            "items(authorDetails(channelId,displayName,isChatModerator,isChatOwner,isChatSponsor,"
                    + "profileImageUrl),snippet(displayMessage,superChatDetails,publishedAt)),"
                    + "nextPageToken,pollingIntervalMillis")

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private var youtube: YouTube? = null

    /**
     * Lists live chat messages and SuperChat details from a live broadcast.
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
            val credential = Auth.authorize(scopes, "listlivechatmessages")

            // This object is used to make YouTube Data API requests.
            youtube = YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
                    .setApplicationName("youtube-cmdline-listchatmessages-sample").build()

            // Get the liveChatId
            val liveChatId = if (args.size == 1)
                GetLiveChatId.getLiveChatId(youtube!!, args[0])
            else
                GetLiveChatId.getLiveChatId(youtube!!)
            if (liveChatId != null) {
                println("Live chat id: $liveChatId")
            } else {
                System.err.println("Unable to find a live chat id")
                System.exit(1)
            }

            // Get live chat messages
            listChatMessages(liveChatId.toString(), null, 0)
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
     * Lists live chat messages, polling at the server supplied interval. Owners and moderators of a
     * live chat will poll at a faster rate.
     *
     * @param liveChatId The live chat id to list messages from.
     * @param nextPageToken The page token from the previous request, if any.
     * @param delayMs The delay in milliseconds before making the request.
     */
    private fun listChatMessages(
            liveChatId: String,
            nextPageToken: String?,
            delayMs: Long) {
        println(
                String.format("Getting chat messages in %1$.3f seconds...", delayMs * 0.001))
        val pollTimer = Timer()
        pollTimer.schedule(
                object : TimerTask() {
                    override fun run() {
                        try {
                            // Get chat messages from YouTube
                            val response = youtube!!
                                    .liveChatMessages()
                                    .list(liveChatId, "snippet, authorDetails")
                                    .setPageToken(nextPageToken)
                                    .setFields(LIVE_CHAT_FIELDS)
                                    .execute()

                            // Display messages and super chat details
                            val messages = response.items
                            for (i in messages.indices) {
                                val message = messages[i]
                                val snippet = message.snippet
                                println(buildOutput(
                                        snippet.displayMessage,
                                        message.authorDetails,
                                        snippet.superChatDetails))
                            }

                            // Request the next page of messages
                            listChatMessages(
                                    liveChatId,
                                    response.nextPageToken,
                                    response.pollingIntervalMillis!!)
                        } catch (t: Throwable) {
                            System.err.println("Throwable: " + t.message)
                            t.printStackTrace()
                        }

                    }
                }, delayMs)
    }

    /**
     * Formats a chat message for console output.
     *
     * @param message The display message to output.
     * @param author The author of the message.
     * @param superChatDetails SuperChat details associated with the message.
     * @return A formatted string for console output.
     */
    private fun buildOutput(
            message: String?,
            author: LiveChatMessageAuthorDetails,
            superChatDetails: LiveChatSuperChatDetails?): String {
        val output = StringBuilder()
        if (superChatDetails != null) {
            output.append(superChatDetails.amountDisplayString)
            output.append("SUPERCHAT RECEIVED FROM ")
        }
        output.append(author.displayName)
        val roles = ArrayList<String>()
        if (author.isChatOwner!!) {
            roles.add("OWNER")
        }
        if (author.isChatModerator!!) {
            roles.add("MODERATOR")
        }
        if (author.isChatSponsor!!) {
            roles.add("SPONSOR")
        }
        if (roles.size > 0) {
            output.append(" (")
            var delim = ""
            for (role in roles) {
                output.append(delim).append(role)
                delim = ", "
            }
            output.append(")")
        }
        if (message != null && !message.isEmpty()) {
            output.append(": ")
            output.append(message)
        }
        return output.toString()
    }
}
