# AGENTS.md

Spring Boot **4.1.1** / Java **17** Maven app. Package root `com.example.demo`. Single module, no frontend.

## Commands (Windows PowerShell)

- Build: `.\mvnw.cmd package`
- Run app: `.\mvnw.cmd spring-boot:run`
- All tests: `.\mvnw.cmd test`
- Single test: `.\mvnw.cmd "-Dtest=DemoApplicationTests" test`

Use the Maven wrapper (Maven 3.9.16). **`JAVA_HOME` on this machine points at JDK 8**, which cannot build this project — override it first, e.g. `$env:JAVA_HOME='D:\soft\worksoft\JDK17'; .\mvnw.cmd test`. `.vscode/settings.json` also hardcodes a machine-specific Maven path; ignore it. No lint/format/typecheck tooling is configured.

## Persistence (MyBatis-Plus + XML mappers)

- All SQL lives in `src/main/resources/mapper/*.xml`; mapper interfaces in `mapper`, entities in `entity`. Keep statements in XML, not annotations.
- Uses `mybatis-plus-spring-boot4-starter` 3.5.15. Do **not** also add `mybatis-spring-boot-starter` — the two autoconfigurations conflict. Config prefix is `mybatis-plus.*` (not `mybatis.*`).
- Datasource: MySQL schema `miaosha` @ `192.168.187.128:3306`, creds in `src/main/resources/application.yaml`. Sample table is `tb_user` (currently empty).
- `spring-boot-starter-actuator` is present but `/actuator/health` returns 503 because Redis (`spring-boot-starter-data-redis`) has no running server. That is expected and unrelated to app correctness.

## Structure

- Layers: `controller` (REST) → `service` + `service.impl` → `mapper` (MyBatis interfaces) → `entity` (POJOs).
- Live endpoints: `GET /user/list`, `GET /user/{username}`, `POST /user`, `PUT /user`, `DELETE /user/{username}`.
- Spring Boot 4 starter names differ from Boot 3: `spring-boot-starter-webmvc` (not `-web`), plus test starters `spring-boot-starter-webmvc-test` / `spring-boot-starter-data-redis-test`.
- Lombok is scoped `annotationProcessor` (an invalid Maven scope — it warns but works). If you change it, verify `@Data` still generates code.
- `ServletInitializer` exists for WAR-style deployment, but packaging is `jar`.
