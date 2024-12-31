package fi.oph.kitu.kotoutumiskoulutus

class MoodleException(
    moodleErrorMessage: MoodleErrorMessage,
) : Throwable(moodleErrorMessage.message)
