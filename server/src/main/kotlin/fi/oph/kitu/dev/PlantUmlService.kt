package fi.oph.kitu.dev

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.stereotype.Service

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
                    rows.add("${umlItem.name} --> ${makeCleanName(dep, depName)}")
                }
            }
        }

        return if (rows.isEmpty()) {
            null
        } else {
            (listOf("@startuml") + rows.distinct() + listOf("@enduml")).joinToString("\n")
        }
    }
}

fun ConfigurableListableBeanFactory.getUmlItem(beanName: String): UmlItem? {
    val bd = getBeanDefinition(beanName)

    return bd.beanClassName?.let { className ->
        makeCleanName(
            beanName,
            className,
        )?.let { name -> UmlItem(name, UmlItemType.CLASS_BEAN) }
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
    override fun toString(): String =
        listOfNotNull(
            "$type ${factory?.let { it.substringBeforeLast(".") + "." }.orEmpty()}$name",
            factory?.let { "$it *-- $name" },
        ).joinToString("\n")
}

enum class UmlItemType(
    val pumlType: String,
) {
    CLASS_BEAN("class"),
    METHOD_BEAN("circle"),
    ;

    override fun toString(): String = pumlType
}

fun makeCleanName(
    name: String,
    className: String?,
): String? =
    (
        className?.let { cn ->
            if (cn.startsWith("jdk.")) {
                null
            } else if (cn.startsWith("fi.oph.kitu")) {
                cn.replace("fi.oph.kitu.", "Kielitutkintorekisteri.")
            } else if (cn.startsWith("org.springframework")) {
                "Spring.${cn.substringAfterLast(".")}"
            } else if (cn.startsWith("io.opentelemetry")) {
                "OpenTelemetry.${cn.substringAfterLast(".")}"
            } else if (cn.startsWith("com.github.kagkarlsson.scheduler")) {
                "Scheduler.${cn.substringAfterLast(".")}"
            } else {
                cn
            }
        } ?: name
    ).substringBefore("$$")
