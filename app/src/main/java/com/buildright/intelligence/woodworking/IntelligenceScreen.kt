package com.buildright.intelligence.woodworking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.buildright.app.Project

/**
 * PRIOR-CHAT RECOVERY: thin Compose entry for Intelligence 16–18.
 * IMPLEMENTED: furniture plan, joinery safety YELLOW/GREEN/RED, moisture note.
 * Does not replace shed planner — additive Intelligence tab.
 */
@Composable
fun IntelligenceScreen(project: Project, onChanged: () -> Unit) {
    var revision by remember { mutableIntStateOf(0) }
    revision

    val tableSpec = remember(project.id, project.width, project.length, project.height, revision) {
        TableBaseSpec(
            topLengthInches = (if (project.length > 0) project.length else 60.0 / 12.0) * 12.0,
            topWidthInches = (if (project.width > 0) project.width else 30.0 / 12.0) * 12.0,
            topThicknessInches = 1.25,
            topWeightLbs = 180.0,
            baseHeightInches = (if (project.height > 0) project.height else 30.0 / 12.0) * 12.0,
            species = project.woodSpecies.ifBlank { "oak" }
        )
    }
    val plan = remember(tableSpec, revision) { WoodworkingEngine.modelTableBase(tableSpec) }

    val jointRequest = remember(project.id, revision) {
        JointRequest(
            id = project.furnitureJoints.firstOrNull()?.id ?: "joint-leg-apron",
            memberAThicknessIn = 1.5,
            memberBThicknessIn = 1.5,
            tenonThicknessIn = 0.5,
            tenonLengthIn = 1.25,
            loadPath = LoadPath.RACKING,
            verified = project.furnitureJoints.firstOrNull()?.safety == JointSafety.GREEN.name,
            crossGrainRigidAttach = project.crossGrainRigidAttach,
            speciesA = project.woodSpecies.ifBlank { "oak" },
            speciesB = project.woodSpecies.ifBlank { "oak" }
        )
    }
    val moistureGate = remember(jointRequest, project.moistureContentPercent, project.equilibriumMoisturePercent, revision) {
        MoistureService.joineryMovementGate(
            request = jointRequest,
            panelWidthInches = tableSpec.topWidthInches,
            moistureNowPercent = project.moistureContentPercent,
            equilibriumPercent = project.equilibriumMoisturePercent
        )
    }
    val recommendation = remember(jointRequest, moistureGate, revision) {
        JoineryEngine.recommend(jointRequest, moistureGate)
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Intelligence — Woodworking", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(
                "Phases 16–18 (furniture, joinery, moisture). Shed planner unchanged.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        item {
            SectionCard("Phase 16 — Table base / cut list") {
                Text(plan.stability.message, fontWeight = FontWeight.SemiBold)
                Text("Stability: ${plan.stability.level} • footprint ${"%.0f".format(plan.stability.footprintCoverageRatio * 100)}%")
                Spacer(Modifier.height(8.dp))
                Text("Cut list (${plan.cutList.size} pcs)", fontWeight = FontWeight.SemiBold)
                plan.cutList.take(8).forEach { row ->
                    Text("#${row.number} ${row.role} ${row.size} @ ${"%.1f".format(row.lengthInches)} in — ${row.note}")
                }
                if (plan.cutList.size > 8) Text("… +${plan.cutList.size - 8} more")
                Spacer(Modifier.height(8.dp))
                Text("Build sequence", fontWeight = FontWeight.SemiBold)
                plan.buildSequence.forEach { step ->
                    Text("${step.order}. ${step.title}: ${step.detail}")
                }
                Spacer(Modifier.height(6.dp))
                Text("Joinery considerations:", fontWeight = FontWeight.SemiBold)
                plan.joineryHints.forEach { Text("• $it") }
            }
        }

        item {
            SectionCard("Phase 17 — Joinery") {
                SafetyBanner(recommendation.safety)
                Text(recommendation.autoMarker, fontWeight = FontWeight.Bold)
                Text("Recommended: ${recommendation.recommended}")
                Text("Hold: ${recommendation.hold} • adhesive=${recommendation.adhesiveOk} fastener=${recommendation.fastenerOk}")
                recommendation.reasons.forEach { Text("• $it") }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val rec = JoineryEngine.recommend(
                            jointRequest.copy(verified = false, preferred = null),
                            moistureGate
                        )
                        persistJoint(project, rec)
                        revision++; onChanged()
                    }) { Text("AUTO recommend") }
                    OutlinedButton(onClick = {
                        val rec = JoineryEngine.recommend(
                            jointRequest.copy(verified = true),
                            moistureGate
                        )
                        persistJoint(project, rec)
                        revision++; onChanged()
                    }) { Text("Mark verified") }
                }
                if (project.furnitureJoints.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Persisted on project:", fontWeight = FontWeight.SemiBold)
                    project.furnitureJoints.forEach { j ->
                        Text("${j.jointType} • ${j.safety} • ${j.autoMarker}${if (j.hold) " HOLD" else ""}")
                    }
                }
            }
        }

        item {
            SectionCard("Phase 18 — Moisture & movement") {
                Text("Species: ${project.woodSpecies.ifBlank { "oak" }}")
                Text("MC now ${project.moistureContentPercent}% → EMC ${project.equilibriumMoisturePercent}%")
                Text(MoistureService.acclimationNote(project.moistureContentPercent, project.equilibriumMoisturePercent))
                Text(moistureGate.message)
                Text("Expected move across top: ${"%.3f".format(moistureGate.expectedMovementIn)} in")
                Text(MoistureService.breadboardGuidance(tableSpec.topWidthInches))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = project.crossGrainRigidAttach,
                        onClick = {
                            project.crossGrainRigidAttach = !project.crossGrainRigidAttach
                            revision++; onChanged()
                        },
                        label = { Text(if (project.crossGrainRigidAttach) "Rigid cross-grain ON" else "Rigid cross-grain OFF") }
                    )
                }
                OutlinedTextField(
                    value = project.woodSpecies,
                    onValueChange = { project.woodSpecies = it; revision++; onChanged() },
                    label = { Text("Species") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun persistJoint(project: Project, rec: JointRecommendation) {
    val record = JoineryEngine.toRecord(rec, "leg", "apron")
    val existing = project.furnitureJoints.indexOfFirst { it.id == record.id }
    if (existing >= 0) project.furnitureJoints[existing] = record
    else project.furnitureJoints.add(record)
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun SafetyBanner(safety: JointSafety) {
    val (bg, label) = when (safety) {
        JointSafety.GREEN -> Color(0xFF1E7A5C) to "GREEN — verified"
        JointSafety.YELLOW -> Color(0xFFB54708) to "YELLOW — provisional"
        JointSafety.RED -> Color(0xFFB42318) to "RED — HOLD"
    }
    Box(
        Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
