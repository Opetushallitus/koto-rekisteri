package fi.oph.kitu.organisaatiot

import fi.oph.kitu.oid.Oid

interface OrganisaatiopalveluRequest

class EmptyRequest : OrganisaatiopalveluRequest

data class GetOrganisatioRequest(
    val oid: Oid,
)
