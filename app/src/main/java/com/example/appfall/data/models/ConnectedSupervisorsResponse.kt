package com.example.appfall.data.models

data class ConnectedSupervisorsResponse(
    val status: String,
    val connectedUsers: List<ConnectedSupervisor>
)