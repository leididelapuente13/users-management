# Etapa 1: Construcción (Build)
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copiar el wrapper de Maven y el archivo de configuración
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Asegurar formato Unix y permisos de ejecución para mvnw
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Descargar las dependencias (mejora el caché de Docker)
RUN ./mvnw dependency:go-offline

# Copiar el código fuente y compilar
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Etapa 2: Ejecución (Run)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiar el JAR generado desde la etapa de construcción
COPY --from=builder /app/target/*.jar app.jar

# Exponer puertos comunes
EXPOSE 8080 8081

# Ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]