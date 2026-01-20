# Fase di Runtime: Usa l'immagine JRE minimalista di Java 17
# 'temurin:17-jre-focal' è una base robusta e leggera per l'esecuzione.
FROM eclipse-temurin:17-jre-focal

# Imposta la directory di lavoro all'interno del container
WORKDIR /app

# Argomento di build per definire il nome del file JAR.
# Quando costruisci l'immagine, dovrai specificare il file JAR (es. -build-arg JAR_FILE=target/mia-app-1.0.0.jar)
# In alternativa, puoi usare un wildcard 'target/*.jar' se non ci sono altri JAR.
ARG JAR_FILE=target/*.jar

# Copia il JAR eseguibile dalla directory 'target' dell'host alla directory '/app' del container.
# ASSICURATI che il file 'target/nome-app.jar' esista prima di lanciare 'docker build'.
COPY ${JAR_FILE} app.jar

# Espone la porta di default di Spring Boot
EXPOSE 8081

# Definisce il comando che verrà eseguito all'avvio del container.
# Vengono incluse le best practice per la gestione della memoria in un container.
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-XX:MaxRAMPercentage=80.0", "-jar", "app.jar"]