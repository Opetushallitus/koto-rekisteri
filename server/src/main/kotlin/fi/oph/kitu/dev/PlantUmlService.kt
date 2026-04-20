package fi.oph.kitu.dev

import fi.oph.kitu.dev.PumlRow.Companion.getDisplayName
import fi.oph.kitu.dev.PumlRow.Companion.hiddenPackages
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

        return beanNames
            .flatMap { beanFactory.getPumlRows(PumlGeneratingContext(it)) }
            .toPumlCode()
    }
}

enum class UmlItemType(
    val stereotype: String? = null,
) {
    GENERIC_BEAN(),
    CONFIGURATION_BEAN("Configuration"),
    SERVICE_BEAN("Service"),
    REPOSITORY_BEAN("Repository"),
    CONTROLLER_BEAN("Controller"),
    PRODUCT_BEAN("Method"),
    ;

    companion object {
        fun of(bd: BeanDefinition): UmlItemType =
            when {
                bd.factoryBeanName != null -> PRODUCT_BEAN
                isAnnotatedAs<Configuration>(bd) -> CONFIGURATION_BEAN
                isAnnotatedAs<Service>(bd) -> SERVICE_BEAN
                isAnnotatedAs<Repository>(bd) -> REPOSITORY_BEAN
                isAnnotatedAs<Controller>(bd) -> CONTROLLER_BEAN
                isAnnotatedAs<RestController>(bd) -> CONTROLLER_BEAN
                else -> GENERIC_BEAN
            }

        private inline fun <reified T> isAnnotatedAs(beanDefinition: BeanDefinition): Boolean =
            beanDefinition is AnnotatedBeanDefinition &&
                beanDefinition.metadata.hasAnnotation(T::class.java.name)
    }
}

data class PumlGeneratingContext(
    val beanName: String,
    val overrideBeanType: UmlItemType? = null,
    val overridePackage: PumlPackage? = null,
    val depthLeft: Int = 1,
) {
    fun next(
        bean: String,
        overrideBeanType: UmlItemType? = null,
        overridePackage: PumlPackage? = null,
    ): PumlGeneratingContext? =
        if (depthLeft <= 0) {
            null
        } else {
            copy(
                beanName = bean,
                overrideBeanType = overrideBeanType,
                overridePackage = overridePackage,
                depthLeft = depthLeft - 1,
            )
        }
}

fun ConfigurableListableBeanFactory.getPumlRows(ctx: PumlGeneratingContext?): List<PumlRow> =
    ctx?.beanName?.let { beanName ->
        getBeanClassNameOrNull(beanName)?.let { className ->
            getBeanDefinitionOrNull(beanName)?.let { bd ->
                val thisPackage =
                    ctx.overridePackage ?: bd.factoryBeanName
                        ?.let { getBeanClassNameOrNull(it) }
                        ?.let { factoryClassName -> PumlPackage.fromClassName(factoryClassName) }
                        ?: PumlPackage.fromClassName(className)

                val thisItem = PumlBean(thisPackage, beanName, ctx.overrideBeanType ?: UmlItemType.of(bd))

                val dependencyRows = getDependenciesForBean(beanName).flatMap { getPumlRows(ctx.next(it)) }
                val childBeanRows =
                    beanDefinitionNames
                        .filter { getBeanDefinition(it).factoryBeanName == beanName }
                        .flatMap {
                            getPumlRows(
                                ctx.next(
                                    bean = it,
                                    overrideBeanType = UmlItemType.PRODUCT_BEAN,
                                    overridePackage = thisPackage,
                                ),
                            )
                        }
                val dependencyRelations =
                    dependencyRows.filterIsInstance<PumlBean>().map {
                        PumlDependency(thisItem, it)
                    }
                val compositionRelations =
                    childBeanRows.filterIsInstance<PumlBean>().map {
                        PumlComposition(thisItem, it)
                    }

                listOf(thisItem) + dependencyRows + childBeanRows + dependencyRelations + compositionRelations
            }
        }
    } ?: emptyList()

fun ConfigurableListableBeanFactory.getBeanClassNameOrNull(beanName: String): String? =
    runCatching { getBean(beanName).javaClass.name }.getOrNull()

fun ConfigurableListableBeanFactory.getBeanDefinitionOrNull(beanName: String): BeanDefinition? =
    runCatching { getBeanDefinition(beanName) }.getOrNull()

interface PumlRow {
    fun getPumlCode(): String

    fun hidden(): Boolean

    companion object {
        val hiddenPackages = listOf("jdk", "java")

        val displayPackageNames: Map<String, String> =
            mapOf(
                "fi.oph.kitu." to "Kielitutkintorekisteri.",
                "org.springframework.*" to "Spring.",
                "io.opentelemetry.*" to "OpenTelemetry.",
                "com.github.kagkarlsson.scheduler.*" to "Scheduler.",
                "tools.jackson.*" to "Jackson.",
                "org.apereo.cas.*" to "CAS.",
                "org.flyway.*" to "Flyway.",
                "io.awspring.*" to "AWS.",
            )

        fun getDisplayName(cn: String): String =
            displayPackageNames.entries.fold(cn) { acc, (pattern, displayPackageName) ->
                if (pattern.endsWith(".*")) {
                    val regex = Regex("${Regex.escape(pattern.substringBeforeLast("*"))}\\S*")
                    acc.replace(regex) { matchResult ->
                        "$displayPackageName${matchResult.value.substringAfterLast(".")}"
                    }
                } else {
                    acc.replace(pattern, displayPackageName)
                }
            }
    }
}

interface PumlItem : PumlRow {
    fun reference(): String
}

data class PumlPackage(
    val name: String,
) : PumlItem {
    override fun getPumlCode(): String = "package $name {}"

    override fun reference(): String = name

    override fun hidden(): Boolean = hiddenPackages.any { name.startsWith("$it.") }

    companion object {
        fun fromClassName(className: String): PumlPackage = PumlPackage(className.substringBeforeLast("."))
    }
}

data class PumlBean(
    val pkg: PumlPackage,
    val beanName: String,
    val beanType: UmlItemType,
) : PumlItem {
    override fun getPumlCode(): String =
        "class ${pkg.name}.$beanName${beanType.stereotype?.let { " <<$it>>" }.orEmpty()}"

    override fun reference(): String = "${pkg.name}.$beanName"

    override fun hidden(): Boolean = pkg.hidden()
}

interface PumlRelation : PumlRow

data class PumlDependency(
    val item: PumlItem,
    val dependency: PumlItem,
) : PumlRelation {
    override fun getPumlCode(): String = "${item.reference()} --> ${dependency.reference()}"

    override fun hidden(): Boolean = item.hidden() || dependency.hidden()
}

data class PumlComposition(
    val parent: PumlItem,
    val child: PumlItem,
) : PumlRelation {
    override fun getPumlCode(): String = "${parent.reference()} *-- ${child.reference()}"

    override fun hidden(): Boolean = parent.hidden() || child.hidden()
}

fun Iterable<PumlRow>.toPumlCode(): String {
    val rows = distinct().filterNot { it.hidden() }
    val rowsToIgnore =
        rows.mapNotNull {
            when (it) {
                is PumlComposition -> PumlDependency(it.child, it.parent)
                else -> null
            }
        }
    val filteredRows = rows.filterNot { rowsToIgnore.contains(it) }

    return (
        listOf("@startuml") +
            filteredRows.map { getDisplayName(it.getPumlCode()) } +
            listOf(
                "hide members",
                "hide methods",
                "hide <<${UmlItemType.PRODUCT_BEAN.stereotype}>> circle",
                "@enduml",
            )
    ).joinToString("\n")
}
