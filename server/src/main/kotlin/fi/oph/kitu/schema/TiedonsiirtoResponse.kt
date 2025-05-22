package fi.oph.kitu.schema

interface TiedonsiirtoResponse {
    val result: TiedonsiirtoStatus
}

enum class TiedonsiirtoStatus {
    OK,
    Failed,
}

class TiedonsiirtoSuccess : TiedonsiirtoResponse {
    override val result = TiedonsiirtoStatus.OK
}

data class TiedonsiirtoFailure(
    val errors: List<String>,
) : TiedonsiirtoResponse {
    override val result = TiedonsiirtoStatus.Failed
}
