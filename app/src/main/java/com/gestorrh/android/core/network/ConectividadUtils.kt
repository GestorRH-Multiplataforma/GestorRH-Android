package com.gestorrh.android.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Comprueba si el dispositivo tiene conectividad validada a Internet.
 * Se consultan las capacidades del `activeNetwork` para evitar falsos positivos
 * cuando hay interfaz de red activa pero sin acceso real al exterior.
 */
fun Context.hayConexion(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val red = cm.activeNetwork ?: return false
    val capacidades = cm.getNetworkCapabilities(red) ?: return false
    return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
