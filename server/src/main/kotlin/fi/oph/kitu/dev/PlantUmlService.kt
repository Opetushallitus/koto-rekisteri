package fi.oph.kitu.dev

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController

@Service
class PlantUmlService(
    val beanFactory: ConfigurableListableBeanFactory,
) {
    fun generatePlantUml(packagePrefix: String): String? {
        val fullPrefix = "fi.oph.kitu.$packagePrefix."
        val beanNames =
            beanFactory.beanDefinitionNames
                .filter {
                    val bd = beanFactory.getBeanDefinition(it)
                    bd.beanClassName?.startsWith(fullPrefix) == true ||
                        bd.source?.toString()?.startsWith(fullPrefix) == true
                }

        val rows = mutableListOf<String>()

        beanNames.forEach { beanName -> buildUml(rows, beanName) }

        return if (rows.isEmpty()) {
            null
        } else {
            (
                listOf("@startuml") +
                    rows.distinct() +
                    listOf(
                        "hide members",
                        "hide methods",
                        "hide <<Method>> circle",
                        "@enduml",
                    )
            ).joinToString("\n")
        }
    }

    private fun buildUml(
        rows: MutableList<String>,
        beanName: String,
        processedBeans: List<String> = emptyList(),
    ) {
        if (beanName in processedBeans || beanName.contains("$") || beanName.contains("@")) return
        beanFactory.getUmlItem(beanName)?.let { umlItem ->
            rows.addAll(umlItem.generatePuml(beanFactory))
            val processed = processedBeans + beanName
            umlItem.dependencies.forEach { dep -> buildUml(rows, dep, processed) }
            umlItem.children.forEach { child -> buildUml(rows, child, processed) }
        }
    }
}

fun ConfigurableListableBeanFactory.getUmlItem(beanName: String): UmlItem? {
    fun beansFromConfig(configBeanName: String): List<String> =
        beanDefinitionNames
            .map { name -> name to getBeanDefinition(name) }
            .filter { (_, bd) ->
                bd.factoryBeanName == configBeanName
            }.map { (name, _) ->
                name
            }

    val bd =
        runCatching { getBeanDefinition(beanName) }.getOrNull()
            ?: return UmlItem(
                name = beanName,
                type = UmlItemType.GENERIC_BEAN,
                dependencies = emptyList(),
                children = emptyList(),
            )

    val itemType = UmlItemType.of(bd)

    return UmlItem(
        name = beanName,
        type = itemType,
        dependencies = getDependenciesForBean(beanName).toList(),
        children = beansFromConfig(beanName),
    )
}

fun ConfigurableListableBeanFactory.getUmlItemChild(
    parentName: String,
    beanName: String,
): UmlItem? =
    runCatching { getBeanDefinition(beanName) }.getOrNull()?.let { bd ->
        UmlItem(
            name = "$parentName.$beanName",
            type = UmlItemType.METHOD_BEAN,
            dependencies = getDependenciesForBean(beanName).toList().filter { it != bd.factoryBeanName },
            children = emptyList(),
        )
    }

data class UmlItem(
    val name: String,
    val type: UmlItemType,
    val dependencies: List<String>,
    val children: List<String>,
) {
    fun generatePuml(beanFactory: ConfigurableListableBeanFactory): List<String> {
        return beanNameToDisplayName(beanFactory, name)?.let { cleanName ->
            if (cleanName.startsWith("Kielitutkintorekisteri")) {
                val stereotype = type.stereotype?.let { " <<$it>>" }.orEmpty()

                return (
                    listOf("$type $cleanName$stereotype") +
                        dependencies.mapNotNull {
                            beanNameToDisplayName(beanFactory, it)?.let { "$cleanName --> $it" }
                        } +
                        children.flatMap {
                            val parentName = cleanName.substringBeforeLast(".")
                            beanFactory
                                .getUmlItemChild(
                                    parentName = parentName,
                                    beanName = it,
                                )?.generatePuml(beanFactory)
                                ?.flatMap { child ->
                                    listOf(child) + "$cleanName *-- $it"
                                }.orEmpty()
                        }
                )
            } else {
                val packageName = cleanName.substringBefore(".")
                listOf("package $packageName {}") +
                    dependencies
                        .mapNotNull {
                            beanNameToDisplayName(beanFactory, it)?.let { depCleanName ->
                                if (depCleanName.startsWith("Kielitutkintorekisteri")) {
                                    "$packageName --> $depCleanName"
                                } else {
                                    null
                                }
                            }
                        }
            }
        } ?: emptyList()
    }

    private fun beanNameToDisplayName(
        beanFactory: ConfigurableListableBeanFactory,
        beanName: String,
    ): String? = clean(runCatching { beanFactory.getBean(beanName) }.getOrNull()?.javaClass?.name ?: beanName)

    private fun clean(cn: String): String? =
        (
            if (cn.startsWith("jdk.") || cn.startsWith("java.")) {
                null
            } else if (cn.startsWith("fi.oph.kitu")) {
                cn.replace("fi.oph.kitu.", "Kielitutkintorekisteri.")
            } else if (cn.startsWith("org.springframework")) {
                "Spring.${cn.substringAfterLast(".")}"
            } else if (cn.startsWith("io.opentelemetry")) {
                "OpenTelemetry.${cn.substringAfterLast(".")}"
            } else if (cn.startsWith("com.github.kagkarlsson.scheduler")) {
                "Scheduler.${cn.substringAfterLast(".")}"
            } else if (cn.startsWith("com.fasterxml.jackson")) {
                "Jackson.${cn.substringAfterLast(".")}"
            } else if (cn.startsWith("org.apereo.cas")) {
                "CAS.${cn.substringAfterLast(".")}"
            } else if (cn.startsWith("org.flyway")) {
                "Flyway.${cn.substringAfterLast(".")}"
            } else {
                cn
            }
        )?.substringBefore("@")?.substringBefore("$")
}

enum class UmlItemType(
    val pumlType: String,
    val stereotype: String? = null,
) {
    GENERIC_BEAN("class"),
    CONFIGURATION_BEAN("class", "Configuration"),
    SERVICE_BEAN("class", "Service"),
    REPOSITORY_BEAN("class", "Repository"),
    CONTROLLER_BEAN("class", "Controller"),
    METHOD_BEAN("class", "Method"),
    ;

    override fun toString(): String = pumlType

    companion object {
        fun of(bd: BeanDefinition): UmlItemType =
            when {
                isAnnotatedConfiguration(bd) -> CONFIGURATION_BEAN
                isAnnotatedService(bd) -> SERVICE_BEAN
                isAnnotatedRepository(bd) -> REPOSITORY_BEAN
                isAnnotatedController(bd) -> CONTROLLER_BEAN
                else -> GENERIC_BEAN
            }
    }
}

fun isAnnotatedConfiguration(beanDefinition: BeanDefinition): Boolean =
    beanDefinition is AnnotatedBeanDefinition &&
        beanDefinition.metadata.hasAnnotation(Configuration::class.java.name)

fun isAnnotatedService(beanDefinition: BeanDefinition): Boolean =
    beanDefinition is AnnotatedBeanDefinition &&
        beanDefinition.metadata.hasAnnotation(Service::class.java.name)

fun isAnnotatedRepository(beanDefinition: BeanDefinition): Boolean =
    beanDefinition is AnnotatedBeanDefinition &&
        beanDefinition.metadata.hasAnnotation(Repository::class.java.name)

fun isAnnotatedController(beanDefinition: BeanDefinition): Boolean =
    beanDefinition is AnnotatedBeanDefinition &&
        (
            beanDefinition.metadata.hasAnnotation(RestController::class.java.name) ||
                beanDefinition.metadata.hasAnnotation(Controller::class.java.name)
        )
