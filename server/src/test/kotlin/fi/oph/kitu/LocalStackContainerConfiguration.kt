package fi.oph.kitu

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistrar
import org.testcontainers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI

/**
 * Spinnaa testikohtaisen LocalStack-kontin S3-palvelulla. Asettaa myös
 * Spring Cloud AWS:n osoittamaan kyseiseen endpointtiin ja luo nimettyjen
 * bucketien `tehtavapankkiBucket` valmiiksi.
 *
 * Käyttö: `@Import(LocalStackContainerConfiguration::class)` ja
 * `@TestPropertySource(properties = ["spring.cloud.aws.s3.enabled=true"])`
 * varsinaisessa testissä — testin oletusprofiili pitää S3:n pois päältä,
 * jotta autoconfigure ei käytä oikeaa AWS-tiliä.
 */
@TestConfiguration(proxyBeanMethods = false)
class LocalStackContainerConfiguration {
    @Bean(initMethod = "start", destroyMethod = "stop")
    fun localStackContainer(): LocalStackContainer =
        LocalStackContainer(DockerImageName.parse("localstack/localstack:4"))
            .withServices("s3")

    @Bean
    fun localStackProperties(localStack: LocalStackContainer): DynamicPropertyRegistrar =
        DynamicPropertyRegistrar { registry ->
            registry.add("spring.cloud.aws.s3.enabled") { "true" }
            registry.add("spring.cloud.aws.s3.endpoint") { localStack.endpoint.toString() }
            registry.add("spring.cloud.aws.s3.path-style-access-enabled") { "true" }
            registry.add("spring.cloud.aws.region.static") { localStack.region }
            registry.add("spring.cloud.aws.credentials.access-key") { localStack.accessKey }
            registry.add("spring.cloud.aws.credentials.secret-key") { localStack.secretKey }
            registry.add("kitu.kotoutumiskoulutus.tehtavapankki.bucket") { TEST_BUCKET }
        }

    @Bean
    fun localStackS3Client(localStack: LocalStackContainer): S3Client =
        S3Client
            .builder()
            .endpointOverride(URI.create(localStack.endpoint.toString()))
            .region(Region.of(localStack.region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(localStack.accessKey, localStack.secretKey),
                ),
            ).forcePathStyle(true)
            .build()

    @Bean
    fun localStackBucketInitializer(s3: S3Client): TestBucketInitializer = TestBucketInitializer(s3)

    companion object {
        const val TEST_BUCKET = "kitu-bucket-test"
    }
}

class TestBucketInitializer(
    s3: S3Client,
) {
    init {
        s3.createBucket { it.bucket(LocalStackContainerConfiguration.TEST_BUCKET) }
    }
}
