package cl.fresk.app.domain

import cl.fresk.app.data.ProductEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

// Los tiempos del catálogo son ejemplos editables
data class CatalogItem(val name: String, val category: String, val ambient: Int?, val fridge: Int?, val freezer: Int?) {
    fun days(storage: String): Int? = when (storage) { "AMBIENT" -> ambient; "FRIDGE" -> fridge; else -> freezer }
}

val categories = listOf(
    "Frutas", "Verduras y hortalizas", "Hongos y setas", "Carnes y aves", "Pescados y mariscos",
    "Lácteos y derivados", "Huevos", "Legumbres", "Cereales, harinas y granos", "Panadería y repostería",
    "Pastas y fideos", "Conservas y enlatados", "Congelados y preparados", "Frutos secos y semillas",
    "Condimentos y especias", "Azúcar y endulzantes", "Aceites, salsas y aderezos", "Bebidas", "Otros"
)

// Catálogo se referencia, versión inicial
val catalog = listOf(
    CatalogItem("Manzana", "Frutas", 7, 14, 90),
    CatalogItem("Plátano", "Frutas", 5, 7, 60),
    CatalogItem("Frutillas", "Frutas", 2, 3, 90),
    CatalogItem("Naranja", "Frutas", 7, 14, 90),
    CatalogItem("Uvas", "Frutas", 3, 7, 90),
    CatalogItem("Lechuga", "Verduras y hortalizas", 1, 5, null),
    CatalogItem("Zanahoria", "Verduras y hortalizas", 3, 14, 90),
    CatalogItem("Tomate", "Verduras y hortalizas", 5, 7, 90),
    CatalogItem("Papa", "Verduras y hortalizas", 14, null, 90),
    CatalogItem("Champiñones", "Hongos y setas", 1, 4, 60),
    CatalogItem("Pan", "Panadería y repostería", 3, null, 90),
    CatalogItem("Queso fresco", "Lácteos y derivados", null, 5, 30),
    CatalogItem("Yogur", "Lácteos y derivados", null, null, null),
    CatalogItem("Leche", "Lácteos y derivados", null, null, null),
    CatalogItem("Huevos", "Huevos", null, null, null),
    CatalogItem("Lentejas secas", "Legumbres", null, null, null),
    CatalogItem("Arroz", "Cereales, harinas y granos", null, null, null),
    CatalogItem("Avena", "Cereales, harinas y granos", null, null, null),
    CatalogItem("Pastas secas", "Pastas y fideos", null, null, null),
    CatalogItem("Harina", "Cereales, harinas y granos", null, null, null)
)

val unitList = listOf("un", "g", "kg", "ml", "L")
fun unitFamily(unit: String) = when (unit) { "un" -> "COUNT"; "g", "kg" -> "MASS"; else -> "VOLUME" }
fun toBase(value: Double, unit: String) = value * if (unit == "kg" || unit == "L") 1000.0 else 1.0
fun fromBase(value: Double, unit: String) = value / if (unit == "kg" || unit == "L") 1000.0 else 1.0
fun unitLabel(unit: String): String = if (unit == "un") "unidad(es)" else unit
fun quantityLabel(base: Double, unit: String): String = "${fmt(fromBase(base,unit))} ${unitLabel(unit)}"
fun fmt(n: Double): String = if (abs(n - n.toLong().toDouble()) < 0.000001) n.toLong().toString() else String.format(Locale("es","CL"),"%.2f", n).trimEnd('0').trimEnd(',')
fun dateText(epoch: Long): String = LocalDate.ofEpochDay(epoch).format(DateTimeFormatter.ofPattern("dd MMM yyyy",Locale("es","CL")))
fun daysRemaining(product: ProductEntity): Long = product.estimatedDate - LocalDate.now().toEpochDay()
fun dateStatus(product: ProductEntity): String = when {
    daysRemaining(product) < 0 -> "PLAZO_CUMPLIDO"
    daysRemaining(product) == 0L -> "HOY"
    daysRemaining(product) <= 3 -> "PROXIMO"
    else -> "NORMAL"
}
