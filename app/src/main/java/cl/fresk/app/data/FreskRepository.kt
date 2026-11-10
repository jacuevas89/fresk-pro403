package cl.fresk.app.data

import androidx.room.withTransaction
import cl.fresk.app.domain.*
import kotlinx.coroutines.flow.Flow

class FreskRepository(private val database: FreskDatabase) {
    private val dao = database.dao()
    val products: Flow<List<ProductEntity>> = dao.observeProducts()
    val movements: Flow<List<MovementEntity>> = dao.observeMovements()

    // Funciones de guardado y movimiento con control de errores
    suspend fun save(product: ProductEntity): Result<Long> = runCatching {
        require(product.name.isNotBlank()) { "Ingresa el nombre del producto." }
        require(product.category.isNotBlank()) { "Selecciona una categoría." }
        require(product.quantityBase > 0 && product.quantityBase.isFinite()) { "La cantidad debe ser mayor a cero." }
        database.withTransaction {
            require(dao.findActiveDuplicate(product.name, product.unitFamily, product.id) == null) {
                "No se puede guardar. Ya existe un producto con ese nombre y tipo de unidad."
            }
            if (product.id == 0L) {
                // Cada registro es individual, no mezcla productos con diferentes tipos de unidades de medida
                val id = dao.insertProduct(product)
                dao.insertMovement(MovementEntity(productId=id,kind="CREATE",quantityBase=product.quantityBase,resultingQuantityBase=product.quantityBase))
                id
            } else {
                val prev = dao.getProduct(product.id) ?: error("El producto ya no existe.")
                require(prev.active) { "El producto fue eliminado." }
                dao.updateProduct(product)
                dao.insertMovement(MovementEntity(productId=product.id,kind="EDIT",quantityBase=product.quantityBase-prev.quantityBase,resultingQuantityBase=product.quantityBase))
                product.id
            }
        }
    }

    suspend fun move(productId: Long, kind: String, qty: Double, inputUnit: String, reason: String): Result<Double> = runCatching {
        require(kind in setOf("CONSUMED","DISCARDED","DEACTIVATED")) { "Movimiento inválido." }
        database.withTransaction {
            val product = dao.getProduct(productId) ?: error("No se encontró el producto.")
            require(product.active) { "El producto ya no está activo." }
            val amount = if (kind == "DEACTIVATED") product.quantityBase else {
                require(unitFamily(inputUnit) == product.unitFamily) { "La unidad no es compatible con este producto." }
                val normalized = toBase(qty,inputUnit)
                require(normalized > 0 && normalized.isFinite()) { "Ingresa una cantidad válida mayor a cero." }
                require(normalized <= product.quantityBase + 0.000001) { "No puedes registrar más de lo que tienes disponible." }
                if (product.unitFamily == "COUNT") require(normalized % 1.0 == 0.0) { "Las unidades deben ser cantidades enteras." }
                normalized
            }
            val remaining = if (kind == "DEACTIVATED") product.quantityBase else (product.quantityBase-amount).coerceAtLeast(0.0)
            dao.updateProduct(product.copy(quantityBase=remaining,active=kind != "DEACTIVATED" && remaining > 0.0000001))
            dao.insertMovement(MovementEntity(productId=productId,kind=kind,quantityBase=amount,resultingQuantityBase=remaining,reason=reason))
            remaining
        }
    }
}
