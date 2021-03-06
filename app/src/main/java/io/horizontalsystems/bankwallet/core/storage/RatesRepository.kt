package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import java.util.concurrent.Executors

class RatesRepository(private val appDatabase: AppDatabase) : IRateStorage {

    private val executor = Executors.newSingleThreadExecutor()

    override fun latestRateObservable(coinCode: CoinCode, currencyCode: String): Flowable<Rate> {
        return appDatabase.ratesDao().getLatestRate(coinCode, currencyCode).distinctUntilChanged()
    }

    override fun rateMaybe(coinCode: CoinCode, currencyCode: String, timestamp: Long): Maybe<Rate> {
        return appDatabase.ratesDao().getRate(coinCode, currencyCode, timestamp)
    }

    override fun save(rate: Rate) {
        executor.execute {
            appDatabase.ratesDao().insert(rate)
        }
    }

    override fun saveLatest(rate: Rate) {
        executor.execute {
            appDatabase.ratesDao().deleteLatest(rate.coinCode, rate.currencyCode)
            appDatabase.ratesDao().insert(rate)
        }
    }

    override fun deleteAll() {
        executor.execute {
            appDatabase.ratesDao().deleteAll()
        }
    }

    override fun zeroRatesObservable(currencyCode: String): Single<List<Rate>> {
        return appDatabase.ratesDao().getZeroRates(currencyCode)
    }
}
