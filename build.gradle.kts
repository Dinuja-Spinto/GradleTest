import java.nio.file.Files

plugins {
    id("java")
    base
}

group = "org.gradleTest"
version = "1.0.2"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    //Creating a Java uber or fat JAR
    implementation("commons-io:commons-io:2.6")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Copy>("copyReport") {
    from(layout.projectDirectory.file("reports/my-report.pdf"))
    into(layout.projectDirectory.dir("toArchive"))
}

tasks.register<Copy>("copyReport2") {
    from(layout.buildDirectory.file("reports/my-report.pdf"))
    into(layout.buildDirectory.dir("toArchive2"))
}

//copy multi file
tasks.register<Copy>("copyReportsForArchiving") {
    from(layout.projectDirectory.file("reports/my-report.pdf"), layout.projectDirectory.file("src/docs/manual.pdf"))
    into(layout.buildDirectory.dir("reports"))
}

//copy all the (**specific files in the dir)  PDFs in a directory
tasks.register<Copy>("copyPdfReportsForArchiving") {
    from(layout.projectDirectory.dir("reports"))
    include("*.pdf")
    into(layout.buildDirectory.dir("toArchive3"))
}

//include files in subDirectories
tasks.register<Copy>("copyAllPdfReportsForArchiving") {
    from(layout.projectDirectory.dir("reports"))
    include("**/*.pdf")
    into(layout.buildDirectory.dir("toArchive4"))
}

//Copying an entire directory
tasks.register<Copy>("copyReportsDirForArchiving") {
    from(layout.projectDirectory.dir("reports"))
    into(layout.buildDirectory.dir("toArchive5"))
}

//Copying an entire directory, including itself
tasks.register<Copy>("copyReportsDirForArchiving2") {
    from(layout.buildDirectory) {
        include("toArchive5/**")
    }
    into(layout.buildDirectory.dir("toArchive6"))
}

//Archiving a directory as a ZIP
tasks.register<Zip>("packageDistribution") {
    archiveFileName = "my-distribution2.zip"
    destinationDirectory = layout.buildDirectory.dir("dist")

    from(layout.buildDirectory.dir("toArchive"))
}

//Using the Base Plugin for its archive name convention
tasks.register<Zip>("packageDistribution2") {
    from(layout.projectDirectory.dir("toArchive")) {
        exclude("**/*.pdf")
    }

    from(layout.projectDirectory.dir("toArchive5")) {
        include("**/*.pdf")
        into("docs")
    }
}

//Unpacking a ZIP file
tasks.register<Copy>("unpackFiles") {
    from(zipTree("src/main/resources/thirdPartyResources.zip"))
    into(layout.buildDirectory.dir("resources"))
}

//Unpacking a subset of a ZIP file
tasks.register<Copy>("unpackLibsDirectory") {
    from(zipTree("src/main/resources/thirdPartyResources2.zip")) {
        include("lib/**")
        eachFile {
            relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
        }
        includeEmptyDirs = false
    }
    into(layout.buildDirectory.dir("reso"))
}

//Creating a Java uber or fat JAR
tasks.register<Jar>("uberJar") {
    archiveClassifier = "uber"

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

//Manually creating a directory
tasks.register("ensureDirectory") {
    // Store target directory into a variable to avoid project reference in the configuration cache
    val directory = file("src/images3")

    doLast {
        //Files.createDirectories(directory.toPath())
        directory.mkdirs()
    }
}

//Moving a directory using the Ant task
tasks.register("moveReports") {
    // Store the build directory into a variable to avoid project reference in the configuration cache
    val dir = buildDir

    doLast {
        ant.withGroovyBuilder {
            "move"("file" to "${dir}/toArchive2", "todir" to "${dir}/toArchive3")
        }
    }
}