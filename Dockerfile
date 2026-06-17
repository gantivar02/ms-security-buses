# ============================================================
# ms-security-buses — Spring Boot 4 + MongoDB (Atlas)
# Multi-stage: Maven compila el JAR en la primera etapa y un JRE
# liviano lo ejecuta en la segunda (la imagen final no trae Maven).
# ============================================================

# ---------- Etapa 1: build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache de dependencias: si pom.xml no cambia, Maven no vuelve a
# descargar todo el arbol en cada build. El "|| true" evita que un fallo
# parcial de go-offline (algun plugin no resoluble offline) rompa el
# build: el "package" de abajo descargara lo que falte (hay red).
COPY pom.xml .
RUN mvn -B dependency:go-offline || true

# Codigo fuente y empaquetado (sin correr tests aqui)
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Etapa 2: runtime ----------
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# El nombre del jar sale de <artifactId>-<version> del pom:
#   ms-security + 0.0.1-SNAPSHOT
COPY --from=build /app/target/ms-security-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
