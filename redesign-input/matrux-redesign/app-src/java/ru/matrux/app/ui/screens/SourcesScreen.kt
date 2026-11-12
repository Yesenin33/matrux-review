package ru.matrux.app.ui.screens

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
import ru.matrux.app.R
import ru.matrux.app.learn.Course
import ru.matrux.app.learn.Source
import ru.matrux.app.ui.components.Card
import ru.matrux.app.ui.theme.Matrux

data class SourcesActions(
    val onBack: () -> Unit = {},
    val onOpen: (String) -> Unit = {},
)

@Composable
fun SourcesScreen(courses: List<Course>, sources: List<Source>, actions: SourcesActions) {
    val colors = Matrux.colors
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
                .padding(horizontal = Matrux.space.page)
                .padding(bottom = Matrux.space.block),
        ) {
            Text(stringResource(R.string.sources_title), style = Matrux.type.display, color = colors.ink)
            Spacer(Modifier.height(Matrux.space.unit))
            Text(stringResource(R.string.sources_lead), style = Matrux.type.body, color = colors.inkSoft)
            courses.forEach { course ->
                val own = sources.filter { it.course == course.id }
                if (own.isEmpty()) return@forEach
                Spacer(Modifier.height(Matrux.space.block))
                Text(course.title, style = Matrux.type.bodyStrong, color = colors.ink)
                Spacer(Modifier.height(Matrux.space.unit))
                Card {
                    Column(verticalArrangement = Arrangement.spacedBy(Matrux.space.unit * 2)) {
                        own.forEach { source -> SourceLine(source) { actions.onOpen(source.url) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceLine(source: Source, onOpen: () -> Unit) {
    val colors = Matrux.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen),
    ) {
        Text(source.title, style = Matrux.type.body, color = colors.accent)
        Spacer(Modifier.height(Matrux.space.unit / 2))
        Text(stringResource(R.string.sources_meta, source.author, source.license), style = Matrux.type.caption, color = colors.muted)
        Text(stringResource(R.string.sources_accessed, source.accessed), style = Matrux.type.caption, color = colors.muted)
        source.attribution?.let {
            Spacer(Modifier.height(Matrux.space.unit / 2))
            Text(it, style = Matrux.type.caption, color = colors.inkSoft)
        }
    }
}
