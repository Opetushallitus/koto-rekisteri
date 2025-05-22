package fi.oph.kitu.jdbc

import org.springframework.data.repository.CrudRepository

/**
 * First deletes the data, and then save all. Returns all the data from the repository
 */
fun <S, K> CrudRepository<S, K>.replaceAll(data: Iterable<S>): Iterable<S> {
    this.deleteAll()
    this.saveAll<S>(data)
    return this.findAll()
}
