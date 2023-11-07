import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import org.apache.tools.ant.filters.FixCrLfFilter
import org.apache.tools.ant.filters.ReplaceTokens
import java.time.Duration

plugins {
    id("java")
    base
    war
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
/*tasks.register("moveReports") {
    // Store the build directory into a variable to avoid project reference in the configuration cache
    val dir = buildDir

    doLast {
        ant.withGroovyBuilder {
            "move"("file" to "${dir}/toArchive/reports", "todir" to "${dir}/")
        }
    }
}*/

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

//Sharing copy patterns only
val webAssetPatterns = Action<CopySpec> {
    include("**/*.java", "**/*.png", "**/*.jpg")
}

tasks.register<Copy>("copyAppAssets") {
    into(layout.buildDirectory.dir("inPlaceApp2"))
    from("src/main", webAssetPatterns)
}

tasks.register<Zip>("archiveDistAssets") {
    archiveFileName = "distribution-assets.zip"
    destinationDirectory = layout.buildDirectory.dir("dists2")

    from("src/main", webAssetPatterns)
}

//Nested copy specs
tasks.register<Copy>("nestedSpecs") {
    into(layout.buildDirectory.dir("explodedWar3"))
    exclude("**/*staging*")
    from("src/main") {
        include("**/*.html", "**/*.png", "**/*.jpg")
    }
    from(sourceSets.main.get().output) {
        into("WEB-INF/classes")
    }
    into("WEB-INF/lib") {
        from(configurations.runtimeClasspath)
    }
}
//Copying files using the copy() method without up-to-date check
tasks.register("copyMethod") {
    doLast {
        copy {
            from("src/main/")
            into(layout.buildDirectory.dir("explodedWar4"))
            include("**/*.html")
            include("**/*.jsp")
        }
    }
}

//Using the Sync task to copy dependencies
tasks.register<Sync>("libs") {
    from(configurations["runtime"])
    into(layout.buildDirectory.dir("libs"))
}

//Using Copy to deploy a WAR file
tasks.register<Copy>("deployToTomcat") {
    from(tasks.war)
    into(layout.projectDirectory.dir("tomcat/webapps"))
    doNotTrackState("Deployment directory contains unreadable files")
}

//Using Copy to install an executable
tasks.register<Copy>("installExecutable") {
    from("build/toArchive3")
    into("/usr/local/bin")
    doNotTrackState("Installation directory contains unrelated files")
}

//Creation of ZIP archive
tasks.register<Zip>("myZip") {
    from("src/main")
    val projectDir = layout.projectDirectory.asFile
    doLast {
        println(archiveFileName.get())
        println(destinationDirectory.get().asFile.relativeTo(projectDir))
        println(archiveFile.get().asFile.relativeTo(projectDir))
    }
}

//Configuration of archive task - custom archive name
tasks.register<Zip>("myCustomZip") {
    archiveBaseName = "customName"
    from("src/main")

    doLast {
        println(archiveFileName.get())
    }
}
// Configuration of archive task - appendix & classifier
base {
    archivesName = "gradle"
    distsDirectory = layout.buildDirectory.dir("custom-dist")
    libsDirectory = layout.buildDirectory.dir("custom-libs")
}

val myyZip by tasks.registering(Zip::class) {
    from("src/main")
}

val myOtherZip by tasks.registering(Zip::class) {
    archiveAppendix = "wrapper"
    archiveClassifier = "src"
    from("src/main")
}

tasks.register("echoNames") {
    val projectNameString = project.name
    val archiveFileName = myyZip.flatMap { it.archiveFileName }
    val myOtherArchiveFileName = myOtherZip.flatMap { it.archiveFileName }
    doLast {
        println("Project name: $projectNameString")
        println(archiveFileName.get())
        println(myOtherArchiveFileName.get())
    }
}

//--Developing Gradle Tasks--

//Defining tasks using strings for task names
tasks.register("hello") {
    doLast {
        println("hello")
    }
}

//Assigning tasks to variables with DSL specific syntax
val hello2 by tasks.registering {
    doLast {
        println("hello2")
    }
}

//Accessing tasks via tasks collection
tasks.register("hello3")
tasks.register<Copy>("copy")

//println(tasks.named("hello3").get().name) // or just 'tasks.hello' if the task was added by a plugin

//println(tasks.named<Copy>("copy").get().destinationDir)
//
//tasks.withType<Tar>().configureEach {
//    enabled = true
//}
//
//tasks.register("test2") {
//    dependsOn(tasks.withType<Copy>())
//}

//Task class with @Inject constructor
abstract class CustomTask @Inject constructor(
        private val message: String,
        private val number: Int
) : DefaultTask()
//Registering a task with constructor arguments using TaskContainer
tasks.register<CustomTask>("myTask", "hello", 42)
//println(tasks.named("hello3"))

//Adding dependency on task from another project
/*
project("project-a") {
    tasks.register("taskX") {
        dependsOn(":project-b:taskY")
        doLast {
            println("taskX")
        }
    }
}

project("project-b") {
    tasks.register("taskY") {
        doLast {
            println("taskY")
        }
    }
}*/
//Adding dependency using task provider object
/*val taskX by tasks.registering {
    doLast {
        println("taskX")
    }
}

val taskY by tasks.registering {
    doLast {
        println("taskY")
    }
}

taskX {
    dependsOn(taskY)
}*/
//Adding a 'must run after' task ordering
/*val taskX by tasks.registering {
    doLast {
        println("taskX")
    }
}
val taskY by tasks.registering {
    doLast {
        println("taskY")
    }
}
taskY {
    mustRunAfter(taskX)
}
*/
val helloNew by tasks.registering {
    doLast {
        println("hello world")
    }
}

helloNew {
    val skipProvider = providers.gradleProperty("skipHello")
    onlyIf("there is no property skipHello") {
        !skipProvider.isPresent()
    }
}

val compile2 by tasks.registering {
    doLast {
        println("We are doing the compile.")
    }
}

compile2 {
    doFirst {
        // Here you would put arbitrary conditions in real life.
        if (false) {
            throw StopExecutionException()
        }
    }
}
tasks.register("myTask2") {
    dependsOn(compile2)
    doLast {
        println("I am not affected")
    }
}

val disableMe by tasks.registering {
    doLast {
        println("This should not be printed if the task is disabled.")
    }
}

disableMe {
    enabled = false
}

tasks.register("hangingTask") {
    doLast {
        Thread.sleep(200)
    }
    timeout = Duration.ofMillis(500)
}

//Task rule
tasks.addRule("Pattern: ping<ID>") {
    val taskName = this
    if (startsWith("ping")) {
        task(taskName) {
            doLast {
                println("Pinging: " + (taskName.replace("ping", "")))
            }
        }
    }
}

//--Task Class--
// A hello world task
abstract class GreetingTask : DefaultTask() {
    @TaskAction
    fun greet() {
        println("hello from GreetingTask")
    }
}

// Create a task using the task type
tasks.register<GreetingTask>("hello4")

//A customizable hello world task
abstract class GreetingTask2 : DefaultTask() {
    @get:Input
    abstract val greeting: Property<String>

    init {
        greeting.convention("hello from GreetingTask")
    }

    @TaskAction
    fun greet() {
        println(greeting.get())
    }

}

// Use the default greeting
tasks.register<GreetingTask2>("hello5")

// Customize the greeting
tasks.register<GreetingTask2>("greeting") {
    greeting = "greetings from GreetingTask"
}

//Defining an incremental task action
abstract class IncrementalReverseTask : DefaultTask() {
    @get:Incremental
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val inputProperty: Property<String>

    @TaskAction
    fun execute(inputChanges: InputChanges) {
        println(
                if (inputChanges.isIncremental) "Executing incrementally"
                else "Executing non-incrementally"
        )

        inputChanges.getFileChanges(inputDir).forEach { change ->
            if (change.fileType == FileType.DIRECTORY) return@forEach

            println("${change.changeType}: ${change.normalizedPath}")
            val targetFile = outputDir.file(change.normalizedPath).get().asFile
            if (change.changeType == ChangeType.REMOVED) {
                targetFile.delete()
            } else {
                targetFile.writeText(change.file.readText().reversed())
            }
        }
    }
}

// Running the incremental task for the first time
tasks.register<IncrementalReverseTask>("incrementalReverse") {
    inputDir = file("src/main/resources")
    outputDir = layout.buildDirectory.dir("outputs")
    inputProperty = project.findProperty("taskInputProperty") as String? ?: "original"
}
//Running the incremental task with updated input files
tasks.register("updateInputs") {
    val inputsDir = layout.projectDirectory.dir("src/main/resources")
    outputs.dir(inputsDir)
    doLast {
        inputsDir.file("1.txt").asFile.writeText("Changed content for existing file 1.")
        inputsDir.file("2.txt").asFile.writeText("Content for new file 2.")
    }
}
//Running the incremental task with an input file removed
tasks.register<Delete>("removeInput") {
    delete("src/main/resources/1.txt")
}
//Running the incremental task with an output file removed
tasks.register<Delete>("removeOutput") {
    delete(layout.buildDirectory.file("outputs/2.txt"))
}

//Using a read-only and configurable property
abstract class Greeting : DefaultTask() {
    @get:Input
    abstract val greeting: Property<String>

    @Internal
    val message: Provider<String> = greeting.map { it + " from Gradle" }

    @TaskAction
    fun printMessage() {
        logger.quiet(message.get())
    }
}

tasks.register<Greeting>("greetingHi") {
    greeting.set("Hi1")
    greeting = "Hi2"
}

//Connecting properties together
// A project extension
interface MessageExtension {
    // A configurable greeting
    abstract val greeting: Property<String>
}

// Create the project extension
val messages = project.extensions.create<MessageExtension>("messages")

// Create the greeting task
tasks.register<Greeting>("greetinghi2") {
    // Attach the greeting from the project extension
    // Note that the values of the project extension have not been configured yet
    greeting = messages.greeting
}

messages.apply {
    // Configure the greeting on the extension
    // Note that there is no need to reconfigure the task's `greeting` property. This is automatically updated as the extension property changes
    greeting = "Hello"
}

//Using file and directory property
// A task that generates a source file and writes the result to an output directory
abstract class GenerateSource : DefaultTask() {
    // The configuration file to use to generate the source file
    @get:InputFile
    abstract val configFile: RegularFileProperty

    // The directory to write source files to
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun compile() {
        val inFile = configFile.get().asFile
        logger.quiet("configuration file = $inFile")
        val dir = outputDir.get().asFile
        logger.quiet("output dir = $dir")
        val className = inFile.readText().trim()
        val srcFile = File(dir, "${className}.java")
        srcFile.writeText("public class ${className} { }")
    }
}

// Create the source generation task
tasks.register<GenerateSource>("generate") {
    // Configure the locations, relative to the project and build directories
    configFile = layout.projectDirectory.file("src/file1.txt")
    outputDir = layout.buildDirectory.dir("generated-source")
}

// Change the build directory
// Don't need to reconfigure the task properties. These are automatically updated as the build directory changes
layout.buildDirectory = layout.projectDirectory.dir("output")

//List property
abstract class Producer : DefaultTask() {
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun produce() {
        val message = "Hello, World!"
        val output = outputFile.get().asFile
        output.writeText( message)
        logger.quiet("Wrote '${message}' to ${output}")
    }
}

abstract class Consumer : DefaultTask() {
    @get:InputFiles
    abstract val inputFiles: ListProperty<RegularFile>

    @TaskAction
    fun consume() {
        inputFiles.get().forEach { inputFile ->
            val input = inputFile.asFile
            val message = input.readText()
            logger.quiet("Read '${message}' from ${input}")
        }
    }
}

val producerOne = tasks.register<Producer>("producerOne")
val producerTwo = tasks.register<Producer>("producerTwo")
tasks.register<Consumer>("consumer") {
    // Connect the producer task outputs to the consumer task input
    // Don't need to add task dependencies to the consumer task. These are automatically added
    inputFiles.add(producerOne.get().outputFile)
    inputFiles.add(producerTwo.get().outputFile)
}

// Set values for the producer tasks lazily
// Don't need to update the consumer.inputFiles property. This is automatically updated as producer.outputFile changes
producerOne { outputFile = layout.buildDirectory.file("one.txt") }
producerTwo { outputFile = layout.buildDirectory.file("two.txt") }

// Change the build directory.
// Don't need to update the task properties. These are automatically updated as the build directory changes
layout.buildDirectory = layout.projectDirectory.dir("output")

//Map property
abstract class Generator: DefaultTask() {
    @get:Input
    abstract val properties: MapProperty<String, Int>

    @TaskAction
    fun generate() {
        properties.get().forEach { entry ->
            logger.quiet("${entry.key} = ${entry.value}")
        }
    }
}

// Some values to be configured later
var b = 0
var c = 0

tasks.register<Generator>("generate2") {
    properties.put("a", 1)
    // Values have not been configured yet
    properties.put("b", providers.provider { b })
    properties.putAll(providers.provider { mapOf("c" to c, "d" to c + 1) })
}

// Configure the values. There is no need to reconfigure the task
b = 2
c = 3

//Property conventions
tasks.register("show") {
    val property = objects.property(String::class)

    // Set a convention
    property.convention("convention 1")

    println("value = " + property.get())

    // Can replace the convention
    property.convention("convention 2")
    println("value = " + property.get())

    property.set("explicit value")

    // Once a value is set, the convention is ignored
    property.convention("ignored convention")

    doLast {
        println("value = " + property.get())
    }
}