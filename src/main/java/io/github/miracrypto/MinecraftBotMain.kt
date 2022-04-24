package io.github.miracrypto

import com.charleskorn.kaml.Yaml
import io.github.miracrypto.bot.Main
import io.github.miracrypto.config.BotConfig
import java.io.FileInputStream

fun main(args: Array<String>) {
    val fileName = args.firstOrNull() ?: "config.yml"

    val botConfig = loadConfig(fileName)

    Main(botConfig)
}

private fun loadConfig(fileName: String): BotConfig =
    FileInputStream(fileName).use {
        Yaml.default.decodeFromStream(BotConfig.serializer(), it)
    }
