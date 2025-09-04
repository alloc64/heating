FROM gradle:8.10.2-jdk21-alpine AS build
COPY --chown=gradle:gradle .. /work
WORKDIR /work
RUN --mount=type=cache,sharing=locked,target=/root/.gradle gradle :clean :build --no-daemon -x test --parallel

FROM eclipse-temurin:21-jre

RUN apt-get update && apt-get install -y \
    libgpiod2 \
    libstdc++6 \
 && rm -rf /var/lib/apt/lists/*
RUN addgroup -gid 993 gpio

WORKDIR /app
COPY --from=build /work/build/libs/work-0.0.1.jar app.jar

ENTRYPOINT java -jar app.jar
