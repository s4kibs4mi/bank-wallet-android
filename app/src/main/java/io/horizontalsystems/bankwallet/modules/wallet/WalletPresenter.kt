package io.horizontalsystems.bankwallet.modules.wallet

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class WalletPresenter(
        private var interactor: WalletModule.IInteractor,
        private val router: WalletModule.IRouter) : WalletModule.IViewDelegate, WalletModule.IInteractorDelegate {

    private var disposables: CompositeDisposable = CompositeDisposable()

    var view: WalletModule.IView? = null

    override fun onReceive(coin: String) {
        router.openReceiveDialog(coin)
    }

    override fun onPay(coin: String) {
        router.openSendDialog(coin)
    }

    override fun viewDidLoad() {
        view?.setTitle(R.string.wallet_title)
        interactor.loadWallets()
        updateView()
    }

    override fun refresh() {
        interactor.refresh()
    }

    override fun didUpdate() {
        updateView()
    }

    override fun didRefresh() {
        view?.didRefresh()
    }

    private fun updateView() {
        val wallets = interactor.wallets
        val rateObservables = wallets.map { interactor.rate(it.coin) }

        Maybe.merge(rateObservables)
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { rates ->

                    var totalBalance = 0.0
                    val viewItems = mutableListOf<WalletViewItem>()
                    val currency = interactor.baseCurrency
                    var allSynced = true

                    rates.forEach { rate ->
                        val wallet = wallets.first { it.coin == rate.coin }
                        val balance = wallet.adapter.balance

                        var rateExpired = false

                        rate?.let { mRate ->
                            val diff = DateHelper.getSecondsAgo(mRate.timestamp)
                            rateExpired = diff > 60 * 10

                            totalBalance += balance * mRate.value
                        }

                        viewItems.add(WalletViewItem(
                                coinValue = CoinValue(coin = wallet.coin, value = balance),
                                exchangeValue = rate?.let { CurrencyValue(currency = currency, value = it.value) },
                                currencyValue = rate?.let { CurrencyValue(currency = currency, value = balance * it.value) },
                                state = wallet.adapter.state,
                                rateExpired = rateExpired
                        ))

                        if (wallet.adapter.state is AdapterState.Syncing) {
                            allSynced = false
                        }

                        if (balance > 0) {
                            allSynced = allSynced && rate != null && !rateExpired
                        }
                    }

                    view?.updateBalanceColor(if (allSynced) R.color.yellow_crypto else R.color.yellow_crypto_40)
                    view?.show(totalBalance = CurrencyValue(currency = currency, value = totalBalance))
                    view?.show(wallets = viewItems)

                }.let {
                    disposables.add(it)
                }
    }

}