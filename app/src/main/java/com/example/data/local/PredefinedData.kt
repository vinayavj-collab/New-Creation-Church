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
        ),
        com.example.data.model.FellowshipEvent(
            id = "default_fellowship_2",
            title = "बुधवार मध्य-सप्ताह प्रार्थना एवं वचन अध्ययन (Midweek Bible Study)",
            description = "वचन के गहरे रहस्यों का अध्ययन और सामूहिक प्रार्थना सभा।",
            dateString = "हर बुधवार (Every Wednesday)",
            timeString = "शाम 07:00 PM - 08:30 PM",
            locationString = "ऑनलाइन ज़ूम / YouTube Live & Prayer Hall",
            speaker = "Bro. Vinay Kumar AVJ",
            category = "Bible Study",
            startTimestamp = System.currentTimeMillis() + (5 * 24 * 60 * 60 * 1000L),
            rsvpCount = 28,
            meetingUrl = "https://meet.google.com",
            isOnline = true,
            imageUrl = ""
        ),
        com.example.data.model.FellowshipEvent(
            id = "default_fellowship_3",
            title = "मासिक उपवास एवं आत्मिक जागृति सभा (Monthly Fasting & Prayer)",
            description = "राष्ट्र, कलीसिया और व्यक्तिगत आत्मिक वृद्धि हेतु विशेष उपवास और प्रार्थना संगति।",
            dateString = "महीने का पहला शनिवार (1st Saturday)",
            timeString = "सुबह 10:00 AM - दोपहर 03:00 PM",
            locationString = "Fellowship Center / Main Sanctuary",
            speaker = "Pastor Vinay Kumar AVJ & Guest Ministers",
            category = "Prayer Meeting",
            startTimestamp = System.currentTimeMillis() + (12 * 24 * 60 * 60 * 1000L),
            rsvpCount = 65,
            meetingUrl = "",
            isOnline = false,
            imageUrl = ""
        ),
        com.example.data.model.FellowshipEvent(
            id = "default_fellowship_4",
            title = "युवा संगति एवं आराधना सत्र (Youth Fellowship & Revival)",
            description = "युवाओं के लिए विशेष गीत, आराधना, गवाहियां और जीवन मार्गदर्शन।",
            dateString = "हर शनिवार (Every Saturday)",
            timeString = "शाम 05:00 PM - 07:00 PM",
            locationString = "Youth Hall, NCCK",
            speaker = "Youth Leaders & Pastor Vinay",
            category = "Youth Fellowship",
            startTimestamp = System.currentTimeMillis() + (4 * 24 * 60 * 60 * 1000L),
            rsvpCount = 35,
            meetingUrl = "",
            isOnline = false,
            imageUrl = ""
        )
    )
}

