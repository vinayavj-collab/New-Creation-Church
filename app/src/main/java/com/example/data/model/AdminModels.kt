package com.example.data.model

import androidx.annotation.Keep

@Keep
enum class AdminFunction(
    val id: String,
    val titleHindi: String,
    val titleEnglish: String,
    val description: String,
    val category: String
) {
    MANAGE_DESIGNATIONS(
        id = "fn_manage_designations",
        titleHindi = "पदनाम व पदानुक्रम प्रबंधन",
        titleEnglish = "Designations & Hierarchy",
        description = "नए पदनाम बनाना, उनके अधिकार सेट करना और फंक्शन कस्टमाइज करना",
        category = "प्रशासन (Administration)"
    ),
    CREATE_SUBORDINATES(
        id = "fn_create_subordinates",
        titleHindi = "जूनियर एडमिन बनाना व पासवर्ड (PIN) जारी करना",
        titleEnglish = "Create Subordinates & Issue PIN",
        description = "अपने अधीन पदनामों के लिए नए एडमिन बनाना और पासवर्ड जारी करना",
        category = "प्रशासन (Administration)"
    ),
    DELEGATE_PERMISSIONS(
        id = "fn_delegate_permissions",
        titleHindi = "अधिकार व फंक्शन वितरण (Delegation)",
        titleEnglish = "Delegate Allowed Authorities",
        description = "जूनियर को अपने अनुमत अधिकारों में से चुने गए अधिकार सौंपना",
        category = "प्रशासन (Administration)"
    ),
    SPECIAL_ANNOUNCEMENTS(
        id = "fn_special_announcements",
        titleHindi = "विशेष घोषणाएं व पॉपअप सूचना",
        titleEnglish = "Special Announcements & Alerts",
        description = "कलीसिया के लिए शीर्ष फ्लैश नोटिस व विशेष घोषणाएं पोस्ट करना",
        category = "प्रकाशन व मीडिया (Broadcast)"
    ),
    TODAY_SCRIPTURE_UPDATE(
        id = "fn_today_scripture",
        titleHindi = "आज का वचन व मनन अपडेट",
        titleEnglish = "Daily Scripture & Reflection",
        description = "प्रतिदिन का मुख्य बाइबल वचन, संदर्भ और आत्मिक विचार अपडेट करना",
        category = "प्रकाशन व मीडिया (Broadcast)"
    ),
    LIVE_STREAM_BROADCAST(
        id = "fn_live_stream",
        titleHindi = "लाइव आराधना सभा व प्रसारण",
        titleEnglish = "Live Worship Stream",
        description = "लाइव यूट्यूब आराधना, शीर्षक व समय सारणी नियंत्रित करना",
        category = "प्रकाशन व मीडिया (Broadcast)"
    ),
    PRAYER_REQUEST_MODERATION(
        id = "fn_prayer_moderation",
        titleHindi = "प्रार्थना निवेदन समीक्षा व स्वीकृति",
        titleEnglish = "Prayer Requests Moderation",
        description = "विश्वासियों द्वारा भेजे गए प्रार्थना निवेदनों की समीक्षा व स्वीकृति",
        category = "कलीसिया सेवा (Church Ministry)"
    ),
    PRAYER_REPLY_ENCOURAGEMENT(
        id = "fn_prayer_reply",
        titleHindi = "प्रार्थना निवेदन उत्तर व प्रोत्साहन",
        titleEnglish = "Prayer Request Reply & Note",
        description = "मास्टर एडमिन अथवा अधिकृत पास्टर द्वारा प्रार्थना निवेदन पर आत्मिक उत्तर व सांत्वना संदेश भेजना",
        category = "कलीसिया सेवा (Church Ministry)"
    ),
    CHURCH_EVENTS_CALENDAR(
        id = "fn_church_events",
        titleHindi = "चर्च कैलेंडर व आगामी कार्यक्रम",
        titleEnglish = "Church Events & Calendar",
        description = "विशेष आराधना, उपवास सभा और सम्मेलन कार्यक्रम जोड़ना",
        category = "कलीसिया सेवा (Church Ministry)"
    ),
    DAILY_PRAYER_AUDIO(
        id = "fn_daily_prayer_audio",
        titleHindi = "दैनिक ऑडियो प्रार्थना व संदेश",
        titleEnglish = "Daily Audio Prayers & Sermons",
        description = "प्रतिदिन की प्रार्थना ऑडियो और वचन संदेश प्रबंधित करना",
        category = "कलीसिया सेवा (Church Ministry)"
    ),
    MEMBER_DIRECTORY_VIEW(
        id = "fn_member_directory",
        titleHindi = "कलीसिया सदस्य सूची व विवरण",
        titleEnglish = "Member Directory & Records",
        description = "रजिस्टर्ड विश्वासी सदस्यों की सूची और संपर्क विवरण देखना",
        category = "कलीसिया सेवा (Church Ministry)"
    ),
    ATTENDANCE_TRACKER(
        id = "fn_attendance_tracker",
        titleHindi = "सभा उपस्थिति व सांख्यिकी",
        titleEnglish = "Attendance Tracker & Stats",
        description = "रविवार आराधना व प्रार्थना सभाओं की उपस्थिति दर्ज व विश्लेषित करना",
        category = "कलीसिया सेवा (Church Ministry)"
    ),
    CHURCH_ACCOUNTS_TITHES(
        id = "fn_church_accounts",
        titleHindi = "दशमांश व चर्च लेखा-जोखा",
        titleEnglish = "Tithes & Church Accounts",
        description = "दशमांश, दान, भेंट और चर्च खर्चों का पारदर्शी हिसाब रखना",
        category = "कलीसिया सेवा (Church Ministry)"
    ),
    PUSH_NOTIFICATION_BROADCAST(
        id = "fn_push_notifications",
        titleHindi = "पुश नोटिफिकेशन व आपात सूचना",
        titleEnglish = "Push Notifications & Alerts",
        description = "सभी विश्वासियों या विशेष दलों को तुरंत संदेश व सूचना भेजना",
        category = "प्रकाशन व मीडिया (Broadcast)"
    ),
    AUDIT_LOGS_VIEW(
        id = "fn_audit_logs",
        titleHindi = "एडमिन ऑडिट व गतिविधि लॉग",
        titleEnglish = "Admin Activity & Audit Trail",
        description = "प्रशासनिक गतिविधियों और परिवर्तनों का समयबद्ध रिकॉर्ड देखना",
        category = "सुरक्षा व व्यवस्था (Security)"
    ),
    DATA_EXPORT_BACKUP(
        id = "fn_data_export",
        titleHindi = "डेटा एक्सपोर्ट व बैकअप रिपोर्ट",
        titleEnglish = "Data Export & Reports",
        description = "सदस्य, उपस्थिति, व खातों का एक्सेल/सीएसवी प्रारूप में एक्सपोर्ट व शेयर",
        category = "सुरक्षा व व्यवस्था (Security)"
    ),
    BIBLE_READING_PLANS(
        id = "fn_bible_reading_plans",
        titleHindi = "बाइबल पठन योजना प्रबंधन",
        titleEnglish = "Bible Reading Plans",
        description = "कलीसिया के लिए बाइबल पठन योजनाएं जोड़ना और प्रबंधित करना",
        category = "कलीसिया सेवा (Church Ministry)"
    ),
    APP_BANNER_PROMO(
        id = "fn_app_banner_promo",
        titleHindi = "होम स्क्रीन बैनर व नोटिस",
        titleEnglish = "Home Screen Banners",
        description = "ऐप की मुख्य स्क्रीन पर विशेष चित्र बैनर और लिंक प्रदर्शित करना",
        category = "प्रकाशन व मीडिया (Broadcast)"
    ),
    SONG_SHEET_UPDATE(
        id = "fn_song_sheet",
        titleHindi = "गीत संग्रह व स्प्रेडशीट लिंक",
        titleEnglish = "Lyrics & Song Sheets Config",
        description = "मसीही गीतों की ऑनलाइन शीट लिंक व डेटा अपडेट करना",
        category = "प्रकाशन व मीडिया (Broadcast)"
    ),
    SECURITY_GMAIL_SETTINGS(
        id = "fn_security_settings",
        titleHindi = "सुरक्षा, Gmail लिंकिंग व रिकवरी",
        titleEnglish = "Security & Account Recovery",
        description = "खाता सुरक्षा, सक्रिय डिवाइस मॉनिटरिंग और Gmail रिकवरी",
        category = "सुरक्षा व व्यवस्था (Security)"
    ),
    APP_USAGE_ANALYTICS(
        id = "fn_app_usage_analytics",
        titleHindi = "📊 ऐप उपयोग आँकड़े व एनालिटिक्स",
        titleEnglish = "App Usage Analytics & Engagement",
        description = "ऑनलाइन यूज़र्स, कुल इंस्टॉल, शेयर और बाइबल रीडिंग आँकड़े देखना",
        category = "प्रशासन (Administration)"
    ),
    FIREBASE_STORAGE_QUOTAS(
        id = "fn_firebase_storage_quotas",
        titleHindi = "🔥 फ़ायरबेस स्टोरेज व कोटा मॉनिटर",
        titleEnglish = "Firebase Cloud Storage & Quotas",
        description = "क्लाउड डेटाबेस MB इस्तेमाल, फ़ोटो कोटा और दैनिक लिमिट देखना",
        category = "सुरक्षा व व्यवस्था (Security)"
    );

    val hindiTitle: String get() = titleHindi

    companion object {
        fun allFunctionIds(): List<String> = entries.map { it.id }

        fun findById(id: String): AdminFunction? = entries.firstOrNull { it.id == id || it.name.equals(id, ignoreCase = true) }

        fun fromKey(key: String): AdminFunction? = findById(key)
    }
}

@Keep
data class PermissionItem(
    val key: String,
    val titleHindi: String,
    val titleEnglish: String,
    val description: String,
    val category: String
)

object AdminPermission {
    // Delegated Subordinate Management
    const val CAN_VIEW_ADMINS = "can_view_admins"
    const val CAN_ADD_ADMINS = "can_add_admins"
    const val CAN_EDIT_ADMINS = "can_edit_admins"
    const val CAN_DISABLE_ADMINS = "can_disable_admins"
    const val CAN_DELETE_ADMINS = "can_delete_admins"
    const val CAN_MANAGE_DESIGNATIONS = "can_manage_designations"
    const val CAN_MANAGE_ROLES = "can_manage_designations"
    const val CAN_GENERATE_OTP = "can_generate_otp"

    // Content & Ministry
    const val CAN_ADD_USERS = "can_add_users"
    const val CAN_EDIT_VERSE = "can_edit_verse"
    const val CAN_BROADCAST_ANNOUNCEMENTS = "can_broadcast_announcements"
    const val CAN_POST_ANNOUNCEMENTS = "can_broadcast_announcements"
    const val CAN_MANAGE_LIVE_STREAM = "can_manage_live_stream"
    const val CAN_BROADCAST_LIVE = "can_manage_live_stream"
    const val CAN_MODERATE_PRAYERS = "can_moderate_prayers"
    const val CAN_REPLY_PRAYERS = "can_reply_prayers"
    const val CAN_MANAGE_EVENTS = "can_manage_events"
    const val CAN_MANAGE_AUDIO_PRAYER = "can_manage_audio_prayer"
    const val CAN_VIEW_MEMBERS = "can_view_members"
    const val CAN_TRACK_ATTENDANCE = "can_track_attendance"
    const val CAN_MANAGE_ATTENDANCE = "can_track_attendance"
    const val CAN_MANAGE_ACCOUNTS = "can_manage_accounts"
    const val CAN_MANAGE_READING_PLANS = "can_manage_reading_plans"
    const val CAN_MANAGE_BANNERS = "can_manage_banners"
    const val CAN_MANAGE_SONG_SHEET = "can_manage_song_sheet"
    const val CAN_MANAGE_POLLS = "can_manage_polls"

    // System & Security
    const val CAN_SEND_PUSH = "can_send_push"
    const val CAN_BROADCAST_PUSH = "can_send_push"
    const val CAN_VIEW_AUDIT_LOGS = "can_view_audit_logs"
    const val CAN_VIEW_HISTORY = "can_view_history"
    const val CAN_EXPORT_DATA = "can_export_data"
    const val CAN_MANAGE_STORAGE_QUOTAS = "can_manage_storage_quotas"
    const val CAN_MANAGE_STORAGE = "can_manage_storage_quotas"
    const val CAN_VIEW_ANALYTICS = "can_view_analytics"
    const val CAN_MANAGE_SECURITY = "can_manage_security"

    fun getAllPermissions(): List<PermissionItem> {
        return listOf(
            // 1. Delegated Subordinate & User Management
            PermissionItem(CAN_VIEW_ADMINS, "अधीनस्थ एडमिन देखना (View Admins)", "View Subordinate Admins", "अपने से कनिष्ठ पदनामों की सूची देखना", "👥 प्रशासन व प्रबंधन"),
            PermissionItem(CAN_ADD_ADMINS, "नया एडमिन बनाना (Add Admins)", "Create Subordinate Admins", "कनिष्ठ पदों के लिए नए एडमिन बनाना व पासवर्ड जारी करना", "👥 प्रशासन व प्रबंधन"),
            PermissionItem(CAN_DISABLE_ADMINS, "एडमिन सक्रिय/निष्क्रिय करना (Disable Admins)", "Enable/Disable Subordinates", "अधीनस्थ एडमिन का खाता ब्लॉक अथवा अनब्लॉक करना", "👥 प्रशासन व प्रबंधन"),
            PermissionItem(CAN_DELETE_ADMINS, "एडमिन हटाना (Delete Admins)", "Delete Subordinates", "अधीनस्थ एडमिन खाते को स्थायी रूप से हटाना", "👥 प्रशासन व प्रबंधन"),
            PermissionItem(CAN_ADD_USERS, "कलीसिया सदस्य जोड़ना/संपादित करना", "Add/Edit Church Users", "चर्च सदस्यों की जानकारी दर्ज व संशोधित करना", "👥 प्रशासन व प्रबंधन"),
            PermissionItem(CAN_MANAGE_DESIGNATIONS, "पदनाम अधिकार प्रबंधन (Designations)", "Manage Designations", "पदनाम अधिकार व डिफ़ॉल्ट फंक्शन सेटिंग्स", "👥 प्रशासन व प्रबंधन"),

            // 2. Publication & Media
            PermissionItem(CAN_EDIT_VERSE, "आज का वचन व मनन अपडेट (Daily Verse)", "Daily Scripture Update", "प्रतिदिन का मुख्य बाइबल वचन व आत्मिक विचार अपडेट करना", "📢 प्रकाशन व मीडिया"),
            PermissionItem(CAN_BROADCAST_ANNOUNCEMENTS, "विशेष घोषणाएं व फ्लैश अलर्ट (Announcements)", "Broadcast Announcements", "शीर्ष फ्लैश नोटिस व विशेष घोषणाएं पोस्ट करना", "📢 प्रकाशन व मीडिया"),
            PermissionItem(CAN_MANAGE_LIVE_STREAM, "लाइव आराधना सभा व प्रसारण (Live Stream)", "Live Worship Stream", "लाइव यूट्यूब आराधना, शीर्षक व समय सारणी", "📢 प्रकाशन व मीडिया"),
            PermissionItem(CAN_MANAGE_AUDIO_PRAYER, "दैनिक ऑडियो प्रार्थना व संदेश (Audio Devotion)", "Daily Audio Prayers", "प्रतिदिन की प्रार्थना ऑडियो और वचन संदेश", "📢 प्रकाशन व मीडिया"),
            PermissionItem(CAN_MANAGE_BANNERS, "होम स्क्रीन बैनर प्रबंधन (Banners)", "Home Banners", "होम स्क्रीन पर प्रचार बैनर व लिंक", "📢 प्रकाशन व मीडिया"),
            PermissionItem(CAN_MANAGE_SONG_SHEET, "गीत संग्रह व शीट लिंक (Song Sheets)", "Lyrics & Song Sheets", "मसीही गीतों की ऑनलाइन शीट लिंक", "📢 प्रकाशन व मीडिया"),
            PermissionItem(CAN_MANAGE_POLLS, "पोल एवं मतदान प्रबंधन (Polls & Voting)", "Polls & Voting Management", "विश्वासियों से महत्वपूर्ण विषयों पर राय व मतदान लेने हेतु पोल बनाना", "📢 प्रकाशन व मीडिया"),

            // 3. Ministry & Fellowship
            PermissionItem(CAN_MODERATE_PRAYERS, "प्रार्थना निवेदन समीक्षा व स्वीकृति (Prayers)", "Prayer Moderation", "विश्वासियों के प्रार्थना निवेदनों की समीक्षा व स्वीकृति", "🕊️ कलीसिया सेवा व संगति"),
            PermissionItem(CAN_REPLY_PRAYERS, "प्रार्थना निवेदन उत्तर व आत्मिक सांत्वना (Prayer Reply)", "Prayer Request Reply", "प्रार्थना निवेदनों पर आत्मिक प्रोत्साहन व उत्तर संदेश भेजना", "🕊️ कलीसिया सेवा व संगति"),
            PermissionItem(CAN_MANAGE_EVENTS, "चर्च कैलेंडर व आगामी कार्यक्रम (Events)", "Church Events Calendar", "विशेष आराधना, उपवास सभा और सम्मेलन कार्यक्रम जोड़ना", "🕊️ कलीसिया सेवा व संगति"),
            PermissionItem(CAN_VIEW_MEMBERS, "कलीसिया सदस्य सूची व विवरण (Directory)", "Member Directory", "रजिस्टर्ड विश्वासी सदस्यों की सूची देखना", "🕊️ कलीसिया सेवा व संगति"),
            PermissionItem(CAN_TRACK_ATTENDANCE, "सभा उपस्थिति व सांख्यिकी (Attendance)", "Attendance Tracker", "रविवार आराधना व सभाओं की उपस्थिति दर्ज करना", "🕊️ कलीसिया सेवा व संगति"),
            PermissionItem(CAN_MANAGE_ACCOUNTS, "दशमांश व चर्च लेखा-जोखा (Accounts)", "Church Accounts & Tithes", "दशमांश, दान, भेंट और खर्चों का हिसाब रखना", "🕊️ कलीसिया सेवा व संगति"),
            PermissionItem(CAN_MANAGE_READING_PLANS, "बाइबल रीडिंग प्लान प्रबंधन (Reading Plans)", "Reading Plans", "आत्मिक रीडिंग प्लान्स और अध्ययन सामग्री", "🕊️ कलीसिया सेवा व संगति"),

            // 4. System & Security
            PermissionItem(CAN_SEND_PUSH, "पुश नोटिफिकेशन व आपात सूचना (Push)", "Push Notifications", "कलीसिया को सीधे मोबाइल पुश नोटिफिकेशन भेजना", "🔒 सिस्टम व सुरक्षा"),
            PermissionItem(CAN_VIEW_AUDIT_LOGS, "ऑडिट लॉग्स व गतिविधि (Audit Logs)", "Audit Trail", "सभी एडमिनों की लॉगिन व बदलाव ऑडिट हिस्ट्री देखना", "🔒 सिस्टम व सुरक्षा"),
            PermissionItem(CAN_EXPORT_DATA, "डेटा बैकअप व रिपोर्ट एक्सपोर्ट (Data Export)", "Data Export & Backup", "कलीसिया डेटा रिपोर्ट एक्सेल/पीडीएफ डाउनलोड", "🔒 सिस्टम व सुरक्षा"),
            PermissionItem(CAN_MANAGE_STORAGE_QUOTAS, "फ़ायरबेस स्टोरेज व कोटा मॉनिटर (Storage)", "Firebase Storage Quotas", "क्लाउड स्टोरेज उपयोग व फोटो कोटा जांचना", "🔒 सिस्टम व सुरक्षा"),
            PermissionItem(CAN_VIEW_ANALYTICS, "ऐप उपयोग आँकड़े व एनालिटिक्स (Analytics)", "Usage Analytics", "ऑनलाइन यूज़र्स, कुल इंस्टॉल और शेयर आंकड़े", "🔒 सिस्टम व सुरक्षा"),
            PermissionItem(CAN_MANAGE_SECURITY, "सुरक्षा, Gmail लिंकिंग व टेस्ट मोड (Security)", "Security Settings", "खाता सुरक्षा, सिंगल डिवाइस और ग्लोबल सेटिंग्स", "🔒 सिस्टम व सुरक्षा")
        )
    }

    fun getFunctionIdForPermission(permKey: String): String? {
        return when (permKey) {
            CAN_ADD_ADMINS, CAN_VIEW_ADMINS, CAN_EDIT_ADMINS, CAN_DISABLE_ADMINS, CAN_DELETE_ADMINS, CAN_GENERATE_OTP -> AdminFunction.CREATE_SUBORDINATES.id
            CAN_MANAGE_DESIGNATIONS, CAN_MANAGE_ROLES -> AdminFunction.MANAGE_DESIGNATIONS.id
            CAN_ADD_USERS -> AdminFunction.MEMBER_DIRECTORY_VIEW.id
            CAN_EDIT_VERSE -> AdminFunction.TODAY_SCRIPTURE_UPDATE.id
            CAN_BROADCAST_ANNOUNCEMENTS, CAN_POST_ANNOUNCEMENTS -> AdminFunction.SPECIAL_ANNOUNCEMENTS.id
            CAN_MANAGE_LIVE_STREAM, CAN_BROADCAST_LIVE -> AdminFunction.LIVE_STREAM_BROADCAST.id
            CAN_MODERATE_PRAYERS -> AdminFunction.PRAYER_REQUEST_MODERATION.id
            CAN_REPLY_PRAYERS -> AdminFunction.PRAYER_REPLY_ENCOURAGEMENT.id
            CAN_MANAGE_EVENTS -> AdminFunction.CHURCH_EVENTS_CALENDAR.id
            CAN_MANAGE_AUDIO_PRAYER -> AdminFunction.DAILY_PRAYER_AUDIO.id
            CAN_VIEW_MEMBERS -> AdminFunction.MEMBER_DIRECTORY_VIEW.id
            CAN_TRACK_ATTENDANCE, CAN_MANAGE_ATTENDANCE -> AdminFunction.ATTENDANCE_TRACKER.id
            CAN_MANAGE_ACCOUNTS -> AdminFunction.CHURCH_ACCOUNTS_TITHES.id
            CAN_SEND_PUSH, CAN_BROADCAST_PUSH -> AdminFunction.PUSH_NOTIFICATION_BROADCAST.id
            CAN_VIEW_AUDIT_LOGS, CAN_VIEW_HISTORY -> AdminFunction.AUDIT_LOGS_VIEW.id
            CAN_EXPORT_DATA -> AdminFunction.DATA_EXPORT_BACKUP.id
            CAN_MANAGE_READING_PLANS -> AdminFunction.BIBLE_READING_PLANS.id
            CAN_MANAGE_BANNERS -> AdminFunction.APP_BANNER_PROMO.id
            CAN_MANAGE_SONG_SHEET -> AdminFunction.SONG_SHEET_UPDATE.id
            CAN_MANAGE_STORAGE_QUOTAS, CAN_MANAGE_STORAGE -> AdminFunction.FIREBASE_STORAGE_QUOTAS.id
            CAN_VIEW_ANALYTICS -> AdminFunction.APP_USAGE_ANALYTICS.id
            CAN_MANAGE_SECURITY -> AdminFunction.SECURITY_GMAIL_SETTINGS.id
            else -> null
        }
    }

    fun getPermissionKeyForFunction(funcId: String): String? {
        return when (funcId) {
            AdminFunction.CREATE_SUBORDINATES.id, AdminFunction.CREATE_SUBORDINATES.name -> CAN_ADD_ADMINS
            AdminFunction.MANAGE_DESIGNATIONS.id, AdminFunction.MANAGE_DESIGNATIONS.name -> CAN_MANAGE_DESIGNATIONS
            AdminFunction.TODAY_SCRIPTURE_UPDATE.id, AdminFunction.TODAY_SCRIPTURE_UPDATE.name -> CAN_EDIT_VERSE
            AdminFunction.SPECIAL_ANNOUNCEMENTS.id, AdminFunction.SPECIAL_ANNOUNCEMENTS.name -> CAN_BROADCAST_ANNOUNCEMENTS
            AdminFunction.LIVE_STREAM_BROADCAST.id, AdminFunction.LIVE_STREAM_BROADCAST.name -> CAN_MANAGE_LIVE_STREAM
            AdminFunction.PRAYER_REQUEST_MODERATION.id, AdminFunction.PRAYER_REQUEST_MODERATION.name -> CAN_MODERATE_PRAYERS
            AdminFunction.PRAYER_REPLY_ENCOURAGEMENT.id, AdminFunction.PRAYER_REPLY_ENCOURAGEMENT.name -> CAN_REPLY_PRAYERS
            AdminFunction.CHURCH_EVENTS_CALENDAR.id, AdminFunction.CHURCH_EVENTS_CALENDAR.name -> CAN_MANAGE_EVENTS
            AdminFunction.DAILY_PRAYER_AUDIO.id, AdminFunction.DAILY_PRAYER_AUDIO.name -> CAN_MANAGE_AUDIO_PRAYER
            AdminFunction.MEMBER_DIRECTORY_VIEW.id, AdminFunction.MEMBER_DIRECTORY_VIEW.name -> CAN_VIEW_MEMBERS
            AdminFunction.ATTENDANCE_TRACKER.id, AdminFunction.ATTENDANCE_TRACKER.name -> CAN_TRACK_ATTENDANCE
            AdminFunction.CHURCH_ACCOUNTS_TITHES.id, AdminFunction.CHURCH_ACCOUNTS_TITHES.name -> CAN_MANAGE_ACCOUNTS
            AdminFunction.PUSH_NOTIFICATION_BROADCAST.id, AdminFunction.PUSH_NOTIFICATION_BROADCAST.name -> CAN_SEND_PUSH
            AdminFunction.AUDIT_LOGS_VIEW.id, AdminFunction.AUDIT_LOGS_VIEW.name -> CAN_VIEW_AUDIT_LOGS
            AdminFunction.DATA_EXPORT_BACKUP.id, AdminFunction.DATA_EXPORT_BACKUP.name -> CAN_EXPORT_DATA
            AdminFunction.BIBLE_READING_PLANS.id, AdminFunction.BIBLE_READING_PLANS.name -> CAN_MANAGE_READING_PLANS
            AdminFunction.APP_BANNER_PROMO.id, AdminFunction.APP_BANNER_PROMO.name -> CAN_MANAGE_BANNERS
            AdminFunction.SONG_SHEET_UPDATE.id, AdminFunction.SONG_SHEET_UPDATE.name -> CAN_MANAGE_SONG_SHEET
            AdminFunction.FIREBASE_STORAGE_QUOTAS.id, AdminFunction.FIREBASE_STORAGE_QUOTAS.name -> CAN_MANAGE_STORAGE_QUOTAS
            AdminFunction.APP_USAGE_ANALYTICS.id, AdminFunction.APP_USAGE_ANALYTICS.name -> CAN_VIEW_ANALYTICS
            AdminFunction.SECURITY_GMAIL_SETTINGS.id, AdminFunction.SECURITY_GMAIL_SETTINGS.name -> CAN_MANAGE_SECURITY
            else -> null
        }
    }
}

@Keep
data class DesignationAuthority(
    val id: String = "",
    val name: String = "", // e.g. "Vinay Kumar Avj (Profile B)", "बिशप (Bishop)", "उप बिशप (Deputy Bishop)", "पास्टर (Pastor)", "पुरनिया (Elder)"
    val rank: Int = 1, // 5 = Vinay Kumar Avj (Super Admin), 4 = Bishop, 3 = Deputy Bishop, 2 = Pastor, 1 = Puraniya/Elder
    val isCustom: Boolean = false,
    val allowedFunctions: List<String> = emptyList(), // Allowed function IDs
    val allowedPermissions: List<String> = emptyList(), // Granular permission keys
    val description: String = "",
    val createdBy: String = "SYSTEM"
)

@Keep
data class AdminUser(
    val id: String = "",
    val designation: String = "", // "Vinay Kumar Avj (Profile B)", "बिशप (Bishop)", "उप बिशप (Deputy Bishop)", "पास्टर (Pastor)", "पुरनिया (Elder)", etc.
    val name: String = "",
    val rank: Int = 1, // 5 = Vinay Kumar Avj (Super Admin), 4 = Bishop, 3 = Deputy Bishop, 2 = Pastor, 1 = Puraniya
    val pin: String = "", // 4 or 6 digits
    val isAutoPin: Boolean = false,
    val isEnabled: Boolean = true,
    val isDeviceBlocked: Boolean = false, // Device blocking by Master/Superior Admin
    val createdByAdminId: String = "",
    val createdByDesignation: String = "",
    val createdTimestamp: Long = System.currentTimeMillis(),
    val activeDeviceId: String = "",
    val lastLoginTimestamp: Long = 0L,
    val linkedGmail: String = "",
    val maxSubordinates: Int = 0, // e.g. 2 for Pastor
    val photoUrl: String = "",
    val isDefaultMaster: Boolean = false,
    val secondaryPin: String = "", // Password 2 / OTP generated by higher authority for non-Vinay roles
    val secondaryPinGeneratedTimestamp: Long = 0L, // Timestamp when OTP was generated (valid for 10 minutes)
    val failedOtpAttempts: Int = 0, // Track failed Password 2 (OTP) login attempts for automated security alerts
    val assignedFunctions: List<String> = emptyList(), // The specific authoritative functions granted to this admin
    val permissions: List<String> = emptyList(), // Granular RBAC permissions array
    val userRole: String = "", // e.g. "master_admin" or subordinate role
    val p1PasswordHash: String = "", // Cryptographic hash for P1 Admin Security Password
    val roleTier: String = "", // "master_admin", "bishop", "deputy_bishop", "pastor", "elder", "believer"
    val reportsToSeniorId: String = "", // Cascading senior authority ID
    val reportsToSeniorName: String = "", // Cascading senior authority Name
    val isVerified: Boolean = false, // Believer verified tick (✓)
    val phone: String = "",
    val email: String = "",
    val customPermissions: List<String> = emptyList(), // Special Access overrides
    val isAdmin: Boolean = true, // False for believers
    val title: String = "", // "Rev.", "Pastor", "Bishop", "Dr.", "Bro."
    val accessiblePrefixes: List<String> = emptyList(), // e.g. ["NCC", "KHWR"]
    val dioceseRegion: String = "", // For Bishops / Deputy Bishops
    val canGenerateP2: Boolean = false, // Live P2 generation rights
    val accountStatus: String = "active", // "active", "disabled"
    val status: String = "active", // "pending_activation", "active", "disabled"
    val assignedAuthorityId: String = "",
    val assignedAuthorityName: String = "",
    val serialNumber: String = "", // Monospace Alphanumeric
    val previousSerials: List<String> = emptyList(),
    val churchId: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun isMasterAdmin(): Boolean {
        if (!isAdmin || roleTier.equals("believer", ignoreCase = true)) return false
        return userRole.equals("master_admin", ignoreCase = true) ||
                isDefaultMaster ||
                (rank == 1 && (designation.contains("Vinay", ignoreCase = true) || designation.contains("Master", ignoreCase = true))) ||
                rank >= AdminHierarchy.RANK_VINAY_KUMAR ||
                designation.contains("Vinay", ignoreCase = true) ||
                designation.contains("Master", ignoreCase = true)
    }

    fun isOtpValid(): Boolean {
        if (isMasterAdmin()) return true
        if (secondaryPin.isBlank()) return false
        val tenMinutesMs = 10 * 60 * 1000L
        return secondaryPinGeneratedTimestamp > 0L && (System.currentTimeMillis() - secondaryPinGeneratedTimestamp) <= tenMinutesMs
    }

    fun getRemainingOtpTimeMs(): Long {
        if (secondaryPinGeneratedTimestamp <= 0L) return 0L
        val tenMinutesMs = 10 * 60 * 1000L
        val diff = (secondaryPinGeneratedTimestamp + tenMinutesMs) - System.currentTimeMillis()
        return if (diff > 0L) diff else 0L
    }

    fun hasPermission(permissionKey: String): Boolean {
        // 5. CRITICAL PRESERVATION RULE (Master Override):
        // If user_role == 'master_admin', grant absolute bypass to all permission checks.
        // The Master Admin automatically gets FULL ACCESS to all old and new tools without needing a permissions array.
        if (isMasterAdmin()) {
            return true
        }
        // Subordinates: Initialize all tools with Visibility = GONE.
        // Only return true if explicitly present in permissions or assignedFunctions
        if (permissions.contains(permissionKey)) return true
        if (assignedFunctions.contains(permissionKey)) return true
        val mappedFn = AdminPermission.getFunctionIdForPermission(permissionKey)
        if (mappedFn != null && (assignedFunctions.contains(mappedFn) || permissions.contains(mappedFn))) return true
        val mappedPerm = AdminPermission.getPermissionKeyForFunction(permissionKey)
        if (mappedPerm != null && (permissions.contains(mappedPerm) || assignedFunctions.contains(mappedPerm))) return true
        return false
    }

    fun hasFunction(function: AdminFunction): Boolean {
        return hasFunction(function.id)
    }

    fun hasFunction(functionId: String): Boolean {
        if (isMasterAdmin()) {
            return true
        }
        if (assignedFunctions.contains(functionId)) return true
        if (permissions.contains(functionId)) return true
        val mappedPerm = AdminPermission.getPermissionKeyForFunction(functionId)
        if (mappedPerm != null && (permissions.contains(mappedPerm) || assignedFunctions.contains(mappedPerm))) return true
        return false
    }
}

object AdminHierarchy {
    const val RANK_VINAY_KUMAR = 5
    const val RANK_BISHOP = 4
    const val RANK_DEPUTY_BISHOP = 3
    const val RANK_PASTOR = 2
    const val RANK_PURANIYA = 1

    const val ROLE_VINAY = "मास्टर एडमिन (Master Admin)"
    const val ROLE_VINAY_KUMAR = "मास्टर एडमिन (Master Admin)"
    const val ROLE_BISHOP = "बिशप (Bishop)"
    const val ROLE_DEPUTY_BISHOP = "उप बिशप (Deputy Bishop)"
    const val ROLE_PASTOR = "पास्टर (Pastor)"
    const val ROLE_PURANIYA = "पुरनिया (Elder)"
    const val ROLE_VERIFIED_VISHWASI = "सत्यापित विश्वासी (Verified Vishwasi)"

    fun isVerifiedVishwasi(role: String): Boolean {
        return role.contains("Verified", ignoreCase = true) || role.contains("सत्यापित", ignoreCase = true)
    }

    fun getDefaultDesignations(): List<DesignationAuthority> {
        return listOf(
            DesignationAuthority(
                id = "desig_vinay_master",
                name = ROLE_VINAY,
                rank = RANK_VINAY_KUMAR,
                isCustom = false,
                allowedFunctions = AdminFunction.allFunctionIds(),
                description = "सर्वोच्च प्रशासनिक अधिकार (Supreme Master) - सभी 14 फंक्शन पूर्णतः नियंत्रित"
            ),
            DesignationAuthority(
                id = "desig_bishop",
                name = ROLE_BISHOP,
                rank = RANK_BISHOP,
                isCustom = false,
                allowedFunctions = listOf(
                    AdminFunction.CREATE_SUBORDINATES.id,
                    AdminFunction.DELEGATE_PERMISSIONS.id,
                    AdminFunction.SPECIAL_ANNOUNCEMENTS.id,
                    AdminFunction.TODAY_SCRIPTURE_UPDATE.id,
                    AdminFunction.LIVE_STREAM_BROADCAST.id,
                    AdminFunction.PRAYER_REQUEST_MODERATION.id,
                    AdminFunction.PRAYER_REPLY_ENCOURAGEMENT.id,
                    AdminFunction.CHURCH_EVENTS_CALENDAR.id,
                    AdminFunction.DAILY_PRAYER_AUDIO.id,
                    AdminFunction.MEMBER_DIRECTORY_VIEW.id,
                    AdminFunction.BIBLE_READING_PLANS.id,
                    AdminFunction.APP_BANNER_PROMO.id,
                    AdminFunction.SECURITY_GMAIL_SETTINGS.id
                ),
                description = "क्षेत्रीय बिशप अधिकार - उप बिशप, पास्टर व पुरनिया प्रबंधन और सभा प्रसारण"
            ),
            DesignationAuthority(
                id = "desig_deputy_bishop",
                name = ROLE_DEPUTY_BISHOP,
                rank = RANK_DEPUTY_BISHOP,
                isCustom = false,
                allowedFunctions = listOf(
                    AdminFunction.CREATE_SUBORDINATES.id,
                    AdminFunction.DELEGATE_PERMISSIONS.id,
                    AdminFunction.SPECIAL_ANNOUNCEMENTS.id,
                    AdminFunction.TODAY_SCRIPTURE_UPDATE.id,
                    AdminFunction.LIVE_STREAM_BROADCAST.id,
                    AdminFunction.PRAYER_REQUEST_MODERATION.id,
                    AdminFunction.PRAYER_REPLY_ENCOURAGEMENT.id,
                    AdminFunction.CHURCH_EVENTS_CALENDAR.id,
                    AdminFunction.DAILY_PRAYER_AUDIO.id,
                    AdminFunction.MEMBER_DIRECTORY_VIEW.id
                ),
                description = "उप बिशप अधिकार - पास्टर व पुरनिया प्रबंधन, सभाएं व प्रार्थनाएं"
            ),
            DesignationAuthority(
                id = "desig_pastor",
                name = ROLE_PASTOR,
                rank = RANK_PASTOR,
                isCustom = false,
                allowedFunctions = listOf(
                    AdminFunction.CREATE_SUBORDINATES.id,
                    AdminFunction.DELEGATE_PERMISSIONS.id,
                    AdminFunction.PRAYER_REQUEST_MODERATION.id,
                    AdminFunction.PRAYER_REPLY_ENCOURAGEMENT.id,
                    AdminFunction.CHURCH_EVENTS_CALENDAR.id,
                    AdminFunction.DAILY_PRAYER_AUDIO.id,
                    AdminFunction.MEMBER_DIRECTORY_VIEW.id
                ),
                description = "पास्टर अधिकार - स्थानीय कलीसिया सेवा, प्रार्थनाएं व पुरनिया नियुक्ति (अधिकतम 2)"
            ),
            DesignationAuthority(
                id = "desig_puraniya",
                name = ROLE_PURANIYA,
                rank = RANK_PURANIYA,
                isCustom = false,
                allowedFunctions = listOf(
                    AdminFunction.PRAYER_REQUEST_MODERATION.id,
                    AdminFunction.CHURCH_EVENTS_CALENDAR.id,
                    AdminFunction.MEMBER_DIRECTORY_VIEW.id
                ),
                description = "पुरनिया / एल्डर अधिकार - प्रार्थना निवेदन समीक्षा व कलीसिया संगति"
            )
        )
    }

    fun getRankForDesignation(designation: String): Int {
        return when {
            designation.contains("Vinay", ignoreCase = true) -> RANK_VINAY_KUMAR
            designation.contains("बिशप") && !designation.contains("उप") -> RANK_BISHOP
            designation.contains("Bishop") && !designation.contains("Deputy") -> RANK_BISHOP
            designation.contains("उप बिशप") || designation.contains("Deputy Bishop") -> RANK_DEPUTY_BISHOP
            designation.contains("पास्टर") || designation.contains("Pastor") -> RANK_PASTOR
            designation.contains("पुरनिया") || designation.contains("Puraniya") || designation.contains("Elder") -> RANK_PURANIYA
            else -> 1
        }
    }

    fun getDefaultFunctionsForRank(rank: Int): List<String> {
        return getDefaultDesignations().firstOrNull { it.rank == rank }?.allowedFunctions ?: emptyList()
    }

    fun getDefaultPermissionsForRank(rank: Int): List<String> {
        return getDefaultFunctionsForRank(rank).mapNotNull { AdminPermission.getPermissionKeyForFunction(it) }
    }

    fun getAllowedSubordinateCategories(creatorRank: Int): List<Pair<String, Int>> {
        return when (creatorRank) {
            RANK_VINAY_KUMAR -> listOf(
                ROLE_BISHOP to RANK_BISHOP,
                ROLE_DEPUTY_BISHOP to RANK_DEPUTY_BISHOP,
                ROLE_PASTOR to RANK_PASTOR,
                ROLE_PURANIYA to RANK_PURANIYA,
                ROLE_VERIFIED_VISHWASI to 0
            )
            RANK_BISHOP -> listOf(
                ROLE_DEPUTY_BISHOP to RANK_DEPUTY_BISHOP,
                ROLE_PASTOR to RANK_PASTOR,
                ROLE_PURANIYA to RANK_PURANIYA,
                ROLE_VERIFIED_VISHWASI to 0
            )
            RANK_DEPUTY_BISHOP -> listOf(
                ROLE_PASTOR to RANK_PASTOR,
                ROLE_PURANIYA to RANK_PURANIYA,
                ROLE_VERIFIED_VISHWASI to 0
            )
            RANK_PASTOR -> listOf(
                ROLE_PURANIYA to RANK_PURANIYA,
                ROLE_VERIFIED_VISHWASI to 0
            )
            else -> emptyList()
        }
    }

    fun canCreateRole(creatorRank: Int, targetRole: String, currentCreatedCount: Int = 0): Boolean {
        if (isVerifiedVishwasi(targetRole)) return creatorRank >= RANK_PASTOR
        val targetRank = getRankForDesignation(targetRole)
        return when (creatorRank) {
            RANK_VINAY_KUMAR -> true // Supreme Admin can create any role & custom designations
            RANK_BISHOP -> targetRank in listOf(RANK_DEPUTY_BISHOP, RANK_PASTOR, RANK_PURANIYA)
            RANK_DEPUTY_BISHOP -> targetRank in listOf(RANK_PASTOR, RANK_PURANIYA)
            RANK_PASTOR -> targetRank == RANK_PURANIYA
            else -> false
        }
    }

    fun canManageAdmin(creatorRank: Int, targetAdminRank: Int): Boolean {
        return creatorRank > targetAdminRank
    }

    // Strict Hierarchy: Master Admin (1) > Bishop (2) > Deputy Bishop (3) > Pastor (4) > Elder (5)
    fun getAuthorityLevel(admin: AdminUser): Int {
        if (admin.isMasterAdmin()) return 1
        val d = admin.designation.lowercase()
        return when {
            d.contains("उप बिशप") || d.contains("deputy") -> 3
            d.contains("बिशप") || d.contains("bishop") -> 2
            d.contains("पास्टर") || d.contains("pastor") -> 4
            d.contains("पुरनिया") || d.contains("elder") -> 5
            admin.rank == 4 -> 2
            admin.rank == 3 -> 3
            admin.rank == 2 -> 4
            admin.rank == 1 -> 5
            else -> 5
        }
    }

    fun canManageAdmin(creator: AdminUser, targetAdmin: AdminUser): Boolean {
        val creatorLevel = getAuthorityLevel(creator)
        val targetLevel = getAuthorityLevel(targetAdmin)
        if (creatorLevel == 1) return targetLevel > 1 // Master Admin can manage all subordinates
        // An admin can ONLY view, edit, disable, or delete profiles that are strictly LOWER in authority (higher rank number)
        return creatorLevel < targetLevel
    }

    fun canReplyToPrayers(admin: AdminUser?): Boolean {
        if (admin == null) return false
        if (admin.isMasterAdmin() || admin.rank >= RANK_VINAY_KUMAR) return true
        return admin.hasPermission(AdminPermission.CAN_REPLY_PRAYERS) ||
               admin.hasFunction(AdminFunction.PRAYER_REPLY_ENCOURAGEMENT) ||
               admin.hasPermission(AdminPermission.CAN_MODERATE_PRAYERS) ||
               admin.hasFunction(AdminFunction.PRAYER_REQUEST_MODERATION)
    }
}

@Keep
data class SpecialAnnouncement(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val postedBy: String = "",
    val postedByRole: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val actionUrl: String = "",
    val isPermanent: Boolean = true,
    val durationHours: Int = 0,
    val durationDays: Int = 0,
    val expiresAtTimestamp: Long = 0L
) {
    fun isExpired(): Boolean {
        if (isPermanent) return false
        if (expiresAtTimestamp <= 0L) return false
        return System.currentTimeMillis() > expiresAtTimestamp
    }

    fun getRemainingTimeFormatted(): String {
        if (isPermanent) return "♾️ स्थायी"
        val diff = expiresAtTimestamp - System.currentTimeMillis()
        if (diff <= 0L) return "❌ समाप्त"
        val hours = diff / (1000 * 60 * 60)
        val days = hours / 24
        val remainingHours = hours % 24
        return if (days > 0) "⏱️ $days दिन ${remainingHours} घंटे शेष" else "⏱️ $hours घंटे ${(diff / (1000 * 60)) % 60} मिनट शेष"
    }
}

@Keep
data class AdminTodayScripture(
    val bookAndVerse: String = "यूहन्ना 3:16",
    val hindiText: String = "क्योंकि परमेश्वर ने जगत से ऐसा प्रेम रखा कि उसने अपना एकलौता पुत्र दे दिया...",
    val referenceText: String = "John 3:16",
    val reflectionThought: String = "प्रभु का प्रेम सदैव हमारे साथ है।",
    val updatedBy: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isPermanent: Boolean = true,
    val durationHours: Int = 0,
    val durationDays: Int = 0,
    val expiresAtTimestamp: Long = 0L
) {
    fun isExpired(): Boolean {
        if (isPermanent) return false
        if (expiresAtTimestamp <= 0L) return false
        return System.currentTimeMillis() > expiresAtTimestamp
    }

    fun getRemainingTimeFormatted(): String {
        if (isPermanent) return "♾️ स्थायी"
        val diff = expiresAtTimestamp - System.currentTimeMillis()
        if (diff <= 0L) return "❌ समाप्त"
        val hours = diff / (1000 * 60 * 60)
        val days = hours / 24
        val remainingHours = hours % 24
        return if (days > 0) "⏱️ $days दिन ${remainingHours} घंटे शेष" else "⏱️ $hours घंटे ${(diff / (1000 * 60)) % 60} मिनट शेष"
    }
}

@Keep
data class DailyGreetingConfig(
    val greetingText: String = "जय मसीह की",
    val isPermanent: Boolean = true,
    val durationHours: Int = 0,
    val durationDays: Int = 0,
    val expiresAtTimestamp: Long = 0L,
    val ttl: Long = 0L,
    val updatedTimestamp: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean {
        if (isPermanent) return false
        val exp = if (expiresAtTimestamp > 0L) expiresAtTimestamp else if (ttl > 0L) ttl * 1000L else 0L
        if (exp <= 0L) return false
        return System.currentTimeMillis() > exp
    }

    fun getEffectiveTtl(): Long {
        return if (ttl > 0L) ttl else if (expiresAtTimestamp > 0L) expiresAtTimestamp / 1000L else 0L
    }

    fun getRemainingTimeFormatted(): String {
        if (isPermanent) return "♾️ स्थायी"
        val exp = if (expiresAtTimestamp > 0L) expiresAtTimestamp else if (ttl > 0L) ttl * 1000L else 0L
        val diff = exp - System.currentTimeMillis()
        if (diff <= 0L) return "❌ समाप्त"
        val hours = diff / (1000 * 60 * 60)
        val days = hours / 24
        val remainingHours = hours % 24
        return if (days > 0) "⏱️ $days दिन ${remainingHours} घंटे शेष" else "⏱️ $hours घंटे ${(diff / (1000 * 60)) % 60} मिनट शेष"
    }

    fun getEffectiveGreeting(): String {
        return if (isExpired()) "" else greetingText.trim()
    }
}

@Keep
data class AdminLiveStreamConfig(
    val isLive: Boolean = false,
    val title: String = "लाइव आराधना सभा (Live Worship)",
    val subtitle: String = "अभी जुड़ें और प्रभु की आशीष प्राप्त करें",
    val youtubeVideoOrChannelUrl: String = "",
    val scheduledTime: String = "",
    val updatedBy: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Keep
data class NavigationTabConfig(
    val id: String = "home",
    val title: String = "Home",
    val isVisible: Boolean = true,
    val order: Int = 0,
    val iconName: String = "home"
)

fun getDefaultNavigationTabs(): List<NavigationTabConfig> {
    return listOf(
        NavigationTabConfig("HOME", "Home", true, 0, "home"),
        NavigationTabConfig("CHAT", "Chat", true, 1, "chat"),
        NavigationTabConfig("BLOGS", "Blogs", true, 2, "article"),
        NavigationTabConfig("YOUTUBE", "YouTube", true, 3, "play_circle"),
        NavigationTabConfig("BIBLE", "Bible", true, 4, "menu_book"),
        NavigationTabConfig("READING", "Reading", true, 5, "auto_stories")
    )
}

@Keep
data class AdminAuditLog(
    val id: String = "",
    val adminId: String = "",
    val adminName: String = "",
    val adminDesignation: String = "",
    val actionType: String = "", // e.g., "LOGIN", "PIN_CHANGED", "ADMIN_CREATED", "ANNOUNCEMENT_POSTED", "PRAYER_ANSWERED", "ATTENDANCE_RECORDED", "ACCOUNT_ENTRY", "PUSH_SENT", "PERMISSION_MODIFIED"
    val description: String = "",
    val targetId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val action: String = "",
    val targetUserId: String = "",
    val performedByAdminId: String = "",
    val p1Validated: Boolean = false,
    val p2Verified: Boolean = false
)

@Keep
data class SpecialAccessOverride(
    val id: String,
    val titleHindi: String,
    val titleEnglish: String,
    val permissionKey: String,
    val functionId: String,
    val description: String
)

object HierarchicalRoleTier {
    const val TIER_MASTER_ADMIN = "master_admin"
    const val TIER_BISHOP = "bishop"
    const val TIER_DEPUTY_BISHOP = "deputy_bishop"
    const val TIER_PASTOR = "pastor"
    const val TIER_ELDER = "elder"
    const val TIER_BELIEVER = "believer"

    val ALL_TIERS = listOf(
        TIER_MASTER_ADMIN to "Master Admin",
        TIER_BISHOP to "बिशप (Bishop)",
        TIER_DEPUTY_BISHOP to "उप बिशप (Deputy Bishop)",
        TIER_PASTOR to "पास्टर (Pastor)",
        TIER_ELDER to "पुरनिया (Elder)",
        TIER_BELIEVER to "विश्वासी (Believer)"
    )

    fun getRankForTier(tier: String): Int {
        return when (tier) {
            TIER_MASTER_ADMIN -> 5
            TIER_BISHOP -> 4
            TIER_DEPUTY_BISHOP -> 3
            TIER_PASTOR -> 2
            TIER_ELDER -> 1
            TIER_BELIEVER -> 0
            else -> 0
        }
    }

    fun getDesignationForTier(tier: String): String {
        return when (tier) {
            TIER_MASTER_ADMIN -> AdminHierarchy.ROLE_VINAY
            TIER_BISHOP -> AdminHierarchy.ROLE_BISHOP
            TIER_DEPUTY_BISHOP -> AdminHierarchy.ROLE_DEPUTY_BISHOP
            TIER_PASTOR -> AdminHierarchy.ROLE_PASTOR
            TIER_ELDER -> AdminHierarchy.ROLE_PURANIYA
            TIER_BELIEVER -> "विश्वासी (Believer)"
            else -> tier
        }
    }

    val SPECIAL_ACCESS_OVERRIDES = listOf(
        SpecialAccessOverride(
            id = "override_prayer_moderation",
            titleHindi = "प्रार्थना समीक्षा (Prayer Wall Moderation)",
            titleEnglish = "Prayer Wall Moderation",
            permissionKey = AdminPermission.CAN_MODERATE_PRAYERS,
            functionId = AdminFunction.PRAYER_REQUEST_MODERATION.id,
            description = "प्रार्थना वॉल पर निवेदनों की समीक्षा व अनुमोदन"
        ),
        SpecialAccessOverride(
            id = "override_event_scheduling",
            titleHindi = "कार्यक्रम निर्धारण (Event Scheduling)",
            titleEnglish = "Event Scheduling",
            permissionKey = AdminPermission.CAN_MANAGE_EVENTS,
            functionId = AdminFunction.CHURCH_EVENTS_CALENDAR.id,
            description = "चर्च कैलेंडर में सभा व इवेंट जोड़ना"
        ),
        SpecialAccessOverride(
            id = "override_media_upload",
            titleHindi = "मीडिया व लाइव (Media Upload)",
            titleEnglish = "Media Upload",
            permissionKey = AdminPermission.CAN_MANAGE_LIVE_STREAM,
            functionId = AdminFunction.LIVE_STREAM_BROADCAST.id,
            description = "लाइव आराधना लिंक व मीडिया अपडेट करना"
        ),
        SpecialAccessOverride(
            id = "override_push_broadcast",
            titleHindi = "पुश ब्रॉडकास्ट (Push Broadcast)",
            titleEnglish = "Push Broadcast",
            permissionKey = AdminPermission.CAN_SEND_PUSH,
            functionId = AdminFunction.PUSH_NOTIFICATION_BROADCAST.id,
            description = "कलीसिया को आपात व मुख्य सूचना अलर्ट भेजना"
        ),
        SpecialAccessOverride(
            id = "override_directory_export",
            titleHindi = "डायरेक्टरी एक्सपोर्ट (Directory Export)",
            titleEnglish = "Directory Export",
            permissionKey = AdminPermission.CAN_EXPORT_DATA,
            functionId = AdminFunction.DATA_EXPORT_BACKUP.id,
            description = "कलीसिया सदस्यों व खातों की रिपोर्ट एक्सपोर्ट करना"
        )
    )
}

@Keep
data class ChurchMember(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val familyRole: String = "मुखिया (Head)", // "मुखिया (Head)", "सदस्य (Member)", "बच्चा (Child)"
    val familyName: String = "",
    val baptismStatus: String = "बपतिस्मा प्राप्त (Baptized)", // "बपतिस्मा प्राप्त (Baptized)", "प्रतीक्षारत (Pending)", "अन्य (Other)"
    val birthDate: String = "",
    val anniversaryDate: String = "",
    val status: String = "सक्रिय (Active)", // "सक्रिय (Active)", "आगंतुक (Visitor)", "स्थानांतरित (Relocated)"
    val notes: String = "",
    val addedByAdmin: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Keep
data class ChurchAttendanceRecord(
    val id: String = "",
    val serviceType: String = "रविवार मुख्य आराधना (Sunday Worship)", // "रविवार मुख्य आराधना", "उपवास प्रार्थना (Fasting)", "युवा संगति (Youth)", "कॉटेज प्रेयर (Cottage)"
    val dateString: String = "", // e.g. "2026-09-20"
    val countMen: Int = 0,
    val countWomen: Int = 0,
    val countChildren: Int = 0,
    val totalCount: Int = 0,
    val newVisitorsCount: Int = 0,
    val topicOrPreacher: String = "",
    val notes: String = "",
    val recordedByAdmin: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Keep
data class ChurchAccountTransaction(
    val id: String = "",
    val type: String = "INCOME", // "INCOME" or "EXPENSE"
    val category: String = "दशमांश (Tithe)", // INCOME: "दशमांश (Tithe)", "साधारण भेंट (Offering)", "धन्यवाद भेंट (Thanksgiving)", "भवन फंड (Building Fund)", "अन्य (Other)"
                                          // EXPENSE: "किराया व बिजली (Rent & Utility)", "प्रचार व यात्रा (Ministry)", "ध्वनि/मीडिया (Audio/Media)", "दान व सहायता (Charity)", "अन्य खर्च (Miscellaneous)"
    val amount: Double = 0.0,
    val donorOrRecipient: String = "",
    val paymentMode: String = "नकद (Cash)", // "नकद (Cash)", "UPI / ऑनलाइन (Online)", "बैंक चेक (Cheque)"
    val dateString: String = "",
    val notes: String = "",
    val recordedByAdmin: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Keep
data class AdminPushNotification(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val targetAudience: String = "सभी विश्वासी (All)", // "सभी विश्वासी (All)", "प्रार्थना दल (Prayer Team)", "युवा संगति (Youth)", "कलीसिया परिवार (Families)"
    val urgency: String = "सामान्य (Normal)", // "सामान्य (Normal)", "महत्वपूर्ण (High)", "आपातकालीन (Urgent)"
    val actionUrl: String = "",
    val sentByAdmin: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Keep
enum class ReminderTargetScope(val titleHindi: String, val description: String) {
    ALL_USERS(
        "सभी उपयोगकर्ता (All Users)",
        "सभी उपयोगकर्ताओं के डिवाइस पर यह नया समय अनिवार्य रूप से लागू होगा"
    ),
    ONLY_UNMODIFIED_DEFAULTS(
        "केवल डिफ़ॉल्ट (जिन्होंने मैनुअली नहीं बदला)",
        "केवल उन लोगों के लिए बदलेगा जिन्होंने सेटिंग्स से समय को खुद नहीं बदला है"
    ),
    SPECIFIC_PROFILE(
        "किसी निश्चित प्रोफाइल का (Targeted Profile)",
        "केवल चुने गए विश्वासी सदस्य/प्रोफाइल के डिवाइस पर लागू होगा"
    )
}

@Keep
data class AdminReminderScheduleConfig(
    val id: String = "global_reminder_schedule",
    // 1. Daily Prayer Notification
    val prayerHour: Int = 4,
    val prayerMinute: Int = 0,
    val prayerEnabled: Boolean = true,

    // 2. Today's Verse TTS Alarm
    val verseAlarmHour: Int = 6,
    val verseAlarmMinute: Int = 0,
    val verseAlarmEnabled: Boolean = true,

    // 3. Bible Reading Reminders
    val readingMorningHour: Int = 5,
    val readingMorningMinute: Int = 0,
    val readingEveningHour: Int = 21,
    val readingEveningMinute: Int = 0,
    val readingPlanEnabled: Boolean = true,

    // Scope & Target details
    val targetScope: ReminderTargetScope = ReminderTargetScope.ONLY_UNMODIFIED_DEFAULTS,
    val targetProfileId: String = "",
    val targetProfileName: String = "",
    val updatedBy: String = "Master Admin",
    val timestamp: Long = System.currentTimeMillis()
)

@Keep
data class ChurchPrefixRecord(
    val prefixId: String = "", // e.g., "NCC", "DIO", "NZ" (3-5 chars uppercase)
    val prefixType: String = "CHILD_CONGREGATION", // "REGIONAL_DIOCESE" (Level 1) or "CHILD_CONGREGATION" (Level 2)
    val churchName: String = "",
    val dioceseRegion: String = "",
    val ownerAuthorityId: String = "", // UID of creator
    val ownerAuthorityName: String = "",
    val assignedPastorId: String = "", // UID of active Pastor
    val assignedPastorName: String = "",
    val supervisingAuthorityId: String = "", // UID of Bishop
    val supervisingAuthorityName: String = "",
    val lastCount: Int = 0, // Atomic count for serial suffix
    val memberCount: Int = 0,
    val status: String = "active", // "active", "transferred", "archived"
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Keep
data class ActiveP2Session(
    val sessionId: String = "",
    val serialNumber: String = "", // Monospace serial e.g. "NCC1"
    val targetUserId: String = "",
    val targetUserName: String = "",
    val designatedAuthorityId: String = "",
    val designatedAuthorityName: String = "",
    val otpCode: String = "", // 6-digit dynamic OTP
    val otpHash: String = "",
    val expiresAt: Long = 0L, // now + 10m
    val isUsed: Boolean = false,
    val createdTimestamp: Long = System.currentTimeMillis()
) {
    fun getRemainingTimeMs(): Long {
        val diff = expiresAt - System.currentTimeMillis()
        return if (diff > 0L) diff else 0L
    }

    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt || isUsed

    fun isWarningState(): Boolean {
        val rem = getRemainingTimeMs()
        return rem in 1..120000L // Under 2 minutes
    }
}

@Keep
data class PrefixTransferAuditLog(
    val id: String = "",
    val prefixId: String = "",
    val fromPastorId: String = "",
    val fromPastorName: String = "",
    val toPastorId: String = "",
    val toPastorName: String = "",
    val bishopAuthorityId: String = "",
    val bishopAuthorityName: String = "",
    val authorizationOtp: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "COMPLETED",
    val notes: String = ""
)

@Keep
data class DemographicAnalyticsSummary(
    val totalUsers: Int = 0,
    val totalBelievers: Int = 0,
    val totalLeaders: Int = 0,
    val maleCount: Int = 0,
    val femaleCount: Int = 0,
    val tacticalBelievers: Int = 0,
    val otherGenderCount: Int = 0,
    val baptizedCount: Int = 0,
    val unbaptizedCount: Int = 0,
    val kidsCount: Int = 0, // < 13
    val youthCount: Int = 0, // 13 - 25
    val adultsCount: Int = 0, // 26 - 59
    val seniorsCount: Int = 0, // 60+
    val ministryInterestCounts: Map<String, Int> = emptyMap()
)

@Keep
data class QrAuditLogEntry(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val targetSerial: String = "",
    val targetRoleTier: String = "believer",
    val targetDesignation: String = "",
    val targetName: String = "",
    val generatedByAdminId: String = "",
    val generatedByAdminName: String = "",
    val generatedByAdminDesignation: String = "",
    val otpCode: String = "",
    val otpExpiresAt: Long = 0L,
    val otpStatus: String = "ACTIVE", // "ACTIVE", "ACTIVATED", "EXPIRED", "PENDING"
    val qrPayloadType: String = "ncck_activation",
    val activatedTimestamp: Long = 0L,
    val activatedDeviceId: String = "",
    val notes: String = ""
) {
    fun isExpired(): Boolean = otpStatus == "EXPIRED" || (otpStatus != "ACTIVATED" && System.currentTimeMillis() > otpExpiresAt && otpExpiresAt > 0L)

    fun isActivated(): Boolean = otpStatus == "ACTIVATED" || otpStatus == "USED" || activatedTimestamp > 0L

    fun isPendingActive(): Boolean = !isActivated() && !isExpired()

    fun getEffectiveStatus(): String {
        return when {
            isActivated() -> "ACTIVATED"
            isExpired() -> "EXPIRED"
            else -> "ACTIVE"
        }
    }

    fun getStatusLabelHindi(): String {
        return when (getEffectiveStatus()) {
            "ACTIVATED" -> "सत्यापित (Activated)"
            "EXPIRED" -> "समाप्त (Expired)"
            else -> "सक्रिय (Active 10-Min)"
        }
    }

    fun getRemainingTimeMs(): Long {
        if (isActivated() || isExpired()) return 0L
        val diff = otpExpiresAt - System.currentTimeMillis()
        return if (diff > 0L) diff else 0L
    }
}

@Keep
data class RoleChangeAuditLog(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val targetUserId: String = "",
    val targetSerial: String = "",
    val targetName: String = "",
    val previousTier: String = "",
    val updatedTier: String = "",
    val authorizedBy: String = "",
    val authorizedByName: String = "",
    val method: String = "MASTER_OVERRIDE",
    val notes: String = ""
)

@Keep
data class PrefixMergeAuditLog(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val sourcePrefix: String = "",
    val targetPrefix: String = "",
    val migratedCount: Int = 0,
    val authorizedBy: String = "",
    val authorizedByName: String = "",
    val notes: String = ""
)

@Keep
data class MemberTransferAuditLog(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "",
    val oldSerial: String = "",
    val newSerial: String = "",
    val fromPrefix: String = "",
    val toPrefix: String = "",
    val transferredByAdminId: String = "",
    val notes: String = ""
)

@Keep
data class PollOption(
    val id: String = "",
    val text: String = "",
    val voteCount: Int = 0,
    val voterIds: List<String> = emptyList()
)

@Keep
data class AdminPollItem(
    val id: String = "",
    val question: String = "",
    val options: List<PollOption> = emptyList(),
    val targetAudience: String = "सभी विश्वासी (All Believers)",
    val isActive: Boolean = true,
    val createdByAdmin: String = "",
    val timestamp: Long = System.currentTimeMillis()
)





