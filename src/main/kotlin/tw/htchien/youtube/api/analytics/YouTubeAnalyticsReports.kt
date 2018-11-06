package tw.htchien.youtube.api.analytics

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtubeAnalytics.YouTubeAnalytics
import com.google.api.services.youtubeAnalytics.model.ResultTable
import com.google.common.collect.Lists
import tw.htchien.youtube.api.Auth

import java.io.IOException
import java.io.PrintStream
import java.math.BigDecimal

/**
 * This example uses the YouTube Data and YouTube Analytics APIs to retrieve
 * YouTube Analytics data. It also uses OAuth 2.0 for authorization.
 *
 * @author Christoph Schwab-Ganser and Jeremy Walker
 */
object YouTubeAnalyticsReports {

    /**
     * Define a global instance of the HTTP transport.
     */
    private val HTTP_TRANSPORT = NetHttpTransport()

    /**
     * Define a global instance of the JSON factory.
     */
    private val JSON_FACTORY = JacksonFactory()

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private var youtube: YouTube? = null

    /**
     * Define a global instance of a YoutubeAnalytics object, which will be
     * used to make YouTube Analytics API requests.
     */
    private var analytics: YouTubeAnalytics? = null

    /**
     * This code authorizes the user, uses the YouTube Data API to retrieve
     * information about the user's YouTube channel, and then fetches and
     * prints statistics for the user's channel using the YouTube Analytics API.
     *
     * @param args command line args (not used).
     */
    @JvmStatic
    fun main(args: Array<String>) {

        // These scopes are required to access information about the
        // authenticated user's YouTube channel as well as Analytics
        // data for that channel.
        val scopes = Lists.newArrayList(
                "https://www.googleapis.com/auth/yt-analytics.readonly",
                "https://www.googleapis.com/auth/youtube.readonly"
        )

        try {
            // Authorize the request.
            val credential = Auth.authorize(scopes, "analyticsreports")

            // This object is used to make YouTube Data API requests.
            youtube = YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName("youtube-analytics-api-report-example")
                    .build()

            // This object is used to make YouTube Analytics API requests.
            analytics = YouTubeAnalytics.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName("youtube-analytics-api-report-example")
                    .build()

            // Construct a request to retrieve the current user's channel ID.
            val channelRequest = youtube!!.channels().list("id,snippet")
            channelRequest.mine = true
            channelRequest.fields = "items(id,snippet/title)"
            val channels = channelRequest.execute()

            // List channels associated with the user.
            val listOfChannels = channels.items

            // The user's default channel is the first item in the list.
            val defaultChannel = listOfChannels[0]
            val channelId = defaultChannel.id

            val writer = System.out
            if (channelId == null) {
                writer.println("No channel found.")
            } else {
                writer.println("Default Channel: " + defaultChannel.snippet.title +
                        " ( " + channelId + " )\n")

                printData(writer, "Views Over Time.", executeViewsOverTimeQuery(analytics!!, channelId))
                printData(writer, "Top Videos", executeTopVideosQuery(analytics!!, channelId))
                printData(writer, "Demographics", executeDemographicsQuery(analytics!!, channelId))
            }
        } catch (e: IOException) {
            System.err.println("IOException: " + e.message)
            e.printStackTrace()
        } catch (t: Throwable) {
            System.err.println("Throwable: " + t.message)
            t.printStackTrace()
        }

    }

    /**
     * Retrieve the views and unique viewers per day for the channel.
     *
     * @param analytics The service object used to access the Analytics API.
     * @param id        The channel ID from which to retrieve data.
     * @return The API response.
     * @throws IOException if an API error occurred.
     */
    @Throws(IOException::class)
    private fun executeViewsOverTimeQuery(analytics: YouTubeAnalytics,
                                          id: String): ResultTable {

        return analytics.reports()
                .query("channel==$id", // channel id
                        "2012-01-01", // Start date.
                        "2012-01-14", // End date.
                        "views,uniques")      // Metrics.
                .setDimensions("day")
                .setSort("day")
                .execute()
    }

    /**
     * Retrieve the channel's 10 most viewed videos in descending order.
     *
     * @param analytics the analytics service object used to access the API.
     * @param id        the string id from which to retrieve data.
     * @return the response from the API.
     * @throws IOException if an API error occurred.
     */
    @Throws(IOException::class)
    private fun executeTopVideosQuery(analytics: YouTubeAnalytics,
                                      id: String): ResultTable {

        return analytics.reports()
                .query("channel==$id", // channel id
                        "2012-01-01", // Start date.
                        "2012-08-14", // End date.
                        "views,subscribersGained,subscribersLost") // Metrics.
                .setDimensions("video")
                .setSort("-views")
                .setMaxResults(10)
                .execute()
    }

    /**
     * Retrieve the demographics report for the channel.
     *
     * @param analytics the analytics service object used to access the API.
     * @param id        the string id from which to retrieve data.
     * @return the response from the API.
     * @throws IOException if an API error occurred.
     */
    @Throws(IOException::class)
    private fun executeDemographicsQuery(analytics: YouTubeAnalytics,
                                         id: String): ResultTable {
        return analytics.reports()
                .query("channel==$id", // channel id
                        "2007-01-01", // Start date.
                        "2012-08-14", // End date.
                        "viewerPercentage")   // Metrics.
                .setDimensions("ageGroup,gender")
                .setSort("-viewerPercentage")
                .execute()
    }

    /**
     * Prints the API response. The channel name is printed along with
     * each column name and all the data in the rows.
     *
     * @param writer  stream to output to
     * @param title   title of the report
     * @param results data returned from the API.
     */
    private fun printData(writer: PrintStream, title: String, results: ResultTable) {
        writer.println("Report: $title")
        if (results.rows == null || results.rows.isEmpty()) {
            writer.println("No results Found.")
        } else {

            // Print column headers.
            for (header in results.columnHeaders) {
                writer.printf("%30s", header.name)
            }
            writer.println()

            // Print actual data.
            for (row in results.rows) {
                for (colNum in 0 until results.columnHeaders.size) {
                    val header = results.columnHeaders[colNum]
                    val column = row[colNum]
                    if ("INTEGER" == header.unknownKeys["dataType"]) {
                        val l = (column as BigDecimal).toLong()
                        writer.printf("%30d", l)
                    } else if ("FLOAT" == header.unknownKeys["dataType"]) {
                        writer.printf("%30f", column)
                    } else if ("STRING" == header.unknownKeys["dataType"]) {
                        writer.printf("%30s", column)
                    } else {
                        // default output.
                        writer.printf("%30s", column)
                    }
                }
                writer.println()
            }
            writer.println()
        }
    }

}
