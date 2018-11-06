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

package tw.htchien.youtube.api.live

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.youtube.YouTube
import com.google.common.collect.Lists
import tw.htchien.youtube.api.Auth

import java.io.IOException

/**
 * Retrieve a list of a channel's streams, using OAuth 2.0 to authorize
 * API requests.
 *
 * @author Ibrahim Ulukaya
 */
object ListStreams {

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private var youtube: YouTube? = null

    /**
     * List streams for the user's channel.
     */
    @JvmStatic
    fun main(args: Array<String>) {

        // This OAuth 2.0 access scope allows for read-only access to the
        // authenticated user's account, but not other types of account access.
        val scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube.readonly")

        try {
            // Authorize the request.
            val credential = Auth.authorize(scopes, "liststreams")

            // This object is used to make YouTube Data API requests.
            youtube = YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
                    .setApplicationName("youtube-cmdline-liststreams-sample")
                    .build()

            // Create a request to list liveStream resources.
            val livestreamRequest = youtube!!.liveStreams().list("id,snippet")

            // Modify results to only return the user's streams.
            livestreamRequest.mine = true

            // Execute the API request and return the list of streams.
            val returnedListResponse = livestreamRequest.execute()
            val returnedList = returnedListResponse.items

            // Print information from the API response.
            println("\n================== Returned Streams ==================\n")
            for (stream in returnedList) {
                println("  - Id: " + stream.id)
                println("  - Title: " + stream.snippet.title)
                println("  - Description: " + stream.snippet.description)
                println("  - Published At: " + stream.snippet.publishedAt)
                println("\n-------------------------------------------------------------\n")
            }

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
