package cl.fresk.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.fresk.app.data.*
import cl.fresk.app.domain.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Petroleum=Color(0xFF2F5D62)
private val Coral=Color(0xFFE97A5F)
private val Cream=Color(0xFFF7F3ED)
private val TealLight=Color(0xFFE4F0F1)
private val Ink=Color(0xFF293039)
private val Muted=Color(0xFF69757B)
private val Amber=Color(0xFFFFE9AE)
private val Blush=Color(0xFFFFE1DC)

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { FreskRoot() } }
}

@Composable
fun FreskRoot(vm: FreskViewModel = viewModel()) {
    // Conserva la bienvenida completada al girar el celular.
    var showingSplash by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(1150); showingSplash=false }
    MaterialTheme(colorScheme=lightColorScheme(primary=Petroleum, secondary=Coral, background=Cream, surface=Color.White, onSurface=Ink)) {
        if(showingSplash) SplashScreen() else FreskNavigation(vm)
    }
}

@Composable
private fun SplashScreen(){
    Box(Modifier.fillMaxSize().background(Color.White),contentAlignment=Alignment.Center){
        Column(horizontalAlignment=Alignment.CenterHorizontally){
            Image(painterResource(R.drawable.fresk_logo),"Logotipo Fresk: F con hoja",Modifier.size(194.dp))
            Spacer(Modifier.height(18.dp))
            Text("Fresk", fontSize=33.sp,fontWeight=FontWeight.Bold,color=Petroleum)
            Text("Tu despensa bajo control",color=Muted)
        }
    }
}

@Composable
private fun FreskNavigation(vm:FreskViewModel){
    val products by vm.products.collectAsState()
    val movements by vm.movements.collectAsState()
    var route by remember { mutableStateOf("home") }
    var selectedId by remember { mutableLongStateOf(0L) }
    var editId by remember { mutableLongStateOf(0L) }
    var outcome by remember { mutableStateOf("") }
    var outcomeSubtitle by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("") }
    val snackbarHost=remember{SnackbarHostState()}
    LaunchedEffect(vm) { vm.notice.collect{snackbarHost.showSnackbar(it)} }
    var previousRoute by remember { mutableStateOf("home") }
    fun navigate(dest:String){if(dest in setOf("form","detail","move"))previousRoute=route;route=dest}
    BackHandler(enabled=route!="home"){route=when(route){"form"->if(editId==0L)previousRoute else "detail";"detail","success"->"products";"move"->if(previousRoute=="detail")"detail" else "home";else->"home"}}
    fun openProduct(id:Long){selectedId=id;navigate("detail")}
    Scaffold(
        containerColor=Cream,
        snackbarHost={SnackbarHost(snackbarHost)},
        bottomBar={if(route in setOf("home","products","categories","alerts")) MainNav(route,::navigate)}
    ){ inset ->
      Box(Modifier.padding(inset).fillMaxSize()){
        when(route){
            "home"->HomeScreen(products,onRegister={editId=0;navigate("form")},onMove={selectedId=0;navigate("move")},onDetails=::openProduct,onAlerts={navigate("alerts")})
            "products"->ProductsScreen(products,onDetails=::openProduct,onAdd={editId=0;navigate("form")},initialCategory=categoryFilter,onCategoryCleared={categoryFilter=""})
            "categories"->CategoriesScreen(products){cat->categoryFilter=cat;navigate("products")}
            "alerts"->AlertsScreen(products,onDetails=::openProduct)
            "form"->ProductForm(vm=vm,existing=products.find{it.id==editId},onBack={navigate(if(editId==0L)previousRoute else "detail")},onSaved={id-> selectedId=id;categoryFilter="";navigate(if(editId==0L)"products" else "detail")})
            "detail"-> {
                val p=products.find{it.id==selectedId}
                if(p==null)EmptyScreen("El producto no está disponible."){navigate("products")}
                else DetailScreen(p,onBack={navigate("products")},onEdit={editId=p.id;navigate("form")},onMove={navigate("move")})
            }
            "move"->MovementScreen(products=products,initial=products.find{it.id==selectedId},onBack={navigate(if(previousRoute=="detail")"detail" else "home")},onConfirm={p,kind,qty,unit,reason->
                 vm.move(p.id,kind,qty,unit,reason){remaining->
                     outcome=when(kind){"CONSUMED"->"Consumo registrado";"DISCARDED"->"Descarte registrado";else->"Registro eliminado"}
                     outcomeSubtitle=if(kind=="DEACTIVATED")"El registro fue eliminado de sus productos" else "Saldo restante: ${quantityLabel(remaining,p.displayUnit)}"
                     navigate("success")
                 }
            })
            "success"->SuccessScreen(outcome,outcomeSubtitle){categoryFilter="";navigate("products")}
        }
      }
    }
}

@Composable
private fun MainNav(selected:String,navigate:(String)->Unit){
    NavigationBar(containerColor=Color.White){
       listOf(Triple("home","Inicio",Icons.Default.Home),Triple("products","Productos",Icons.Default.Inventory2),Triple("categories","Categorías",Icons.Default.Category),Triple("alerts","Alertas",Icons.Default.Notifications)).forEach{(screen,label,icon)->
          NavigationBarItem(selected=selected==screen,onClick={navigate(screen)},icon={Icon(icon,label)},label={Text(label,maxLines=1)},colors=NavigationBarItemDefaults.colors(selectedIconColor=Petroleum,selectedTextColor=Petroleum,indicatorColor=TealLight))
       }
    }
}

@Composable
private fun Page(title:String,description:String?=null,onBack:(()->Unit)?=null,showLogo:Boolean=false,headerAction:(@Composable ()->Unit)?=null,content:@Composable ColumnScope.()->Unit){
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){if(onBack!=null)IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Volver")};if(showLogo){Image(painterResource(R.drawable.fresk_logo),"Ícono Fresk",modifier=Modifier.size(43.dp));Spacer(Modifier.width(9.dp));Column{Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,color=Ink);if(description!=null)Text(description,color=Muted,style=MaterialTheme.typography.bodySmall)}} else Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,color=Ink);if(headerAction!=null){Spacer(Modifier.weight(1f));headerAction()}}
        if(description!=null && !showLogo){Text(description,color=Muted,style=MaterialTheme.typography.bodySmall);Spacer(Modifier.height(18.dp))} else Spacer(Modifier.height(14.dp))
        content()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SurfaceCard(content:@Composable ColumnScope.()->Unit){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Color.White),content={Column(Modifier.padding(16.dp),content=content)})}

@Composable
private fun SectionHeading(title:String){Text(title,fontWeight=FontWeight.Bold,fontSize=19.sp,color=Ink);Spacer(Modifier.height(9.dp))}

@Composable
private fun PrimaryAction(text:String,onClick:()->Unit,modifier:Modifier=Modifier){Button(onClick=onClick,modifier=modifier.fillMaxWidth().heightIn(min=49.dp),shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=Petroleum)){Text(text,textAlign=TextAlign.Center)}}

@Composable
private fun SecondaryAction(text:String,onClick:()->Unit){OutlinedButton(onClick=onClick,modifier=Modifier.fillMaxWidth().heightIn(min=49.dp),shape=RoundedCornerShape(12.dp)){Text(text)}}

@Composable
private fun StatusChip(text:String,kind:String){val bg=when(kind){"PROXIMO","HOY"->Amber;"PLAZO_CUMPLIDO"->Blush;else->TealLight};Text(text,color=Ink,style=MaterialTheme.typography.labelSmall,modifier=Modifier.background(bg,RoundedCornerShape(24.dp)).padding(horizontal=10.dp,vertical=7.dp))}

@Composable
private fun ProductCard(p:ProductEntity,onClick:()->Unit){
    val status=dateStatus(p)
    val detail="${p.category} - ${quantityLabel(p.quantityBase,p.displayUnit)} - ${storageLabel(p.storage)}"
    SurfaceCard{
        Row(Modifier.fillMaxWidth().clickable(onClick=onClick),verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)){Text(p.name,fontWeight=FontWeight.SemiBold);Text(detail,color=Muted,style=MaterialTheme.typography.bodySmall,maxLines=2)}
            Spacer(Modifier.width(8.dp))
            StatusChip(statusText(p),status)
        }
    }
}

private fun statusText(p:ProductEntity):String = when(dateStatus(p)) {
    "PROXIMO" -> "Vence en ${daysRemaining(p)} día(s)"
    "HOY"-> "Vence hoy"
    "PLAZO_CUMPLIDO"-> "Fecha caducada"
    else-> "Vigente"
}
private fun storageLabel(v:String)=when(v){"FRIDGE"->"Refrigerador";"FREEZER"->"Freezer";else->"Temperatura ambiente"}

@Composable
private fun HomeScreen(products:List<ProductEntity>,onRegister:()->Unit,onMove:()->Unit,onDetails:(Long)->Unit,onAlerts:()->Unit){
    val concern=products.filter{dateStatus(it)!="NORMAL"}.sortedBy{it.estimatedDate}
    Page(title="Fresk",description="Tu despensa bajo control",showLogo=true){
        Card(colors=CardDefaults.cardColors(containerColor=TealLight),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth()){
            Column(Modifier.padding(19.dp)){
                Text(products.size.toString(),fontSize=36.sp,fontWeight=FontWeight.Bold,color=Petroleum)
                Text("productos registrados")
                Spacer(Modifier.height(9.dp))
                Text("${products.count{dateStatus(it)=="PROXIMO" || dateStatus(it)=="HOY"}} próximo(s) a vencer - ${products.count{dateStatus(it)=="PLAZO_CUMPLIDO"}} vencido(s)",style=MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(23.dp));SectionHeading("Requieren atención")
        if(concern.isEmpty()) Text("No hay productos con fechas próximas o cumplidas.",color=Muted) else concern.take(3).forEach{p->ProductCard(p){onDetails(p.id)};Spacer(Modifier.height(8.dp))}
        TextButton(onClick=onAlerts){Text("Ver todas las alertas")}
        Spacer(Modifier.height(12.dp));SectionHeading("Accesos rápidos")
        PrimaryAction("Registrar producto",onRegister);Spacer(Modifier.height(9.dp))
        Button(onClick=onMove,modifier=Modifier.fillMaxWidth().height(49.dp),colors=ButtonDefaults.buttonColors(containerColor=Coral),shape=RoundedCornerShape(12.dp)){Text("Registrar consumo")}
    }
}

@Composable
private fun ProductsScreen(products:List<ProductEntity>,onDetails:(Long)->Unit,onAdd:()->Unit,initialCategory:String,onCategoryCleared:()->Unit){
    var search by remember { mutableStateOf("") }
    var storage by remember {mutableStateOf("ALL")}
    val filtered=products.filter{it.name.contains(search,true) && (storage=="ALL"||it.storage==storage) && (initialCategory.isBlank()||it.category==initialCategory)}
    Page("Mis productos","${products.size} productos en tu despensa",headerAction={
        Button(onClick=onAdd,modifier=Modifier.height(40.dp),contentPadding=PaddingValues(horizontal=10.dp),shape=RoundedCornerShape(11.dp)){
            Text("+ Registrar producto",fontSize=12.sp,maxLines=1)
        }
    }){
        OutlinedTextField(value=search,onValueChange={search=it},label={Text("Buscar producto")},singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},modifier=Modifier.fillMaxWidth())
        if(initialCategory.isNotBlank()){Spacer(Modifier.height(9.dp));InputChip(selected=true,onClick=onCategoryCleared,label={Text(initialCategory+"  ×")})}
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
            listOf("ALL" to "Todos","FRIDGE" to "Refrigerador","FREEZER" to "Freezer").forEach{(key,label)->FilterChip(selected=storage==key,onClick={storage=key},label={Text(label,maxLines=1)})}
        }
        Spacer(Modifier.height(12.dp))
        if(filtered.isEmpty()) Text("No hay productos que coincidan con la búsqueda.",color=Muted) else filtered.forEach{ProductCard(it){onDetails(it.id)};Spacer(Modifier.height(8.dp))}
    }
}

@Composable
private fun CategoriesScreen(products:List<ProductEntity>,onCategory:(String)->Unit){
    Page("Categorías","Explora los productos por categoría"){
        categories.forEach{category->
            val n=products.count{it.category==category}
            if(n>0){SurfaceCard{Row(Modifier.fillMaxWidth().clickable{onCategory(category)},verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(category,fontWeight=FontWeight.SemiBold);Text("$n productos",color=Muted,style=MaterialTheme.typography.bodySmall)};Icon(Icons.Default.ChevronRight,null,tint=Petroleum)}};Spacer(Modifier.height(8.dp))}
        }
        if(products.isEmpty()) Text("Aún no tienes productos. Registra uno para organizarlo por categoría.",color=Muted)
    }
}

@Composable
private fun AlertsScreen(products:List<ProductEntity>,onDetails:(Long)->Unit){
    var tab by remember {mutableStateOf(0)}
    val soon=products.filter{dateStatus(it)=="PROXIMO"||dateStatus(it)=="HOY"}
    val late=products.filter{dateStatus(it)=="PLAZO_CUMPLIDO"}
    Page("Alertas","Productos que requieren revisión"){
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){
            FilterChip(selected=tab==0,onClick={tab=0},label={Text("Próximos a vencer (${soon.size})")})
            FilterChip(selected=tab==1,onClick={tab=1},label={Text("Vencidos (${late.size})")})
        }
        if(tab==0){
            Card(modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=TealLight),shape=RoundedCornerShape(12.dp)){
                Text("Aquí encontrarás los productos cuya fecha estimada de caducidad está próxima a cumplirse. Revisa su estado y prioriza su consumo.",modifier=Modifier.padding(14.dp),style=MaterialTheme.typography.bodySmall,color=Ink)
            }
        }
        if(tab==1){
            Card(modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=TealLight),shape=RoundedCornerShape(12.dp)){
                Text("Aquí encontrarás los productos cuya fecha estimada de caducidad ya se cumplió. Revisa su estado y deséchalos si es necesario.",modifier=Modifier.padding(14.dp),style=MaterialTheme.typography.bodySmall,color=Ink)
            }
        }
        Spacer(Modifier.height(12.dp))
        val displayed=(if(tab==0)soon else late).sortedBy{it.estimatedDate}
        if(displayed.isEmpty()) Text("No hay productos en esta sección.",color=Muted)
        displayed.forEach{p->
            SurfaceCard{Column(Modifier.clickable{onDetails(p.id)}){
                Text(p.name,fontWeight=FontWeight.Bold);Spacer(Modifier.height(3.dp))
                Text("${p.category} - ${dateText(p.estimatedDate)}",color=Muted,style=MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(5.dp))
                StatusChip(statusText(p),dateStatus(p))
            }};Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductForm(vm:FreskViewModel,existing:ProductEntity?,onBack:()->Unit,onSaved:(Long)->Unit){
    val today=remember{LocalDate.now()}
    var name by remember(existing?.id){mutableStateOf(existing?.name?:"")}
    var category by remember(existing?.id){mutableStateOf(existing?.category?:"Frutas")}
    var storage by remember(existing?.id){mutableStateOf(existing?.storage?:"AMBIENT")}
    var quantity by remember(existing?.id){mutableStateOf(existing?.let{fmt(fromBase(it.quantityBase,it.displayUnit))}?:"")}
    var unit by remember(existing?.id){mutableStateOf(existing?.displayUnit?:"un")}
    var dateMode by remember(existing?.id){mutableStateOf(existing?.dateMethod?:"DATE")}
    var days by remember(existing?.id){mutableStateOf(existing?.let{(it.estimatedDate-today.toEpochDay()).toString()}?:"")}
    var date by remember(existing?.id){mutableStateOf(existing?.let{LocalDate.ofEpochDay(it.estimatedDate)})}
    var selectedCatalog by remember(existing?.id){mutableStateOf(catalog.firstOrNull{it.name.equals(existing?.name,true)})}
    var suggestionsExpanded by remember{mutableStateOf(false)}
    var categoryExpanded by remember{mutableStateOf(false)}
    var unitExpanded by remember{mutableStateOf(false)}
    var datePickerOpen by remember{mutableStateOf(false)}
    var confirmSave by remember{mutableStateOf(false)}
    var problem by remember{mutableStateOf("")}

    fun updateRecommendation(item:CatalogItem?=selectedCatalog,place:String=storage){
        val d=item?.days(place)
        if(d!=null && existing==null){days=d.toString();date=today.plusDays(d.toLong())}
    }
    val options = remember(name,selectedCatalog){if(name.trim().length>=2 && selectedCatalog==null)catalog.filter{it.name.contains(name.trim(),ignoreCase=true)}.take(6) else emptyList()}
    val calculatedDate=if(dateMode=="DAYS")days.toLongOrNull()?.let{today.plusDays(it)} else date
    val units=unitList

    if(datePickerOpen){
        val picker=rememberDatePickerState(initialSelectedDateMillis=calculatedDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli())
        DatePickerDialog(onDismissRequest={datePickerOpen=false},confirmButton={TextButton(onClick={
            picker.selectedDateMillis?.let{date=Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()}
            datePickerOpen=false
        }){Text("Seleccionar")}},dismissButton={TextButton(onClick={datePickerOpen=false}){Text("Cancelar")}}){DatePicker(state=picker)}
    }
    if(confirmSave){ConfirmationDialog(title=if(existing==null)"Agregar producto" else "Guardar cambios",message=if(existing==null)"¿Deseas registrar «${name.trim()}» con ${quantity.trim()} ${unitLabel(unit)}?" else "¿Deseas actualizar la información de «${name.trim()}»?",confirm="Confirmar",onDismiss={confirmSave=false},onConfirm={
        confirmSave=false
        val count=quantity.replace(',','.').toDoubleOrNull()?:0.0
        val dateValue=calculatedDate?:today
        vm.save(ProductEntity(id=existing?.id?:0,name=name.trim(),category=category,storage=storage,quantityBase=toBase(count,unit),unitFamily=unitFamily(unit),displayUnit=unit,dateMethod=dateMode,estimatedDate=dateValue.toEpochDay(),registeredDate=existing?.registeredDate?:today.toEpochDay(),source=if(selectedCatalog!=null)"CATALOG" else "USER",active=existing?.active?:true,notes=existing?.notes?:""),onSaved)
    })}
    Page(if(existing==null)"Registrar producto" else "Editar producto",onBack=onBack){
        // Usamos el mismo formulario al crear y editar.
        Text("Nombre del producto",color=Muted,style=MaterialTheme.typography.labelLarge)
        Box{
          OutlinedTextField(value=name,onValueChange={v->name=v.lowercase(Locale("es","CL")).replaceFirstChar{it.uppercaseChar()};selectedCatalog=null;suggestionsExpanded=v.length>=2},singleLine=true,placeholder={Text("Escribe un producto")},keyboardOptions=KeyboardOptions(capitalization=androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),modifier=Modifier.fillMaxWidth())
          // La lista de sugerencias no interrumpe la escritura.
          if(suggestionsExpanded && options.isNotEmpty()){
              Column(Modifier.fillMaxWidth().padding(top=58.dp).background(Color.White,RoundedCornerShape(12.dp)).border(1.dp,TealLight,RoundedCornerShape(12.dp))){
                  options.forEach{item->
                      Column(Modifier.fillMaxWidth().clickable{ name=item.name;category=item.category;selectedCatalog=item;suggestionsExpanded=false;updateRecommendation(item,storage) }.padding(horizontal=13.dp,vertical=9.dp)){
                          Text(item.name,fontWeight=FontWeight.SemiBold)
                          Text(item.category,style=MaterialTheme.typography.bodySmall,color=Muted)
                      }
                      HorizontalDivider(color=TealLight)
                  }
              }
          }
        }
        Spacer(Modifier.height(12.dp))
        Text("Categoría",color=Muted,style=MaterialTheme.typography.labelLarge)
        Box{OutlinedButton(onClick={categoryExpanded=true},modifier=Modifier.fillMaxWidth()){Text(category,modifier=Modifier.weight(1f),textAlign=TextAlign.Start);Icon(Icons.Default.ArrowDropDown,null)}
            DropdownMenu(expanded=categoryExpanded,onDismissRequest={categoryExpanded=false}){categories.forEach{c->DropdownMenuItem(text={Text(c)},onClick={category=c;categoryExpanded=false})}}
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically){
            OutlinedTextField(value=quantity,onValueChange={quantity=it},label={Text("Cantidad")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.weight(1f),singleLine=true)
            Box(Modifier.weight(1f)){OutlinedButton(onClick={unitExpanded=true},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)){Text("${unitLabel(unit)}  ▾")}
                DropdownMenu(expanded=unitExpanded,onDismissRequest={unitExpanded=false}){units.forEach{u->DropdownMenuItem(text={Text(unitLabel(u))},onClick={unit=u;unitExpanded=false})}}
            }
        }
        Spacer(Modifier.height(17.dp))
        Text("Almacenamiento",fontWeight=FontWeight.SemiBold)
        // Mismo alto para los tres botones, con el estilo original de los filtros.
        Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            listOf("AMBIENT" to "Temperatura ambiente","FRIDGE" to "Refrigerador","FREEZER" to "Freezer").forEach{(key,label)->
                FilterChip(
                    selected=storage==key,
                    onClick={storage=key;updateRecommendation(selectedCatalog,key)},
                    modifier=Modifier.weight(if(key=="AMBIENT")1.55f else if(key=="FRIDGE")1.25f else 0.95f).height(62.dp),
                    label={Text(label,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center,fontSize=11.sp,lineHeight=14.sp,maxLines=2)}
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("¿Cómo deseas indicar la duración?",fontWeight=FontWeight.SemiBold)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            FilterChip(selected=dateMode=="DAYS",onClick={dateMode="DAYS";if(days.isBlank()&&date!=null)days=(date!!.toEpochDay()-today.toEpochDay()).toString()},label={Text("Plazo estimado")})
            FilterChip(selected=dateMode=="DATE",onClick={dateMode="DATE";if(date==null)date=days.toLongOrNull()?.let{today.plusDays(it)}},label={Text("Fecha estimada")})
        }
        Spacer(Modifier.height(8.dp))
        if(dateMode=="DAYS"){
            OutlinedTextField(value=days,onValueChange={days=it.filterIndexed{index,c->c.isDigit()||(index==0&&c=='-')}},label={Text("Duración del producto (días)")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Ascii),modifier=Modifier.fillMaxWidth())
        } else {
            OutlinedButton(onClick={datePickerOpen=true},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)){
                Icon(Icons.Default.CalendarMonth,null);Spacer(Modifier.width(7.dp));Text(calculatedDate?.let{dateText(it.toEpochDay())}?:"Seleccionar fecha estimada")
            }
        }
        Spacer(Modifier.height(11.dp))
        Card(colors=CardDefaults.cardColors(containerColor=TealLight),shape=RoundedCornerShape(13.dp),modifier=Modifier.fillMaxWidth()){
            Column(Modifier.padding(14.dp)){Text("Fecha estimada de caducidad",style=MaterialTheme.typography.bodySmall,color=Muted);Text(calculatedDate?.let{dateText(it.toEpochDay())}?:"Sin definir",fontWeight=FontWeight.Bold,color=Petroleum)}
        }
        Spacer(Modifier.height(9.dp))
        Text("La fecha es estimada y puede variar. Verifica siempre el estado del producto.",style=MaterialTheme.typography.bodySmall,color=Muted)
        if(problem.isNotBlank()){Spacer(Modifier.height(6.dp));Text(problem,color=Color(0xFFB3362D))}
        Spacer(Modifier.height(22.dp))
        PrimaryAction(if(existing==null)"Guardar producto" else "Guardar cambios",onClick={
            val q=quantity.replace(',','.').toDoubleOrNull()
            problem=when{
                name.isBlank()->"Ingresa el nombre del producto."
                category.isBlank()->"Selecciona una categoría."
                q==null || q<=0 || !q.isFinite()->"Ingresa una cantidad válida mayor a cero."
                unit=="un" && q%1.0!=0.0->"Las unidades deben registrarse como números enteros."
                calculatedDate==null->"Indica los días o una fecha estimada."
                else->""
            }
            if(problem.isBlank())confirmSave=true
        })
    }
}

@Composable
private fun DetailScreen(p:ProductEntity,onBack:()->Unit,onEdit:()->Unit,onMove:()->Unit){
    Page("Detalle del producto",onBack=onBack){
        Card(colors=CardDefaults.cardColors(containerColor=TealLight),modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){
            Column(Modifier.padding(20.dp)){Text(p.name,fontSize=28.sp,fontWeight=FontWeight.Bold,color=Petroleum);Text("${p.category} - ${storageLabel(p.storage)}",color=Muted);Spacer(Modifier.height(10.dp));StatusChip(statusText(p),dateStatus(p))}
        }
        Spacer(Modifier.height(24.dp));SectionHeading("Información registrada")
        SurfaceCard{
            DetailRow("Cantidad disponible",quantityLabel(p.quantityBase,p.displayUnit))
            DetailRow("Fecha de registro producto",dateText(p.registeredDate))
            DetailRow("Fecha estimada vencimiento",dateText(p.estimatedDate))
            DetailRow("Tipo de fecha",if(p.dateMethod=="DAYS")"Plazo calculado" else "Fecha ingresada")
            // Origen disponible solo para estadísticas internas.
        }
        Spacer(Modifier.height(28.dp))
        SecondaryAction("Editar producto",onEdit);Spacer(Modifier.height(10.dp))
        PrimaryAction("Registrar consumo",onMove)
    }
}

@Composable
private fun DetailRow(label:String,value:String){
    Row(Modifier.fillMaxWidth().padding(vertical=12.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.Top){Text(label,Modifier.weight(1f),color=Muted,style=MaterialTheme.typography.bodySmall);Text(value,Modifier.weight(1f),textAlign=TextAlign.End,fontWeight=FontWeight.SemiBold,style=MaterialTheme.typography.bodySmall)}
    HorizontalDivider(color=Color(0xFFE6E8EA))
}

@Composable
private fun MovementScreen(products:List<ProductEntity>,initial:ProductEntity?,onBack:()->Unit,onConfirm:(ProductEntity,String,Double,String,String)->Unit){
    var selected by remember {mutableLongStateOf(initial?.id?:0L)}
    var kind by remember{mutableStateOf("CONSUMED")}
    var amount by remember{mutableStateOf("")}
    var unit by remember{mutableStateOf(initial?.displayUnit?:"un")}
    var reason by remember{mutableStateOf("")}
    var reasonCode by remember { mutableStateOf("") }
    var reasonDropdown by remember { mutableStateOf(false) }
    val discardReasons=listOf(
        "DESCARTE_FECHA" to "Descartado por su fecha de caducación",
        "DESCARTE_CONSERVACION" to "Descartado por fallas en la conservación",
        "DESCARTE_DETERIORO" to "Descartado por signos de deterioro",
        "DESCARTE_ENVASE" to "Descartado por daños en el paquete o sospecha de contaminación",
        "DESCARTE_OTRO" to "Otro"
    )
    val deletionReasons=listOf(
        "ELIMINACION_INGRESO_ERRONEO" to "Producto ingresado por error",
        "ELIMINACION_DUPLICADO" to "Registro duplicado",
        "ELIMINACION_DATOS_INCORRECTOS" to "Datos incorrectos o registro no válido",
        "ELIMINACION_OTRO" to "Otro"
    )
    val reasonOptions=if(kind=="DISCARDED")discardReasons else deletionReasons
    val storedReason=if(kind=="CONSUMED")"" else if(reasonCode.endsWith("_OTRO") && reason.isNotBlank()) "$reasonCode: ${reason.trim()}" else reasonCode
    var productDropdown by remember{mutableStateOf(false)}
    var unitDropdown by remember{mutableStateOf(false)}
    var confirm by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf("")}
    val product=products.find{it.id==selected}
    LaunchedEffect(selected){unit=products.find{it.id==selected}?.displayUnit?:"un";amount=""}
    val parsed=amount.replace(',','.').toDoubleOrNull()?:0.0
    val base=toBase(parsed,unit)
    val remainder=product?.let{it.quantityBase-base}
    if(confirm && product!=null){
        val action=when(kind){"CONSUMED"->"consumo";"DISCARDED"->"descarte";else->"eliminación lógica"}
        ConfirmationDialog(
            title=when(kind){"CONSUMED"->"Confirmar consumo";"DISCARDED"->"Confirmar descarte";else->"Eliminar producto"},
            message=if(kind=="DEACTIVATED")"Esta opción sirve para quitar registros mal ingresados o productos que no corresponden a consumo ni descarte. ¿Deseas continuar?" else "¿Confirmas el $action de $amount $unit de «${product.name}»? Se actualizará la cantidad disponible.",
            confirm=if(kind=="DEACTIVATED")"Eliminar producto" else "Confirmar",
            onDismiss={confirm=false},onConfirm={confirm=false;onConfirm(product,kind,parsed,unit,storedReason)}
        )
    }
    Page("Registrar consumo",onBack=onBack){
        Text("Seleccionar producto",color=Muted)
        Box{
            OutlinedButton(onClick={productDropdown=true},modifier=Modifier.fillMaxWidth()){Text(product?.name?:"Seleccionar producto",modifier=Modifier.weight(1f),textAlign=TextAlign.Start);Icon(Icons.Default.ArrowDropDown,null)}
            DropdownMenu(expanded=productDropdown,onDismissRequest={productDropdown=false}){
                products.forEach{p->DropdownMenuItem(text={Text("${p.name} - ${quantityLabel(p.quantityBase,p.displayUnit)}")},onClick={selected=p.id;productDropdown=false})}
            }
        }
        Spacer(Modifier.height(10.dp))
        Card(colors=CardDefaults.cardColors(containerColor=TealLight),modifier=Modifier.fillMaxWidth()){
            Column(Modifier.padding(14.dp)){Text("Cantidad disponible",color=Muted,style=MaterialTheme.typography.bodySmall);Text(product?.let{quantityLabel(it.quantityBase,it.displayUnit)}?:"Sin producto",fontWeight=FontWeight.Bold,fontSize=22.sp,color=Petroleum)}
        }
        Spacer(Modifier.height(19.dp));Text("¿Qué deseas registrar?",fontWeight=FontWeight.SemiBold)
        Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
            listOf("CONSUMED" to "Consumido","DISCARDED" to "Descartado","DEACTIVATED" to "Eliminar producto").forEach{(key,label)->FilterChip(selected=kind==key,onClick={kind=key;reasonCode="";reason="";reasonDropdown=false},label={Text(label,fontSize=10.sp,maxLines=1)})}
        }
        Spacer(Modifier.height(10.dp))
        if(kind!="DEACTIVATED"){
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){
                OutlinedTextField(amount,onValueChange={amount=it},label={Text("Cantidad")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.weight(1f),singleLine=true)
                Box(Modifier.weight(1f)){
                    OutlinedButton(onClick={unitDropdown=true},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)){Text("${unitLabel(unit)}  ▾")}
                    DropdownMenu(expanded=unitDropdown,onDismissRequest={unitDropdown=false}){unitList.filter{product==null||unitFamily(it)==product.unitFamily}.forEach{u->DropdownMenuItem(text={Text(unitLabel(u))},onClick={unit=u;unitDropdown=false})}}
                }
            }
            Spacer(Modifier.height(10.dp))
            Card(colors=CardDefaults.cardColors(containerColor=TealLight),modifier=Modifier.fillMaxWidth()){
                Column(Modifier.padding(15.dp)){Text("Saldo después del movimiento",color=Muted,style=MaterialTheme.typography.bodySmall);Text(if(product!=null&&remainder!=null)quantityLabel(remainder.coerceAtLeast(0.0),product.displayUnit) else "—",fontWeight=FontWeight.Bold,color=Petroleum)}
            }
            if(kind=="DISCARDED"){
                Spacer(Modifier.height(10.dp))
                ReasonSelector(reasonCode,reasonOptions,onSelect={reasonCode=it;reason="";reasonDropdown=false},expanded=reasonDropdown,onExpandedChange={reasonDropdown=it})
                if(reasonCode=="DESCARTE_OTRO")OutlinedTextField(reason,onValueChange={reason=it},label={Text("Otro motivo (opcional)")},modifier=Modifier.fillMaxWidth())
            }
        } else {
            Card(colors=CardDefaults.cardColors(containerColor=Blush),modifier=Modifier.fillMaxWidth()){
                Text("Eliminar producto se usa para productos mal ingresados o que no corresponden a consumo ni descarte.",
                    modifier=Modifier.padding(15.dp),style=MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
            ReasonSelector(reasonCode,reasonOptions,onSelect={reasonCode=it;reason="";reasonDropdown=false},expanded=reasonDropdown,onExpandedChange={reasonDropdown=it})
            if(reasonCode=="ELIMINACION_OTRO")OutlinedTextField(reason,onValueChange={reason=it},label={Text("Otro motivo (opcional)")},modifier=Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(13.dp))
        Text("Consumido: producto que fue consumido o utilizado.\n" +
                "Descartado: producto desechado a la basura.\n" +
                "Eliminar producto: quitar u eliminar un producto mal ingresado.",
            color=Muted,style=MaterialTheme.typography.bodySmall)
        if(error.isNotBlank()){Spacer(Modifier.height(8.dp));Text(error,color=Color(0xFFB3362D))}
        Spacer(Modifier.height(25.dp));PrimaryAction("Confirmar movimiento",onClick={
            error=when{
                product==null->"Selecciona un producto."
                kind!="DEACTIVATED" && (parsed<=0||!parsed.isFinite())->"Ingresa una cantidad válida mayor a cero."
                kind!="DEACTIVATED" && base > (product?.quantityBase?:0.0)+0.000001->"No puedes utilizar más de la cantidad disponible."
                kind!="DEACTIVATED" && product?.unitFamily=="COUNT" && parsed%1.0!=0.0->"La cantidad de unidades debe ser entera."
                kind!="CONSUMED" && reasonCode.isBlank()->"Selecciona un motivo para registrar este movimiento."
                else->""
            }
            if(error.isBlank())confirm=true
        })
    }
}

@Composable
private fun ReasonSelector(selected:String,options:List<Pair<String,String>>,onSelect:(String)->Unit,expanded:Boolean,onExpandedChange:(Boolean)->Unit){
    Text("Motivo",color=Muted,style=MaterialTheme.typography.labelLarge)
    Box(Modifier.fillMaxWidth()){
        OutlinedButton(onClick={onExpandedChange(true)},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)){
            Text(options.firstOrNull{it.first==selected}?.second?:"Seleccionar motivo",modifier=Modifier.weight(1f),textAlign=TextAlign.Start)
            Icon(Icons.Default.ArrowDropDown,null)
        }
        DropdownMenu(expanded=expanded,onDismissRequest={onExpandedChange(false)}){
            options.forEach{(code,label)->DropdownMenuItem(text={Text(label)},onClick={onSelect(code)})}
        }
    }
}

@Composable
private fun SuccessScreen(title:String,subtitle:String,onBack:()->Unit){
    Page("Movimiento registrado"){
        Card(colors=CardDefaults.cardColors(containerColor=TealLight),modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){
            Column(Modifier.fillMaxWidth().padding(31.dp),horizontalAlignment=Alignment.CenterHorizontally){
                Icon(Icons.Default.CheckCircle,null,tint=Petroleum,modifier=Modifier.size(62.dp));Spacer(Modifier.height(13.dp))
                Text(title,fontSize=21.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                Spacer(Modifier.height(7.dp));Text(subtitle,textAlign=TextAlign.Center,color=Muted)
            }
        }
        Spacer(Modifier.height(27.dp));PrimaryAction("Volver a Mis productos",onBack)
    }
}

@Composable
private fun EmptyScreen(message:String,onBack:()->Unit){Page("Producto no disponible"){Text(message);Spacer(Modifier.height(13.dp));PrimaryAction("Volver",onBack)}}

@Composable
private fun ConfirmationDialog(title:String,message:String,confirm:String,onDismiss:()->Unit,onConfirm:()->Unit){
    AlertDialog(onDismissRequest=onDismiss,title={Text(title,fontWeight=FontWeight.Bold)},text={Text(message)},confirmButton={TextButton(onClick=onConfirm){Text(confirm)}},dismissButton={TextButton(onClick=onDismiss){Text("Cancelar")}})
}
