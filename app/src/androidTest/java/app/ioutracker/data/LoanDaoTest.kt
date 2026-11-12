package app.ioutracker.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
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
}
