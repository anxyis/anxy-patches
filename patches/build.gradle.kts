plugins {
    kotlin("jvm") version "2.2.21"
    `maven-publish`
}

group = "anxyis.morphe"

repositories {
    mavenLocal()
    mavenCentral()
    google()
    maven {
        url = uri("https://jitpack.io")
        content {
            includeGroup("com.github.MorpheApp.smali")
            includeGroup("com.github.REAndroid")
        }
    }
}

dependencies {
    implementation("app.morphe:morphe-patcher:1.9.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.google.guava:guava:33.3.1-jre")
    implementation("com.github.MorpheApp.smali:smali:d856bad65f")
    implementation("com.github.REAndroid:arsclib:a28c6fb2a7")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

tasks {
    test {
        useJUnitPlatform()
    }

    // Build .mpp patch bundle matching semantic-release naming
    register<Jar>("buildPatchBundle") {
        archiveBaseName.set("patches")
        archiveVersion.set(project.version.toString())
        archiveExtension.set("mpp")
        from(sourceSets["main"].output)
    }

    build {
        dependsOn("buildPatchBundle")
    }

    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"
        dependsOn(build)
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("anxyis.morphe.util.PatchListGeneratorKt")
    }
}
