package cl.fresk.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.fresk.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ViewModel para el repositorio de datos
class FreskViewModel(app: Application): AndroidViewModel(app) {
    private val repo = FreskRepository(FreskDatabase.instance(app))
    val products: StateFlow<List<ProductEntity>> = repo.products.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val movements: StateFlow<List<MovementEntity>> = repo.movements.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _notice = MutableSharedFlow<String>(extraBufferCapacity=1)
    val notice = _notice.asSharedFlow()
    fun save(product: ProductEntity, onSuccess:(Long)->Unit) {viewModelScope.launch {
        repo.save(product).fold(onSuccess={_notice.emit(if(product.id==0L)"Producto agregado correctamente." else "Producto actualizado correctamente.");onSuccess(it)},onFailure={_notice.emit(it.message?:"No se pudo guardar el producto.")})
    }}
    fun move(id:Long,kind:String,qty:Double,unit:String,reason:String,onSuccess:(Double)->Unit) {viewModelScope.launch {
        repo.move(id,kind,qty,unit,reason).fold(onSuccess={_notice.emit(when(kind){"CONSUMED"->"Consumo registrado correctamente.";"DISCARDED"->"Descarte registrado correctamente.";else->"Producto eliminado correctamente."});onSuccess(it)},onFailure={_notice.emit(it.message?:"No se pudo registrar la operación.")})
    }}
}
