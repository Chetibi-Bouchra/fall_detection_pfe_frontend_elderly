package com.example.appfall.models

data class ConnectedSupervisorsResponse(
    val status: String,
    val connectedSupervisors: List<ConnectedSupervisor>
)