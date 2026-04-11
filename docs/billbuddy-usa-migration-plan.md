# BillBuddy USA Migration Plan

## Current Architecture Assessment

The existing app already has most of the infrastructure needed for `BillBuddy USA`:

- `Room + Repository + Hilt` are in place via [VyapaarDatabase.kt](E:/Billbuddy/app/src/main/java/com/vyapaarhisaab/app/data/local/VyapaarDatabase.kt), [TransactionRepository.kt](E:/Billbuddy/app/src/main/java/com/vyapaarhisaab/app/data/repository/TransactionRepository.kt), and DI modules under `app/src/main/java/com/vyapaarhisaab/app/di`.
- `WorkManager` is already wired and `PaymentReminderWorker` exists in [PaymentReminderWorker.kt](E:/Billbuddy/app/src/main/java/com/vyapaarhisaab/app/worker/PaymentReminderWorker.kt).
- `AdMob` is initialized and reusable in [AdManager.kt](E:/Billbuddy/app/src/main/java/com/vyapaarhisaab/app/ui/common/util/AdManager.kt).
- `Firebase Analytics/Crashlytics/Perf` dependencies are already present in [app/build.gradle.kts](E:/Billbuddy/app/build.gradle.kts).
- Navigation, edge-to-edge handling, and bottom navigation already exist in [MainActivity.kt](E:/Billbuddy/app/src/main/java/com/vyapaarhisaab/app/MainActivity.kt) and [nav_graph.xml](E:/Billbuddy/app/src/main/res/navigation/nav_graph.xml).

## Main Gaps

The current product is a customer-ledger app, not a bill tracker:

- `CustomerEntity` and customer flows dominate the app.
- `TransactionEntity` is optimized for lending/settlement, not recurring bills.
- Reminder logic is too narrow: it shows a single immediate notification and does not model `7/3/1 day` offsets.
- Export is CSV-based, not PDF, and is currently unlocked via interstitial instead of rewarded ad.
- UI structure is centered around `Home / Customers / Summary`, while the new product needs `Bills / Dashboard / History / Settings / Add/Edit`.

## Design Direction From Figma Context

The screens in `C:\Users\Saurav Singh\Desktop\billbuddy` define the right product shell:

- `stitch (2)` is the new Home/Bill List direction with total outstanding, upcoming bills, bottom navigation, and a single FAB.
- `stitch (4)` is the Dashboard direction with monthly total, paid, remaining, and upcoming bills.
- `stitch (1)` is the History direction with trend chart and past bill cards.
- `stitch (3)` and `stitch (5)` define Settings and rewarded-PDF monetization UX.
- `stitch` defines the Add/Edit Bill screen and confirms the form fields and hierarchy.

The visual system is clearly premium-lightweight, but implementation should stay XML-first to preserve delivery speed and reuse the existing fragment stack.

## Migration Strategy

### 1. Reframe the domain instead of rewriting the app

Do not replace the architecture. Replace the business model.

- Keep:
  - Hilt
  - Room
  - Repositories
  - Fragment + Navigation architecture
  - WorkManager
  - Firebase
  - AdMob
- Remove or phase out:
  - customer-ledger specific screens
  - Hindi/language toggle for MVP US audience
  - PIN lock if it slows delivery
  - import/export flows tied to transaction CSV

### 2. Treat `transactions` as legacy and introduce a new bill table

Lowest-risk path:

- Add `BillEntity` instead of mutating `TransactionEntity` aggressively.
- Keep `TransactionEntity` temporarily so the app still compiles while the UI is migrated.
- Switch navigation and bottom tabs to Bills-first screens.
- Once all screens stop depending on customers/transactions, remove old modules in a cleanup pass.

This avoids breaking every query, adapter, and fragment at the same time.

### 3. Deliver in five phases with vertical slices

1. Data layer and bill CRUD
2. Notification scheduling and reboot-safe reminders
3. Dashboard and history
4. Rewarded PDF export and ad placement rules
5. Firebase analytics, polish, and cleanup

## Updated Code Structure

Recommended target structure:

```text
app/src/main/java/com/vyapaarhisaab/app/
  data/
    local/
      dao/
        BillDao.kt
        BillHistoryDao.kt
      entity/
        BillEntity.kt
      mapper/
        BillMappers.kt
      VyapaarDatabase.kt
    repository/
      BillRepository.kt
      ReminderRepository.kt
      ExportRepository.kt
    export/
      BillPdfExporter.kt
    analytics/
      AnalyticsTracker.kt
  domain/
    model/
      Bill.kt
      BillCategory.kt
      RepeatType.kt
      ReminderOffset.kt
      MonthlySummary.kt
      HistoryPoint.kt
  ui/
    bills/
      BillListFragment.kt
      BillListViewModel.kt
      BillListAdapter.kt
    billform/
      AddEditBillFragment.kt
      AddEditBillViewModel.kt
    dashboard/
      DashboardFragment.kt
      DashboardViewModel.kt
    history/
      HistoryFragment.kt
      HistoryViewModel.kt
    settings/
      SettingsFragment.kt
      SettingsViewModel.kt
    common/
      ads/
        AdPlacementController.kt
      util/
        UsCurrencyFormatter.kt
        UsDateFormatter.kt
  worker/
    BillReminderWorker.kt
    ReminderRescheduleWorker.kt
    BootReceiver.kt
```

## Mapping From Existing Modules

### Reuse directly

- `TransactionRepository` patterns -> base for `BillRepository`
- `TransactionViewModel` save/update/delete flow -> base for `AddEditBillViewModel`
- `SummaryViewModel` combine-pattern -> base for dashboard aggregates
- `ExportImportManager` -> replace CSV writer with PDF generator shell
- `AdManager` -> keep as ad entry point, but change trigger policy

### Rename/refactor

- `ui.transaction` -> `ui.billform`
- `ui.summary` -> `ui.dashboard`
- `ui.home` -> `ui.bills`
- `worker.PaymentReminderWorker` -> `worker.BillReminderWorker`

### Retire from MVP

- `ui.customer.*`
- customer DAO/repository queries
- import CSV
- language switching
- lending-specific balance semantics

## Database Migration Plan

Add a dedicated entity:

```kotlin
@Entity(
    tableName = "bills",
    indices = [
        Index("dueDate"),
        Index("isPaid"),
        Index("category")
    ]
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amountCents: Long,
    val dueDate: Long,
    val category: String,
    val isPaid: Boolean,
    val repeatType: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastPaidAt: Long? = null
)
```

DAO aligned to the MVP:

```kotlin
@Dao
interface BillDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillEntity): Long

    @Update
    suspend fun updateBill(bill: BillEntity)

    @Query("DELETE FROM bills WHERE id = :billId")
    suspend fun deleteBill(billId: Long)

    @Query("SELECT * FROM bills ORDER BY dueDate ASC")
    fun getAllBills(): Flow<List<BillEntity>>

    @Query("""
        SELECT * FROM bills
        WHERE isPaid = 0
          AND dueDate BETWEEN :from AND :to
        ORDER BY dueDate ASC
    """)
    fun getUpcomingBills(from: Long, to: Long): Flow<List<BillEntity>>

    @Query("UPDATE bills SET isPaid = :paid, lastPaidAt = :paidAt, updatedAt = :updatedAt WHERE id = :billId")
    suspend fun markAsPaid(
        billId: Long,
        paid: Boolean,
        paidAt: Long?,
        updatedAt: Long = System.currentTimeMillis()
    )
}
```

### Room versioning

Current database version is `1` and uses `fallbackToDestructiveMigration()` in [VyapaarDatabase.kt](E:/Billbuddy/app/src/main/java/com/vyapaarhisaab/app/data/local/VyapaarDatabase.kt), which is not acceptable for a real shipped migration. Change this before release:

- increment DB version to `2`
- add a real migration for the `bills` table
- keep `transactions`/`customers` intact until the old UI is removed

Example:

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS bills (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                amountCents INTEGER NOT NULL,
                dueDate INTEGER NOT NULL,
                category TEXT NOT NULL,
                isPaid INTEGER NOT NULL,
                repeatType TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                lastPaidAt INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_bills_dueDate ON bills(dueDate)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_bills_isPaid ON bills(isPaid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_bills_category ON bills(category)")
    }
}
```

## Repository and Domain Layer

Use `StateFlow` consistently for all new screens.

```kotlin
class BillRepository @Inject constructor(
    private val billDao: BillDao,
    @IoDispatcher private val io: CoroutineDispatcher
) {
    fun getAllBills(): Flow<List<BillEntity>> = billDao.getAllBills().flowOn(io)

    fun getUpcomingBills(now: Long, until: Long): Flow<List<BillEntity>> =
        billDao.getUpcomingBills(now, until).flowOn(io)

    suspend fun saveBill(bill: BillEntity): Long = withContext(io) {
        billDao.insertBill(bill)
    }

    suspend fun updateBill(bill: BillEntity) = withContext(io) {
        billDao.updateBill(bill.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun markAsPaid(billId: Long, paid: Boolean) = withContext(io) {
        billDao.markAsPaid(
            billId = billId,
            paid = paid,
            paidAt = if (paid) System.currentTimeMillis() else null
        )
    }
}
```

For monthly dashboard and history, add query models instead of computing everything in fragments:

- `MonthlySummary(totalBills, paidAmount, unpaidAmount, paidCount, unpaidCount)`
- `HistoryPoint(monthLabel, amountPaid)`
- `BillListItem(id, title, formattedAmount, dueLabel, status, category)`

## Notification System

The current [PaymentReminderWorker.kt](E:/Billbuddy/app/src/main/java/com/vyapaarhisaab/app/worker/PaymentReminderWorker.kt) is not sufficient for product requirements because:

- it uses transaction/customer semantics
- it does not support 7/3/1 day offsets
- it is not unique per bill reminder stage
- it does not reschedule recurring bills

### Recommended approach

For each bill, enqueue three unique one-time works:

- `bill_{id}_7d`
- `bill_{id}_3d`
- `bill_{id}_1d`

Each work should:

- use `ExistingWorkPolicy.REPLACE`
- compute trigger times in the user timezone
- be skipped if the bill is already paid

Scheduling helper:

```kotlin
fun scheduleBillReminders(
    context: Context,
    billId: Long,
    dueDateMillis: Long
) {
    val offsets = listOf(7L, 3L, 1L)
    offsets.forEach { daysBefore ->
        val triggerAt = dueDateMillis - TimeUnit.DAYS.toMillis(daysBefore)
        val delay = (triggerAt - System.currentTimeMillis()).coerceAtLeast(0L)

        val request = OneTimeWorkRequestBuilder<BillReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    "bill_id" to billId,
                    "days_before" to daysBefore.toInt()
                )
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "bill_${billId}_${daysBefore}d",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
```

Worker contract:

```kotlin
@HiltWorker
class BillReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val billRepository: BillRepository,
    private val analyticsTracker: AnalyticsTracker
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val billId = inputData.getLong("bill_id", -1L)
        val daysBefore = inputData.getInt("days_before", 0)
        if (billId <= 0L) return Result.failure()

        val bill = billRepository.getBillById(billId) ?: return Result.success()
        if (bill.isPaid) return Result.success()

        // Build and post a notification.
        // If bill is recurring, reschedule next cycle after payment or rollover.
        analyticsTracker.logReminderShown(billId, daysBefore)
        return Result.success()
    }
}
```

### Reboot persistence

`WorkManager` already persists work. The current [BootReceiver.kt](E:/Billbuddy/app/src/main/java/com/vyapaarhisaab/app/worker/BootReceiver.kt) can stay minimal, but if you add manual consistency checks, trigger a resync worker instead of scheduling directly in the receiver.

## UI Screen Plan

Keep XML fragments for MVP. Re-skin and repurpose existing screens.

### Screen 1: Bill List (Home)

Base on current `HomeFragment`, but remove ledger metrics.

Content:

- total outstanding hero
- upcoming bills list
- paid/unpaid chips or tabs
- bottom banner ad
- FAB for add bill

Use `RecyclerView + ListAdapter + ItemTouchHelper` for:

- swipe right: mark paid/unpaid
- swipe left: delete

### Screen 2: Add/Edit Bill

Refactor [AddTransactionFragment.kt](E:/Billbuddy/app/src/main/java/com/vyapaarhisaab/app/ui/transaction/AddTransactionFragment.kt) into an add/edit bill form:

- title
- amount
- due date
- category
- repeat type
- paid toggle optional for edit mode

Use the `stitch` design as the exact structural reference.

### Screen 3: Dashboard

Refactor `SummaryFragment` into a monthly dashboard:

- Total Bills This Month
- Paid Amount
- Remaining
- progress bar
- upcoming next 7 days
- optional premium/upsell card

Use `stitch (4)` as the reference.

### Screen 4: History

Replace ledger summary/history with:

- month filter
- chart over last 6 months
- past paid bills list

Use `MPAndroidChart`, which is already in dependencies.

### Screen 5: Settings

Simplify aggressively:

- notification timing toggles
- currency fixed to USD in MVP
- PDF export locked behind rewarded ad
- privacy/about

Use `stitch (3)` and `stitch (5)` for the rewarded sheet.

## AdMob Changes

Current ad logic in [AdManager.kt](E:/Billbuddy/app/src/main/java/com/vyapaarhisaab/app/ui/common/util/AdManager.kt) is transaction-save based, which is wrong for this product.

### Replace current policy with this

- `Interstitial`
  - preload at app launch
  - show when Dashboard opens
  - max once per app session
- `Banner`
  - only Bill List bottom container
- `Rewarded`
  - required for PDF export
  - do not substitute with interstitial

Suggested structure:

```kotlin
object AdPlacementController {
    private var hasShownDashboardInterstitialThisSession = false

    fun maybeShowDashboardInterstitial(activity: Activity, onDone: () -> Unit) {
        if (hasShownDashboardInterstitialThisSession) {
            onDone()
            return
        }
        hasShownDashboardInterstitialThisSession = true
        // show loaded interstitial if available, else continue
    }
}
```

### Debug/test ad IDs

The build already includes AdMob. Add build-config based switching:

- debug -> Google test IDs
- release -> real IDs

This is mandatory for safer QA.

## PDF Export

The existing `ExportImportManager` exports CSV. Replace this for the product path:

- keep import out of MVP
- add `BillPdfExporter`
- generate a simple branded PDF summary:
  - app title
  - month range
  - total paid
  - total unpaid
  - bill table
  - optional chart snapshot in phase 2

Rewarded flow:

1. user taps export
2. show rewarded ad
3. on reward grant, create PDF
4. share via `FileProvider`

Do not generate the file before the reward callback succeeds.

## Firebase Analytics

Add a thin wrapper instead of logging inline everywhere:

```kotlin
class AnalyticsTracker @Inject constructor(
    private val analytics: FirebaseAnalytics
) {
    fun billAdded(category: String, repeatType: String) {
        analytics.logEvent("bill_added", bundleOf(
            "category" to category,
            "repeat_type" to repeatType
        ))
    }

    fun billMarkedPaid(billId: Long) {
        analytics.logEvent("bill_marked_paid", bundleOf("bill_id" to billId))
    }

    fun dashboardOpened() {
        analytics.logEvent("dashboard_opened", null)
    }

    fun rewardedCompleted(entryPoint: String) {
        analytics.logEvent("rewarded_completed", bundleOf("entry_point" to entryPoint))
    }
}
```

Track exactly the requested events:

- `bill_added`
- `bill_marked_paid`
- `dashboard_opened`
- `ad_clicked`
- `rewarded_completed`

## US Market Localization Changes

Update utilities:

- replace rupee formatting with USD
- replace `yyyy-MM-dd` and India-oriented strings with `MM/dd/yyyy`
- default categories:
  - Rent
  - Utilities
  - Credit Card
  - Insurance
  - Subscriptions

Recommended utility:

```kotlin
object UsCurrencyFormatter {
    private val formatter = NumberFormat.getCurrencyInstance(Locale.US)

    fun formatCents(amountCents: Long): String = formatter.format(amountCents / 100.0)
}
```

## Testing Plan

### Unit tests

- `BillListViewModel`
  - load bills
  - mark paid
  - delete bill
- `DashboardViewModel`
  - monthly summary aggregation
- `AddEditBillViewModel`
  - input validation
  - scheduling call after save

### Worker tests

- reminder work uses correct unique names
- paid bills do not notify
- 7/3/1 day delays compute correctly

### Ad tests

- rewarded callback unlocks export only after reward
- interstitial shows once per session

## Step-by-Step Delivery Plan

### Phase 1: Bill CRUD

- add `BillEntity`, `BillDao`, `BillRepository`
- add DB migration `1 -> 2`
- build `BillListFragment`
- refactor add transaction screen into `AddEditBillFragment`
- wire bill save, update, delete, mark paid

### Phase 2: Notifications

- replace `PaymentReminderWorker` with `BillReminderWorker`
- add reminder scheduling helper
- add settings for 7/3/1 day toggles
- reschedule on bill updates

### Phase 3: Dashboard + History

- replace `SummaryFragment` with dashboard
- add monthly aggregates
- add history queries and `MPAndroidChart`
- add empty states

### Phase 4: Monetization

- update `AdManager` to session-based dashboard interstitials
- add rewarded ad loader and callback
- gate PDF export
- keep banner only on bill list

### Phase 5: Analytics + Cleanup

- add analytics wrapper
- log requested events
- remove customer-ledger screens from nav graph
- strip unused customer imports, strings, drawables, and tests

## Scalability Improvements That Are Worth Doing

These are worth doing now because they stay MVP-friendly:

- Standardize all new ViewModels on `StateFlow`
- Introduce UI models instead of binding entities directly
- Add `Clock` abstraction for reminder/date testability
- Separate ad policy from fragments
- Replace `fallbackToDestructiveMigration()` before shipping

These are not worth doing yet:

- full Compose rewrite
- multi-module breakup
- cloud sync
- complex recurring exceptions
- backend-driven reminders

## Recommended First Implementation Slice

Start with the smallest end-to-end shipped slice:

1. Add `BillEntity`, `BillDao`, and migration.
2. Build `BillListFragment` using the current `HomeFragment` shell.
3. Refactor `AddTransactionFragment` into `AddEditBillFragment`.
4. Add mark-paid and delete interactions.
5. Switch currency/date formatting to US defaults.

That slice gives a working `BillBuddy USA` core quickly, while preserving the current app skeleton.
