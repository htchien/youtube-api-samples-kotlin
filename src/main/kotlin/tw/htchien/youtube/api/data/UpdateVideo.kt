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
import com.google.common.collect.Lists
import tw.htchien.youtube.api.Auth

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.ArrayList

/**
 * Update a video by adding a keyword tag to its metadata. The demo uses the
 * YouTube Data API (v3) and OAuth 2.0 for authorization.
 *
 * @author Ibrahim Ulukaya
 */
object UpdateVideo {

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private var youtube: YouTube? = null

    /*
     * Prompt the user to enter a keyword tag.
     */
    private// If the user doesn't enter a tag, use the default value "New Tag."
    val tagFromUser: String
        @Throws(IOException::class)
        get() {

            var keyword = ""

            print("Please enter a tag for your video: ")
            val bReader = BufferedReader(InputStreamReader(System.`in`))
            keyword = bReader.readLine()

            if (keyword.length < 1) {
                keyword = "New Tag"
            }
            return keyword
        }

    /*
     * Prompt the user to enter a video ID.
     */
    private// Exit if the user doesn't provide a value.
    val videoIdFromUser: String
        @Throws(IOException::class)
        get() {

            var videoId = ""

            print("Please enter a video Id to update: ")
            val bReader = BufferedReader(InputStreamReader(System.`in`))
            videoId = bReader.readLine()

            if (videoId.length < 1) {
                print("Video Id can't be empty!")
                System.exit(1)
            }

            return videoId
        }

    /**
     * Add a keyword tag to a video that the user specifies. Use OAuth 2.0 to
     * authorize the API request.
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
            val credential = Auth.authorize(scopes, "updatevideo")

            // This object is used to make YouTube Data API requests.
            youtube = YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
                    .setApplicationName("youtube-cmdline-updatevideo-sample").build()

            // Prompt the user to enter the video ID of the video being updated.
            val videoId = videoIdFromUser
            println("You chose $videoId to update.")

            // Prompt the user to enter a keyword tag to add to the video.
            val tag = tagFromUser
            println("You chose $tag as a tag.")

            // Call the YouTube Data API's youtube.videos.list method to
            // retrieve the resource that represents the specified video.
            val listVideosRequest = youtube!!.videos().list("snippet").setId(videoId)
            val listResponse = listVideosRequest.execute()

            // Since the API request specified a unique video ID, the API
            // response should return exactly one video. If the response does
            // not contain a video, then the specified video ID was not found.
            val videoList = listResponse.items
            if (videoList.isEmpty()) {
                println("Can't find a video with ID: $videoId")
                return
            }

            // Extract the snippet from the video resource.
            val video = videoList[0]
            val snippet = video.snippet

            // Preserve any tags already associated with the video. If the
            // video does not have any tags, create a new array. Append the
            // provided tag to the list of tags associated with the video.
            var tags: MutableList<String>? = snippet.tags
            if (tags == null) {
                tags = ArrayList(1)
                snippet.tags = tags
            }
            tags.add(tag)

            // Update the video resource by calling the videos.update() method.
            val updateVideosRequest = youtube!!.videos().update("snippet", video)
            val videoResponse = updateVideosRequest.execute()

            // Print information from the updated resource.
            println("\n================== Returned Video ==================\n")
            println("  - Title: " + videoResponse.snippet.title)
            println("  - Tags: " + videoResponse.snippet.tags)

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
