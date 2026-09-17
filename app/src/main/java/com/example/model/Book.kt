package com.example.model

data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val coverUrl: String = "",
    val pdfUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "author" to author,
            "coverUrl" to coverUrl,
            "pdfUrl" to pdfUrl,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Book {
            return Book(
                id = id,
                title = map["title"] as? String ?: "",
                author = map["author"] as? String ?: "",
                coverUrl = map["coverUrl"] as? String ?: "",
                pdfUrl = map["pdfUrl"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
