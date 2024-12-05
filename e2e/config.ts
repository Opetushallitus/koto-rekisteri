import * as fs from "node:fs"

/**
 * Mangles the Spring datasource configuration properties to a `pg`-compatible
 * connection string.
 *
 * To avoid having to configure the E2E test DB in multiple places, it is most
 * convenient to just reuse the server configuration, or more specifically, the
 * `application-e2e.properties` -file. The main issue with this is that server
 * configuration is in a `*.properties` format and that the DB credentials and
 * DB urls differ slightly between JDBC and the `pg` npm package.
 *
 * In brief, the .properties -file contains something like this:
 * ```
 * spring.datasource.url=jdbc:postgresql://host:port/database?stringtype=unspecified
 * spring.datasource.username=user
 * spring.datasource.password=pass
 * ```
 * With Spring, datasource credentials are separate, while the host, port and
 * the database name are combined into a connection URL.
 *
 * On the other hand `pg` expects each part separately; the configuration needs
 * host, port and the database name separately, and the credentials as separate
 * fields. Alternatively it accepts a single connection string, with the
 * credentials and the connection URL combined into one. That is:
 * ```
 * postgresql://user:pass@host:port/database
 * ```
 *
 * It is simpler to strip JDBC-specific parts out of the JDBC URL and inject the
 * `user:pass@` to the stripped DB URL than it is to parse the individual parts
 * out of the JDBC URL.
 *
 * Therefore, to re-use the JDBC-formatted configuration, this function:
 *  1. parses the *.properties -file to a series of key value pairs
 *  2. parses the database URL and credentials from Spring datasource properties
 *  3. Constructs a `pg`-compatible connection string from the parsed parts
 */
function getDatabaseConnectionStringFromServerE2EConfiguration() {
  const propertiesFile =
    "../server/src/main/resources/application-e2e.properties"
  const properties = readJavaPropertiesFile(propertiesFile)

  const dbUrl = parseDatabaseUrlFromJDBCUrl(properties["spring.datasource.url"])
  const user = properties["spring.datasource.username"]
  const password = properties["spring.datasource.password"]

  return `postgresql://${user}:${password}@${dbUrl.substring("postgresql://".length)}`
}

function parseDatabaseUrlFromJDBCUrl(jdbcUrl: string): string {
  // The actual DB url is the part beginning with `postgresql://` up to any
  // possible JDBC parameters provided as URL query parameters. The start of
  // JDBC parameters is thus the first `?` in the string.
  const dbUrlBodyStart = jdbcUrl.indexOf("postgresql://")
  const jdbcParamsStart = jdbcUrl.indexOf("?")

  return jdbcUrl.substring(
    dbUrlBodyStart,
    jdbcParamsStart < 0 ? undefined : jdbcParamsStart,
  )
}

function readJavaPropertiesFile(path: string): Record<string, string> {
  const propertiesFileContent = fs.readFileSync(path, "utf-8")
  const propertyLines = propertiesFileContent
    .split("\n")
    // Only non-empty lines
    .filter((line) => line.trim().length > 0)
    // Only lines that are likely to contain a valid property definition
    .filter((line) => line.includes("="))

  const propertiesEntries = propertyLines.flatMap((line) => {
    const indexOfFirstEqualsSign = line.indexOf("=")
    if (indexOfFirstEqualsSign < 0) {
      return []
    }

    const key = line.substring(0, indexOfFirstEqualsSign)
    const value = line.substring(indexOfFirstEqualsSign + 1)
    return [[key, value]]
  })
  return Object.fromEntries(propertiesEntries)
}

export default {
  database: {
    connectionString: getDatabaseConnectionStringFromServerE2EConfiguration(),
  },
}
