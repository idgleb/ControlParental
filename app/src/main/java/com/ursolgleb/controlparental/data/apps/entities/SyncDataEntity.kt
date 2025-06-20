package com.ursolgleb.controlparental.data.apps.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_data")
data class SyncDataEntity(
    @PrimaryKey val deviceId: String,
    var isPushHorarioPendiente: Boolean = false,
    var isPushAppsPendiente: Boolean = false,
)