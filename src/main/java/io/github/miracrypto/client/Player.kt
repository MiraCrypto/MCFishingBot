package io.github.miracrypto.client

data class Player(var health: Float = 0f,
                  var food: Int = 0,
                  var saturation: Float = 0f,
                  var experience: Float = 0f,
                  var level: Int = 0,
                  var totalExperience: Int = 0,
                  var entityId: Int = 0 )
