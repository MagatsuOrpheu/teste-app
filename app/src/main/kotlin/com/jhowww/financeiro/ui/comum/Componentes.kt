package com.jhowww.financeiro.ui.comum

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jhowww.financeiro.domain.Competencia
import com.jhowww.financeiro.domain.Dinheiro
import com.jhowww.financeiro.ui.theme.Cores
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Cor de um valor: negativo em vermelho, o resto no texto padrão. */
fun corDoValor(valor: Dinheiro): Color = if (valor.ehNegativo) Cores.Negativo else Cores.Texto

/** Bloco com fundo de superfície, usado para agrupar informações nas telas. */
@Composable
fun Bloco(
    modifier: Modifier = Modifier,
    titulo: String? = null,
    acao: (@Composable () -> Unit)? = null,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (titulo != null || acao != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        titulo.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    acao?.invoke()
                }
            }
            conteudo()
        }
    }
}

/** Linha "rótulo ........ valor". */
@Composable
fun LinhaValor(rotulo: String, valor: Dinheiro, destaque: Boolean = false, cor: Color = corDoValor(valor)) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            valor.formatar(),
            style = if (destaque) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (destaque) FontWeight.SemiBold else FontWeight.Normal,
            color = cor,
        )
    }
}

@Composable
fun PontoCor(cor: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(10.dp).background(cor, CircleShape))
}

/** Navegação entre meses: ‹ Novembro de 2026 › com atalho para o mês atual. */
@Composable
fun SeletorMes(mes: Competencia, atual: Competencia, onMudar: (Long) -> Unit, onAtual: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onMudar(-1) }) { Icon(Icons.Outlined.ChevronLeft, "Mês anterior") }
        Text(
            mes.nome(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(onClick = { onMudar(1) }) { Icon(Icons.Outlined.ChevronRight, "Próximo mês") }
        if (mes != atual) {
            TextButton(onClick = onAtual) { Text("Hoje") }
        }
    }
}

@Composable
fun TextoSecundario(texto: String, modifier: Modifier = Modifier) {
    Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

/** Campo de valor em reais; aceita vírgula ou ponto. */
@Composable
fun CampoValor(rotulo: String, texto: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, erro: Boolean = false) {
    OutlinedTextField(
        value = texto,
        onValueChange = onChange,
        label = { Text(rotulo) },
        prefix = { Text("R$ ") },
        isError = erro,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun CampoTexto(rotulo: String, texto: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, numerico: Boolean = false) {
    OutlinedTextField(
        value = texto,
        onValueChange = onChange,
        label = { Text(rotulo) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (numerico) KeyboardType.Number else KeyboardType.Text),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Lista de escolha: campo somente leitura que abre um menu. */
@Composable
fun <T> Seletor(
    rotulo: String,
    opcoes: List<T>,
    selecionado: T?,
    texto: (T) -> String,
    onSelecionar: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var aberto by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selecionado?.let(texto).orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(rotulo) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable { aberto = true })
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            if (opcoes.isEmpty()) {
                DropdownMenuItem(text = { Text("Nenhuma opção cadastrada") }, onClick = { aberto = false }, enabled = false)
            }
            opcoes.forEach { opcao ->
                DropdownMenuItem(text = { Text(texto(opcao)) }, onClick = { onSelecionar(opcao); aberto = false })
            }
        }
    }
}

/** Campo de data com o calendário do Material. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoData(rotulo: String, data: LocalDate, onChange: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    var aberto by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = data.completa(),
            onValueChange = {},
            readOnly = true,
            label = { Text(rotulo) },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable { aberto = true })
    }
    if (aberto) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = data.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { aberto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    aberto = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { aberto = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = estado)
        }
    }
}

/** Diálogo de formulário com rolagem; o botão de confirmar só habilita com os dados válidos. */
@Composable
fun DialogoFormulario(
    titulo: String,
    onFechar: () -> Unit,
    onConfirmar: () -> Unit,
    podeConfirmar: Boolean,
    textoConfirmar: String = "Salvar",
    acaoExtra: (@Composable () -> Unit)? = null,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(titulo) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = conteudo,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirmar, enabled = podeConfirmar) { Text(textoConfirmar) }
        },
        dismissButton = {
            Row {
                acaoExtra?.invoke()
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onFechar) { Text("Cancelar") }
            }
        },
    )
}

/** Confirmação simples para ações que apagam dados. */
@Composable
fun DialogoConfirmacao(titulo: String, texto: String, textoConfirmar: String, onConfirmar: () -> Unit, onFechar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = { TextButton(onClick = { onConfirmar(); onFechar() }) { Text(textoConfirmar, color = Cores.Negativo) } },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    )
}
