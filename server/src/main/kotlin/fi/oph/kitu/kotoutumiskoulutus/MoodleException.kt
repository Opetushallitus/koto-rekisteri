package fi.oph.kitu.kotoutumiskoulutus

class MoodleException(
    moodleErrorMessage: MoodleErrorMessage,
) : Exception(moodleErrorMessage.message)
