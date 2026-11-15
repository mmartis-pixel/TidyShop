plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "cz.mmartis"
version = "0.1.0"

val mcVersion = "26.3"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    val paperApi = "io.papermc.paper:paper-api:26.3.build.252-stable"
    compileOnly(paperApi)
    testImplementation(paperApi)

    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.3:4.120.0")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filesMatching("plugin.yml") { expand(props) }
    }
    test {
        useJUnitPlatform()
    }
    runServer {
        minecraftVersion(mcVersion)
    }
}
