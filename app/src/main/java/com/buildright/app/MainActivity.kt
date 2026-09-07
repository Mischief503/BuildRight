package com.buildright.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BuildRightApp() }
    }
}

data class MaterialItem(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var quantity: Double = 1.0,
    var unit: String = "pcs",
    var unitPrice: Double = 0.0,
    var purchased: Boolean = false,
    var generated: Boolean = false,
    var retailer: String = "",
    var storeName: String = "",
    var sourceUrl: String = "",
    var priceCheckedAt: Long = 0L
) {
    val subtotal: Double get() = quantity * unitPrice
}

data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var completed: Boolean = false
)

data class ProjectOpening(
    val id: String = UUID.randomUUID().toString(),
    var type: String = "WINDOW",
    var wall: String = "Front",
    var offsetFt: Double = 1.0,
    var widthFt: Double = 3.0,
    var heightFt: Double = 3.0,
    var sillHeightFt: Double = 3.0,
    var label: String = ""
)

data class FieldMeasurementRecord(
    val id: String = UUID.randomUUID().toString(),
    var label: String = "",
    var value: Double = 0.0,
    var unit: String = "ft",
    var notes: String = "",
    var createdAt: Long = System.currentTimeMillis()
)

data class AttachmentRecord(
    val id: String = UUID.randomUUID().toString(),
    var kind: String = "PHOTO",
    var uri: String = "",
    var label: String = "",
    var createdAt: Long = System.currentTimeMillis()
)

data class PermitRecordData(
    val id: String = UUID.randomUUID().toString(),
    var permitType: String = "Building permit",
    var stage: String = "NOT_STARTED",
    var applicationNumber: String = "",
    var officialSourceUrl: String = "",
    var verified: Boolean = false,
    var notes: String = ""
)

data class CodeReferenceRecord(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "Official code source",
    var sourceUrl: String = "",
    var jurisdictionNote: String = "",
    var verified: Boolean = false,
    var notes: String = "",
    var checkedAt: Long = System.currentTimeMillis()
)

data class PriceHistoryRecord(
    val id: String = UUID.randomUUID().toString(),
    var materialId: String = "",
    var retailer: String = "",
    var store: String = "",
    var unitPrice: Double = 0.0,
    var sourceUrl: String = "",
    var checkedAt: Long = System.currentTimeMillis()
)

data class ProjectPriceQuote(
    val id: String = UUID.randomUUID().toString(),
    var materialId: String = "",
    var retailerId: String = "",
    var retailerName: String = "",
    var storeId: String = "",
    var storeName: String = "",
    var sku: String = "",
    var productName: String = "",
    var packageQuantity: Double = 1.0,
    var packageUnit: String = "pcs",
    var packagePrice: Double = 0.0,
    var currency: String = "USD",
    var sourceUrl: String = "",
    var checkedAt: Long = System.currentTimeMillis()
) {
    val normalizedUnitPrice: Double get() = if (packageQuantity > 0) packagePrice / packageQuantity else 0.0
}

data class Project(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "Untitled Project",
    var type: String = "Custom",
    var width: Double = 0.0,
    var length: Double = 0.0,
    var height: Double = 0.0,
    var notes: String = "",
    var studSpacingIn: Double = 16.0,
    var joistSpacingIn: Double = 16.0,
    var rafterSpacingIn: Double = 24.0,
    var roofPitchRise: Double = 4.0,
    var wastePercent: Double = 10.0,
    var doorWidthFt: Double = 3.0,
    var doorHeightFt: Double = 6.67,
    var windowCount: Int = 0,
    var windowWidthFt: Double = 3.0,
    var windowHeightFt: Double = 3.0,
    var foundationType: String = "Pier / Blocks",
    var foundationSpacingFt: Double = 4.0,
    var skidCount: Int = 3,
    var skidSize: String = "4×6",
    var slabThicknessIn: Double = 4.0,
    var floorJoistSize: String = "2×6",
    var studSize: String = "2×4",
    var rafterSize: String = "2×6",
    var ridgeBoardSize: String = "2×8",
    var roofStyle: String = "Gable",
    var eaveOverhangFt: Double = 1.0,
    var rakeOverhangFt: Double = 0.5,
    var stockLengthFt: Double = 12.0,
    var doorWall: String = "Front",
    var windowWall: String = "Left",
    var country: String = "United States",
    var stateOrProvince: String = "",
    var county: String = "",
    var city: String = "",
    val openings: MutableList<ProjectOpening> = mutableListOf(),
    val measurements: MutableList<FieldMeasurementRecord> = mutableListOf(),
    val attachments: MutableList<AttachmentRecord> = mutableListOf(),
    val permits: MutableList<PermitRecordData> = mutableListOf(),
    val codeReferences: MutableList<CodeReferenceRecord> = mutableListOf(),
    val priceHistory: MutableList<PriceHistoryRecord> = mutableListOf(),
    val priceQuotes: MutableList<ProjectPriceQuote> = mutableListOf(),
    val materials: MutableList<MaterialItem> = mutableListOf(),
    val tasks: MutableList<TaskItem> = mutableListOf()
) {
    val area: Double get() = width * length
    val perimeter: Double get() = if (width > 0 && length > 0) 2 * (width + length) else 0.0
    val budget: Double get() = materials.sumOf { it.subtotal }
    val progress: Int get() = if (tasks.isEmpty()) 0 else ((tasks.count { it.completed }.toDouble() / tasks.size) * 100).roundToInt()
}

class ProjectStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("buildright_store", Context.MODE_PRIVATE)

    fun load(): MutableList<Project> {
        val raw = prefs.getString("projects", null) ?: return mutableListOf()
        return runCatching {
            val arr = JSONArray(raw)
            MutableList(arr.length()) { i -> projectFromJson(arr.getJSONObject(i)) }
        }.getOrElse { mutableListOf() }
    }

    fun save(projects: List<Project>) {
        val arr = JSONArray()
        projects.forEach { arr.put(projectToJson(it)) }
        prefs.edit().putString("projects", arr.toString()).apply()
    }

    fun encodeProjects(projects: List<Project>): String {
        val arr = JSONArray()
        projects.forEach { arr.put(projectToJson(it)) }
        return arr.toString(2)
    }

    fun decodeProjects(raw: String): MutableList<Project> {
        val arr = JSONArray(raw)
        return MutableList(arr.length()) { i -> projectFromJson(arr.getJSONObject(i)) }
    }

    fun encodeProject(project: Project): String = projectToJson(project).toString(2)

    fun decodeProject(raw: String): Project = projectFromJson(JSONObject(raw))

    private fun projectToJson(p: Project): JSONObject = JSONObject().apply {
        put("id", p.id)
        put("name", p.name)
        put("type", p.type)
        put("width", p.width)
        put("length", p.length)
        put("height", p.height)
        put("notes", p.notes)
        put("studSpacingIn", p.studSpacingIn)
        put("joistSpacingIn", p.joistSpacingIn)
        put("rafterSpacingIn", p.rafterSpacingIn)
        put("roofPitchRise", p.roofPitchRise)
        put("wastePercent", p.wastePercent)
        put("doorWidthFt", p.doorWidthFt)
        put("doorHeightFt", p.doorHeightFt)
        put("windowCount", p.windowCount)
        put("windowWidthFt", p.windowWidthFt)
        put("windowHeightFt", p.windowHeightFt)
        put("foundationType", p.foundationType)
        put("foundationSpacingFt", p.foundationSpacingFt)
        put("skidCount", p.skidCount)
        put("skidSize", p.skidSize)
        put("slabThicknessIn", p.slabThicknessIn)
        put("floorJoistSize", p.floorJoistSize)
        put("studSize", p.studSize)
        put("rafterSize", p.rafterSize)
        put("ridgeBoardSize", p.ridgeBoardSize)
        put("roofStyle", p.roofStyle)
        put("eaveOverhangFt", p.eaveOverhangFt)
        put("rakeOverhangFt", p.rakeOverhangFt)
        put("stockLengthFt", p.stockLengthFt)
        put("doorWall", p.doorWall)
        put("windowWall", p.windowWall)
        put("country", p.country)
        put("stateOrProvince", p.stateOrProvince)
        put("county", p.county)
        put("city", p.city)
        put("openings", JSONArray().apply {
            p.openings.forEach { x -> put(JSONObject().apply {
                put("id", x.id); put("type", x.type); put("wall", x.wall); put("offsetFt", x.offsetFt)
                put("widthFt", x.widthFt); put("heightFt", x.heightFt); put("sillHeightFt", x.sillHeightFt); put("label", x.label)
            }) }
        })
        put("measurements", JSONArray().apply {
            p.measurements.forEach { x -> put(JSONObject().apply {
                put("id", x.id); put("label", x.label); put("value", x.value); put("unit", x.unit)
                put("notes", x.notes); put("createdAt", x.createdAt)
            }) }
        })
        put("attachments", JSONArray().apply {
            p.attachments.forEach { x -> put(JSONObject().apply {
                put("id", x.id); put("kind", x.kind); put("uri", x.uri); put("label", x.label); put("createdAt", x.createdAt)
            }) }
        })
        put("permits", JSONArray().apply {
            p.permits.forEach { x -> put(JSONObject().apply {
                put("id", x.id); put("permitType", x.permitType); put("stage", x.stage)
                put("applicationNumber", x.applicationNumber); put("officialSourceUrl", x.officialSourceUrl)
                put("verified", x.verified); put("notes", x.notes)
            }) }
        })
        put("codeReferences", JSONArray().apply {
            p.codeReferences.forEach { x -> put(JSONObject().apply {
                put("id", x.id); put("title", x.title); put("sourceUrl", x.sourceUrl)
                put("jurisdictionNote", x.jurisdictionNote); put("verified", x.verified)
                put("notes", x.notes); put("checkedAt", x.checkedAt)
            }) }
        })
        put("priceHistory", JSONArray().apply {
            p.priceHistory.forEach { x -> put(JSONObject().apply {
                put("id", x.id); put("materialId", x.materialId); put("retailer", x.retailer); put("store", x.store)
                put("unitPrice", x.unitPrice); put("sourceUrl", x.sourceUrl); put("checkedAt", x.checkedAt)
            }) }
        })
        put("priceQuotes", JSONArray().apply {
            p.priceQuotes.forEach { x -> put(JSONObject().apply {
                put("id", x.id); put("materialId", x.materialId); put("retailerId", x.retailerId); put("retailerName", x.retailerName)
                put("storeId", x.storeId); put("storeName", x.storeName); put("sku", x.sku); put("productName", x.productName)
                put("packageQuantity", x.packageQuantity); put("packageUnit", x.packageUnit); put("packagePrice", x.packagePrice)
                put("currency", x.currency); put("sourceUrl", x.sourceUrl); put("checkedAt", x.checkedAt)
            }) }
        })
        put("materials", JSONArray().apply {
            p.materials.forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id); put("name", m.name); put("quantity", m.quantity)
                    put("unit", m.unit); put("unitPrice", m.unitPrice); put("purchased", m.purchased); put("generated", m.generated)
                    put("retailer", m.retailer); put("storeName", m.storeName); put("sourceUrl", m.sourceUrl); put("priceCheckedAt", m.priceCheckedAt)
                })
            }
        })
        put("tasks", JSONArray().apply {
            p.tasks.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id); put("title", t.title); put("completed", t.completed)
                })
            }
        })
    }

    private fun projectFromJson(o: JSONObject): Project {
        val p = Project(
            id = o.optString("id", UUID.randomUUID().toString()),
            name = o.optString("name", "Untitled Project"),
            type = o.optString("type", "Custom"),
            width = o.optDouble("width", 0.0),
            length = o.optDouble("length", 0.0),
            height = o.optDouble("height", 0.0),
            notes = o.optString("notes", ""),
            studSpacingIn = o.optDouble("studSpacingIn", 16.0),
            joistSpacingIn = o.optDouble("joistSpacingIn", 16.0),
            rafterSpacingIn = o.optDouble("rafterSpacingIn", 24.0),
            roofPitchRise = o.optDouble("roofPitchRise", 4.0),
            wastePercent = o.optDouble("wastePercent", 10.0),
            doorWidthFt = o.optDouble("doorWidthFt", 3.0),
            doorHeightFt = o.optDouble("doorHeightFt", 6.67),
            windowCount = o.optInt("windowCount", 0),
            windowWidthFt = o.optDouble("windowWidthFt", 3.0),
            windowHeightFt = o.optDouble("windowHeightFt", 3.0),
            foundationType = o.optString("foundationType", "Pier / Blocks"),
            foundationSpacingFt = o.optDouble("foundationSpacingFt", 4.0),
            skidCount = o.optInt("skidCount", 3),
            skidSize = o.optString("skidSize", "4×6"),
            slabThicknessIn = o.optDouble("slabThicknessIn", 4.0),
            floorJoistSize = o.optString("floorJoistSize", "2×6"),
            studSize = o.optString("studSize", "2×4"),
            rafterSize = o.optString("rafterSize", "2×6"),
            ridgeBoardSize = o.optString("ridgeBoardSize", "2×8"),
            roofStyle = o.optString("roofStyle", "Gable"),
            eaveOverhangFt = o.optDouble("eaveOverhangFt", 1.0),
            rakeOverhangFt = o.optDouble("rakeOverhangFt", 0.5),
            stockLengthFt = o.optDouble("stockLengthFt", 12.0),
            doorWall = o.optString("doorWall", "Front"),
            windowWall = o.optString("windowWall", "Left"),
            country = o.optString("country", "United States"),
            stateOrProvince = o.optString("stateOrProvince", ""),
            county = o.optString("county", ""),
            city = o.optString("city", "")
        )
        val openings = o.optJSONArray("openings") ?: JSONArray()
        for (i in 0 until openings.length()) {
            val x = openings.getJSONObject(i)
            p.openings += ProjectOpening(
                id=x.optString("id", UUID.randomUUID().toString()), type=x.optString("type","WINDOW"),
                wall=x.optString("wall","Front"), offsetFt=x.optDouble("offsetFt",1.0),
                widthFt=x.optDouble("widthFt",3.0), heightFt=x.optDouble("heightFt",3.0),
                sillHeightFt=x.optDouble("sillHeightFt",3.0), label=x.optString("label","")
            )
        }
        val measurements = o.optJSONArray("measurements") ?: JSONArray()
        for (i in 0 until measurements.length()) {
            val x=measurements.getJSONObject(i)
            p.measurements += FieldMeasurementRecord(
                id=x.optString("id", UUID.randomUUID().toString()), label=x.optString("label",""),
                value=x.optDouble("value",0.0), unit=x.optString("unit","ft"), notes=x.optString("notes",""),
                createdAt=x.optLong("createdAt", System.currentTimeMillis())
            )
        }
        val attachments = o.optJSONArray("attachments") ?: JSONArray()
        for (i in 0 until attachments.length()) {
            val x=attachments.getJSONObject(i)
            p.attachments += AttachmentRecord(
                id=x.optString("id", UUID.randomUUID().toString()), kind=x.optString("kind","PHOTO"),
                uri=x.optString("uri",""), label=x.optString("label",""), createdAt=x.optLong("createdAt",System.currentTimeMillis())
            )
        }
        val permits = o.optJSONArray("permits") ?: JSONArray()
        for (i in 0 until permits.length()) {
            val x=permits.getJSONObject(i)
            p.permits += PermitRecordData(
                id=x.optString("id", UUID.randomUUID().toString()), permitType=x.optString("permitType","Building permit"),
                stage=x.optString("stage","NOT_STARTED"), applicationNumber=x.optString("applicationNumber",""),
                officialSourceUrl=x.optString("officialSourceUrl",""), verified=x.optBoolean("verified",false), notes=x.optString("notes","")
            )
        }
        val codeRefs = o.optJSONArray("codeReferences") ?: JSONArray()
        for (i in 0 until codeRefs.length()) {
            val x=codeRefs.getJSONObject(i)
            p.codeReferences += CodeReferenceRecord(
                id=x.optString("id", UUID.randomUUID().toString()), title=x.optString("title","Official code source"),
                sourceUrl=x.optString("sourceUrl",""), jurisdictionNote=x.optString("jurisdictionNote",""),
                verified=x.optBoolean("verified",false), notes=x.optString("notes",""),
                checkedAt=x.optLong("checkedAt",System.currentTimeMillis())
            )
        }
        val history = o.optJSONArray("priceHistory") ?: JSONArray()
        for (i in 0 until history.length()) {
            val x=history.getJSONObject(i)
            p.priceHistory += PriceHistoryRecord(
                id=x.optString("id", UUID.randomUUID().toString()), materialId=x.optString("materialId",""),
                retailer=x.optString("retailer",""), store=x.optString("store",""), unitPrice=x.optDouble("unitPrice",0.0),
                sourceUrl=x.optString("sourceUrl",""), checkedAt=x.optLong("checkedAt",System.currentTimeMillis())
            )
        }
        val quoteArr = o.optJSONArray("priceQuotes") ?: JSONArray()
        for (i in 0 until quoteArr.length()) {
            val x = quoteArr.getJSONObject(i)
            p.priceQuotes += ProjectPriceQuote(
                id=x.optString("id", UUID.randomUUID().toString()), materialId=x.optString("materialId",""),
                retailerId=x.optString("retailerId",""), retailerName=x.optString("retailerName",""),
                storeId=x.optString("storeId",""), storeName=x.optString("storeName",""), sku=x.optString("sku",""),
                productName=x.optString("productName",""), packageQuantity=x.optDouble("packageQuantity",1.0),
                packageUnit=x.optString("packageUnit","pcs"), packagePrice=x.optDouble("packagePrice",0.0),
                currency=x.optString("currency","USD"), sourceUrl=x.optString("sourceUrl",""), checkedAt=x.optLong("checkedAt",0L)
            )
        }
        if (p.openings.isEmpty()) {
            if (p.doorWidthFt > 0 && p.doorHeightFt > 0) {
                val wallLen = if (p.doorWall == "Front" || p.doorWall == "Rear") p.length else p.width
                p.openings += ProjectOpening(type="DOOR", wall=p.doorWall,
                    offsetFt=((wallLen-p.doorWidthFt)/2.0).coerceAtLeast(0.0), widthFt=p.doorWidthFt, heightFt=p.doorHeightFt,
                    sillHeightFt=0.0, label="Main door")
            }
            if (p.windowCount > 0) {
                val wallLen = if (p.windowWall == "Front" || p.windowWall == "Rear") p.length else p.width
                repeat(p.windowCount) { idx ->
                    val center = wallLen * (idx+1) / (p.windowCount+1)
                    p.openings += ProjectOpening(type="WINDOW", wall=p.windowWall,
                        offsetFt=(center-p.windowWidthFt/2.0).coerceAtLeast(0.0),
                        widthFt=p.windowWidthFt, heightFt=p.windowHeightFt, sillHeightFt=3.0, label="Window ${idx+1}")
                }
            }
        }
        val mats = o.optJSONArray("materials") ?: JSONArray()
        for (i in 0 until mats.length()) {
            val m = mats.getJSONObject(i)
            p.materials += MaterialItem(
                id = m.optString("id", UUID.randomUUID().toString()),
                name = m.optString("name", ""),
                quantity = m.optDouble("quantity", 1.0),
                unit = m.optString("unit", "pcs"),
                unitPrice = m.optDouble("unitPrice", 0.0),
                purchased = m.optBoolean("purchased", false),
                generated = m.optBoolean("generated", false),
                retailer = m.optString("retailer", ""),
                storeName = m.optString("storeName", ""),
                sourceUrl = m.optString("sourceUrl", ""),
                priceCheckedAt = m.optLong("priceCheckedAt", 0L)
            )
        }
        val tasks = o.optJSONArray("tasks") ?: JSONArray()
        for (i in 0 until tasks.length()) {
            val t = tasks.getJSONObject(i)
            p.tasks += TaskItem(
                id = t.optString("id", UUID.randomUUID().toString()),
                title = t.optString("title", ""),
                completed = t.optBoolean("completed", false)
            )
        }
        return p
    }
}

@Composable
fun BuildRightApp() {
    val context = LocalContext.current
    val store = remember { ProjectStore(context) }
    val projects = remember { mutableStateListOf<Project>().also { it.addAll(store.load()) } }

    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) BuildRightDarkColors else BuildRightLightColors
    ) {
        BuildRightRoot(
            projects = projects,
            onPersist = { store.save(projects) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    projects: List<Project>,
    onOpen: (Project) -> Unit,
    onCreate: () -> Unit,
    onDelete: (Project) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("BuildRight", fontWeight = FontWeight.Bold); Text("DIY Project Planner", style = MaterialTheme.typography.labelMedium) } },
                navigationIcon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.padding(start = 16.dp)) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onCreate, icon = { Icon(Icons.Default.Add, null) }, text = { Text("New project") })
        }
    ) { padding ->
        if (projects.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Start your first build", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Create a project to track dimensions, materials, tasks, notes and budget.")
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onCreate) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Create project") }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(projects, key = { _, p -> p.id }) { _, p ->
                    ElevatedCard(onClick = { onOpen(p) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(p.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                    Text(p.type, style = MaterialTheme.typography.labelLarge)
                                }
                                IconButton(onClick = { onDelete(p) }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("${fmt(p.width)} × ${fmt(p.length)} ft • ${fmt(p.area)} sq ft")
                            Text("Budget: $${money(p.budget)} • Progress: ${p.progress}%")
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(progress = { p.progress / 100f }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectEditor(project: Project, onBack: () -> Unit, onSave: () -> Unit) {
    var refresh by remember { mutableIntStateOf(0) }
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Build Engine", "Materials", "Tasks", "Notes")

    fun changed() { refresh++; onSave() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project.name, maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = { IconButton(onClick = onSave) { Icon(Icons.Default.Save, "Save") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
                tabs.forEachIndexed { index, title -> Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) }) }
            }
            when (tab) {
                0 -> OverviewTab(project, refresh, ::changed)
                1 -> BuildEngineTab(project, refresh, ::changed)
                2 -> MaterialsTab(project, refresh, ::changed)
                3 -> TasksTab(project, refresh, ::changed)
                else -> NotesTab(project, ::changed)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewTab(project: Project, refresh: Int, changed: () -> Unit) {
    var name by remember(project.id) { mutableStateOf(project.name) }
    var type by remember(project.id) { mutableStateOf(project.type) }
    var width by remember(project.id) { mutableStateOf(numberText(project.width)) }
    var length by remember(project.id) { mutableStateOf(numberText(project.length)) }
    var height by remember(project.id) { mutableStateOf(numberText(project.height)) }
    var typeMenu by remember { mutableStateOf(false) }
    val types = listOf("Shed", "Deck", "Fence", "Workbench", "Raised Bed", "Custom")

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            OutlinedTextField(name, { name = it; project.name = it; changed() }, label = { Text("Project name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        item {
            ExposedDropdownMenuBox(expanded = typeMenu, onExpandedChange = { typeMenu = !typeMenu }) {
                OutlinedTextField(value = type, onValueChange = {}, readOnly = true, label = { Text("Project type") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeMenu) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                    types.forEach { choice -> DropdownMenuItem(text = { Text(choice) }, onClick = {
                        type = choice; project.type = choice; applyTemplateDefaults(project); width = numberText(project.width); length = numberText(project.length); height = numberText(project.height); typeMenu = false; changed()
                    }) }
                }
            }
        }
        item { Text("Dimensions (feet)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Width", width, Modifier.weight(1f)) { width = it; project.width = it.toDoubleOrNull() ?: 0.0; changed() }
                NumberField("Length", length, Modifier.weight(1f)) { length = it; project.length = it.toDoubleOrNull() ?: 0.0; changed() }
                NumberField("Height", height, Modifier.weight(1f)) { height = it; project.height = it.toDoubleOrNull() ?: 0.0; changed() }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Calculated", fontWeight = FontWeight.Bold)
                    Text("Floor / footprint area: ${fmt(project.area)} sq ft")
                    Text("Perimeter: ${fmt(project.perimeter)} ft")
                    Text("Material budget: $${money(project.budget)}")
                    Text("Task progress: ${project.progress}%")
                    LinearProgressIndicator(progress = { project.progress / 100f }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item {
            AssistChip(onClick = {
                seedStarterPlan(project)
                changed()
            }, label = { Text("Add starter ${project.type.lowercase()} plan") })
        }
        item { Text("BuildRight v0.3 calculations are planning aids, not structural engineering or permit approval.", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
fun NumberField(label: String, value: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    OutlinedTextField(value, onValueChange, label = { Text(label) }, modifier = modifier, singleLine = true)
}


@Composable
fun BuildEngineTab(project: Project, refresh: Int, changed: () -> Unit) {
    if (project.type != "Shed") {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Card { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${project.type} engine is coming next", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("v0.3 is proving the deeper system against the reference shed first. Materials, tasks and notes still work for every project type.")
            } }
        }
        return
    }

    fun text(v: Double) = numberText(v)
    var stud by remember(project.id) { mutableStateOf(text(project.studSpacingIn)) }
    var joist by remember(project.id) { mutableStateOf(text(project.joistSpacingIn)) }
    var rafter by remember(project.id) { mutableStateOf(text(project.rafterSpacingIn)) }
    var pitch by remember(project.id) { mutableStateOf(text(project.roofPitchRise)) }
    var waste by remember(project.id) { mutableStateOf(text(project.wastePercent)) }
    var eave by remember(project.id) { mutableStateOf(text(project.eaveOverhangFt)) }
    var rake by remember(project.id) { mutableStateOf(text(project.rakeOverhangFt)) }
    var stock by remember(project.id) { mutableStateOf(text(project.stockLengthFt)) }
    var foundationSpacing by remember(project.id) { mutableStateOf(text(project.foundationSpacingFt)) }
    var skidCount by remember(project.id) { mutableStateOf(project.skidCount.toString()) }
    var slabThickness by remember(project.id) { mutableStateOf(text(project.slabThicknessIn)) }
    var doorW by remember(project.id) { mutableStateOf(text(project.doorWidthFt)) }
    var doorH by remember(project.id) { mutableStateOf(text(project.doorHeightFt)) }
    var windows by remember(project.id) { mutableStateOf(project.windowCount.toString()) }
    var windowW by remember(project.id) { mutableStateOf(text(project.windowWidthFt)) }
    var windowH by remember(project.id) { mutableStateOf(text(project.windowHeightFt)) }

    val result = BuildEngine.calculateShed(project)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Shed Builder v0.3", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Foundation, framing, roof geometry, wall openings, takeoff and cut planning recalculate as the design changes.")
        }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Foundation system", fontWeight = FontWeight.Bold)
            OutlinedTextField(project.foundationType, { project.foundationType = it; changed() }, label={Text("Type: Pier / Blocks, Skids, or Concrete Slab")}, modifier=Modifier.fillMaxWidth(), singleLine=true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Support spacing ft", foundationSpacing, Modifier.weight(1f)) { foundationSpacing=it; project.foundationSpacingFt=it.toDoubleOrNull()?:4.0; changed() }
                NumberField("Skid count", skidCount, Modifier.weight(1f)) { skidCount=it; project.skidCount=it.toIntOrNull()?:3; changed() }
                NumberField("Slab in", slabThickness, Modifier.weight(1f)) { slabThickness=it; project.slabThicknessIn=it.toDoubleOrNull()?:4.0; changed() }
            }
            OutlinedTextField(project.skidSize, {project.skidSize=it;changed()}, label={Text("Skid size")}, modifier=Modifier.fillMaxWidth(), singleLine=true)
        } } }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Framing system", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(project.floorJoistSize,{project.floorJoistSize=it;changed()},label={Text("Joist size")},modifier=Modifier.weight(1f),singleLine=true)
                OutlinedTextField(project.studSize,{project.studSize=it;changed()},label={Text("Stud size")},modifier=Modifier.weight(1f),singleLine=true)
                OutlinedTextField(project.rafterSize,{project.rafterSize=it;changed()},label={Text("Rafter size")},modifier=Modifier.weight(1f),singleLine=true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Stud O.C. in", stud, Modifier.weight(1f)) { stud=it; project.studSpacingIn=it.toDoubleOrNull()?:16.0; changed() }
                NumberField("Joist O.C. in", joist, Modifier.weight(1f)) { joist=it; project.joistSpacingIn=it.toDoubleOrNull()?:16.0; changed() }
                NumberField("Rafter O.C. in", rafter, Modifier.weight(1f)) { rafter=it; project.rafterSpacingIn=it.toDoubleOrNull()?:24.0; changed() }
            }
            NumberField("Preferred stock length ft", stock, Modifier.fillMaxWidth()) { stock=it; project.stockLengthFt=it.toDoubleOrNull()?:12.0; changed() }
        } } }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Roof system", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(project.roofStyle,{project.roofStyle=it;changed()},label={Text("Gable or Shed")},modifier=Modifier.weight(1f),singleLine=true)
                OutlinedTextField(project.ridgeBoardSize,{project.ridgeBoardSize=it;changed()},label={Text("Ridge size")},modifier=Modifier.weight(1f),singleLine=true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Rise / 12", pitch, Modifier.weight(1f)) { pitch=it; project.roofPitchRise=it.toDoubleOrNull()?:4.0; changed() }
                NumberField("Eave overhang ft", eave, Modifier.weight(1f)) { eave=it; project.eaveOverhangFt=it.toDoubleOrNull()?:0.0; changed() }
                NumberField("Rake overhang ft", rake, Modifier.weight(1f)) { rake=it; project.rakeOverhangFt=it.toDoubleOrNull()?:0.0; changed() }
            }
            NumberField("Waste %", waste, Modifier.fillMaxWidth()) { waste=it; project.wastePercent=it.toDoubleOrNull()?:10.0; changed() }
        } } }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Openings by wall", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(project.doorWall,{project.doorWall=it;changed()},label={Text("Door wall")},modifier=Modifier.weight(1f),singleLine=true)
                NumberField("Door W ft", doorW, Modifier.weight(1f)) {doorW=it;project.doorWidthFt=it.toDoubleOrNull()?:0.0;changed()}
                NumberField("Door H ft", doorH, Modifier.weight(1f)) {doorH=it;project.doorHeightFt=it.toDoubleOrNull()?:0.0;changed()}
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(project.windowWall,{project.windowWall=it;changed()},label={Text("Window wall")},modifier=Modifier.weight(1f),singleLine=true)
                NumberField("Windows", windows, Modifier.weight(1f)) {windows=it;project.windowCount=it.toIntOrNull()?:0;changed()}
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Window W ft", windowW, Modifier.weight(1f)) {windowW=it;project.windowWidthFt=it.toDoubleOrNull()?:0.0;changed()}
                NumberField("Window H ft", windowH, Modifier.weight(1f)) {windowH=it;project.windowHeightFt=it.toDoubleOrNull()?:0.0;changed()}
            }
        } } }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Calculated geometry", fontWeight = FontWeight.Bold)
            Text("Floor ${fmt(result.floorAreaSqFt)} sq ft • Net walls ${fmt(result.wallAreaNetSqFt)} sq ft • Roof ${fmt(result.roofAreaSqFt)} sq ft")
            Text("Foundation supports ${result.foundationSupportCount} • Joists ${result.floorJoistCount} • Studs ${result.wallStudCount} • Rafters ${result.rafterCount}")
            Text("Rafter geometric length ${fmt(result.rafterLengthFt)} ft" + if(result.ridgeLengthFt>0) " • Ridge ${fmt(result.ridgeLengthFt)} ft" else "")
        } } }
        item { Text("Wall-by-wall framing", style=MaterialTheme.typography.titleLarge, fontWeight=FontWeight.Bold) }
        itemsIndexed(result.walls) { _, wall -> ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
            Text("${wall.wall} wall • ${fmt(wall.lengthFt)} ft", fontWeight=FontWeight.SemiBold)
            Text("${wall.layoutStuds} layout studs + ${wall.extraOpeningStuds} opening studs • opening area ${fmt(wall.openingAreaSqFt)} sq ft", style=MaterialTheme.typography.bodySmall)
        } } }
        item { Row(verticalAlignment=Alignment.CenterVertically) {
            Text("Generated takeoff", style=MaterialTheme.typography.titleLarge, fontWeight=FontWeight.Bold, modifier=Modifier.weight(1f))
            Button(onClick={
                val existingPrices=project.materials.associateBy({it.name},{it.unitPrice})
                project.materials.removeAll{it.generated}
                result.lines.forEach{line->project.materials+=MaterialItem(name=line.name,quantity=line.quantity,unit=line.unit,unitPrice=existingPrices[line.name]?:0.0,generated=true)}
                changed()
            }) { Text("Sync") }
        } }
        itemsIndexed(result.lines) { _, line -> ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
            Text("${line.category} • ${line.name}", fontWeight=FontWeight.SemiBold); Text("${fmt(line.quantity)} ${line.unit}"); if(line.detail.isNotBlank()) Text(line.detail,style=MaterialTheme.typography.bodySmall)
        } } }
        item { Text("Cut plan", style=MaterialTheme.typography.titleLarge, fontWeight=FontWeight.Bold) }
        itemsIndexed(result.cutList) { _, cut -> ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
            Text("${cut.system} • ${cut.member}",fontWeight=FontWeight.SemiBold)
            Text("${cut.count} × ${fmt(cut.cutLengthFt)} ft")
            if(cut.note.isNotBlank()) Text(cut.note,style=MaterialTheme.typography.bodySmall)
        } } }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text("Verification notes",fontWeight=FontWeight.Bold); result.warnings.forEach{Text("• $it",style=MaterialTheme.typography.bodySmall)}
        } } }
    }
}

@Composable
fun MaterialsTab(project: Project, refresh: Int, changed: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("pcs") }
    var price by remember { mutableStateOf("") }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Materials & shopping", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Material") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("Qty", qty, Modifier.weight(1f)) { qty = it }
                        OutlinedTextField(unit, { unit = it }, label = { Text("Unit") }, modifier = Modifier.weight(1f), singleLine = true)
                        NumberField("Unit $", price, Modifier.weight(1f)) { price = it }
                    }
                    Button(onClick = {
                        if (name.isNotBlank()) {
                            project.materials += MaterialItem(name = name.trim(), quantity = qty.toDoubleOrNull() ?: 1.0, unit = unit.ifBlank { "pcs" }, unitPrice = price.toDoubleOrNull() ?: 0.0)
                            name = ""; qty = "1"; price = ""; changed()
                        }
                    }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Add material") }
                }
            }
        }
        item { Text("Estimated total: $${money(project.budget)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        itemsIndexed(project.materials, key = { _, m -> m.id }) { index, m ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = m.purchased, onCheckedChange = { m.purchased = it; changed() })
                    Column(Modifier.weight(1f)) {
                        Text(m.name + if (m.generated) "  • generated" else "", fontWeight = FontWeight.SemiBold)
                        Text("${fmt(m.quantity)} ${m.unit} × $${money(m.unitPrice)} = $${money(m.subtotal)}")
                    }
                    IconButton(onClick = { project.materials.removeAt(index); changed() }) { Icon(Icons.Default.Delete, "Delete") }
                }
            }
        }
    }
}

@Composable
fun TasksTab(project: Project, refresh: Int, changed: () -> Unit) {
    var title by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Build steps", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(title, { title = it }, label = { Text("New task") }, modifier = Modifier.weight(1f), singleLine = true)
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = { if (title.isNotBlank()) { project.tasks += TaskItem(title = title.trim()); title = ""; changed() } }) { Icon(Icons.Default.Add, "Add") }
            }
        }
        item {
            Text("${project.progress}% complete")
            LinearProgressIndicator(progress = { project.progress / 100f }, modifier = Modifier.fillMaxWidth())
        }
        itemsIndexed(project.tasks, key = { _, t -> t.id }) { index, t ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = t.completed, onCheckedChange = { t.completed = it; changed() })
                    Text(t.title, modifier = Modifier.weight(1f))
                    IconButton(onClick = { project.tasks.removeAt(index); changed() }) { Icon(Icons.Default.Delete, "Delete") }
                }
            }
        }
    }
}

@Composable
fun NotesTab(project: Project, changed: () -> Unit) {
    var notes by remember(project.id) { mutableStateOf(project.notes) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Project notes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it; project.notes = it; changed() },
            label = { Text("Measurements, ideas, reminders…") },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}

fun applyTemplateDefaults(p: Project) {
    when (p.type) {
        "Shed" -> { if (p.width == 0.0) p.width = 10.0; if (p.length == 0.0) p.length = 12.0; if (p.height == 0.0) p.height = 8.0 }
        "Deck" -> { if (p.width == 0.0) p.width = 12.0; if (p.length == 0.0) p.length = 16.0; if (p.height == 0.0) p.height = 2.0 }
        "Fence" -> { if (p.width == 0.0) p.width = 50.0; if (p.length == 0.0) p.length = 1.0; if (p.height == 0.0) p.height = 6.0 }
        "Workbench" -> { if (p.width == 0.0) p.width = 2.5; if (p.length == 0.0) p.length = 6.0; if (p.height == 0.0) p.height = 3.0 }
        "Raised Bed" -> { if (p.width == 0.0) p.width = 4.0; if (p.length == 0.0) p.length = 8.0; if (p.height == 0.0) p.height = 1.5 }
    }
}

fun seedStarterPlan(p: Project) {
    if (p.tasks.isNotEmpty() || p.materials.isNotEmpty()) return
    when (p.type) {
        "Shed" -> {
            p.tasks += listOf(TaskItem(title = "Confirm site and local requirements"), TaskItem(title = "Prepare and level foundation"), TaskItem(title = "Build floor frame"), TaskItem(title = "Frame walls"), TaskItem(title = "Frame roof"), TaskItem(title = "Install sheathing and weather layer"), TaskItem(title = "Install roofing, siding and trim"))
            val floorArea = p.area.coerceAtLeast(1.0)
            p.materials += listOf(MaterialItem(name = "Floor sheathing 4×8 sheets", quantity = kotlin.math.ceil(floorArea / 32.0), unit = "sheets"), MaterialItem(name = "Wall/roof framing lumber", quantity = kotlin.math.ceil(p.perimeter / 1.33) + 20, unit = "pcs"), MaterialItem(name = "Construction screws/nails", quantity = 1.0, unit = "box"))
        }
        "Deck" -> {
            p.tasks += listOf(TaskItem(title = "Confirm layout and local requirements"), TaskItem(title = "Set footing locations"), TaskItem(title = "Install posts and beams"), TaskItem(title = "Install joists"), TaskItem(title = "Install decking"), TaskItem(title = "Add stairs/rails as required"))
            p.materials += listOf(MaterialItem(name = "Decking boards", quantity = kotlin.math.ceil(p.area * 1.1), unit = "sq ft"), MaterialItem(name = "Exterior structural fasteners", quantity = 1.0, unit = "box"))
        }
        "Fence" -> {
            p.tasks += listOf(TaskItem(title = "Mark fence line"), TaskItem(title = "Locate utilities"), TaskItem(title = "Set posts"), TaskItem(title = "Install rails"), TaskItem(title = "Install pickets/panels"), TaskItem(title = "Install gate and finish"))
            p.materials += listOf(MaterialItem(name = "Fence posts", quantity = kotlin.math.ceil(p.width / 8.0) + 1, unit = "pcs"), MaterialItem(name = "Concrete mix", quantity = kotlin.math.ceil(p.width / 8.0) + 1, unit = "bags"))
        }
        else -> {
            p.tasks += listOf(TaskItem(title = "Finalize measurements"), TaskItem(title = "Create material list"), TaskItem(title = "Prepare work area"), TaskItem(title = "Build"), TaskItem(title = "Inspect and finish"))
        }
    }
}

fun fmt(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else "%.2f".format(v).trimEnd('0').trimEnd('.')
fun money(v: Double): String = "%.2f".format(v)
fun numberText(v: Double): String = if (v == 0.0) "" else fmt(v)
