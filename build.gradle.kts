plugins {
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.4"
    id("java")
    id("me.champeau.jmh") version "0.7.2"
    jacoco
}

group = "com.example"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.maven.apache.org/maven2")
    }
}

dependencies {
    // Spring
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")

    // Oracle DB
    implementation("com.oracle.database.jdbc:ojdbc11:23.2.0.0")

    // HikariCP
    implementation("com.zaxxer:HikariCP:5.0.1")

    // FastUtil
    implementation("it.unimi.dsi:fastutil:8.5.13")

    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")

    // Mockito
    testImplementation("org.mockito:mockito-core:5.11.0")

    // JMH
    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmh("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

tasks.test {
    useJUnitPlatform()
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                minimum = "1.0".toBigDecimal() // 100% coverage required
            }
        }
    }
}

tasks.build {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

jmh {
    warmupIterations.set(3)
    iterations.set(5)
    fork.set(1)
    timeOnIteration.set("1s")
    resultFormat.set("JSON")
    includes.set(listOf(".*BookLookupServicePerformanceTest.*"))
}
