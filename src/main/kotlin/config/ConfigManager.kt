package config

import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.RealmResults

class ConfigManager {
    private val config = RealmConfiguration.create(schema = setOf(Config::class))
    private val realm: Realm = Realm.open(config)

    fun saveConfig(config: Config) {
        realm.writeBlocking {
            copyToRealm(config)
        }
    }

    fun getConfig(): Config {
        val items: RealmResults<Config> = realm.query<Config>().find()
        if (items.isNotEmpty()) {
            return items[0]
        }
        return Config()
    }

    fun deleteConfig() {
        realm.writeBlocking {
            val writeTransactionItems = query<Config>().find()
            delete(writeTransactionItems.first())
        }
    }
}
