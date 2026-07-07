package fi.oph.kitu.i18n

import kotlinx.html.Tag

context(tag: Tag)
operator fun LocalizedString.unaryPlus() {
    tag.text(get(CurrentLanguage.get()))
}
