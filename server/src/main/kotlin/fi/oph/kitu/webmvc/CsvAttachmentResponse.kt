package fi.oph.kitu.webmvc

import fi.oph.kitu.csvparsing.writeExcelCsvPrelude
import fi.oph.kitu.html.table.ColumnTag
import fi.oph.kitu.html.table.DisplayTableCsvRenderer
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

inline fun <reified C : Enum<C>, T> csvAttachmentResponse(
    filename: String,
    data: Iterable<T>,
    excludeTags: Set<ColumnTag> = emptySet(),
): ResponseEntity<StreamingResponseBody> =
    ResponseEntity
        .ok()
        .contentType(MediaType.parseMediaType("text/csv"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
        .body(
            StreamingResponseBody { output ->
                output.writeExcelCsvPrelude()
                DisplayTableCsvRenderer.renderCsv<C, _>(
                    output = output,
                    data = data,
                    excludeTags = excludeTags,
                )
            },
        )
