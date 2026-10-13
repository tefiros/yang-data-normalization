plugins {
    `java-library`
    application
    `maven-publish`
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.upokecenter:cbor:4.5.6")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

group = "com.telefonica.yang"
version = "0.1.0-SNAPSHOT"
description = "Serialization-independent normalization of YANG state data (XML, JSON, CBOR)"

java {
    // Java 8 bytecode keeps the library consumable from cose-provenance.
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()
}

application {
    mainClass.set("com.telefonica.yang.normalization.tool.NormalizeCli")
}

tasks.register<JavaExec>("runDemo") {
    group = "application"
    description = "Runs the small screen demonstration of AST normalization"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.telefonica.yang.normalization.tool.SimpleDemo")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-options"))
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "com.telefonica.yang"
            artifactId = "yang-data-normalization"

            pom {
                name.set("YANG Data Normalization")
                description.set(
                    "Schema-independent normalization of YANG state data across XML, JSON and CBOR"
                )
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
            }
        }
    }

    repositories {
        mavenLocal()
    }
}
