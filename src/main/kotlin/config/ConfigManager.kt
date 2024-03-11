package config

import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.copyFromRealm
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.RealmResults

class ConfigManager {
    companion object {
        private var audioConfiguration = RealmConfiguration.create(schema = setOf(AudioConfig::class))
        private var openAiConfiguration = RealmConfiguration.create(schema = setOf(OpenAiConfig::class))
        private var workersAiConfiguration = RealmConfiguration.create(schema = setOf(WorkersAiConfig::class))
        private var audioRealm: Realm = Realm.open(audioConfiguration)
        private var openAiRealm: Realm = Realm.open(openAiConfiguration)
        private var workersAiRealm: Realm = Realm.open(workersAiConfiguration)

        fun saveConfig() {
            audioRealm.writeBlocking {
                copyToRealm(Config.config.audioConfig, UpdatePolicy.ALL)
            }
            openAiRealm.writeBlocking {
                copyToRealm(Config.config.openAiConfig, UpdatePolicy.ALL)
            }
            workersAiRealm.writeBlocking {
                copyToRealm(Config.config.workersAiConfig, UpdatePolicy.ALL)
            }
        }

        fun getConfig(): Config {
            val config = Config()
            val audioConfigItems: RealmResults<AudioConfig> = audioRealm.query<AudioConfig>().find()
            if (audioConfigItems.isNotEmpty()) {
                config.audioConfig = audioConfigItems[0].copyFromRealm()
            }
            val openAiConfigItems: RealmResults<OpenAiConfig> = openAiRealm.query<OpenAiConfig>().find()
            if (openAiConfigItems.isNotEmpty()) {
                config.openAiConfig = openAiConfigItems[0].copyFromRealm()
            }
            val workersAiConfigItems: RealmResults<WorkersAiConfig> = workersAiRealm.query<WorkersAiConfig>().find()
            if (workersAiConfigItems.isNotEmpty()) {
                config.workersAiConfig = workersAiConfigItems[0].copyFromRealm()
            }
            return config
        }

        fun deleteConfig() {
            audioRealm.writeBlocking {
                val writeTransactionItems = query<AudioConfig>().find()
                delete(writeTransactionItems.first())
            }
            openAiRealm.writeBlocking {
                val writeTransactionItems = query<OpenAiConfig>().find()
                delete(writeTransactionItems.first())
            }
            workersAiRealm.writeBlocking {
                val writeTransactionItems = query<WorkersAiConfig>().find()
                delete(writeTransactionItems.first())
            }
        }
    }
}
