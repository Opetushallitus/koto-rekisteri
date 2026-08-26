package fi.oph.kitu.html

import fi.oph.kitu.i18n.LocalizedString
import fi.oph.kitu.util.validation.Validation
import kotlinx.html.InputType
import kotlinx.html.section
import kotlinx.html.stream.createHTML
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FormErrorsTest {
    @Test
    fun `groups errors by dotted path and exposes general errors separately`() {
        val errors =
            FormErrors.of(
                listOf(
                    Validation.ValidationError(listOf("sukunimi"), "Sukunimi puuttuu"),
                    Validation.ValidationError(listOf("kausi", "alku"), "Virheellinen päivämäärä"),
                    Validation.ValidationError(listOf(), "Arvioijalla on jo voimassa oleva kausi"),
                ),
            )

        assertEquals(listOf("Sukunimi puuttuu"), errors["sukunimi"])
        assertEquals(listOf("Virheellinen päivämäärä"), errors["kausi.alku"])
        assertEquals(listOf("Arvioijalla on jo voimassa oleva kausi"), errors.yleiset)
        assertEquals(emptyList(), errors["etunimet"])
        assertTrue(errors.isNotEmpty())
        assertTrue(FormErrors.EMPTY.isEmpty())
    }

    @Test
    fun `collects several errors for the same field`() {
        val errors =
            FormErrors.of(
                listOf(
                    Validation.ValidationError(listOf("hetu"), "Hetu puuttuu"),
                    Validation.ValidationError(listOf("hetu"), "Hetu on virheellinen"),
                ),
            )

        assertEquals(listOf("Hetu puuttuu", "Hetu on virheellinen"), errors["hetu"])
    }

    @Test
    fun `valid field renders without aria-invalid and without error text`() {
        val html = renderSukunimiField(FormErrors.EMPTY)

        assertFalse(html.contains("aria-invalid"), "should not mark a valid field:\n$html")
        assertFalse(html.contains("<small"), "should not render error text:\n$html")
        assertTrue(html.contains("""value="Möttönen""""), "should keep the entered value:\n$html")
    }

    @Test
    fun `invalid field is marked aria-invalid and keeps the entered value`() {
        val errors = FormErrors.of(listOf(Validation.ValidationError(listOf("sukunimi"), "Sukunimi puuttuu")))
        val html = renderSukunimiField(errors)

        assertTrue(html.contains("""aria-invalid="true""""), "missing aria-invalid:\n$html")
        assertTrue(html.contains("Sukunimi puuttuu"), "missing error text:\n$html")
        assertTrue(html.contains("""value="Möttönen""""), "should keep the entered value:\n$html")
        assertTrue(html.contains("""data-testid="sukunimi-error""""), "missing error testid:\n$html")
    }

    @Test
    fun `error text is the immediate sibling of the input so Pico colours it`() {
        val errors = FormErrors.of(listOf(Validation.ValidationError(listOf("sukunimi"), "Sukunimi puuttuu")))
        val html = renderSukunimiField(errors)

        val adjacent = Regex("""<input[^>]*aria-invalid="true"[^>]*>\s*<small""")
        assertTrue(adjacent.containsMatchIn(html), "small must directly follow the input:\n$html")
    }

    @Test
    fun `summary renders nothing when there are no general errors`() {
        val errors = FormErrors.of(listOf(Validation.ValidationError(listOf("sukunimi"), "Sukunimi puuttuu")))
        val html = createHTML().section { formErrorSummary(errors) }

        assertFalse(html.contains("error-text"), "should not render a summary:\n$html")
    }

    @Test
    fun `summary lists every general error`() {
        val errors =
            FormErrors.of(
                listOf(
                    Validation.ValidationError(listOf(), "Arvioijalla on jo voimassa oleva kausi"),
                    Validation.ValidationError(listOf(), "Valitse vähintään yksi tutkintokieli"),
                    Validation.ValidationError(listOf("sukunimi"), "Sukunimi puuttuu"),
                ),
            )
        val html = createHTML().section { formErrorSummary(errors) }

        assertTrue(html.contains("error-text"), "missing summary article:\n$html")
        assertTrue(html.contains("Arvioijalla on jo voimassa oleva kausi"), "missing first error:\n$html")
        assertTrue(html.contains("Valitse vähintään yksi tutkintokieli"), "missing second error:\n$html")
        assertFalse(html.contains("Sukunimi puuttuu"), "field errors belong to the field, not the summary:\n$html")
    }

    private fun renderSukunimiField(errors: FormErrors): String =
        createHTML().section {
            formField(
                label = LocalizedString(fi = "Sukunimi"),
                name = "sukunimi",
                errors = errors,
                testId = "sukunimi",
            ) { invalid ->
                input(type = InputType.text, name = "sukunimi", value = "Möttönen") {
                    ariaInvalid(invalid)
                }
            }
        }

    @Test
    fun `piilokentan virhe voidaan nostaa yhteenvetoon`() {
        val errors =
            FormErrors.of(
                listOf(
                    Validation.ValidationError(listOf("arvioijaOid"), "Oppijanumeroa ei loydy"),
                ),
            )

        assertEquals(emptyList(), errors.yleiset, "kentalle kohdistettu virhe ei ole yleinen")
        assertEquals(listOf("Oppijanumeroa ei loydy"), errors["arvioijaOid"])
    }
}
