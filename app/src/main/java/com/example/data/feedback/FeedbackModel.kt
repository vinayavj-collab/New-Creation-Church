package com.example.data.feedback

enum class FeedbackType(val titleHindi: String, val titleEnglish: String) {
    BUG_REPORT("समस्या / बग रिपोर्ट (Bug / Issue)", "Bug / Issue Report"),
    FEATURE_SUGGESTION("सुधार / नया फीचर सुझाव (Suggestion)", "Feature Suggestion"),
    CONTENT_CORRECTION("कंटेंट / वचन सुधार (Content Correction)", "Content Correction"),
    GENERAL_FEEDBACK("सामान्य प्रतिक्रिया (General Feedback)", "General Feedback")
}

data class FeedbackSubmission(
    val id: String = "",
    val type: FeedbackType = FeedbackType.GENERAL_FEEDBACK,
    val subject: String = "",
    val message: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val appVersion: String = "",
    val androidVersion: Int = 0,
    val deviceModel: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "NEW"
)
