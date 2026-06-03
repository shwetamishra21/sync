package com.jsac.sync.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jsac.sync.data.local.db.entity.FormEntity
import com.jsac.sync.data.local.db.entity.FormFieldEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FormDao {

    @Query("SELECT * FROM forms ORDER BY created_at DESC")
    fun getAllForms(): Flow<List<FormEntity>>

    @Query("SELECT * FROM forms WHERE id = :formId")
    fun getFormById(formId: String): Flow<FormEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForm(form: FormEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForms(forms: List<FormEntity>)

    @Query("SELECT * FROM form_fields WHERE form_id = :formId ORDER BY field_order ASC")
    fun getFormFields(formId: String): Flow<List<FormFieldEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertField(field: FormFieldEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFields(fields: List<FormFieldEntity>)

    @Delete
    suspend fun deleteForm(form: FormEntity)

    @Query("DELETE FROM forms")
    suspend fun deleteAllForms()

    @Query("DELETE FROM form_fields WHERE form_id = :formId")
    suspend fun deleteFormFields(formId: String)

    @Query("SELECT COUNT(*) FROM forms WHERE id = :formId")
    suspend fun isFormCached(formId: String): Int

    @Query("SELECT COUNT(*) FROM forms")
    fun getFormCount(): Flow<Int>
}