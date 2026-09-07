package com.buildright.app

import androidx.compose.animation.AnimatedContent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalContext
import java.text.DateFormat
import java.util.Date
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.min
import com.buildright.cost.MaterialRequirement
import com.buildright.cost.PriceComparisonEngine
import com.buildright.cost.PriceQuote
import com.buildright.cost.ProductPageImportResult
import com.buildright.cost.ProductPageImporter
import com.buildright.cost.RetailerIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val BuildRightLightColors = lightColorScheme(
    primary = Color(0xFF155EEF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F0FF),
    onPrimaryContainer = Color(0xFF0B2E67),
    secondary = Color(0xFF1E7A5C),
    tertiary = Color(0xFFB54708),
    background = Color(0xFFF6F8FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFEEF2F7),
    outlineVariant = Color(0xFFD8DEE8)
)

val BuildRightDarkColors = darkColorScheme(
    primary = Color(0xFF8DB4FF),
    onPrimary = Color(0xFF002B64),
    primaryContainer = Color(0xFF123F82),
    onPrimaryContainer = Color(0xFFD8E5FF),
    secondary = Color(0xFF71D7B1),
    tertiary = Color(0xFFFFB77B),
    background = Color(0xFF0F141B),
    surface = Color(0xFF171D26),
    surfaceVariant = Color(0xFF222A35),
    outlineVariant = Color(0xFF354052)
)

private enum class RootScreen { DASHBOARD, PROJECT, SETTINGS }
private enum class ProjectScreen {
    OVERVIEW, DESIGNER, BUILD, MATERIALS, FIELD, COSTS, CODE, ASSISTANT, TASKS, NOTES
}

private data class NavItem(val screen: ProjectScreen, val label: String, val icon: ImageVector)

@Composable
fun BuildRightRoot(
    projects: MutableList<Project>,
    onPersist: () -> Unit
) {
    var root by remember { mutableStateOf(RootScreen.DASHBOARD) }
    var selectedProjectId by remember { mutableStateOf<String?>(null) }
    var projectScreen by remember { mutableStateOf(ProjectScreen.OVERVIEW) }
    var uiRevision by remember { mutableIntStateOf(0) }
    uiRevision

    val selected = projects.firstOrNull { it.id == selectedProjectId }
    val persistAndRefresh = { uiRevision++; onPersist() }

    BackHandler(enabled = root != RootScreen.DASHBOARD) { root = RootScreen.DASHBOARD }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AnimatedContent(targetState = root, label = "root") { screen ->
            when (screen) {
                RootScreen.DASHBOARD -> BuildRightDashboard(
                    projects = projects,
                    onOpenProject = {
                        selectedProjectId = it.id
                        projectScreen = ProjectScreen.OVERVIEW
                        root = RootScreen.PROJECT
                    },
                    onNewProject = { type ->
                        val p = Project(name = "New $type", type = type)
                        applyTemplateDefaults(p)
                        if (type == "Shed") seedStarterPlan(p)
                        projects.add(0, p)
                        onPersist()
                        selectedProjectId = p.id
                        projectScreen = ProjectScreen.OVERVIEW
                        root = RootScreen.PROJECT
                    },
                    onSettings = { root = RootScreen.SETTINGS },
                    onDeleteProject = { doomed ->
                        projects.remove(doomed)
                        if (selectedProjectId == doomed.id) selectedProjectId = null
                        persistAndRefresh()
                    }
                )
                RootScreen.PROJECT -> {
                    if (selected == null) {
                        root = RootScreen.DASHBOARD
                    } else {
                        ProjectWorkspace(
                            project = selected,
                            current = projectScreen,
                            onNavigate = { projectScreen = it },
                            onBack = { root = RootScreen.DASHBOARD },
                            onChanged = persistAndRefresh
                        )
                    }
                }
                RootScreen.SETTINGS -> SettingsScreen(
                    projects = projects,
                    onReplaceProjects = { imported -> projects.clear(); projects.addAll(imported); persistAndRefresh() },
                    onBack = { root = RootScreen.DASHBOARD }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuildRightDashboard(
    projects: List<Project>,
    onOpenProject: (Project) -> Unit,
    onNewProject: (String) -> Unit,
    onSettings: () -> Unit,
    onDeleteProject: (Project) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandMark()
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("BuildRight", fontWeight = FontWeight.Black)
                            Text("Plan it. Price it. Build it.", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(Modifier.padding(22.dp)) {
                        Text("Your builds, under control.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(8.dp))
                        Text("Design, frame, budget, shop, verify permits and work from the field — all from one project.")
                        Spacer(Modifier.height(18.dp))
                        Button(onClick = { onNewProject("Shed") }) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Start a build")
                        }
                    }
                }
            }

            item {
                Text("Quick start", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickTemplate("Shed", Icons.Outlined.HomeWork) { onNewProject("Shed") }
                    QuickTemplate("Deck", Icons.Outlined.Deck) { onNewProject("Deck") }
                    QuickTemplate("Fence", Icons.Outlined.ViewDay) { onNewProject("Fence") }
                    QuickTemplate("Workbench", Icons.Outlined.Handyman) { onNewProject("Workbench") }
                    QuickTemplate("Custom", Icons.Outlined.Draw) { onNewProject("Custom") }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Projects", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${projects.size} total", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (projects.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Construction,
                        title = "No projects yet",
                        message = "Start with a shed, deck, fence or custom build."
                    )
                }
            } else {
                items(projects, key = { it.id }) { p ->
                    ProjectCard(p, onOpen = { onOpenProject(p) }, onDelete = { onDeleteProject(p) })
                }
            }
        }
    }
}

@Composable
private fun BrandMark() {
    Box(
        Modifier.size(38.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Construction, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun QuickTemplate(label: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.width(120.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(
                Modifier.size(38.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }
            Text(label, fontWeight = FontWeight.SemiBold)
            Text("New project", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProjectCard(project: Project, onOpen: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(onClick = onOpen, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.HomeWork, null, tint = MaterialTheme.colorScheme.primary) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(project.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${project.type} • ${fmt(project.width)} × ${fmt(project.length)} ft", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete project") }
                Icon(Icons.Default.ChevronRight, null)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricPill("${fmt(project.area)} sq ft", Icons.Outlined.SquareFoot, Modifier.weight(1f))
                MetricPill("$${money(project.budget)}", Icons.Outlined.Payments, Modifier.weight(1f))
                MetricPill("${project.progress}%", Icons.Outlined.TaskAlt, Modifier.weight(1f))
            }

            LinearProgressIndicator(
                progress = { project.progress / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun MetricPill(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectWorkspace(
    project: Project,
    current: ProjectScreen,
    onNavigate: (ProjectScreen) -> Unit,
    onBack: () -> Unit,
    onChanged: () -> Unit
) {
    val primaryNav = listOf(
        NavItem(ProjectScreen.OVERVIEW, "Home", Icons.Outlined.Dashboard),
        NavItem(ProjectScreen.DESIGNER, "Design", Icons.Outlined.Draw),
        NavItem(ProjectScreen.BUILD, "Build", Icons.Outlined.Construction),
        NavItem(ProjectScreen.FIELD, "Field", Icons.Outlined.Checklist),
        NavItem(ProjectScreen.ASSISTANT, "Ask", Icons.Outlined.AutoAwesome)
    )

    var showMore by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Projects") } },
                title = {
                    Column {
                        Text(project.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                        Text("${project.type} • ${fmt(project.width)} × ${fmt(project.length)} ft", style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    IconButton(onClick = { showMore = true }) { Icon(Icons.Default.MoreVert, "More") }
                    DropdownMenu(expanded = showMore, onDismissRequest = { showMore = false }) {
                        listOf(
                            ProjectScreen.MATERIALS to "Materials",
                            ProjectScreen.COSTS to "Costs",
                            ProjectScreen.CODE to "Code & permits",
                            ProjectScreen.TASKS to "Tasks",
                            ProjectScreen.NOTES to "Notes"
                        ).forEach { (screen, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { showMore = false; onNavigate(screen) })
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                primaryNav.forEach { item ->
                    NavigationBarItem(
                        selected = current == item.screen,
                        onClick = { onNavigate(item.screen) },
                        icon = { Icon(item.icon, null) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = current,
            label = "project-screen",
            modifier = Modifier.fillMaxSize().padding(padding)
        ) { screen ->
            when (screen) {
                ProjectScreen.OVERVIEW -> OverviewDashboard(project, onNavigate, onChanged)
                ProjectScreen.DESIGNER -> DesignerScreen(project, onChanged)
                ProjectScreen.BUILD -> BuildCenterScreen(project, onChanged)
                ProjectScreen.MATERIALS -> MaterialsScreenModern(project, onChanged)
                ProjectScreen.FIELD -> FieldModeScreen(project, onChanged)
                ProjectScreen.COSTS -> CostScreen(project, onChanged)
                ProjectScreen.CODE -> CodePermitScreen(project, onChanged)
                ProjectScreen.ASSISTANT -> AssistantScreen(project, onNavigate, onChanged)
                ProjectScreen.TASKS -> TasksScreenModern(project, onChanged)
                ProjectScreen.NOTES -> NotesScreenModern(project, onChanged)
            }
        }
    }
}

@Composable
private fun OverviewDashboard(project: Project, onNavigate: (ProjectScreen) -> Unit, onChanged: () -> Unit) {
    var name by remember(project.id) { mutableStateOf(project.name) }
    var width by remember(project.id) { mutableStateOf(fmt(project.width)) }
    var length by remember(project.id) { mutableStateOf(fmt(project.length)) }
    var height by remember(project.id) { mutableStateOf(fmt(project.height)) }
    var revision by remember { mutableIntStateOf(0) }
    revision

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Build status", style = MaterialTheme.typography.labelLarge)
                    Text(if (project.progress == 100) "Build complete" else "You're ${project.progress}% through the plan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    LinearProgressIndicator(progress = { project.progress / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp))
                    project.tasks.firstOrNull { !it.completed }?.let { Text("Next: ${it.title}", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
        item {
            ElevatedCard(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Project details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; project.name = it; revision++; onChanged() },
                        label = { Text("Project name") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumericProjectField("Width (ft)", width, { v -> width = v; v.toDoubleOrNull()?.takeIf { it > 0 }?.let { project.width = it; revision++; onChanged() } }, Modifier.weight(1f))
                        NumericProjectField("Length (ft)", length, { v -> length = v; v.toDoubleOrNull()?.takeIf { it > 0 }?.let { project.length = it; revision++; onChanged() } }, Modifier.weight(1f))
                    }
                    NumericProjectField("Wall height (ft)", height, { v -> height = v; v.toDoubleOrNull()?.takeIf { it > 0 }?.let { project.height = it; revision++; onChanged() } }, Modifier.fillMaxWidth())
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigMetric("Area", "${fmt(project.area)} ft²", Icons.Outlined.SquareFoot, Modifier.weight(1f))
                BigMetric("Budget", "$${money(project.budget)}", Icons.Outlined.AccountBalanceWallet, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigMetric("Materials", "${project.materials.count { !it.purchased }} left", Icons.Outlined.Inventory2, Modifier.weight(1f))
                BigMetric("Steps", "${project.tasks.count { it.completed }}/${project.tasks.size}", Icons.Outlined.CheckCircle, Modifier.weight(1f))
            }
        }
        item { SectionHeader("Project tools", "Everything stays connected to this project.") }
        item {
            ToolGrid(listOf(
                Triple("Visual designer", Icons.Outlined.Draw, ProjectScreen.DESIGNER),
                Triple("Build engine", Icons.Outlined.Construction, ProjectScreen.BUILD),
                Triple("Materials", Icons.Outlined.Inventory2, ProjectScreen.MATERIALS),
                Triple("Costs", Icons.Outlined.Payments, ProjectScreen.COSTS),
                Triple("Code & permits", Icons.Outlined.Gavel, ProjectScreen.CODE),
                Triple("Field mode", Icons.Outlined.Checklist, ProjectScreen.FIELD)
            ), onNavigate)
        }
        item {
            OutlinedButton(onClick = { onNavigate(ProjectScreen.ASSISTANT) }, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Icon(Icons.Outlined.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Ask BuildRight about this project")
            }
        }
    }
}

@Composable
private fun BigMetric(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    ElevatedCard(modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ToolGrid(items: List<Triple<String, ImageVector, ProjectScreen>>, onNavigate: (ProjectScreen) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (name, icon, screen) ->
                    Surface(
                        Modifier.weight(1f).clickable { onNavigate(screen) },
                        shape = RoundedCornerShape(20.dp),
                        tonalElevation = 1.dp
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(10.dp))
                            Text(name, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DesignerScreen(project: Project, onChanged: () -> Unit) {
    var selectedView by remember { mutableStateOf("Floor") }
    var width by remember(project.id) { mutableStateOf(fmt(project.width)) }
    var length by remember(project.id) { mutableStateOf(fmt(project.length)) }
    var height by remember(project.id) { mutableStateOf(fmt(project.height)) }
    var selectedOpeningId by remember { mutableStateOf<String?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    revision
    val views = listOf("Floor", "Front", "Rear", "Left", "Right", "Roof")
    val wallOpenings = project.openings.filter { it.wall == selectedView }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SectionHeader("Visual Designer", "Drag doors and windows directly on wall views. Every edit is saved to the project.") }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                views.forEach { FilterChip(selected = selectedView == it, onClick = { selectedView = it; selectedOpeningId = null }, label = { Text(it) }) }
            }
        }
        item {
            ElevatedCard(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$selectedView view", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (selectedView !in listOf("Floor","Roof")) Text("Drag openings left/right", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.height(10.dp))
                    ProjectPlanCanvas(
                        project = project,
                        view = selectedView,
                        selectedOpeningId = selectedOpeningId,
                        onOpeningSelected = { selectedOpeningId = it },
                        onOpeningMoved = { id, offset ->
                            project.openings.firstOrNull { it.id == id }?.let { it.offsetFt = offset }
                            ProjectCommandEngine.syncLegacyOpenings(project)
                            revision++; onChanged()
                        },
                        modifier = Modifier.fillMaxWidth().height(310.dp)
                    )
                }
            }
        }
        item {
            ElevatedCard(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Project geometry", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumericProjectField("Width ft", width, { v -> width=v; v.toDoubleOrNull()?.takeIf{it>0}?.let{project.width=it; revision++; onChanged()} }, Modifier.weight(1f))
                        NumericProjectField("Length ft", length, { v -> length=v; v.toDoubleOrNull()?.takeIf{it>0}?.let{project.length=it; revision++; onChanged()} }, Modifier.weight(1f))
                    }
                    NumericProjectField("Height ft", height, { v -> height=v; v.toDoubleOrNull()?.takeIf{it>0}?.let{project.height=it; revision++; onChanged()} }, Modifier.fillMaxWidth())
                }
            }
        }
        if (selectedView !in listOf("Floor","Roof")) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val wallLen = if(selectedView=="Front" || selectedView=="Rear") project.length else project.width
                        val w = project.windowWidthFt.coerceAtLeast(1.0)
                        project.openings += ProjectOpening(type="WINDOW", wall=selectedView, offsetFt=((wallLen-w)/2).coerceAtLeast(0.0), widthFt=w, heightFt=project.windowHeightFt.coerceAtLeast(1.0), sillHeightFt=3.0, label="Window ${project.openings.count{it.type=="WINDOW"}+1}")
                        ProjectCommandEngine.syncLegacyOpenings(project); revision++; onChanged()
                    }, modifier=Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Window, null); Spacer(Modifier.width(6.dp)); Text("Add window")
                    }
                    OutlinedButton(onClick = {
                        val wallLen = if(selectedView=="Front" || selectedView=="Rear") project.length else project.width
                        val w = project.doorWidthFt.coerceAtLeast(1.0)
                        project.openings += ProjectOpening(type="DOOR", wall=selectedView, offsetFt=((wallLen-w)/2).coerceAtLeast(0.0), widthFt=w, heightFt=project.doorHeightFt.coerceAtLeast(1.0), sillHeightFt=0.0, label="Door")
                        ProjectCommandEngine.syncLegacyOpenings(project); revision++; onChanged()
                    }, modifier=Modifier.weight(1f)) {
                        Icon(Icons.Outlined.DoorFront, null); Spacer(Modifier.width(6.dp)); Text("Add door")
                    }
                }
            }
            if (wallOpenings.isNotEmpty()) {
                item { SectionHeader("Openings on $selectedView", "Tap a card to select it on the drawing.") }
                items(wallOpenings, key={it.id}) { opening ->
                    OpeningEditorCard(
                        project=project,
                        opening=opening,
                        selected=selectedOpeningId==opening.id,
                        onSelect={selectedOpeningId=opening.id},
                        onChanged={ProjectCommandEngine.syncLegacyOpenings(project); revision++; onChanged()},
                        onDelete={project.openings.remove(opening); ProjectCommandEngine.syncLegacyOpenings(project); selectedOpeningId=null; revision++; onChanged()}
                    )
                }
            }
        }
        item { InfoBanner("Designer openings are now persisted individually and are the source used by the newer framing engine.", Icons.Outlined.AccountTree) }
    }
}

@Composable
private fun ProjectPlanCanvas(
    project: Project,
    view: String,
    selectedOpeningId: String?,
    onOpeningSelected: (String?) -> Unit,
    onOpeningMoved: (String, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val grid = MaterialTheme.colorScheme.outlineVariant
    val line = MaterialTheme.colorScheme.primary
    val fill = MaterialTheme.colorScheme.primaryContainer
    val accent = MaterialTheme.colorScheme.tertiary
    val selectedColor = MaterialTheme.colorScheme.secondary
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragStartOffset by remember { mutableStateOf(0.0) }

    fun wallLength(): Double = if(view=="Front" || view=="Rear") project.length else project.width

    val gestureModifier = if (view !in listOf("Floor","Roof")) {
        Modifier
            .onSizeChanged { canvasSize = it }
            .pointerInput(view, project.openings.size, selectedOpeningId, canvasSize) {
                detectDragGestures(
                    onDragStart = { point ->
                        val w = canvasSize.width.toFloat().coerceAtLeast(1f)
                        val h = canvasSize.height.toFloat().coerceAtLeast(1f)
                        val left=w*.12f; val right=w*.88f
                        val top=h*.26f; val bottom=h*.82f
                        val wallLen=wallLength().coerceAtLeast(.1)
                        val hit = project.openings.filter{it.wall==view}.lastOrNull { o ->
                            val ox = left + ((o.offsetFt/wallLen).toFloat()*(right-left))
                            val ow = ((o.widthFt/wallLen).toFloat()*(right-left)).coerceAtLeast(18f)
                            val oh = if(o.type=="DOOR") (bottom-top)*.72f else (bottom-top)*.32f
                            val oy = if(o.type=="DOOR") bottom-oh else bottom - ((o.sillHeightFt+o.heightFt)/project.height.coerceAtLeast(.1)).toFloat()*(bottom-top)
                            point.x in ox..(ox+ow) && point.y in oy..(oy+oh)
                        }
                        draggingId = hit?.id
                        dragStartOffset = hit?.offsetFt ?: 0.0
                        onOpeningSelected(hit?.id)
                    },
                    onDrag = { change, dragAmount ->
                        val id=draggingId ?: return@detectDragGestures
                        change.consume()
                        val o=project.openings.firstOrNull{it.id==id} ?: return@detectDragGestures
                        val usable=(canvasSize.width*.76f).coerceAtLeast(1f)
                        val deltaFt=dragAmount.x/usable*wallLength().toFloat()
                        dragStartOffset += deltaFt
                        val max=(wallLength()-o.widthFt).coerceAtLeast(0.0)
                        val next=dragStartOffset.coerceIn(0.0,max)
                        onOpeningMoved(id,next)
                    },
                    onDragEnd = { draggingId=null },
                    onDragCancel = { draggingId=null }
                )
            }
    } else Modifier.onSizeChanged { canvasSize=it }

    Canvas(modifier.then(gestureModifier).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))) {
        val step = size.minDimension / 16f
        var x = 0f
        while (x < size.width) { drawLine(grid, Offset(x, 0f), Offset(x, size.height), 1f); x += step }
        var y = 0f
        while (y < size.height) { drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f); y += step }

        val left = size.width * .12f
        val top = size.height * .14f
        val right = size.width * .88f
        val bottom = size.height * .82f

        when (view) {
            "Floor" -> {
                drawRect(fill, Offset(left, top), Size(right-left, bottom-top))
                drawRect(line, Offset(left, top), Size(right-left, bottom-top), style = Stroke(5f))
                val joists = maxOf(2, ceil(project.length * 12 / project.joistSpacingIn).toInt())
                repeat(joists) { i ->
                    val yy = top + (bottom-top) * (i+1) / (joists+1)
                    drawLine(line.copy(alpha=.45f), Offset(left, yy), Offset(right, yy), 2f)
                }
            }
            "Roof" -> {
                drawRect(fill, Offset(left, top), Size(right-left, bottom-top))
                drawRect(line, Offset(left, top), Size(right-left, bottom-top), style = Stroke(5f))
                if(project.roofStyle.equals("Gable",true)) drawLine(accent, Offset((left+right)/2, top), Offset((left+right)/2, bottom), 5f)
            }
            else -> {
                val wallTop = top + size.height * .12f
                val wallBottom = bottom
                drawRect(fill, Offset(left, wallTop), Size(right-left, wallBottom-wallTop))
                drawRect(line, Offset(left, wallTop), Size(right-left, wallBottom-wallTop), style = Stroke(5f))
                if (project.roofStyle.equals("Gable", true) && (view == "Front" || view == "Rear")) {
                    val roof = Path().apply { moveTo(left, wallTop); lineTo((left+right)/2, top); lineTo(right, wallTop) }
                    drawPath(roof, line, style = Stroke(5f))
                }
                val wallLen=wallLength().coerceAtLeast(.1)
                project.openings.filter{it.wall==view}.forEach { o ->
                    val ox=left + (o.offsetFt/wallLen).toFloat()*(right-left)
                    val ow=((o.widthFt/wallLen).toFloat()*(right-left)).coerceAtLeast(18f)
                    val wallPx=wallBottom-wallTop
                    val oh=(o.heightFt/project.height.coerceAtLeast(.1)).toFloat()*wallPx
                    val oy=wallBottom - ((o.sillHeightFt+o.heightFt)/project.height.coerceAtLeast(.1)).toFloat()*wallPx
                    val c=if(o.id==selectedOpeningId) selectedColor else accent
                    drawRect(c, Offset(ox,oy), Size(ow,oh.coerceAtLeast(20f)), style=Stroke(if(o.id==selectedOpeningId) 7f else 4f))
                }
            }
        }
    }
}

@Composable
private fun OpeningEditorCard(
    project: Project,
    opening: ProjectOpening,
    selected: Boolean,
    onSelect: () -> Unit,
    onChanged: () -> Unit,
    onDelete: () -> Unit
) {
    var label by remember(opening.id){ mutableStateOf(opening.label) }
    var offset by remember(opening.id){ mutableStateOf(fmt(opening.offsetFt)) }
    var width by remember(opening.id){ mutableStateOf(fmt(opening.widthFt)) }
    var height by remember(opening.id){ mutableStateOf(fmt(opening.heightFt)) }
    var sill by remember(opening.id){ mutableStateOf(fmt(opening.sillHeightFt)) }
    ElevatedCard(
        onClick=onSelect,
        shape=RoundedCornerShape(18.dp),
        colors=CardDefaults.elevatedCardColors(containerColor=if(selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text(opening.type, fontWeight=FontWeight.Black)
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(label,{label=it; opening.label=it; onChanged()},label={Text("Label")},modifier=Modifier.weight(1f),singleLine=true)
                IconButton(onClick=onDelete){Icon(Icons.Default.Delete,"Delete opening")}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                NumericProjectField("Offset ft",offset,{v->offset=v;v.toDoubleOrNull()?.let{n->
                    val wall=if(opening.wall=="Front"||opening.wall=="Rear") project.length else project.width
                    opening.offsetFt=n.coerceIn(0.0,(wall-opening.widthFt).coerceAtLeast(0.0)); onChanged()
                }},Modifier.weight(1f))
                NumericProjectField("Width ft",width,{v->width=v;v.toDoubleOrNull()?.takeIf{it>0}?.let{opening.widthFt=it;onChanged()}},Modifier.weight(1f))
                NumericProjectField("Height ft",height,{v->height=v;v.toDoubleOrNull()?.takeIf{it>0}?.let{opening.heightFt=it;onChanged()}},Modifier.weight(1f))
            }
            if(opening.type=="WINDOW") NumericProjectField("Sill height ft",sill,{v->sill=v;v.toDoubleOrNull()?.takeIf{it>=0}?.let{opening.sillHeightFt=it;onChanged()}},Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun EditValueCard(label: String, value: String, icon: ImageVector, modifier: Modifier, onPlus: () -> Unit) {
    ElevatedCard(modifier, shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                Text(value, fontWeight = FontWeight.Bold)
            }
            FilledIconButton(onClick = onPlus, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Add, null) }
        }
    }
}

@Composable
private fun BuildCenterScreen(project: Project, onChanged: () -> Unit) {
    var stud by remember(project.id) { mutableStateOf(fmt(project.studSpacingIn)) }
    var joist by remember(project.id) { mutableStateOf(fmt(project.joistSpacingIn)) }
    var rafter by remember(project.id) { mutableStateOf(fmt(project.rafterSpacingIn)) }
    var pitch by remember(project.id) { mutableStateOf(fmt(project.roofPitchRise)) }
    var waste by remember(project.id) { mutableStateOf(fmt(project.wastePercent)) }
    var foundation by remember(project.id) { mutableStateOf(project.foundationType) }
    var roofStyle by remember(project.id) { mutableStateOf(project.roofStyle) }
    var revision by remember { mutableIntStateOf(0) }
    revision
    val result = remember(project.width, project.length, project.height, project.studSpacingIn, project.joistSpacingIn, project.rafterSpacingIn, project.roofPitchRise, project.windowCount, revision) {
        if (project.type == "Shed") BuildEngine.calculateShed(project) else null
    }
    val framingPlan = remember(project.width, project.length, project.height, project.studSpacingIn, project.windowCount, project.doorWall, project.windowWall, revision) {
        if (project.type == "Shed") ProjectIntegration.framing(project) else null
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionHeader("Build Center", "Edit framing inputs, then regenerate takeoff.") }
        item {
            ElevatedCard(shape=RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text("Framing settings", fontWeight=FontWeight.Bold)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        NumericProjectField("Stud O.C. in", stud, {v->stud=v;v.toDoubleOrNull()?.takeIf{it>0}?.let{project.studSpacingIn=it;revision++;onChanged()}}, Modifier.weight(1f))
                        NumericProjectField("Joist O.C. in", joist, {v->joist=v;v.toDoubleOrNull()?.takeIf{it>0}?.let{project.joistSpacingIn=it;revision++;onChanged()}}, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        NumericProjectField("Rafter O.C. in", rafter, {v->rafter=v;v.toDoubleOrNull()?.takeIf{it>0}?.let{project.rafterSpacingIn=it;revision++;onChanged()}}, Modifier.weight(1f))
                        NumericProjectField("Roof pitch rise", pitch, {v->pitch=v;v.toDoubleOrNull()?.takeIf{it>=0}?.let{project.roofPitchRise=it;revision++;onChanged()}}, Modifier.weight(1f))
                    }
                    NumericProjectField("Waste %", waste, {v->waste=v;v.toDoubleOrNull()?.takeIf{it>=0}?.let{project.wastePercent=it;revision++;onChanged()}}, Modifier.fillMaxWidth())
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        ChoiceProjectField("Foundation", foundation, listOf("Pier / Blocks","Skids","Concrete slab"), {foundation=it;project.foundationType=it;revision++;onChanged()}, Modifier.weight(1f))
                        ChoiceProjectField("Roof style", roofStyle, listOf("Gable","Shed"), {roofStyle=it;project.roofStyle=it;revision++;onChanged()}, Modifier.weight(1f))
                    }
                }
            }
        }
        if (result == null) {
            item { EmptyState(Icons.Outlined.Engineering, "Engine not ready for ${project.type}", "The deeper framing engine is currently built around sheds.") }
        } else {
            item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigMetric("Wall pieces", "${framingPlan?.members?.sumOf { it.quantity } ?: result.wallStudCount}", Icons.Outlined.ViewWeek, Modifier.weight(1f))
                BigMetric("Joists", "${result.floorJoistCount}", Icons.Outlined.Reorder, Modifier.weight(1f))
            } }
            if (framingPlan != null && framingPlan.warnings.isNotEmpty()) {
                item { InfoBanner("Framing engine: ${framingPlan.warnings.joinToString(" • ") { it.message }}", Icons.Outlined.WarningAmber) }
            }
            item {
                Button(onClick = {
                    val existing = project.materials.associateBy({ it.name }, { it.unitPrice })
                    project.materials.removeAll { it.generated }
                    result.lines.forEach { project.materials += MaterialItem(name=it.name, quantity=it.quantity, unit=it.unit, unitPrice=existing[it.name] ?: 0.0, generated=true) }
                    revision++; onChanged()
                }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Icon(Icons.Outlined.Sync, null); Spacer(Modifier.width(8.dp)); Text("Sync takeoff to materials") }
            }
            item { SectionHeader("Cut list", "${result.cutList.size} cut groups generated") }
            items(result.cutList.take(12)) { cut -> ListItem(headlineContent = { Text(cut.member, fontWeight = FontWeight.SemiBold) }, supportingContent = { Text(cut.note.ifBlank { cut.system }) }, leadingContent = { Text("${cut.count}×", fontWeight=FontWeight.Bold) }, trailingContent = { Text("${fmt(cut.cutLengthFt)} ft") }) }
            item { InfoBanner("Structural member sizing and connection adequacy still require validated prescriptive data or engineering.", Icons.Outlined.WarningAmber) }
        }
    }
}

@Composable
private fun MaterialsScreenModern(project: Project, onChanged: () -> Unit) {
    var newName by remember { mutableStateOf("") }
    var newQty by remember { mutableStateOf("1") }
    var newUnit by remember { mutableStateOf("pcs") }
    var newPrice by remember { mutableStateOf("0") }
    var revision by remember { mutableIntStateOf(0) }
    revision
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Materials", "${project.materials.count { it.purchased }}/${project.materials.size} purchased") }
        item {
            ElevatedCard(shape=RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("Add material", fontWeight=FontWeight.Bold)
                    OutlinedTextField(newName,{newName=it},label={Text("Material name")},modifier=Modifier.fillMaxWidth(),singleLine=true)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        NumericProjectField("Qty",newQty,{newQty=it},Modifier.weight(1f))
                        OutlinedTextField(newUnit,{newUnit=it},label={Text("Unit")},modifier=Modifier.weight(1f),singleLine=true)
                        NumericProjectField("Unit price",newPrice,{newPrice=it},Modifier.weight(1f))
                    }
                    Button(onClick={
                        val qty=newQty.toDoubleOrNull(); val price=newPrice.toDoubleOrNull()
                        if(newName.isNotBlank() && qty!=null && qty>0 && price!=null && price>=0){
                            project.materials += MaterialItem(name=newName.trim(),quantity=qty,unit=newUnit.ifBlank{"pcs"},unitPrice=price)
                            newName="";newQty="1";newPrice="0";revision++;onChanged()
                        }
                    },modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Add,null);Spacer(Modifier.width(6.dp));Text("Add material")}
                }
            }
        }
        items(project.materials, key={it.id}) { m -> MaterialEditableCard(m, {revision++;onChanged()}, {project.materials.remove(m);revision++;onChanged()}) }
        if(project.materials.isEmpty()) item { EmptyState(Icons.Outlined.Inventory2,"No materials yet","Generate a takeoff or add a material above.") }
    }
}

@Composable
private fun FieldModeScreen(project: Project, onChanged: () -> Unit) {
    val context = LocalContext.current
    var newTask by remember { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    var showMeasure by remember { mutableStateOf(false) }
    var measureLabel by remember { mutableStateOf("") }
    var measureValue by remember { mutableStateOf("") }
    var measureUnit by remember { mutableStateOf("ft") }
    var measureNotes by remember { mutableStateOf("") }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingReceiptUri by remember { mutableStateOf<Uri?>(null) }
    revision

    fun newCaptureUri(prefix: String): Uri {
        val dir = File(context.filesDir, "field_captures").apply { mkdirs() }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingPhotoUri
        if (ok && uri != null) {
            project.attachments += AttachmentRecord(kind="PHOTO", uri=uri.toString(), label="Field photo")
            revision++; onChanged()
        }
        pendingPhotoUri = null
    }
    val receiptLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingReceiptUri
        if (ok && uri != null) {
            project.attachments += AttachmentRecord(kind="RECEIPT", uri=uri.toString(), label="Receipt")
            revision++; onChanged()
        }
        pendingReceiptUri = null
    }

    val next = project.tasks.firstOrNull { !it.completed }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("FIELD MODE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp)); Text(next?.title ?: "All planned steps complete", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    if(next!=null){Spacer(Modifier.height(14.dp));Button(onClick={next.completed=true;revision++;onChanged()},modifier=Modifier.fillMaxWidth().height(56.dp)){Icon(Icons.Default.Check,null);Spacer(Modifier.width(8.dp));Text("Mark step complete")}}
                }
            }
        }
        item {
            Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(newTask,{newTask=it},label={Text("Add field/build step")},modifier=Modifier.weight(1f),singleLine=true)
                FilledIconButton(onClick={if(newTask.isNotBlank()){project.tasks+=TaskItem(title=newTask.trim());newTask="";revision++;onChanged()}}){Icon(Icons.Default.Add,"Add")}
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldAction("Photo", Icons.Outlined.PhotoCamera, Modifier.weight(1f)) { val uri=newCaptureUri("photo"); pendingPhotoUri=uri; photoLauncher.launch(uri) }
                FieldAction("Measure", Icons.Outlined.Straighten, Modifier.weight(1f)) { showMeasure=true }
                FieldAction("Receipt", Icons.Outlined.ReceiptLong, Modifier.weight(1f)) { val uri=newCaptureUri("receipt"); pendingReceiptUri=uri; receiptLauncher.launch(uri) }
            }
        }
        if (project.measurements.isNotEmpty()) {
            item { SectionHeader("Measurements", "${project.measurements.size} saved") }
            items(project.measurements, key={it.id}) { x ->
                ElevatedCard(shape=RoundedCornerShape(18.dp)) { Row(Modifier.padding(14.dp), verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(x.label.ifBlank { "Measurement" }, fontWeight=FontWeight.SemiBold); Text("${fmt(x.value)} ${x.unit}${if(x.notes.isBlank()) "" else " • ${x.notes}"}", style=MaterialTheme.typography.bodySmall) }
                    IconButton(onClick={project.measurements.remove(x);revision++;onChanged()}) { Icon(Icons.Outlined.Delete,"Delete measurement") }
                } }
            }
        }
        if (project.attachments.isNotEmpty()) {
            item { SectionHeader("Field attachments", "${project.attachments.size} saved") }
            items(project.attachments, key={it.id}) { a ->
                ElevatedCard(shape=RoundedCornerShape(18.dp)) { Row(Modifier.padding(14.dp), verticalAlignment=Alignment.CenterVertically) {
                    Icon(if(a.kind=="RECEIPT") Icons.Outlined.ReceiptLong else Icons.Outlined.Photo, null)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) { Text(a.label.ifBlank { a.kind.lowercase().replaceFirstChar{it.uppercase()} },fontWeight=FontWeight.SemiBold); Text(DateFormat.getDateTimeInstance().format(Date(a.createdAt)), style=MaterialTheme.typography.bodySmall) }
                    TextButton(onClick={ runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a.uri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) } }) { Text("Open") }
                    IconButton(onClick={project.attachments.remove(a);revision++;onChanged()}) { Icon(Icons.Outlined.Delete,"Delete attachment") }
                } }
            }
        }
        item { SectionHeader("Build checklist", "${project.progress}% complete") }
        items(project.tasks, key={it.id}) { task -> ElevatedCard(shape=RoundedCornerShape(18.dp)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Checkbox(task.completed,{task.completed=it;revision++;onChanged()});Spacer(Modifier.width(6.dp));Text(task.title,Modifier.weight(1f),fontWeight=FontWeight.SemiBold)}} }
    }

    if (showMeasure) {
        AlertDialog(
            onDismissRequest={showMeasure=false},
            title={Text("Save measurement")},
            text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedTextField(measureLabel,{measureLabel=it},label={Text("Label")},singleLine=true)
                OutlinedTextField(measureValue,{measureValue=it.filter{c->c.isDigit()||c=='.'}},label={Text("Value")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
                OutlinedTextField(measureUnit,{measureUnit=it},label={Text("Unit")},singleLine=true)
                OutlinedTextField(measureNotes,{measureNotes=it},label={Text("Notes")})
            }},
            confirmButton={Button(enabled=measureValue.toDoubleOrNull()!=null,onClick={
                project.measurements += FieldMeasurementRecord(label=measureLabel.trim(), value=measureValue.toDoubleOrNull()?:0.0, unit=measureUnit.trim().ifBlank{"ft"}, notes=measureNotes.trim())
                measureLabel="";measureValue="";measureUnit="ft";measureNotes="";showMeasure=false;revision++;onChanged()
            }){Text("Save")}},
            dismissButton={TextButton(onClick={showMeasure=false}){Text("Cancel")}}
        )
    }
}

@Composable
private fun FieldAction(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ElevatedCard(onClick=onClick, modifier=modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(vertical=18.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint=MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(label, fontWeight=FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CostScreen(project: Project, onChanged: () -> Unit) {
    var revision by remember { mutableIntStateOf(0) }
    revision
    val scope = rememberCoroutineScope()
    var selectedMaterialId by remember(project.id, project.materials.size) { mutableStateOf(project.materials.firstOrNull()?.id ?: "") }
    var productUrl by remember { mutableStateOf("") }
    var importStatus by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }

    val purchased = project.materials.filter { it.purchased }.sumOf { it.subtotal }
    val remaining = project.budget - purchased
    val requirements = project.materials.map { MaterialRequirement(it.id, it.name, it.quantity, it.unit) }
    val savedQuotes = project.priceQuotes.mapNotNull { q ->
        if (q.packageQuantity <= 0 || q.packagePrice < 0) null else PriceQuote(
            requirementKey=q.materialId, retailerId=q.retailerId, storeId=q.storeId.ifBlank { null }, sku=q.sku.ifBlank { null },
            productName=q.productName, packageQuantity=q.packageQuantity, packageUnit=q.packageUnit, packagePrice=q.packagePrice,
            currency=q.currency, checkedAt=q.checkedAt, sourceReference=q.sourceUrl.ifBlank { null }
        )
    }
    val comparison = if (requirements.isNotEmpty() && savedQuotes.isNotEmpty()) PriceComparisonEngine.compare(requirements, savedQuotes) else null

    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(16.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { SectionHeader("Cost Intelligence", "Live-ready universal pricing, package normalization and store comparison.") }
        item { Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=RoundedCornerShape(26.dp)){Column(Modifier.padding(20.dp)){Text("Estimated materials");Text("$${money(project.budget)}",style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Black);Spacer(Modifier.height(12.dp));Row{Column(Modifier.weight(1f)){Text("Purchased");Text("$${money(purchased)}",fontWeight=FontWeight.Bold)};Column(Modifier.weight(1f)){Text("Remaining");Text("$${money(remaining)}",fontWeight=FontWeight.Bold)}}}} }

        if (project.materials.isNotEmpty()) item {
            ElevatedCard(shape=RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text("Universal price importer", fontWeight=FontWeight.Black, style=MaterialTheme.typography.titleMedium)
                    Text("Choose a material, paste a retailer product-page URL, and BuildRight will read structured product/price data when automated access is allowed. It never bypasses a blocked source.", style=MaterialTheme.typography.bodySmall)
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick={expanded=true}, modifier=Modifier.fillMaxWidth()) {
                            val selected=project.materials.firstOrNull{it.id==selectedMaterialId}
                            Text(selected?.name ?: "Select material", maxLines=1, overflow=TextOverflow.Ellipsis)
                            Spacer(Modifier.weight(1f)); Icon(Icons.Default.ArrowDropDown,null)
                        }
                        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                            project.materials.forEach { m -> DropdownMenuItem(text={Text(m.name)},onClick={selectedMaterialId=m.id;expanded=false}) }
                        }
                    }
                    OutlinedTextField(productUrl,{productUrl=it},label={Text("Product page URL")},modifier=Modifier.fillMaxWidth(),singleLine=true)
                    Button(
                        enabled=!importing && selectedMaterialId.isNotBlank() && productUrl.startsWith("http"),
                        onClick={
                            val material=project.materials.firstOrNull{it.id==selectedMaterialId} ?: return@Button
                            importing=true; importStatus="Checking source and reading product page…"
                            scope.launch {
                                val result=withContext(Dispatchers.IO){ ProductPageImporter.fetch(productUrl.trim(), respectRobots=true) }
                                when(result){
                                    is ProductPageImportResult.Success -> {
                                        val p=result.product
                                        val normalized=if(p.packageQuantity>0) p.price/p.packageQuantity else p.price
                                        material.unitPrice=normalized
                                        material.retailer=RetailerIdentity.fromUrl(p.sourceUrl).second
                                        material.storeName="Web"
                                        material.sourceUrl=p.sourceUrl
                                        material.priceCheckedAt=p.checkedAt
                                        project.priceQuotes.removeAll{it.materialId==material.id && it.retailerId==p.retailerId && it.sku==p.sku.orEmpty()}
                                        project.priceQuotes += ProjectPriceQuote(
                                            materialId=material.id, retailerId=p.retailerId, retailerName=RetailerIdentity.fromUrl(p.sourceUrl).second,
                                            sku=p.sku.orEmpty(), productName=p.productName, packageQuantity=p.packageQuantity, packageUnit=p.packageUnit,
                                            packagePrice=p.price, currency=p.currency, sourceUrl=p.sourceUrl, checkedAt=p.checkedAt
                                        )
                                        project.priceHistory += PriceHistoryRecord(materialId=material.id,retailer=material.retailer,store=material.storeName,unitPrice=normalized,sourceUrl=p.sourceUrl,checkedAt=p.checkedAt)
                                        importStatus="Imported ${p.productName}: $${money(p.price)} for ${fmt(p.packageQuantity)} ${p.packageUnit} ($${money(normalized)} normalized)."
                                        productUrl=""; revision++; onChanged()
                                    }
                                    is ProductPageImportResult.Blocked -> importStatus="Source blocked automated access: ${result.reason}. Use manual price entry or an authorized feed/API."
                                    is ProductPageImportResult.Failure -> importStatus="Could not import price: ${result.reason}"
                                }
                                importing=false
                            }
                        }, modifier=Modifier.fillMaxWidth()
                    ){ if(importing) CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp) else Icon(Icons.Outlined.TravelExplore,null); Spacer(Modifier.width(8.dp)); Text(if(importing) "Reading…" else "Import current price") }
                    importStatus?.let { Text(it, style=MaterialTheme.typography.bodySmall) }
                }
            }
        }

        if (comparison != null) item {
            ElevatedCard(shape=RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("Best purchase plans", fontWeight=FontWeight.Black)
                    Text("Cheapest split order: $${money(comparison.cheapestSplit.total)} across ${comparison.cheapestSplit.retailerCount} source(s)")
                    Text("Fewest-store plan: $${money(comparison.fewestStores.total)} across ${comparison.fewestStores.retailerCount} source(s)")
                    comparison.cheapestCompleteSingleStore?.let { Text("Best complete single source: ${it.retailerId} — $${money(it.plan.total)}") }
                    if (comparison.splitSavingsVsSingleStore > 0.005) Text("Split-order savings: $${money(comparison.splitSavingsVsSingleStore)}", color=MaterialTheme.colorScheme.primary, fontWeight=FontWeight.Bold)
                    comparison.byRetailer.take(5).forEach { r ->
                        Text("${r.retailerId}: $${money(r.plan.total)} • ${r.coverageCount}/${r.requirementCount} materials", style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item { SectionHeader("Material prices", "Manual prices still work offline; imported quotes retain source and timestamp.") }
        items(project.materials, key={it.id}) { m ->
            Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                MaterialPriceCard(m,{revision++;onChanged()})
                if(m.sourceUrl.isNotBlank()) {
                    Text("${m.retailer.ifBlank{"Web source"}} • checked ${DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(m.priceCheckedAt))}", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant, modifier=Modifier.padding(horizontal=8.dp))
                }
            }
        }
        if(project.materials.isEmpty()) item { EmptyState(Icons.Outlined.Payments,"No priced materials","Add materials first, then enter or import prices here.") }
        item { InfoBanner("Provider architecture is now universal. Authorized APIs/feeds and permitted HTML search profiles can feed the same comparison engine; blocked retailer automation is not bypassed.", Icons.Outlined.Storefront) }
    }
}

@Composable
private fun CodePermitScreen(project: Project, onChanged: () -> Unit) {
    val context=LocalContext.current
    var country by remember(project.id){ mutableStateOf(project.country) }
    var state by remember(project.id){ mutableStateOf(project.stateOrProvince) }
    var county by remember(project.id){ mutableStateOf(project.county) }
    var city by remember(project.id){ mutableStateOf(project.city) }
    var showPermit by remember { mutableStateOf(false) }
    var editingPermitId by remember { mutableStateOf<String?>(null) }
    var permitType by remember { mutableStateOf("Building permit") }
    var permitStage by remember { mutableStateOf("NOT_STARTED") }
    var permitNumber by remember { mutableStateOf("") }
    var permitUrl by remember { mutableStateOf("") }
    var permitNotes by remember { mutableStateOf("") }
    var permitVerified by remember { mutableStateOf(false) }
    var showCodeRef by remember { mutableStateOf(false) }
    var codeTitle by remember { mutableStateOf("") }
    var codeUrl by remember { mutableStateOf("") }
    var codeNote by remember { mutableStateOf("") }

    fun openUrl(url:String){ if(url.startsWith("http")) runCatching{context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))} }
    fun editPermit(p:PermitRecordData){ editingPermitId=p.id;permitType=p.permitType;permitStage=p.stage;permitNumber=p.applicationNumber;permitUrl=p.officialSourceUrl;permitNotes=p.notes;permitVerified=p.verified;showPermit=true }
    fun newPermit(){ editingPermitId=null;permitType="Building permit";permitStage="NOT_STARTED";permitNumber="";permitUrl="";permitNotes="";permitVerified=false;showPermit=true }

    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(16.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { SectionHeader("Code & Permits", "Track jurisdiction, official sources and permit progress without treating unverified rules as fact.") }
        item {
            ElevatedCard(shape=RoundedCornerShape(22.dp)) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Jurisdiction",fontWeight=FontWeight.Bold)
                OutlinedTextField(country,{country=it;project.country=it;onChanged()},label={Text("Country")},modifier=Modifier.fillMaxWidth(),singleLine=true)
                OutlinedTextField(state,{state=it;project.stateOrProvince=it;onChanged()},label={Text("State / Province")},modifier=Modifier.fillMaxWidth(),singleLine=true)
                OutlinedTextField(county,{county=it;project.county=it;onChanged()},label={Text("County")},modifier=Modifier.fillMaxWidth(),singleLine=true)
                OutlinedTextField(city,{city=it;project.city=it;onChanged()},label={Text("City")},modifier=Modifier.fillMaxWidth(),singleLine=true)
            } }
        }
        item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={newPermit()},modifier=Modifier.weight(1f)){Icon(Icons.Default.Add,null);Spacer(Modifier.width(6.dp));Text("Permit") }
            OutlinedButton(onClick={showCodeRef=true},modifier=Modifier.weight(1f)){Icon(Icons.Outlined.Verified,null);Spacer(Modifier.width(6.dp));Text("Code source") }
        } }
        if(project.permits.isEmpty()) item { EmptyState(Icons.Outlined.Description,"No permits tracked","Add a permit to track application number, status and official source.") }
        items(project.permits,key={it.id}) { p ->
            ElevatedCard(onClick={editPermit(p)},shape=RoundedCornerShape(18.dp)) { Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){
                Icon(Icons.Outlined.Description,null,tint=if(p.verified) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(p.permitType,fontWeight=FontWeight.SemiBold);Text("${p.stage.replace('_',' ')}${if(p.applicationNumber.isBlank())"" else " • #${p.applicationNumber}"}",style=MaterialTheme.typography.bodySmall)}
                if(p.officialSourceUrl.isNotBlank()) IconButton(onClick={openUrl(p.officialSourceUrl)}){Icon(Icons.Outlined.OpenInNew,"Open official source")}
            } }
        }
        if(project.codeReferences.isNotEmpty()) {
            item { SectionHeader("Official / verified references", "${project.codeReferences.count{it.verified}} verified by you") }
            items(project.codeReferences,key={it.id}) { r -> ElevatedCard(shape=RoundedCornerShape(18.dp)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                Icon(if(r.verified) Icons.Outlined.Verified else Icons.Outlined.Link,null,tint=if(r.verified) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary);Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)){Text(r.title,fontWeight=FontWeight.SemiBold);Text(r.jurisdictionNote.ifBlank{r.sourceUrl},style=MaterialTheme.typography.bodySmall,maxLines=2,overflow=TextOverflow.Ellipsis)}
                IconButton(onClick={openUrl(r.sourceUrl)}){Icon(Icons.Outlined.OpenInNew,"Open source")};IconButton(onClick={project.codeReferences.remove(r);onChanged()}){Icon(Icons.Outlined.Delete,"Delete") }
            }} }
        }
        item { InfoBanner("BuildRight records sources and your verification status. It does not certify structural adequacy or substitute for the authority having jurisdiction.",Icons.Outlined.Verified) }
    }

    if(showPermit){ AlertDialog(onDismissRequest={showPermit=false},title={Text(if(editingPermitId==null)"Add permit" else "Edit permit")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        OutlinedTextField(permitType,{permitType=it},label={Text("Permit type")},singleLine=true)
        OutlinedTextField(permitStage,{permitStage=it.uppercase().replace(' ','_')},label={Text("Stage (NOT_STARTED / APPLIED / APPROVED / CLOSED)")},singleLine=true)
        OutlinedTextField(permitNumber,{permitNumber=it},label={Text("Application / permit number")},singleLine=true)
        OutlinedTextField(permitUrl,{permitUrl=it},label={Text("Official source URL")},singleLine=true)
        OutlinedTextField(permitNotes,{permitNotes=it},label={Text("Notes")})
        Row(verticalAlignment=Alignment.CenterVertically){Checkbox(permitVerified,{permitVerified=it});Text("I verified this against the official source") }
    }},confirmButton={Button(enabled=permitType.isNotBlank(),onClick={
        val existing=editingPermitId?.let{id->project.permits.firstOrNull{it.id==id}}
        if(existing==null) project.permits+=PermitRecordData(permitType=permitType.trim(),stage=permitStage.ifBlank{"NOT_STARTED"},applicationNumber=permitNumber.trim(),officialSourceUrl=permitUrl.trim(),verified=permitVerified,notes=permitNotes.trim())
        else { existing.permitType=permitType.trim();existing.stage=permitStage.ifBlank{"NOT_STARTED"};existing.applicationNumber=permitNumber.trim();existing.officialSourceUrl=permitUrl.trim();existing.verified=permitVerified;existing.notes=permitNotes.trim() }
        showPermit=false;onChanged()
    }){Text("Save")}},dismissButton={Row{if(editingPermitId!=null) TextButton(onClick={project.permits.removeAll{it.id==editingPermitId};showPermit=false;onChanged()}){Text("Delete")};TextButton(onClick={showPermit=false}){Text("Cancel")}}}) }

    if(showCodeRef){ AlertDialog(onDismissRequest={showCodeRef=false},title={Text("Add code / official source")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        OutlinedTextField(codeTitle,{codeTitle=it},label={Text("Reference title")},singleLine=true)
        OutlinedTextField(codeUrl,{codeUrl=it},label={Text("Official URL")},singleLine=true)
        OutlinedTextField(codeNote,{codeNote=it},label={Text("Jurisdiction / notes")})
    }},confirmButton={Button(enabled=codeTitle.isNotBlank()&&codeUrl.startsWith("http"),onClick={project.codeReferences+=CodeReferenceRecord(title=codeTitle.trim(),sourceUrl=codeUrl.trim(),jurisdictionNote=codeNote.trim(),verified=true);codeTitle="";codeUrl="";codeNote="";showCodeRef=false;onChanged()}){Text("Save verified source")}},dismissButton={TextButton(onClick={showCodeRef=false}){Text("Cancel")}}) }
}

@Composable
private fun AssistantScreen(project: Project, onNavigate: (ProjectScreen)->Unit, onChanged: () -> Unit) {
    var draft by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf<String?>(null) }
    var pendingEdit by remember { mutableStateOf<ProposedProjectEdit?>(null) }
    var editResult by remember { mutableStateOf<String?>(null) }
    val suggestions = listOf("What’s my next step?", "What do I still need to buy?", "Set width to 12", "Add a window on left")
    val snapshot = remember(project.id, project.name, project.width, project.length, project.height, project.materials.size, project.tasks.size, submitted, editResult) { ProjectIntegration.assistantSnapshot(project) }

    fun answer(question: String): String {
        return when (com.buildright.assistant.IntentParser.classify(question)) {
            com.buildright.assistant.AssistantIntent.NEXT_STEP -> snapshot.nextStepTitle?.let { "Your next planned step is: $it" } ?: "All current planned steps are complete."
            com.buildright.assistant.AssistantIntent.MISSING_MATERIALS -> "${snapshot.unpurchasedMaterialCount} material item(s) still need purchasing."
            com.buildright.assistant.AssistantIntent.COST_STATUS -> com.buildright.assistant.ProjectExplainer.summary(snapshot)
            com.buildright.assistant.AssistantIntent.WARNINGS -> (snapshot.framingWarnings + snapshot.codeWarnings).joinToString("\n• ", prefix = "• ")
            com.buildright.assistant.AssistantIntent.PROJECT_SUMMARY -> com.buildright.assistant.ProjectExplainer.summary(snapshot)
            else -> "I can answer project status questions or propose safe project edits such as dimensions and openings. Edits require your confirmation before saved data changes."
        }
    }

    fun submit(question:String){
        submitted=question
        editResult=null
        pendingEdit=ProjectCommandEngine.propose(question,project)
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding=PaddingValues(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item { Row(verticalAlignment=Alignment.CenterVertically) { Box(Modifier.size(46.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment=Alignment.Center) { Icon(Icons.Outlined.AutoAwesome, null, tint=MaterialTheme.colorScheme.primary) };Spacer(Modifier.width(12.dp));Column { Text("BuildRight Assistant", fontWeight=FontWeight.Black, style=MaterialTheme.typography.titleLarge); Text("Grounded in ${project.name}") } } }
            item { AssistantBubble("I can explain this project and propose deterministic edits. Any edit is shown to you before it changes saved project data.", false) }
            item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement=Arrangement.spacedBy(8.dp)) { suggestions.forEach { q -> SuggestionChip(onClick={ draft=q }, label={Text(q)}) } } }
            submitted?.let { q ->
                item { AssistantBubble(q, true) }
                val edit=pendingEdit
                if(edit!=null){
                    item { ElevatedCard(shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Proposed project edit",fontWeight=FontWeight.Black);Text(edit.summary);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={editResult=ProjectCommandEngine.execute(edit.command,project);pendingEdit=null;onChanged()}){Text("Confirm edit")};OutlinedButton(onClick={pendingEdit=null;editResult="Edit cancelled."}){Text("Cancel")}}}} }
                } else item { AssistantBubble(answer(q), false) }
                editResult?.let { result -> item { AssistantBubble(result,false) } }
                if (q.contains("verification", true)) item { TextButton(onClick={onNavigate(ProjectScreen.CODE)}) { Text("Open Code & Permits") } }
            }
        }
        Surface(tonalElevation=3.dp) { Row(Modifier.padding(12.dp), verticalAlignment=Alignment.CenterVertically) {
            OutlinedTextField(draft, {draft=it}, placeholder={Text("Ask or propose an edit…")}, modifier=Modifier.weight(1f), maxLines=3)
            Spacer(Modifier.width(8.dp));FilledIconButton(enabled=draft.isNotBlank(), onClick={ val q=draft.trim();draft="";submit(q) }) { Icon(Icons.Default.ArrowUpward, "Send") }
        } }
    }
}

@Composable
private fun AssistantBubble(message: String, user: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if(user) Arrangement.End else Arrangement.Start) {
        Surface(
            modifier=Modifier.fillMaxWidth(.86f),
            color=if(user) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor=if(user) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            shape=RoundedCornerShape(20.dp)
        ) { Text(message, Modifier.padding(14.dp)) }
    }
}

@Composable
private fun TasksScreenModern(project: Project, onChanged: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(16.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item { SectionHeader("Tasks", "${project.progress}% complete") }
        items(project.tasks, key={it.id}) { t ->
            ElevatedCard(shape=RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(t.completed, {t.completed=it; onChanged()})
                    Text(t.title, Modifier.weight(1f), fontWeight=FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun NotesScreenModern(project: Project, onChanged: () -> Unit) {
    var notes by remember(project.id) { mutableStateOf(project.notes) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        SectionHeader("Project Notes", "Measurements, reminders and field observations.")
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value=notes,
            onValueChange={notes=it; project.notes=it; onChanged()},
            placeholder={Text("Write anything important about this build…")},
            modifier=Modifier.fillMaxWidth().weight(1f),
            shape=RoundedCornerShape(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(projects: MutableList<Project>, onReplaceProjects:(List<Project>)->Unit, onBack:()->Unit) {
    val context=LocalContext.current
    val store=remember{ProjectStore(context)}
    var status by remember{mutableStateOf<String?>(null)}
    var pendingImport by remember{mutableStateOf<List<Project>?>(null)}

    val exportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
        if(uri!=null) runCatching{(context.contentResolver.openOutputStream(uri) ?: error("Could not open destination")).bufferedWriter().use{it.write(store.encodeProjects(projects))}}.onSuccess{status="Backup exported successfully."}.onFailure{status="Export failed: ${it.message}"}
    }
    val importLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null) runCatching{context.contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()} ?: error("Could not read file")}.mapCatching{store.decodeProjects(it)}.onSuccess{pendingImport=it}.onFailure{status="Import failed: ${it.message}"}
    }

    Scaffold(topBar={TopAppBar(title={Text("Settings", fontWeight=FontWeight.Bold)}, navigationIcon={IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Back")}})}) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding=PaddingValues(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item { SectionHeader("BuildRight", "Local project storage, backups and release-safe preferences") }
            item { ElevatedCard(shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.CloudOff,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(12.dp));Column{Text("Offline-first",fontWeight=FontWeight.SemiBold);Text("Project data is stored locally on this device",style=MaterialTheme.typography.bodySmall)}}
                HorizontalDivider()
                Text("Project backup",fontWeight=FontWeight.Bold)
                Text("Export all projects to a JSON file or restore a BuildRight JSON backup.",style=MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={exportLauncher.launch("BuildRight-backup-${System.currentTimeMillis()}.json")},modifier=Modifier.weight(1f)){Icon(Icons.Outlined.UploadFile,null);Spacer(Modifier.width(6.dp));Text("Export")};OutlinedButton(onClick={importLauncher.launch(arrayOf("application/json","text/plain"))},modifier=Modifier.weight(1f)){Icon(Icons.Outlined.Download,null);Spacer(Modifier.width(6.dp));Text("Import")}}
            }} }
            item { SettingsRow(Icons.Outlined.DarkMode, "Appearance", "Follows Android system light/dark mode") }
            item { SettingsRow(Icons.Outlined.Straighten, "Units", "Project dimensions use decimal feet; field measurements store their own unit") }
            item { SettingsRow(Icons.Outlined.Security, "Safety boundaries", "Structural sizing and code claims remain verification-gated") }
            status?.let{item{InfoBanner(it,Icons.Outlined.Info)}}
        }
    }

    pendingImport?.let{imported->AlertDialog(onDismissRequest={pendingImport=null},title={Text("Replace local projects?")},text={Text("This backup contains ${imported.size} project(s). Importing replaces the projects currently stored on this device.")},confirmButton={Button(onClick={onReplaceProjects(imported);pendingImport=null;status="Imported ${imported.size} project(s)."}){Text("Replace and import")}},dismissButton={TextButton(onClick={pendingImport=null}){Text("Cancel")}})}
}

@Composable
private fun SettingsRow(icon:ImageVector, title:String, subtitle:String) {
    ElevatedCard(shape=RoundedCornerShape(18.dp)) { Row(Modifier.padding(16.dp), verticalAlignment=Alignment.CenterVertically) { Icon(icon,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.SemiBold);Text(subtitle,style=MaterialTheme.typography.bodySmall)} } }
}

@Composable
private fun NumericProjectField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, integer: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val filtered = if (integer) raw.filter { it.isDigit() } else raw.filterIndexed { i, c -> c.isDigit() || (c == '.' && raw.indexOf('.') == i) }
            onValueChange(filtered)
        },
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if(integer) KeyboardType.Number else KeyboardType.Decimal)
    )
}

@Composable
private fun SimpleTextProjectField(label: String, initial: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var text by remember(initial) { mutableStateOf(initial) }
    OutlinedTextField(value=text,onValueChange={text=it;onValueChange(it)},label={Text(label)},modifier=modifier,singleLine=true)
}


@Composable
private fun ChoiceProjectField(label: String, value: String, choices: List<String>, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Text(value, maxLines = 1)
            }
            Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { choice ->
                DropdownMenuItem(text = { Text(choice) }, onClick = { expanded = false; onValueChange(choice) })
            }
        }
    }
}

@Composable
private fun MaterialEditableCard(m: MaterialItem, onChanged: () -> Unit, onDelete: () -> Unit) {
    var name by remember(m.id){mutableStateOf(m.name)}
    var qty by remember(m.id){mutableStateOf(fmt(m.quantity))}
    var unit by remember(m.id){mutableStateOf(m.unit)}
    var price by remember(m.id){mutableStateOf(fmt(m.unitPrice))}
    ElevatedCard(shape=RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically){Checkbox(m.purchased,{m.purchased=it;onChanged()});OutlinedTextField(name,{name=it;m.name=it;onChanged()},label={Text("Material")},modifier=Modifier.weight(1f),singleLine=true);IconButton(onClick=onDelete){Icon(Icons.Default.Delete,"Delete")}}
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                NumericProjectField("Qty",qty,{v->qty=v;v.toDoubleOrNull()?.takeIf{it>=0}?.let{m.quantity=it;onChanged()}},Modifier.weight(1f))
                OutlinedTextField(unit,{unit=it;m.unit=it;onChanged()},label={Text("Unit")},modifier=Modifier.weight(1f),singleLine=true)
                NumericProjectField("Price",price,{v->price=v;v.toDoubleOrNull()?.takeIf{it>=0}?.let{m.unitPrice=it;onChanged()}},Modifier.weight(1f))
            }
            Text("Subtotal: $${money(m.subtotal)}",fontWeight=FontWeight.Bold)
        }
    }
}

@Composable
private fun MaterialPriceCard(m: MaterialItem, onChanged: () -> Unit) {
    var price by remember(m.id){mutableStateOf(fmt(m.unitPrice))}
    ElevatedCard(shape=RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)){Text(m.name,fontWeight=FontWeight.SemiBold);Text("${fmt(m.quantity)} ${m.unit}",style=MaterialTheme.typography.bodySmall)}
            Spacer(Modifier.width(10.dp))
            NumericProjectField("Unit $",price,{v->price=v;v.toDoubleOrNull()?.takeIf{it>=0}?.let{m.unitPrice=it;onChanged()}},Modifier.width(120.dp))
        }
    }
}

@Composable
private fun SectionHeader(title:String, subtitle:String?=null) {
    Column {
        Text(title, style=MaterialTheme.typography.titleLarge, fontWeight=FontWeight.Black)
        if(subtitle!=null) Text(subtitle, color=MaterialTheme.colorScheme.onSurfaceVariant, style=MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun InfoBanner(text:String, icon:ImageVector) {
    Surface(shape=RoundedCornerShape(18.dp), color=MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.padding(14.dp), verticalAlignment=Alignment.Top) {
            Icon(icon,null,tint=MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(text, style=MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EmptyState(icon:ImageVector, title:String, message:String) {
    Surface(shape=RoundedCornerShape(22.dp), color=MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(26.dp), horizontalAlignment=Alignment.CenterHorizontally) {
            Icon(icon,null,modifier=Modifier.size(42.dp), tint=MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(title,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(message, style=MaterialTheme.typography.bodySmall)
        }
    }
}
