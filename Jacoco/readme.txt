plugins {
    jacoco
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // run tests before report
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
                minimum = "1.0".toBigDecimal() // 100% coverage
            }
        }
    }
}

tasks.build {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

./gradlew clean build

Coverage Report Location
    •   HTML report: build/reports/jacoco/test/html/index.html
    •   XML: build/reports/jacoco/test/jacocoTestReport.xml
    