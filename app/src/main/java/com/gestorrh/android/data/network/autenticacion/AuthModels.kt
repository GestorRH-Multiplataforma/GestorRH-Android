package com.gestorrh.android.data.network.autenticacion

import com.google.gson.annotations.SerializedName

data class PeticionLoginDTO(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RespuestaLoginDTO(
    @SerializedName("token") val token: String,
    @SerializedName("rol") val rol: String,
    @SerializedName("id") val id: Long,
    @SerializedName("nombre") val nombre: String
)
