package fi.oph.kitu

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class DBContainerConfiguration {
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> {
        val image =
            ImageFromDockerfile()
                .withDockerfileFromBuilder { builder ->
                    builder
                        .from("postgres:16")
                        .run("localedef -i fi_FI -c -f UTF-8 -A /usr/share/locale/locale.alias fi_FI.UTF-8")
                        .build()
                }
        val imageName = DockerImageName.parse(image.get()).asCompatibleSubstituteFor(PostgreSQLContainer.IMAGE)

        return PostgreSQLContainer(imageName)
            .withUrlParam("stringtype", "unspecified")!!
    }
}
