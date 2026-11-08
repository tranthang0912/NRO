FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
COPY src ./src
COPY db ./db
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --system nro && useradd --system --gid nro --home /app nro
COPY --from=build /build/target/nro-web.jar ./nro-web.jar
COPY --chown=nro:nro web ./web
COPY --chown=nro:nro config/ca.pem ./config/ca.pem
COPY --chown=nro:nro Config.properties ./Config.properties
COPY --chown=nro:nro data/map ./data/map
COPY --chown=nro:nro data/update_data ./data/update_data
COPY --chown=nro:nro data/icon/x1 ./data/icon/x1
COPY --chown=nro:nro data/res/x1 ./data/res/x1
COPY --chown=nro:nro data/item_bg_temp/x1 ./data/item_bg_temp/x1
COPY --chown=nro:nro data/mob/x1 ./data/mob/x1
COPY --chown=nro:nro data/web-mob ./data/web-mob
COPY --chown=nro:nro data/effdata ./data/effdata
USER nro
ENV PORT=10000 DB_POOL_MAX=3 MAX_PLAYERS=5 TZ=Asia/Ho_Chi_Minh
EXPOSE 10000
CMD ["java", "-Xms64m", "-XX:MaxRAMPercentage=65", "-XX:+UseSerialGC", "-Djava.awt.headless=true", "-Dfile.encoding=UTF-8", "-jar", "nro-web.jar"]
