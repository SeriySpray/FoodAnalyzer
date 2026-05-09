package com.example.recipefood.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dateCreated: Long = System.currentTimeMillis(),
    val isExpanded: Boolean = true // для збереження стану згортання/розгортання
) {
    companion object {
        const val DEFAULT_FOLDER_ID = 1L
        const val DEFAULT_FOLDER_NAME = "Ваші рецепти"
    }
}
