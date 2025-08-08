package android.databinding.tool.writer

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import java.io.OutputStreamWriter
import java.io.Writer
import java.io.IOException
import android.databinding.tool.util.L


class KspJavaFileWriter(
    private val codeGenerator: CodeGenerator
) : JavaFileWriter() {

    override fun writeToFile(canonicalName: String, contents: String) {
        var writer: Writer? = null
        try {
            L.d("writing file %s", canonicalName)
            val pkgEnd = canonicalName.lastIndexOf('.')
            val packageName = canonicalName.substring(0, pkgEnd)
            val fileName = canonicalName.substring(pkgEnd + 1)

            val outputStream = codeGenerator.createNewFile(
                Dependencies(false),
                packageName,
                fileName
            )
            writer = OutputStreamWriter(outputStream, Charsets.UTF_8)
            writer.write(contents)
        } catch (e: IOException) {
            L.e(e, "Could not write to %s", canonicalName)
        } finally {
            writer?.let {
                try {
                    it.close()
                } catch (_: IOException) {

                }
            }
        }
    }

    override fun deleteFile(canonicalName: String) {
        throw UnsupportedOperationException("cannot delete file in ksp processor")
    }
}
