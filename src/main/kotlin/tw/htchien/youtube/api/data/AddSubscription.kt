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
import com.google.api.services.youtube.model.ResourceId
import com.google.api.services.youtube.model.Subscription
import com.google.api.services.youtube.model.SubscriptionSnippet
import com.google.common.collect.Lists
import tw.htchien.youtube.api.Auth

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Subscribe a user to a channel using the YouTube Data API (v3). Use
 * OAuth 2.0 for authorization.
 *
 * @author Ibrahim Ulukaya
 */
object AddSubscription {

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private var youtube: YouTube? = null

    /*
     * Prompt the user to enter a channel ID and return it.
     */
    private// If nothing is entered, defaults to "YouTube For Developers."
    val channelId: String
        @Throws(IOException::class)
        get() {
            print("Please enter a channel id: ")
            val bReader = BufferedReader(InputStreamReader(System.`in`))
            var channelId = bReader.readLine()

            if (channelId.length < 1) {
                channelId = "UCtVd0c0tGXuTSbU5d8cSBUg"
            }
            return channelId
        }

    /**
     * Subscribe the user's YouTube account to a user-selected channel.
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
            val credential = Auth.authorize(scopes, "addsubscription")

            // This object is used to make YouTube Data API requests.
            youtube = YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential).setApplicationName(
                    "youtube-cmdline-addsubscription-sample").build()

            // We get the user selected channel to subscribe.
            // Retrieve the channel ID that the user is subscribing to.
            val channelId = channelId
            println("You chose $channelId to subscribe.")

            // Create a resourceId that identifies the channel ID.
            val resourceId = ResourceId()
            resourceId.channelId = channelId
            resourceId.kind = "youtube#channel"

            // Create a snippet that contains the resourceId.
            val snippet = SubscriptionSnippet()
            snippet.resourceId = resourceId

            // Create a request to add the subscription and send the request.
            // The request identifies subscription metadata to insert as well
            // as information that the API server should return in its response.
            val subscription = Subscription()
            subscription.snippet = snippet
            val subscriptionInsert = youtube!!.subscriptions().insert("snippet,contentDetails", subscription)
            val returnedSubscription = subscriptionInsert.execute()

            // Print information from the API response.
            println("\n================== Returned Subscription ==================\n")
            println("  - Id: " + returnedSubscription.id)
            println("  - Title: " + returnedSubscription.snippet.title)

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
