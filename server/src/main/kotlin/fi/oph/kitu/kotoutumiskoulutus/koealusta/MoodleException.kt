package fi.oph.kitu.kotoutumiskoulutus.koealusta

class MoodleException(
    moodleErrorMessage: MoodleErrorMessage,
) : Throwable(moodleErrorMessage.message)
