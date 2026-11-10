package ru.palma.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import ru.palma.app.R
import ru.palma.app.learn.Course
import ru.palma.app.learn.Source
import ru.palma.app.ui.components.Card
import ru.palma.app.ui.theme.Palma

data class SourcesActions(
    val onBack: () -> Unit = {},
    val onOpen: (String) -> Unit = {},
)

@Composable
fun SourcesScreen(courses: List<Course>, sources: List<Source>, actions: SourcesActions) {
    val colors = Palma.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.surface)
            .statusBarsPadding(),
    ) {
        BackRow(stringResource(R.string.course_back), actions.onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Palma.space.page)
                .padding(bottom = Palma.space.block),
        ) {
            Text(stringResource(R.string.sources_title), style = Palma.type.display, color = colors.ink)
            Spacer(Modifier.height(Palma.space.unit))
            Text(stringResource(R.string.sources_lead), style = Palma.type.body, color = colors.inkSoft)
            courses.forEach { course ->
                val own = sources.filter { it.course == course.id }
                if (own.isEmpty()) return@forEach
                Spacer(Modifier.height(Palma.space.block))
                Text(course.title, style = Palma.type.bodyStrong, color = colors.ink)
                Spacer(Modifier.height(Palma.space.unit))
                Card {
                    Column(verticalArrangement = Arrangement.spacedBy(Palma.space.unit * 2)) {
                        own.forEach { source -> SourceLine(source) { actions.onOpen(source.url) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceLine(source: Source, onOpen: () -> Unit) {
    val colors = Palma.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen),
    ) {
        Text(source.title, style = Palma.type.body, color = colors.accent)
        Spacer(Modifier.height(Palma.space.unit / 2))
        Text(stringResource(R.string.sources_meta, source.author, source.license), style = Palma.type.caption, color = colors.muted)
        Text(stringResource(R.string.sources_accessed, source.accessed), style = Palma.type.caption, color = colors.muted)
        source.attribution?.let {
            Spacer(Modifier.height(Palma.space.unit / 2))
            Text(it, style = Palma.type.caption, color = colors.inkSoft)
        }
    }
}
