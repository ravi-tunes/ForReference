plugins {
    java
    id("me.champeau.jmh") version "0.7.2"
}

group = "com.example"
version = "1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")

    // Mockito
    testImplementation("org.mockito:mockito-core:5.11.0")

    // FastUtil
    implementation("it.unimi.dsi:fastutil:8.5.13")

    // JMH (auto-configured by plugin)
    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmh("org.openjdk.jmh:jmh-generator-annprocess:1.37")

    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("com.zaxxer:HikariCP:5.0.1") // Optional, included by default in starter-jdbc
    implementation("com.oracle.database.jdbc:ojdbc11:23.2.0.0")
}

tasks.test {
    useJUnitPlatform()
}

jmh {
    warmupIterations.set(3)
    iterations.set(5)
    fork.set(1)
    timeOnIteration.set("1s")
    resultFormat.set("JSON")
    includes.set(listOf(".*BookLookupServicePerformanceTest.*"))
}

./gradlew test
./gradlew jmh