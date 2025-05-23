package fi.oph.kitu.auth

import org.springframework.security.cas.authentication.CasAuthenticationToken
import org.springframework.security.cas.authentication.StatelessTicketCache

class CasStatelessTicketCache : StatelessTicketCache {
    private var tickets: MutableMap<String, CasAuthenticationToken> = mutableMapOf()

    override fun getByTicketId(serviceTicket: String): CasAuthenticationToken? = tickets[serviceTicket]

    override fun putTicketInCache(token: CasAuthenticationToken) {
        tickets.put(token.principal.toString(), token)
    }

    override fun removeTicketFromCache(token: CasAuthenticationToken) {
        tickets.remove(token.principal.toString())
    }

    override fun removeTicketFromCache(serviceTicket: String) {
        tickets.remove(serviceTicket)
    }
}
