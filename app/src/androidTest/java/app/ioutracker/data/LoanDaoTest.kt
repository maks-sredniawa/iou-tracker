package app.ioutracker.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ioutracker.ui.common.ParsedAmount
import app.ioutracker.ui.common.parseAmount
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Instrumented because Room generates real SQLite code, and a plain unit test has no SQLite. */
@RunWith(AndroidJUnit4::class)
class LoanDaoTest {
    private var database: AppDatabase? = null

    private fun open(systemCurrency: String? = "PLN"): AppDatabase =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .addCallback(CurrencyRowOnOpen { systemCurrency })
            .allowMainThreadQueries()
            .build()
            .also { database = it }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val fileName = "loan-dao-test.db"

    private fun openFile(
        systemCurrency: String?,
        destructive: Boolean = false,
    ): AppDatabase =
        Room
            .databaseBuilder(context, AppDatabase::class.java, fileName)
            .apply { if (destructive) fallbackToDestructiveMigration(dropAllTables = true) }
            .addCallback(CurrencyRowOnOpen { systemCurrency })
            .allowMainThreadQueries()
            .build()
            .also { database = it }

    @After
    fun close() {
        database?.close()
        context.deleteDatabase(fileName)
    }

    private val ola = CounterpartyEntity(name = "Ola", kind = CounterpartyKind.PERSON)
    private val savings = CounterpartyEntity(name = "joint savings", kind = CounterpartyKind.POT)
    private val day = LocalDate.of(2026, 11, 12)

    @Test
    fun theCurrencyRowIsWrittenWhenTheDatabaseIsCreated() =
        runTest {
            assertEquals("PLN", open("PLN").currencyDao().observeCode().first())
        }

    @Test
    fun aRegionWithNoCurrencyLeavesTheRowEmpty() =
        runTest {
            val db = open(null)
            assertNull(db.currencyDao().observeCode().first())
            db.query("SELECT COUNT(*) FROM currency", emptyArray()).use {
                it.moveToFirst()
                assertEquals(1, it.getInt(0))
            }
        }

    @Test
    fun aBalanceIsOutstandingLentMinusOutstandingBorrowed() =
        runTest {
            val dao = open().loanDao()
            dao.insert(ola)
            val lent = LoanEntity(counterpartyId = ola.id, direction = Direction.LENT, amount = 10_000, day = day)
            dao.insert(lent)
            dao.insert(RepaymentEntity(loanId = lent.id, amount = 3_000, day = day))
            dao.insert(LoanEntity(counterpartyId = ola.id, direction = Direction.BORROWED, amount = 2_500, day = day))

            assertEquals(listOf(4_500L), dao.observeBalances().first().map { it.balance })
        }

    @Test
    fun anOverpaidLoanCountsAsZeroOutstandingNeverNegative() =
        runTest {
            val dao = open().loanDao()
            dao.insert(ola)
            val lent = LoanEntity(counterpartyId = ola.id, direction = Direction.LENT, amount = 1_000, day = day)
            dao.insert(lent)
            dao.insert(RepaymentEntity(loanId = lent.id, amount = 1_500, day = day))

            assertEquals(
                0L,
                dao
                    .observeBalances()
                    .first()
                    .single()
                    .balance,
            )
            assertEquals(
                0L,
                dao
                    .observeLoansOf(ola.id)
                    .first()
                    .single()
                    .outstanding,
            )
        }

    @Test
    fun aCounterpartyWithNoLoansIsListedEvenAndNamesSortIgnoringCase() =
        runTest {
            val dao = open().loanDao()
            dao.insert(ola)
            dao.insert(savings)
            dao.insert(
                LoanEntity(counterpartyId = savings.id, direction = Direction.BORROWED, amount = 30_000, day = day),
            )

            val balances = dao.observeBalances().first()
            assertEquals(listOf("joint savings", "Ola"), balances.map { it.name })
            assertEquals(listOf(-30_000L, 0L), balances.map { it.balance })
            assertEquals(CounterpartyKind.POT, balances.first().kind)
        }

    @Test
    fun loansComeBackNewestDayFirstWithTheirDatesIntact() =
        runTest {
            val dao = open().loanDao()
            dao.insert(ola)
            val older =
                LoanEntity(counterpartyId = ola.id, direction = Direction.LENT, amount = 100, day = day.minusDays(3))
            val newer =
                LoanEntity(
                    counterpartyId = ola.id,
                    direction = Direction.BORROWED,
                    amount = 200,
                    day = day,
                    dueDate = day.plusMonths(1),
                )
            dao.insert(older)
            dao.insert(newer)

            val loans = dao.observeLoansOf(ola.id).first()
            assertEquals(listOf(newer.id, older.id), loans.map { it.id })
            assertEquals(day.plusMonths(1), loans.first().dueDate)
            assertNull(loans.last().dueDate)
        }

    @Test
    fun theFlowReEmitsWhenALoanIsAdded() =
        runTest {
            val dao = open().loanDao()
            assertEquals(0, dao.observeLoanCount().first())
            dao.insert(ola)
            dao.insert(LoanEntity(counterpartyId = ola.id, direction = Direction.LENT, amount = 100, day = day))
            assertEquals(1, dao.observeLoanCount().first())
            assertEquals(1, dao.loanCount())
        }

    @Test
    fun enumsAndDaysAreStoredAsText() =
        runTest {
            val db = open()
            db.loanDao().insert(savings)
            db.loanDao().insert(
                LoanEntity(counterpartyId = savings.id, direction = Direction.BORROWED, amount = 1, day = day),
            )
            db
                .query(
                    "SELECT c.kind, l.direction, l.day FROM loans l JOIN counterparties c ON c.id = l.counterpartyId",
                    emptyArray(),
                ).use {
                    it.moveToFirst()
                    assertEquals(
                        listOf("POT", "BORROWED", "2026-11-12"),
                        listOf(it.getString(0), it.getString(1), it.getString(2)),
                    )
                }
        }

    @Test
    fun deletingALoanTakesItsRepaymentsAndACounterpartyWithLoansCannotBeDeleted() =
        runTest {
            val dao = open().loanDao()
            dao.insert(ola)
            val lent = LoanEntity(counterpartyId = ola.id, direction = Direction.LENT, amount = 1_000, day = day)
            dao.insert(lent)
            dao.insert(RepaymentEntity(loanId = lent.id, amount = 400, day = day))

            assertThrows(SQLiteConstraintException::class.java) { runBlocking { dao.delete(ola) } }

            dao.delete(lent)
            database!!.query("SELECT COUNT(*) FROM repayments", emptyArray()).use {
                it.moveToFirst()
                assertEquals(0, it.getInt(0))
            }
            dao.delete(ola)
            assertEquals(emptyList<CounterpartyBalance>(), dao.observeBalances().first())
        }

    @Test
    fun aLostCurrencyRowIsWrittenBackOnTheNextOpen() =
        runTest {
            context.deleteDatabase(fileName)
            openFile("PLN").openHelper.writableDatabase.execSQL("DELETE FROM currency")
            database!!.close()

            assertEquals("EUR", openFile("EUR").currencyDao().observeCode().first())
        }

    @Test
    fun theDebugDestructiveRecreateWritesTheCurrencyRow() =
        runTest {
            context.deleteDatabase(fileName)
            openFile("PLN").loanDao().insert(ola)
            database!!.close()
            SQLiteDatabase
                .openDatabase(
                    context.getDatabasePath(fileName).path,
                    null,
                    SQLiteDatabase.OPEN_READWRITE,
                ).use {
                    it.version = APP_SCHEMA_VERSION + 1
                }

            val db = openFile("PLN", destructive = true)
            assertEquals("PLN", db.currencyDao().observeCode().first())
            assertEquals(emptyList<CounterpartyBalance>(), db.loanDao().observeBalances().first())
        }

    private fun longs(sql: String): List<Long> =
        database!!.query(sql, emptyArray()).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getLong(0)) }
        }

    private fun storedRows() =
        longs("SELECT amount FROM loans ORDER BY rowid") to longs("SELECT amount FROM repayments ORDER BY rowid")

    private suspend fun LoanDao.lend(
        counterparty: CounterpartyEntity,
        amount: Long,
        on: LocalDate = day,
        repaid: List<Long> = emptyList(),
    ): LoanEntity {
        val loan = LoanEntity(counterpartyId = counterparty.id, direction = Direction.LENT, amount = amount, day = on)
        insert(loan)
        repaid.forEach { insert(RepaymentEntity(loanId = loan.id, amount = it, day = on)) }
        return loan
    }

    @Test
    fun storedValuesComeInTheCounterpartyScreensOrderWithEachLoansRepaymentsAfterIt() =
        runTest {
            val dao = open().loanDao()
            dao.insert(ola)
            dao.insert(savings)
            val older = dao.lend(ola, 1_000, on = day.minusDays(2), repaid = listOf(100, 200))
            val sameDayFirst = dao.lend(savings, 3_000)
            val sameDaySecond = dao.lend(ola, 5_000, repaid = listOf(500))

            assertEquals(
                listOf(
                    StoredValue(sameDaySecond.id, 5_000),
                    StoredValue(sameDaySecond.id, 500),
                    StoredValue(sameDayFirst.id, 3_000),
                    StoredValue(older.id, 1_000),
                    StoredValue(older.id, 100),
                    StoredValue(older.id, 200),
                ),
                dao.storedValues(),
            )
            assertEquals(5_000L, dao.newestAmount())
        }

    @Test
    fun noLoansHasNoNewestAmount() =
        runTest {
            assertNull(open().loanDao().newestAmount())
        }

    @Test
    fun blockingLoansNameALoanBlockedOnlyByARepaymentSortedByNameThenNewestFirst() =
        runTest {
            val dao = open().loanDao()
            dao.insert(ola)
            dao.insert(savings)
            val olaOlder = dao.lend(ola, 1_250, on = day.minusDays(1))
            dao.lend(ola, 2_000)
            val olaNewer = dao.lend(ola, 3_050)
            val savingsByRepayment = dao.lend(savings, 10_000, repaid = listOf(2_550))

            val blocking = dao.observeBlockingLoans(100).first()
            assertEquals(listOf(savingsByRepayment.id, olaNewer.id, olaOlder.id), blocking.map { it.loan.id })
            assertEquals(listOf("joint savings", "Ola", "Ola"), blocking.map { it.counterpartyName })
            assertEquals(CounterpartyKind.POT, blocking.first().counterpartyKind)
            assertEquals(savings.id, blocking.first().counterpartyId)
            assertEquals(7_450L, blocking.first().loan.outstanding)
            assertEquals(emptyList<BlockingLoan>(), dao.observeBlockingLoans(1).first())
        }

    @Test
    fun anAllowedChangeKeepsEveryOutstandingAndSettledStateInBothDirections() =
        runTest {
            val db = open("PLN")
            val dao = db.loanDao()
            dao.insert(ola)
            dao.lend(ola, 10_000, repaid = listOf(3_000))
            dao.lend(ola, 2_500, on = day.minusDays(1), repaid = listOf(2_500))
            val before = dao.observeLoansOf(ola.id).first()

            val up = planRescale(2, 3, dao.storedValues())
            assertTrue(dao.changeCurrency("PLN", "KWD", up))
            assertEquals("KWD", db.currencyDao().observeCode().first())
            assertEquals(listOf(100_000L, 25_000L) to listOf(30_000L, 25_000L), storedRows())
            val multiplied = dao.observeLoansOf(ola.id).first()
            assertEquals(before.map { it.outstanding * 10 }, multiplied.map { it.outstanding })
            assertEquals(before.map { it.outstanding == 0L }, multiplied.map { it.outstanding == 0L })

            val down = planRescale(3, 2, dao.storedValues())
            assertTrue(dao.changeCurrency("KWD", "PLN", down))
            assertEquals("PLN", db.currencyDao().observeCode().first())
            assertEquals(before, dao.observeLoansOf(ola.id).first())
        }

    @Test
    fun aLossyChangeIsRefusedAndAStaleDivideWritesNothing() =
        runTest {
            val db = open("PLN")
            val dao = db.loanDao()
            dao.insert(ola)
            val blocked = dao.lend(ola, 1_250)
            dao.lend(ola, 10_000, repaid = listOf(5_000))
            val rows = storedRows()

            assertEquals(RescalePlan.Refused(listOf(blocked.id)), planRescale(2, 0, dao.storedValues()))
            assertFalse(dao.changeCurrency("PLN", "JPY", RescalePlan.Divide(100)))
            assertEquals(rows, storedRows())
            assertEquals("PLN", db.currencyDao().observeCode().first())
        }

    @Test
    fun aSecondConfirmOfTheSameMultiplyWritesNothing() =
        runTest {
            val db = open("PLN")
            val dao = db.loanDao()
            dao.insert(ola)
            dao.lend(ola, 10_050, repaid = listOf(50))
            val plan = planRescale(2, 3, dao.storedValues())

            assertTrue(dao.changeCurrency("PLN", "KWD", plan))
            assertFalse(dao.changeCurrency("PLN", "KWD", plan))
            assertEquals(listOf(100_500L) to listOf(500L), storedRows())
            assertEquals("KWD", db.currencyDao().observeCode().first())
        }

    @Test
    fun theFirstChoiceFromNoCurrencyIsARelabel() =
        runTest {
            val db = open(null)
            assertFalse(db.loanDao().changeCurrency("EUR", "PLN", RescalePlan.Relabel))
            assertNull(db.currencyDao().observeCode().first())
            assertTrue(db.loanDao().changeCurrency(null, "PLN", RescalePlan.Relabel))
            assertEquals("PLN", db.currencyDao().observeCode().first())
        }

    @Test
    fun theLargestParseableAmountMultipliedByAThousandDoesNotWrap() =
        runTest {
            val db = open("JPY")
            val dao = db.loanDao()
            val largest = parseAmount("999 999 999 999", 0) as ParsedAmount.Valid
            assertEquals(ParsedAmount.TooLarge, parseAmount("1 000 000 000 000", 0))
            dao.insert(ola)
            dao.lend(ola, largest.minorUnits)

            assertTrue(dao.changeCurrency("JPY", "KWD", planRescale(0, 3, dao.storedValues())))
            assertEquals(listOf(999_999_999_999_000L), longs("SELECT amount FROM loans"))
            db.query("SELECT typeof(amount) FROM loans", emptyArray()).use {
                it.moveToFirst()
                assertEquals("integer", it.getString(0))
            }
        }
}
