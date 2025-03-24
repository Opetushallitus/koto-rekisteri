import SQL from "sql-template-strings"
import { TestDB } from "./baseFixture"

export interface YkiSuoritusError {
  message: string
  context: string
  exception_message: string
  stack_trace: string
  created: string
  key_values: string
  source_type: string
}

const toTimestamp = (date: Date): string => {
  const yyyy = date.getFullYear()
  const MM = date.getMonth() + 1
  const dd = date.getDate()
  const hh = date.getHours()
  const mm = date.getMinutes()
  const ss = date.getSeconds()

  // "yyyy-MM-dd HH:mm:ss"
  return `${yyyy}-${MM}-${dd} ${hh}:${mm}:${ss}`
}

export const fixtureData = {
  invalidFormatCsvExportError: {
    message: "InvalidFormatCsvExportError",
    context:
      ' "1.2.246.562.24.59267607404","010116A9518","CORRUPTED","Kivinen-Testi","Petro Testi","","Testikuja 10","40100","Testinsuu","testi.petro@testi.fi",183425,2024-10-30T13:55:09Z,2024-09-01,"fin","YT","1.2.246.562.10.14893989377","Jyväskylän yliopisto, Soveltavan kielentutkimuksen keskus",2024-10-30,6,6,,6,6,,,,0,0,,',
    exception_message:
      'Cannot deserialize value of type `fi.oph.kitu.yki.Sukupuoli` from String "CORRUPTED": not one of the values accepted for Enum class: [E, N, M]\n' +
      ' at [Source: (StringReader); line: 4, column: 45] (through reference chain: fi.oph.kitu.yki.suoritukset.YkiSuoritusCsv["sukupuoli"])',
    stack_trace:
      "com.fasterxml.jackson.databind.exc.InvalidFormatException.from(InvalidFormatException.java:67)\n" +
      "com.fasterxml.jackson.databind.DeserializationContext.weirdStringException(DeserializationContext.java:1959)\n" +
      "com.fasterxml.jackson.databind.DeserializationContext.handleWeirdStringValue(DeserializationContext.java:1245)\n" +
      "com.fasterxml.jackson.databind.deser.std.EnumDeserializer._deserializeAltString(EnumDeserializer.java:447)\n" +
      "com.fasterxml.jackson.databind.deser.std.EnumDeserializer._fromString(EnumDeserializer.java:304)\n" +
      "com.fasterxml.jackson.databind.deser.std.EnumDeserializer.deserialize(EnumDeserializer.java:273)\n" +
      "com.fasterxml.jackson.databind.deser.SettableBeanProperty.deserialize(SettableBeanProperty.java:543)\n" +
      "com.fasterxml.jackson.databind.deser.BeanDeserializer._deserializeWithErrorWrapping(BeanDeserializer.java:585)\n" +
      "com.fasterxml.jackson.databind.deser.BeanDeserializer._deserializeUsingPropertyBased(BeanDeserializer.java:447)\n" +
      "com.fasterxml.jackson.databind.deser.BeanDeserializerBase.deserializeFromObjectUsingNonDefault(BeanDeserializerBase.java:1497)\n" +
      "com.fasterxml.jackson.databind.deser.BeanDeserializer.deserializeFromObject(BeanDeserializer.java:348)\n" +
      "com.fasterxml.jackson.databind.deser.BeanDeserializer.deserialize(BeanDeserializer.java:185)\n" +
      "com.fasterxml.jackson.databind.MappingIterator.nextValue(MappingIterator.java:283)\n" +
      "fi.oph.kitu.csvparsing.CsvParserKt.toDataWithErrorHandling(CsvParser.kt:163)\n" +
      "fi.oph.kitu.yki.YkiService.importYkiSuoritukset$lambda$1(YkiService.kt:270)\n" +
      "fi.oph.kitu.logging.EventLoggerKt.withEventAndPerformanceCheck(EventLogger.kt:108)\n" +
      "fi.oph.kitu.yki.YkiService.importYkiSuoritukset(YkiService.kt:53)\n" +
      "fi.oph.kitu.yki.YkiService.importYkiSuoritukset$default(YkiService.kt:46)\n" +
      "fi.oph.kitu.yki.YkiScheduledTasks.dailyImport$lambda$0(YkiScheduledTasks.kt:26)\n" +
      "com.github.kagkarlsson.scheduler.task.helper.Tasks$RecurringTaskBuilder$3.execute(Tasks.java:171)\n" +
      "com.github.kagkarlsson.scheduler.event.ExecutionChain.proceed(ExecutionChain.java:37)\n" +
      "com.github.kagkarlsson.scheduler.ExecutePicked.executePickedExecution(ExecutePicked.java:113)\n" +
      "com.github.kagkarlsson.scheduler.ExecutePicked.run(ExecutePicked.java:89)\n" +
      "com.github.kagkarlsson.scheduler.FetchCandidates.lambda$run$1(FetchCandidates.java:117)\n" +
      "java.base/java.util.Optional.ifPresent(Optional.java:178)\n" +
      "com.github.kagkarlsson.scheduler.FetchCandidates.lambda$run$2(FetchCandidates.java:103)\n" +
      "com.github.kagkarlsson.scheduler.Executor.lambda$addToQueue$0(Executor.java:53)\n" +
      "java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)\n" +
      "java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)\n" +
      "java.base/java.lang.Thread.run(Thread.java:1583)",
    created: "2025-03-24 12:51:29.373287",
    key_values: JSON.stringify([
      { lineNumber: "3" },
      {
        exception:
          'com.fasterxml.jackson.databind.exc.InvalidFormatException: Cannot deserialize value of type `fi.oph.kitu.yki.Sukupuoli` from String "CORRUPTED": not one of the values accepted for Enum class: [E, N, M]\n at [Source: (StringReader); line: 4, column: 45] (through reference chain: fi.oph.kitu.yki.suoritukset.YkiSuoritusCsv["sukupuoli"])',
      },
      { value: "CORRUPTED" },
      { path: '[fi.oph.kitu.yki.suoritukset.YkiSuoritusCsv["sukupuoli"]]' },
      { field: "sukupuoli" },
      { targetType: "class fi.oph.kitu.yki.Sukupuoli" },
    ]),
    source_type: "YkiSuoritusCsv",
  },
}

const insertQuery = (error: YkiSuoritusError) => SQL`
  INSERT INTO yki_suoritus_error(
      message,
      context,
      exception_message,
      stack_trace,
      created,
      key_values,
      source_type
  ) VALUES (${error.message},
            ${error.context},
            ${error.exception_message},
            ${error.stack_trace},
            ${error.created},
            ${error.key_values},
            ${error.source_type})
`

export type YkiSuorittajaErrorName = keyof typeof fixtureData

export const insert = async (db: TestDB, error: YkiSuorittajaErrorName) =>
  await db.dbClient.query(insertQuery(fixtureData[error]))
