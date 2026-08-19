package fi.oph.kitu.kotoutumiskoulutus.koealusta.tehtavapankki

import fi.oph.kitu.tehtavapankki.TehtavapankkiRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class TehtavapankkiAikaleimaMigration(
    private val repository: TehtavapankkiRepository,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        val korjattu = repository.korjaaVirheellisetLahdeAikaleimat(RAJA)
        if (korjattu > 0) {
            logger.info("Korjattiin $korjattu tehtäväpaketin epoch-sekunteina tallentuneet lähdeaikaleimat")
        }
    }

    companion object {
        // Virheelliset aikaleimat ovat epoch-sekunteja millisekunneiksi
        // tulkittuina eli kaikki 1970-luvun alussa; aidot arvot ovat 2020-lukua.
        val RAJA: OffsetDateTime = OffsetDateTime.of(1980, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    }
}
