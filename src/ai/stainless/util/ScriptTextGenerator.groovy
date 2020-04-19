package ai.stainless.util

// read all the files in the previous directory and write them to a single file
// Get a writer to your new file
new File('../../release_job_script.groovy').withWriter { w ->
    // For each input file path
    new File('..').listFiles({ !it.isDirectory() } as FileFilter).each { f ->
        println f
        // Get a reader for the input file
        f.withReader { r ->
            // And write data from the input into the output
            w << r << '\n'
        }
    }
}

