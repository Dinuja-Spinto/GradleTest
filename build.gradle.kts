import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import org.apache.tools.ant.filters.FixCrLfFilter
import org.apache.tools.ant.filters.ReplaceTokens

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
    implementation("commons-io:commons-io:2.7")
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
            "move"("file" to "${dir}/toArchive/reports", "todir" to "${dir}/")
        }
    }
}

//Renaming files as they are copied
tasks.register<Copy>("copyFromStaging") {
    from("src/main/java/org/gradleTest/report")
    into(layout.buildDirectory.dir("explodedWar"))

    rename("(.+)-staging(.+)", "$1$2")
}

//Truncating filenames as they are copied
tasks.register<Copy>("copyWithTruncate") {
    from(layout.buildDirectory.dir("reports"))
    rename { filename: String ->
        if (filename.length > 10) {
            filename.slice(0..7) + "~" + filename.length
        }
        else filename
    }
    into(layout.buildDirectory.dir("toArchive"))
}

//Deleting a directory
tasks.register<Delete>("myClean") {
    delete(layout.buildDirectory.dir("toArchive3"))
}

//Deleting files matching a specific pattern
tasks.register<Delete>("cleanPdfFiles") {
    delete(fileTree("src").matching {
        include("**/*.pdf")
    })
}

//How to minimize the number of hard-coded paths in your build
val archivesDirPath: Provider<Directory> = layout.buildDirectory.dir("archives")

tasks.register<Zip>("packageClasses") {
    archiveAppendix = "classes"
    destinationDirectory = archivesDirPath

    from(tasks.compileJava)
}

//--Locating files--

// Using a relative path
var configFile = file("src/config.xml")
//println("*")
//println(configFile)

// Using an absolute path
configFile = file(configFile.absolutePath)
//println("*")
//println(configFile)

// Using a File object with a relative path
configFile = file(File("src/config.xml"))
//println("*")
//println(configFile)

// Using a java.nio.file.Path object with a relative path
configFile = file(Paths.get("src", "config.xml"))
//println("*")
//println(configFile)

// Using an absolute java.nio.file.Path object
configFile = file(Paths.get(System.getProperty("user.home")).resolve("global-config.xml"))
//println("*")
//println(configFile)

//--Creating a path relative to a parent project--

val configFile2 = file("$rootDir/shared/config.xml")

//Creating a file collection
val collection: FileCollection = layout.files(
        "src/file1.txt",
        File("src/file2.txt"),
        listOf("src/file3.csv", "src/file4.csv"),
        Paths.get("src", "file5.txt")
)

//Implementing a file collection
tasks.register("list") {
    val projectDirectory = layout.projectDirectory
    doLast {
        var srcDir: File? = null

        val collection = projectDirectory.files({
            srcDir?.listFiles()
        })

        srcDir = projectDirectory.file("src").asFile
        println("Contents of ${srcDir.name}")
        collection.map { it.relativeTo(projectDirectory.asFile) }.sorted().forEach { println(it) }

        srcDir = projectDirectory.file("src2").asFile
        println("Contents of ${srcDir.name}")
        collection.map { it.relativeTo(projectDirectory.asFile) }.sorted().forEach { println(it) }
    }
}

//--Using a file collection--
// Iterate over the files in the collection
//collection.forEach { file: File ->
    //println(file.name)
    //Files.createFile(file.toPath())
//}

// Convert the collection to various types
val set: Set<File> = collection.files
val list: List<File> = collection.toList()
val path: String = collection.asPath
//val file: File = collection.singleFile
//println(path)
// Add and subtract collections
val projectLayout: Directory = layout.projectDirectory
val union: FileCollection = collection + projectLayout.files("src/dinuja2.txt")
val difference: FileCollection = collection - projectLayout.files("src/file2.txt")
//println(difference.files)

//--Filtering a file collection--
tasks.register("filterTextFiles "){
    doFirst{
        val textFiles: FileCollection = collection.filter { f: File ->
            f.name.endsWith(".txt")
        }

        textFiles.forEach { file: File ->
            println(file.name)
        }
    }
}

//--File trees--
//Creating a file tree
// Create a file tree with a base directory
var tree: ConfigurableFileTree = fileTree("src/main")
// Add include and exclude patterns to the tree
tree.include("**/*.java")
tree.exclude("**/Abstract*")
//// Create a tree using closure
//tree = fileTree("src") {
//    include("**/*.java")
//}
// Create a tree using a map
//tree = fileTree("dir" to "src", "include" to "**/*.java")
//tree = fileTree("dir" to "src", "includes" to listOf("**/*.java", "**/*.xml"))
//tree = fileTree("dir" to "src", "include" to "**/*.java", "exclude" to "**/*test*/**")

// Using a file tree
// Iterate over the contents of a tree
//tree.forEach{ file: File ->
//    println(file)
//}

// Filter a tree
val filtered: FileTree = tree.matching {
    include("org/gradleTest/**")
}

// Add trees together
val sum: FileTree = tree + fileTree("src/test")

// Visit the elements of the tree
/*
tree.visit {
    println("${this.relativePath} => ${this.file}")
}*/

//Using an archive as a file tree
// Create a ZIP file tree using path
val zip: FileTree = zipTree("src/main/resources/thirdPartyResources.zip")

// Create a TAR file tree using path
val tar: FileTree = tarTree("thirdPartyResources.tar")
//zip.forEach{file : File ->
//    println(file.name)
//}

//Specifying a set of files
tasks.register<JavaCompile>("compile") {
    // Use a File object to specify the source directory
    source = fileTree(file("src/main/java"))

    // Use a String path to specify the source directory
    source = fileTree("src/main/java")

    // Use a collection to specify multiple source directories
    source = fileTree(listOf("src/main/java", "../shared/java"))

    // Use a FileCollection (or FileTree in this case) to specify the source files
    source = fileTree("src/main/java").matching { include("org/gradle/api/**") }

    // Using a closure to specify the source files.
    setSource({
        // Use the contents of each zip file in the src dir
        file("src").listFiles()!!.filter { it.name.endsWith(".zip") }.map { zipTree(it) }
    })
}

//Appending a set of files
tasks.named<JavaCompile>("compile") {
    // Add some source directories use String paths
    source("src/main/java", "src/main/resources")

    // Add a source directory using a File object
    source(file("../shared/java"))

    // Add some source directories using a closure
    setSource({ file("src/test/").listFiles() })
}

//Specifying copy task source files and destination directory
tasks.register<Copy>("anotherCopyTask") {
    // Copy everything under src/main/webapp
    from("src/main/webapp")
    // Copy a single file
    from("src/staging/index.html")
    // Copy the output of a task
    //from(copyTask)
    // Copy the output of a task using Task outputs explicitly.
    from(tasks["copyTaskWithPatterns"].outputs)
    // Copy the contents of a Zip file
    from(zipTree("src/main/assets.zip"))
    // Determine the destination directory later
    //into({ getDestDir() })
}

//Selecting the files to copy
tasks.register<Copy>("copyTaskWithPatterns") {
    from("src/main/")
    into(layout.buildDirectory.dir("explodedWar"))
    include("**/*.html")
    include("**/*.jsp")
    exclude { details: FileTreeElement ->
        details.file.name.endsWith(".html") &&
                details.file.readText().contains("DRAFT")
    }
}
// Renaming files as they are copied
tasks.register<Copy>("rename") {
    from("src/main/java")
    into(layout.buildDirectory.dir("explodedWar2"))
    // Use a regular expression to map the file name
    rename("(.+)-staging(.+)", "$1$2")
    rename("(.+)-staging(.+)".toRegex().pattern, "$1$2")
    // Use a closure to convert all file names to upper case
    rename { fileName: String ->
        fileName.uppercase(Locale.getDefault())
    }
}

//Filtering files as they are copied
tasks.register<Copy>("filter") {
    from("src/main")
    into(layout.buildDirectory.dir("explodedWar"))
    // Substitute property tokens in files
    expand("copyright" to "2009", "version" to "2.3.1")
    // Use some of the filters provided by Ant
    filter(FixCrLfFilter::class)
    filter(ReplaceTokens::class, "tokens" to mapOf("copyright" to "2009", "version" to "2.3.1"))
    // Use a closure to filter each line
    filter { line: String ->
        "[$line]"
    }
    // Use a closure to remove lines
    filter { line: String ->
        if (line.startsWith('-')) null.toString() else line
    }
    filteringCharset = "UTF-8"
}

//Setting permissions for destination files
//tasks.register<Copy>("permissions") {
//    from("src/main/webapp")
//    into(layout.buildDirectory.dir("explodedWar"))
//    CopySpec.filePermissions {
//        user {
//            read = true
//            execute = true
//        }
//        other.execute = false
//    }
//    CopySpec.dirPermissions {
//        unix("r-xr-x---")
//    }
//}

//Sharing copy specifications
val webAssetsSpec: CopySpec = copySpec {
    from("src/main")
    include("**/*.html", "**/*.png", "**/*.jpg")
    rename("(.+)-staging(.+)", "$1$2")
}

tasks.register<Copy>("copyAssets") {
    into(layout.buildDirectory.dir("inPlaceApp"))
    with(webAssetsSpec)
}

tasks.register<Zip>("distApp") {
    archiveFileName = "my-app-dist.zip"
    destinationDirectory = layout.buildDirectory.dir("dists")

    from("src/main")
    with(webAssetsSpec)
}