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

    val hardcodedFellowshipBlogs: List<BlogPost> = listOf(
        BlogPost(
            id = "local_fellowship_1",
            source = BlogSourceType.FELLOWSHIP_EVENTS,
            title = "प्रभु यीशु मसीह के साथ संगति का आनंद (Joy of Fellowship in Christ)",
            publishedDate = "2025-01-10",
            publishedTimestamp = 1736467200000L,
            labels = listOf("Fellowship", "Message", "Spiritual"),
            featuredImageUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEj_local_fellowship_1.jpg",
            allImages = listOf("https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEj_local_fellowship_1.jpg"),
            plainTextExcerpt = "मसीही संगति हमारे विश्वास को मजबूत बनाती है और परमेश्वर के वचन में बढ़ने की प्रेरणा देती है।",
            contentHtml = "<p>मसीही संगति हमारे विश्वास को मजबूत बनाती है और परमेश्वर के वचन में बढ़ने की प्रेरणा देती है। जब हम एक मन होकर प्रभु की आराधना करते हैं तो उसकी उपस्थिति का अनुभव होता है।</p>",
            url = "https://vinaykumaravj.blogspot.com/2025/01/joy-of-fellowship-in-christ.html"
        ),
        BlogPost(
            id = "local_fellowship_2",
            source = BlogSourceType.FELLOWSHIP_EVENTS,
            title = "प्रार्थना और उपवास सभा की रिपोर्ट (Prayer & Fasting Fellowship)",
            publishedDate = "2024-12-25",
            publishedTimestamp = 1735084800000L,
            labels = listOf("Events", "Prayer", "Revival"),
            featuredImageUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEj_local_fellowship_2.jpg",
            allImages = listOf("https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEj_local_fellowship_2.jpg"),
            plainTextExcerpt = "विशेष उपवास एवं प्रार्थना सभा में कई भाइयों और बहनों ने गवाहियां दीं और प्रभु की स्तुति की।",
            contentHtml = "<p>विशेष उपवास एवं प्रार्थना सभा में कई भाइयों और बहनों ने गवाहियां दीं और प्रभु की स्तुति की। परमेश्वर ने अनेकों प्रार्थनाओं का उत्तर दिया।</p>",
            url = "https://vinaykumaravj.blogspot.com/2024/12/prayer-and-fasting-fellowship.html"
        ),
        BlogPost(
            id = "local_fellowship_3",
            source = BlogSourceType.FELLOWSHIP_EVENTS,
            title = "वचन का प्रचार: अनुग्रह और सत्य (Grace and Truth Message)",
            publishedDate = "2024-11-15",
            publishedTimestamp = 1731628800000L,
            labels = listOf("Sermon", "Bible Study"),
            featuredImageUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEj_local_fellowship_3.jpg",
            allImages = listOf("https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEj_local_fellowship_3.jpg"),
            plainTextExcerpt = "यूहन्ना 1:17 - क्योंकि व्यवस्था तो मूसा के द्वारा दी गई; परन्तु अनुग्रह और सच्चाई यीशु मसीह के द्वारा पहुंची।",
            contentHtml = "<p>यूहन्ना 1:17 - क्योंकि व्यवस्था तो मूसा के द्वारा दी गई; परन्तु अनुग्रह और सच्चाई यीशु मसीह के द्वारा पहुंची।</p>",
            url = "https://vinaykumaravj.blogspot.com/2024/11/grace-and-truth-message.html"
        )
    )

    val hardcodedPersonalVlogs: List<BlogPost> = listOf(
        BlogPost(
            id = "local_vlog_1",
            source = BlogSourceType.PERSONAL_VLOG,
            title = "मेरी सेवकाई यात्रा और प्रभु की अगुवाई (Ministry Journey Vlog)",
            publishedDate = "2025-01-05",
            publishedTimestamp = 1736035200000L,
            labels = listOf("Vlog", "Testimony", "Life"),
            featuredImageUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEj_local_vlog_1.jpg",
            allImages = listOf("https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEj_local_vlog_1.jpg"),
            plainTextExcerpt = "इस व्लॉग में मैं अपनी सेवकाई यात्रा और प्रभु यीशु के अद्भुत अनुग्रह के अनुभव साझा कर रहा हूँ।",
            contentHtml = "<p>इस व्लॉग में मैं अपनी सेवकाई यात्रा और प्रभु यीशु के अद्भुत अनुग्रह के अनुभव साझा कर रहा हूँ।</p>",
            url = "https://vinayavj.blogspot.com/2025/01/ministry-journey-vlog.html"
        ),
        BlogPost(
            id = "local_vlog_2",
            source = BlogSourceType.PERSONAL_VLOG,
            title = "संगीत साधना और आराधना सत्र (Worship Recording Session)",
            publishedDate = "2024-12-18",
            publishedTimestamp = 1734480000000L,
            labels = listOf("Vlog", "Music", "Behind The Scenes"),
            featuredImageUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEj_local_vlog_2.jpg",
            allImages = listOf("https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEj_local_vlog_2.jpg"),
            plainTextExcerpt = "नए मसीही गीतों की रिकॉर्डिंग और अभ्यास सत्र की एक झलक।",
            contentHtml = "<p>नए मसीही गीतों की रिकॉर्डिंग और अभ्यास सत्र की एक झलक। प्रभु का धन्यवाद हो हर एक गीत के लिए।</p>",
            url = "https://vinayavj.blogspot.com/2024/12/worship-recording-session.html"
        )
    )

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
            imageUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEj_local_fellowship_1.jpg"
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
            imageUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEj_local_fellowship_3.jpg"
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
            imageUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEj_local_fellowship_2.jpg"
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
            imageUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEj_local_fellowship_1.jpg"
        )
    )
}

