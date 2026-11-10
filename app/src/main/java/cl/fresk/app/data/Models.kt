package cl.fresk.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import androidx.sqlite.db.SupportSQLiteDatabase


// Construcción de entidades y DAO
@Entity(tableName = "products", indices = [Index("name"), Index("category"), Index("active")])
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val storage: String, // AMBIENT, FRIDGE, FREEZER
    val quantityBase: Double, // piezas, gramos o mililitros
    val unitFamily: String, // COUNT, MASS, VOLUME
    val displayUnit: String, // un, g, kg, ml, L
    val dateMethod: String, // DAYS, DATE
    val estimatedDate: Long, // LocalDate.toEpochDay()
    val registeredDate: Long,
    val source: String, // CATALOG, USER
    val notes: String = "",
    val active: Boolean = true
)

@Entity(tableName = "movements", foreignKeys = [ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productId"], onDelete = ForeignKey.NO_ACTION)], indices = [Index("productId")])
data class MovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val kind: String, // CREATE, EDIT, CONSUMED, DISCARDED, DEACTIVATED
    val quantityBase: Double,
    val resultingQuantityBase: Double,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val reason: String = ""
)

@Dao
interface FreskDao {
    @Query("SELECT * FROM products WHERE active = 1 ORDER BY estimatedDate ASC, name ASC")
    fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM movements ORDER BY createdAtMillis DESC")
    fun observeMovements(): Flow<List<MovementEntity>>

    @Query("SELECT * FROM products WHERE active = 1 AND lower(trim(name)) = lower(trim(:name)) AND unitFamily = :unitFamily AND id != :excludeId LIMIT 1")
    suspend fun findActiveDuplicate(name: String, unitFamily: String, excludeId: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProduct(id: Long): ProductEntity?

    @Insert suspend fun insertProduct(p: ProductEntity): Long
    @Update suspend fun updateProduct(p: ProductEntity)
    @Insert suspend fun insertMovement(m: MovementEntity): Long
}

@Database(entities = [ProductEntity::class, MovementEntity::class], version = 1, exportSchema = false)
abstract class FreskDatabase : RoomDatabase() {
    abstract fun dao(): FreskDao
    companion object {
        @Volatile private var INSTANCE: FreskDatabase? = null
        fun instance(context: android.content.Context): FreskDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, FreskDatabase::class.java, "fresk_local.db")
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // Vistas con nombres fáciles de consultar.
                        db.execSQL("""
                            CREATE VIEW IF NOT EXISTS productos_consulta AS
                            SELECT id, name AS nombre, category AS categoria,
                                   storage AS almacenamiento, quantityBase AS cantidad_base,
                                   unitFamily AS tipo_unidad, displayUnit AS unidad,
                                   dateMethod AS tipo_fecha, estimatedDate AS fecha_caducidad,
                                   registeredDate AS fecha_registro, source AS origen,
                                   notes AS notas, active AS activo
                            FROM products
                        """.trimIndent())
                        db.execSQL("""
                            CREATE VIEW IF NOT EXISTS movimientos_consulta AS
                            SELECT id, productId AS id_producto, kind AS tipo_movimiento,
                                   quantityBase AS cantidad_base,
                                   resultingQuantityBase AS cantidad_restante,
                                   createdAtMillis AS fecha_movimiento, reason AS motivo
                            FROM movements
                        """.trimIndent())
                    }
                })
                .build().also { INSTANCE = it }
        }
    }
}
