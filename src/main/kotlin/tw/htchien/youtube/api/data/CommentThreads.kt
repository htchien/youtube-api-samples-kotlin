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

package tw.htchien.youtube.api.data

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.Comment
import com.google.api.services.youtube.model.CommentSnippet
import com.google.api.services.youtube.model.CommentThread
import com.google.api.services.youtube.model.CommentThreadSnippet
import com.google.common.collect.Lists
import tw.htchien.youtube.api.Auth

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * This sample creates and manages top-level comments by:
 *
 * 1. Creating a top-level comments for a video and a channel via "commentThreads.insert" method.
 * 2. Retrieving the top-level comments for a video and a channel via "commentThreads.list" method.
 * 3. Updating an existing comments via "commentThreads.update" method.
 *
 * @author Ibrahim Ulukaya
 */
object CommentThreads {

    /**
     * Define a global instance of a YouTube object, which will be used to make
     * YouTube Data API requests.
     */
    private var youtube: YouTube? = null

    /*
     * Prompt the user to enter a channel ID. Then return the ID.
     */
    private val channelId: String
        @Throws(IOException::class)
        get() {

            var channelId = ""

            print("Please enter a channel id: ")
            val bReader = BufferedReader(InputStreamReader(System.`in`))
            channelId = bReader.readLine()

            return channelId
        }

    /*
     * Prompt the user to enter a video ID. Then return the ID.
     */
    private val videoId: String
        @Throws(IOException::class)
        get() {

            var videoId = ""

            print("Please enter a video id: ")
            val bReader = BufferedReader(InputStreamReader(System.`in`))
            videoId = bReader.readLine()

            return videoId
        }

    /*
     * Prompt the user to enter text for a comment. Then return the text.
     */
    private// If nothing is entered, defaults to "YouTube For Developers."
    val text: String
        @Throws(IOException::class)
        get() {

            var text = ""

            print("Please enter a comment text: ")
            val bReader = BufferedReader(InputStreamReader(System.`in`))
            text = bReader.readLine()

            if (text.length < 1) {
                text = "YouTube For Developers."
            }
            return text
        }

    /**
     * Create, list and update top-level channel and video comments.
     *
     * @param args command line args (not used).
     */
    @JvmStatic
    fun main(args: Array<String>) {

        // This OAuth 2.0 access scope allows for full read/write access to the
        // authenticated user's account and requires requests to use an SSL connection.
        val scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube.force-ssl")

        try {
            // Authorize the request.
            val credential = Auth.authorize(scopes, "commentthreads")

            // This object is used to make YouTube Data API requests.
            youtube = YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
                    .setApplicationName("youtube-cmdline-commentthreads-sample").build()

            // Prompt the user for the ID of a channel to comment on.
            // Retrieve the channel ID that the user is commenting to.
            val channelId = channelId
            println("You chose $channelId to subscribe.")

            // Prompt the user for the ID of a video to comment on.
            // Retrieve the video ID that the user is commenting to.
            val videoId = videoId
            println("You chose $videoId to subscribe.")

            // Prompt the user for the comment text.
            // Retrieve the text that the user is commenting.
            val text = text
            println("You chose $text to subscribe.")


            // Insert channel comment by omitting videoId.
            // Create a comment snippet with text.
            val commentSnippet = CommentSnippet()
            commentSnippet.textOriginal = text

            // Create a top-level comment with snippet.
            val topLevelComment = Comment()
            topLevelComment.snippet = commentSnippet

            // Create a comment thread snippet with channelId and top-level
            // comment.
            val commentThreadSnippet = CommentThreadSnippet()
            commentThreadSnippet.channelId = channelId
            commentThreadSnippet.topLevelComment = topLevelComment

            // Create a comment thread with snippet.
            val commentThread = CommentThread()
            commentThread.snippet = commentThreadSnippet

            // Call the YouTube Data API's commentThreads.insert method to
            // create a comment.
            val channelCommentInsertResponse = youtube!!.commentThreads()
                    .insert("snippet", commentThread).execute()
            // Print information from the API response.
            println("\n================== Created Channel Comment ==================\n")
            var snippet = channelCommentInsertResponse.snippet.topLevelComment
                    .snippet
            println("  - Author: " + snippet.authorDisplayName)
            println("  - Comment: " + snippet.textDisplay)
            println("\n-------------------------------------------------------------\n")


            // Insert video comment
            commentThreadSnippet.videoId = videoId
            // Call the YouTube Data API's commentThreads.insert method to
            // create a comment.
            val videoCommentInsertResponse = youtube!!.commentThreads()
                    .insert("snippet", commentThread).execute()
            // Print information from the API response.
            println("\n================== Created Video Comment ==================\n")
            snippet = videoCommentInsertResponse.snippet.topLevelComment
                    .snippet
            println("  - Author: " + snippet.authorDisplayName)
            println("  - Comment: " + snippet.textDisplay)
            println("\n-------------------------------------------------------------\n")


            // Call the YouTube Data API's commentThreads.list method to
            // retrieve video comment threads.
            val videoCommentsListResponse = youtube!!.commentThreads()
                    .list("snippet").setVideoId(videoId).setTextFormat("plainText").execute()
            val videoComments = videoCommentsListResponse.items

            if (videoComments.isEmpty()) {
                println("Can't get video comments.")
            } else {
                // Print information from the API response.
                println("\n================== Returned Video Comments ==================\n")
                for (videoComment in videoComments) {
                    snippet = videoComment.snippet.topLevelComment
                            .snippet
                    println("  - Author: " + snippet.authorDisplayName)
                    println("  - Comment: " + snippet.textDisplay)
                    println("\n-------------------------------------------------------------\n")
                }
                val firstComment = videoComments[0]
                firstComment.snippet.topLevelComment.snippet.textOriginal = "updated"
                val videoCommentUpdateResponse = youtube!!.commentThreads()
                        .update("snippet", firstComment).execute()
                // Print information from the API response.
                println("\n================== Updated Video Comment ==================\n")
                snippet = videoCommentUpdateResponse.snippet.topLevelComment
                        .snippet
                println("  - Author: " + snippet.authorDisplayName)
                println("  - Comment: " + snippet.textDisplay)
                println("\n-------------------------------------------------------------\n")

            }

            // Call the YouTube Data API's commentThreads.list method to
            // retrieve channel comment threads.
            val channelCommentsListResponse = youtube!!.commentThreads()
                    .list("snippet").setChannelId(channelId).setTextFormat("plainText").execute()
            val channelComments = channelCommentsListResponse.items

            if (channelComments.isEmpty()) {
                println("Can't get channel comments.")
            } else {
                // Print information from the API response.
                println("\n================== Returned Channel Comments ==================\n")
                for (channelComment in channelComments) {
                    snippet = channelComment.snippet.topLevelComment
                            .snippet
                    println("  - Author: " + snippet.authorDisplayName)
                    println("  - Comment: " + snippet.textDisplay)
                    println("\n-------------------------------------------------------------\n")
                }
                val firstComment = channelComments[0]
                firstComment.snippet.topLevelComment.snippet.textOriginal = "updated"
                val channelCommentUpdateResponse = youtube!!.commentThreads()
                        .update("snippet", firstComment).execute()
                // Print information from the API response.
                println("\n================== Updated Channel Comment ==================\n")
                snippet = channelCommentUpdateResponse.snippet.topLevelComment
                        .snippet
                println("  - Author: " + snippet.authorDisplayName)
                println("  - Comment: " + snippet.textDisplay)
                println("\n-------------------------------------------------------------\n")

            }

        } catch (e: GoogleJsonResponseException) {
            System.err.println("GoogleJsonResponseException code: " + e.details.code
                    + " : " + e.details.message)
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
