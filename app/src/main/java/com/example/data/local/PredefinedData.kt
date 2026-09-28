package com.example.data.local

import com.example.data.model.BlogPost
import com.example.data.model.BlogSourceType
import com.example.data.model.PredefinedPlaylists
import com.example.data.model.YouTubePlaylist
import com.example.data.model.YouTubeVideo

/**
 * Local hardcoded datasets providing baseline offline-first content.
 * Any list retrieved from Firebase Realtime Database is smartly merged with these,
 * deduplicated by ID/URL, and sorted with newest Firebase items on top.
 */
object PredefinedData {

    const val FALLBACK_SONG_SPREADSHEET_URL =
        "https://docs.google.com/spreadsheets/d/1GTftiR70HU4KAGEKndUR88blCRvFBbLKcbGQ8IFPAk4/export?format=csv"

    val hardcodedFellowshipBlogs: List<BlogPost> = emptyList()

    val hardcodedPersonalVlogs: List<BlogPost> = emptyList()

    val hardcodedVideos: List<YouTubeVideo> = emptyList()

    val hardcodedPlaylists: List<YouTubePlaylist> = PredefinedPlaylists.items

    val hardcodedFellowshipEvents: List<com.example.data.model.FellowshipEvent> = listOf(
        com.example.data.model.FellowshipEvent(
            id = "default_fellowship_1",
            title = "रविवार की मुख्य आराधना संगति (Sunday Morning Worship Service)",
            description = "परमेश्वर की स्तुति, आराधना और प्रभु के जीवंत वचन का प्रचार। आप अपने परिवार सहित सादर आमंत्रित हैं।",
            dateString = "हर रविवार (Every Sunday)",
            timeString = "सुबह 10:00 AM - 12:30 PM",
            locationString = "New Creation Church, Main Hall",
            speaker = "Pastor Vinay Kumar AVJ",
            category = "Sunday Worship",
            startTimestamp = System.currentTimeMillis() + (2 * 24 * 60 * 60 * 1000L),
            rsvpCount = 42,
            meetingUrl = "https://www.youtube.com/@vinaykumaravj",
            isOnline = false,
            imageUrl = ""
        )
    )
}

