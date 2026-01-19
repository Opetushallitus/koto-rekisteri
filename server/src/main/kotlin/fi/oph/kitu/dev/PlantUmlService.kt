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

        beanNames.forEach { beanName ->
            beanFactory.getUmlItem(beanName)?.let { umlItem ->
                rows.add(umlItem.toString())
                beanFactory.getDependenciesForBean(beanName).forEach { dep ->
                    val depName = runCatching { beanFactory.getBean(dep).javaClass.name }.getOrNull()
                    val cleanDepName = makeCleanName(dep, depName)
                    if (cleanDepName == umlItem.factory) {
                        rows.add("$cleanDepName *-- ${umlItem.name}")
                    } else {
                        beanFactory.getUmlItem(dep)?.let { rows.add(it.toString()) }
                        rows.add("${umlItem.name} --> $cleanDepName")
                    }
                }
            }
        }

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
}

fun ConfigurableListableBeanFactory.getUmlItem(beanName: String): UmlItem? {
    val bd = getBeanDefinition(beanName)

    return bd.beanClassName?.let { className ->
        makeCleanName(
            beanName,
            className,
        )?.let { name ->
            UmlItem(
                name,
                if (isAnnotatedConfiguration(bd)) {
                    UmlItemType.CONFIGURATION_BEAN
                } else if (isAnnotatedService(bd)) {
                    UmlItemType.SERVICE_BEAN
                } else if (isAnnotatedRepository(bd)) {
                    UmlItemType.REPOSITORY_BEAN
                } else if (isAnnotatedController(bd)) {
                    UmlItemType.CONTROLLER_BEAN
                } else {
                    UmlItemType.GENERIC_BEAN
                },
            )
        }
    }
        ?: UmlItem(
            beanName,
            UmlItemType.METHOD_BEAN,
            makeCleanName(
                "UNKNOWN",
                (bd.factoryBeanName ?: bd.factoryMethodName)?.let { getBean(it).javaClass.name },
            ),
        )
}

data class UmlItem(
    val name: String,
    val type: UmlItemType,
    val factory: String? = null,
) {
    fun fullName(): String {
        val fullName =
            factory?.let {
                "${factory.substringBeforeLast(".")}.$name"
            } ?: name
        return listOfNotNull(
            fullName,
            type.stereotype?.let { "<<$it>>" },
        ).joinToString(" ")
    }

    override fun toString(): String = "${type.pumlType} ${fullName()}"
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
}

fun makeCleanName(
    name: String,
    className: String?,
): String? =
    (
        className?.let { cn ->
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
            } else {
                cn
            }
        } ?: name
    ).substringBefore("$$")

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
