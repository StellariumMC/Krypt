plugins {
    id("dev.deftu.gradle.multiversion-root")
}

preprocess {
    "1.21.10-fabric"(1_21_10, "srg") {
        "1.21.8-fabric"(1_21_08, "srg") {
            "1.21.5-fabric"(1_21_05, "srg")
        }
    }
}

subprojects {
    afterEvaluate {
        extensions.findByName("loom")?.let {
            (it as net.fabricmc.loom.api.LoomGradleExtensionAPI).apply {
                runConfigs.configureEach {
                    runDir("../../run")
                }
            }
        }
    }
}

// Line ending preservation for cross-platform compatibility
val storedLineEndings = mutableMapOf<String, String>()
var preprocessRan = false

subprojects {
    tasks.withType<com.replaymod.gradle.preprocess.PreprocessTask> {
        val taskEndings = mutableMapOf<String, String>()
        val exts = listOf("java", "kt", "kts", "json")

        doFirst {
            if (storedLineEndings.isEmpty()) {
                rootProject.file("src/main").takeIf { it.exists() }?.walkTopDown()
                    ?.filter { it.isFile && exts.contains(it.extension) }
                    ?.forEach {
                        storedLineEndings[it.absolutePath] = it.readText().let { c ->
                            when {
                                c.contains("\r\n") -> "\r\n"; c.contains("\n") -> "\n"; else -> System.lineSeparator()
                            }
                        }
                    }
            }
            preprocessRan = true
            outputs.files.forEach {
                it.takeIf { it.exists() }?.walkTopDown()?.filter { it.isFile && exts.contains(it.extension) }
                    ?.forEach {
                        taskEndings[it.absolutePath] = it.readText().let { c ->
                            when {
                                c.contains("\r\n") -> "\r\n"; c.contains("\n") -> "\n"; else -> System.lineSeparator()
                            }
                        }
                    }
            }
        }

        doLast {
            outputs.files.forEach {
                it.takeIf { it.exists() }?.walkTopDown()?.filter { it.isFile && exts.contains(it.extension) }
                    ?.forEach { f ->
                        taskEndings[f.absolutePath]?.let { orig ->
                            f.readText().let { c ->
                                when {
                                    orig == "\r\n" && !c.contains("\r\n") && c.contains("\n") -> f.writeText(
                                        c.replace(
                                            "\n",
                                            "\r\n"
                                        )
                                    )

                                    orig == "\n" && c.contains("\r\n") -> f.writeText(c.replace("\r\n", "\n"))
                                }
                            }
                        }
                    }
            }
        }
    }
}

gradle.taskGraph.whenReady {
    gradle.taskGraph.allTasks.lastOrNull()?.doLast {
        if (preprocessRan && storedLineEndings.isNotEmpty()) {
            var restored = 0
            rootProject.file("src/main").takeIf { it.exists() }?.walkTopDown()
                ?.filter { it.isFile && listOf("java", "kt", "kts", "json").contains(it.extension) }
                ?.forEach { f ->
                    storedLineEndings[f.absolutePath]?.let { orig ->
                        f.readText().let { c ->
                            when {
                                orig == "\r\n" && !c.contains("\r\n") && c.contains("\n") -> {
                                    f.writeText(c.replace("\n", "\r\n")); restored++
                                }

                                orig == "\n" && c.contains("\r\n") -> {
                                    f.writeText(c.replace("\r\n", "\n")); restored++
                                }
                            }
                        }
                    }
                }
            if (restored > 0) println("[Line Endings] Restored $restored files")
            storedLineEndings.clear()
            preprocessRan = false
        }
    }
}