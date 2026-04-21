package com.gestorrh.android.core.archivos

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Utilidades de archivos para el flujo de adjuntos de ausencias.
 *
 * Centraliza la creación del `Uri` de destino para el intent de cámara vía
 * [FileProvider], la compresión de imágenes tomadas con cámara para evitar uploads
 * innecesariamente grandes, la lectura de metadatos del `ContentResolver` y la
 * apertura de archivos descargados con el visor del sistema.
 */
object GestorArchivosJustificante {

    private const val ANCHO_MAXIMO_IMAGEN_PX = 1920
    private const val CALIDAD_JPEG = 80
    private const val SUBDIRECTORIO_CAMARA = "justificantes"
    private const val SUBDIRECTORIO_DESCARGAS = "descargas"

    /**
     * Crea un archivo temporal vacío en el caché del dispositivo y devuelve el `Uri`
     * compartible vía [FileProvider] que puede pasarse al intent de cámara como destino.
     */
    fun crearUriCapturaCamara(contexto: Context): UriCaptura {
        val directorio = File(contexto.cacheDir, SUBDIRECTORIO_CAMARA).apply { mkdirs() }
        val archivo = File(directorio, "justificante_${System.currentTimeMillis()}.jpg")
        archivo.createNewFile()
        val authority = "${contexto.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(contexto, authority, archivo)
        return UriCaptura(uri = uri, rutaAbsoluta = archivo.absolutePath)
    }

    /**
     * Lee los bytes del archivo indicado por [uri], comprimiéndolos si es una imagen
     * (escalando el lado mayor a [ANCHO_MAXIMO_IMAGEN_PX] y recodificando a JPEG al
     * [CALIDAD_JPEG] por ciento) o devolviéndolos sin modificar si es un PDF.
     */
    suspend fun leerBytesParaSubida(
        contexto: Context,
        uri: Uri,
        esImagen: Boolean
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            if (esImagen) {
                comprimirImagen(contexto, uri)
            } else {
                contexto.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Obtiene el nombre mostrable del archivo detrás de [uri] consultando el
     * `ContentResolver`. Si no se puede resolver cae al último segmento del path.
     */
    suspend fun obtenerNombre(contexto: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            contexto.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val indice = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (indice >= 0 && cursor.moveToFirst()) cursor.getString(indice) else null
            } ?: uri.lastPathSegment
        } catch (e: Exception) {
            uri.lastPathSegment
        }
    }

    /**
     * Persiste [bytes] en un archivo temporal bajo el caché y devuelve un `Uri`
     * compartible vía [FileProvider], adecuado para abrir el contenido con el visor
     * del sistema mediante `Intent.ACTION_VIEW`.
     */
    suspend fun guardarEnCacheYObtenerUri(
        contexto: Context,
        bytes: ByteArray,
        nombreArchivo: String
    ): Uri = withContext(Dispatchers.IO) {
        val directorio = File(contexto.cacheDir, SUBDIRECTORIO_DESCARGAS).apply { mkdirs() }
        val archivo = File(directorio, nombreArchivo)
        FileOutputStream(archivo).use { it.write(bytes) }
        val authority = "${contexto.packageName}.fileprovider"
        FileProvider.getUriForFile(contexto, authority, archivo)
    }

    /**
     * Lanza un `Intent.ACTION_VIEW` con el [uri] y el tipo MIME inferido del nombre
     * para que el sistema muestre el justificante con la aplicación preferida del usuario.
     */
    fun abrirConVisorSistema(contexto: Context, uri: Uri, nombreArchivo: String) {
        val mime = mimeDesdeNombre(nombreArchivo)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        contexto.startActivity(intent)
    }

    /** Devuelve `true` si [nombre] termina con una extensión de imagen soportada. */
    fun esImagenPorNombre(nombre: String?): Boolean {
        val ext = nombre?.substringAfterLast('.', "")?.lowercase().orEmpty()
        return ext == "jpg" || ext == "jpeg" || ext == "png"
    }

    /** Devuelve el tipo MIME adecuado para el nombre dado o `application/octet-stream`. */
    fun mimeDesdeNombre(nombre: String?): String {
        val ext = nombre?.substringAfterLast('.', "")?.lowercase().orEmpty()
        return when (ext) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "application/octet-stream"
        }
    }

    private fun comprimirImagen(contexto: Context, uri: Uri): ByteArray? {
        val opcionesLectura = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contexto.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opcionesLectura)
        } ?: return null

        val anchoOriginal = opcionesLectura.outWidth
        val altoOriginal = opcionesLectura.outHeight
        if (anchoOriginal <= 0 || altoOriginal <= 0) return null

        val ladoMayor = maxOf(anchoOriginal, altoOriginal)
        val factorSubmuestreo = calcularInSampleSize(ladoMayor, ANCHO_MAXIMO_IMAGEN_PX)
        val opcionesDecodificado = BitmapFactory.Options().apply {
            inSampleSize = factorSubmuestreo
        }
        val bitmap = contexto.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opcionesDecodificado)
        } ?: return null

        val bitmapEscalado = escalarSiNecesario(bitmap, ANCHO_MAXIMO_IMAGEN_PX)
        val salida = ByteArrayOutputStream()
        bitmapEscalado.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida)
        if (bitmapEscalado !== bitmap) bitmapEscalado.recycle()
        bitmap.recycle()
        return salida.toByteArray()
    }

    private fun calcularInSampleSize(ladoMayor: Int, limite: Int): Int {
        var factor = 1
        var valor = ladoMayor
        while (valor / 2 >= limite) {
            valor /= 2
            factor *= 2
        }
        return factor
    }

    private fun escalarSiNecesario(bitmap: Bitmap, limite: Int): Bitmap {
        val ladoMayor = maxOf(bitmap.width, bitmap.height)
        if (ladoMayor <= limite) return bitmap
        val factor = limite.toFloat() / ladoMayor.toFloat()
        val nuevoAncho = (bitmap.width * factor).toInt().coerceAtLeast(1)
        val nuevoAlto = (bitmap.height * factor).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, nuevoAncho, nuevoAlto, true)
    }

    /**
     * Datos devueltos al crear el `Uri` de captura de cámara: el [uri] que recibe
     * el intent y la [rutaAbsoluta] del archivo físico (útil para leer los bytes
     * tras la captura sin pasar por el `ContentResolver`).
     */
    data class UriCaptura(val uri: Uri, val rutaAbsoluta: String)
}
