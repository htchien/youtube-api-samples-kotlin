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
import com.google.common.collect.Lists
import tw.htchien.youtube.api.Auth

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * This sample creates and manages comments by:
 *
 * 1. Retrieving the top-level comments for a video via "commentThreads.list" method.
 * 2. Replying to a comment thread via "comments.insert" method.
 * 3. Retrieving comment replies via "comments.list" method.
 * 4. Updating an existing comment via "comments.update" method.
 * 5. Sets moderation status of an existing comment via "comments.setModerationStatus" method.
 * 6. Marking a comment as spam via "comments.markAsSpam" method.
 * 7. Deleting an existing comment via "comments.delete" method.
 *
 * @author Ibrahim Ulukaya
 */
object CommentHandling {

    /**
     * Define a global instance of a YouTube object, which will be used to make
     * YouTube Data API requests.
     */
    private var youtube: YouTube? = null

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
     * List, reply to comment threads; list, update, moderate, mark and delete
     * replies.
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

            // Prompt the user for the ID of a video to comment on.
            // Retrieve the video ID that the user is commenting to.
            val videoId = videoId
            println("You chose $videoId to subscribe.")

            // Prompt the user for the comment text.
            // Retrieve the text that the user is commenting.
            val text = text
            println("You chose $text to subscribe.")

            // All the available methods are used in sequence just for the sake
            // of an example.

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
                    val snippet = videoComment.snippet.topLevelComment
                            .snippet
                    println("  - Author: " + snippet.authorDisplayName)
                    println("  - Comment: " + snippet.textDisplay)
                    println("\n-------------------------------------------------------------\n")
                }
                val firstComment = videoComments[0]

                // Will use this thread as parent to new reply.
                val parentId = firstComment.id

                // Create a comment snippet with text.
                val commentSnippet = CommentSnippet()
                commentSnippet.textOriginal = text
                commentSnippet.parentId = parentId

                // Create a comment with snippet.
                val comment = Comment()
                comment.snippet = commentSnippet

                // Call the YouTube Data API's comments.insert method to reply
                // to a comment.
                // (If the intention is to create a new top-level comment,
                // commentThreads.insert
                // method should be used instead.)
                val commentInsertResponse = youtube!!.comments().insert("snippet", comment)
                        .execute()

                // Print information from the API response.
                println("\n================== Created Comment Reply ==================\n")
                var snippet = commentInsertResponse.snippet
                println("  - Author: " + snippet.authorDisplayName)
                println("  - Comment: " + snippet.textDisplay)
                println("\n-------------------------------------------------------------\n")

                // Call the YouTube Data API's comments.list method to retrieve
                // existing comment
                // replies.
                val commentsListResponse = youtube!!.comments().list("snippet")
                        .setParentId(parentId).setTextFormat("plainText").execute()
                val comments = commentsListResponse.items

                if (comments.isEmpty()) {
                    println("Can't get comment replies.")
                } else {
                    // Print information from the API response.
                    println("\n================== Returned Comment Replies ==================\n")
                    for (commentReply in comments) {
                        snippet = commentReply.snippet
                        println("  - Author: " + snippet.authorDisplayName)
                        println("  - Comment: " + snippet.textDisplay)
                        println("\n-------------------------------------------------------------\n")
                    }
                    val firstCommentReply = comments[0]
                    firstCommentReply.snippet.textOriginal = "updated"
                    val commentUpdateResponse = youtube!!.comments()
                            .update("snippet", firstCommentReply).execute()
                    // Print information from the API response.
                    println("\n================== Updated Video Comment ==================\n")
                    snippet = commentUpdateResponse.snippet
                    println("  - Author: " + snippet.authorDisplayName)
                    println("  - Comment: " + snippet.textDisplay)
                    println("\n-------------------------------------------------------------\n")

                    // Call the YouTube Data API's comments.setModerationStatus
                    // method to set moderation
                    // status of an existing comment.
                    youtube!!.comments().setModerationStatus(firstCommentReply.id, "published")
                    println("  -  Changed comment status to published: " + firstCommentReply.id)

                    // Call the YouTube Data API's comments.markAsSpam method to
                    // mark an existing comment as spam.
                    youtube!!.comments().markAsSpam(firstCommentReply.id)
                    println("  -  Marked comment as spam: " + firstCommentReply.id)

                    // Call the YouTube Data API's comments.delete method to
                    // delete an existing comment.
                    youtube!!.comments().delete(firstCommentReply.id)
                    println("  -  Deleted comment as spam: " + firstCommentReply.id)
                }
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
