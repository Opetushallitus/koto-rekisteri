# io.rocketbase.extension — vendored fork

This is a vendored copy of `io.rocketbase.extension:db-scheduler-log` +
`:db-scheduler-log-spring-boot-starter` at tag **0.7.0** (commit from
<https://github.com/rocketbase-io/db-scheduler-log/tree/0.7.0>).

Upstream does not (yet) publish a Spring Boot 4 compatible release, so the
sources are copied into this project under the original package names so that
autoconfiguration picks up exactly as if the artifact were on the classpath.
Keep the package names (`io.rocketbase.extension.*`) unchanged so rolling back
to an upstream release is a one-shot change:

1. Delete this `io/rocketbase/` directory and the
   `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
   entries for the two auto-configs.
2. Add back the dependency in `server/pom.xml`:
   ```xml
   <dependency>
     <groupId>io.rocketbase.extension</groupId>
     <artifactId>db-scheduler-log-spring-boot-starter</artifactId>
     <version>X.Y.Z</version>
   </dependency>
   ```
3. Ensure `com.github.kagkarlsson:micro-jdbc` is no longer declared directly;
   upstream pulls it transitively.

Licensed under Apache 2.0 by Marten Prieß (see source file headers).
