package fi.oph.kitu.config

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.core.type.AnnotatedTypeMetadata

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Conditional(NonEmptyPropertyCondition::class)
annotation class ConditionalOnNonEmptyProperty(
    val name: String,
)

class NonEmptyPropertyCondition : Condition {
    override fun matches(
        context: ConditionContext,
        metadata: AnnotatedTypeMetadata,
    ): Boolean {
        val attributes =
            metadata
                .getAnnotationAttributes(ConditionalOnNonEmptyProperty::class.java.name)
                ?: return false

        val propertyName = attributes["name"] as String
        val value = context.environment.getProperty(propertyName)

        return !value.isNullOrBlank()
    }
}
