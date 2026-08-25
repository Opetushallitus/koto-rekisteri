package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

data class TehtavapankkiQuiz(
    val questions: List<Question> = emptyList(),
)

sealed class Question {
    abstract val type: String
}

// Question-alityypit, jotka tallennetaan TehtavaEntity-riveiksi.
// CategoryQuestion (ryhmäraja) ja UnknownQuestion (tuntematon <question type="…">)
// eivät tarkoituksella laajenna tätä.
sealed class IngestableQuestion : Question()

data class CategoryQuestion(
    val category: FormattedText? = null,
    val info: FormattedText? = null,
    val idnumber: String? = null,
) : Question() {
    override val type: String = "category"
}

data class DescriptionQuestion(
    val name: FormattedText? = null,
    val questiontext: RichText? = null,
    val generalfeedback: FormattedText? = null,
    val defaultgrade: Double? = null,
    val penalty: Double? = null,
    val hidden: Int? = null,
    val idnumber: String? = null,
) : IngestableQuestion() {
    override val type: String = "description"
}

data class MultichoiceQuestion(
    val name: FormattedText? = null,
    val questiontext: RichText? = null,
    val generalfeedback: FormattedText? = null,
    val defaultgrade: Double? = null,
    val penalty: Double? = null,
    val hidden: Int? = null,
    val idnumber: String? = null,
    val single: Boolean? = null,
    val shuffleanswers: Boolean? = null,
    val answernumbering: String? = null,
    val showstandardinstruction: Int? = null,
    val correctfeedback: FormattedText? = null,
    val partiallycorrectfeedback: FormattedText? = null,
    val incorrectfeedback: FormattedText? = null,
    val answers: List<Answer> = emptyList(),
) : IngestableQuestion() {
    override val type: String = "multichoice"
}

data class ShortanswerQuestion(
    val name: FormattedText? = null,
    val questiontext: RichText? = null,
    val generalfeedback: FormattedText? = null,
    val defaultgrade: Double? = null,
    val penalty: Double? = null,
    val hidden: Int? = null,
    val idnumber: String? = null,
    val usecase: Int? = null,
    val answers: List<Answer> = emptyList(),
) : IngestableQuestion() {
    override val type: String = "shortanswer"
}

data class EssayQuestion(
    val name: FormattedText? = null,
    val questiontext: RichText? = null,
    val generalfeedback: FormattedText? = null,
    val defaultgrade: Double? = null,
    val penalty: Double? = null,
    val hidden: Int? = null,
    val idnumber: String? = null,
    val responseformat: String? = null,
    val responserequired: Int? = null,
    val responsefieldlines: Int? = null,
    val minwordlimit: String? = null,
    val maxwordlimit: String? = null,
    val attachments: Int? = null,
    val attachmentsrequired: Int? = null,
    val maxbytes: Long? = null,
    val filetypeslist: String? = null,
    val graderinfo: FormattedText? = null,
    val responsetemplate: FormattedText? = null,
) : IngestableQuestion() {
    override val type: String = "essay"
}

data class CloudpoodllQuestion(
    val name: FormattedText? = null,
    val questiontext: RichText? = null,
    val generalfeedback: FormattedText? = null,
    val defaultgrade: Double? = null,
    val penalty: Double? = null,
    val hidden: Int? = null,
    val idnumber: String? = null,
    val responseformat: String? = null,
    val graderinfo: FormattedText? = null,
    val qresource: String? = null,
    val language: String? = null,
    val expiredays: Int? = null,
    val transcriber: Int? = null,
    val studentplayer: Int? = null,
    val teacherplayer: Int? = null,
    val transcode: Int? = null,
    val audioskin: String? = null,
    val videoskin: String? = null,
    val timelimit: Int? = null,
    val safesave: Int? = null,
    val noaudiofilters: Int? = null,
    val tags: List<Tag> = emptyList(),
) : IngestableQuestion() {
    override val type: String = "cloudpoodll"
}

data class FormattedText(
    val format: String? = null,
    val text: String? = null,
)

data class RichText(
    val format: String? = null,
    val text: String? = null,
    val embeddedFiles: List<EmbeddedFile> = emptyList(),
)

data class EmbeddedFile(
    val name: String = "",
    val path: String = "/",
    val encoding: String = "base64",
    // Base64-sisältö on tehtäväpankin ylivoimaisesti raskain osa. Se on
    // muuttuva, jotta se voidaan vapauttaa heti S3-viennin jälkeen sen sijaan
    // että se pysyisi keossa niin kauan kuin quiz on elossa.
    var content: String = "",
) {
    fun releaseContent() {
        content = ""
    }
}

data class Answer(
    val fraction: Double = 0.0,
    val format: String? = null,
    val text: String? = null,
    val feedback: FormattedText? = null,
)

data class Tag(
    val text: String? = null,
)

data class UnknownQuestion(
    override val type: String,
) : Question()
