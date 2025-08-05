package fi.oph.kitu

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.utility.DockerImageName
import java.nio.file.Paths

@TestConfiguration(proxyBeanMethods = false)
class DBContainerConfiguration {
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> {
        val image =
            ImageFromDockerfile()
                .withDockerfile(Paths.get("..", "possu.Dockerfile"))

        val imageName =
            DockerImageName
                .parse(image.get())
                .asCompatibleSubstituteFor(PostgreSQLContainer.IMAGE)

        return PostgreSQLContainer(imageName)
            .withUrlParam("stringtype", "unspecified")!!
    }
}
