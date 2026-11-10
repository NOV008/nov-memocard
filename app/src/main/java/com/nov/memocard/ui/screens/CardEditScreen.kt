@file:OptIn(ExperimentalMaterial3Api::class)

package com.nov.memocard.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nov.memocard.ui.CardEditViewModel
import com.nov.memocard.util.ImageStore

@Composable
fun CardEditScreen(
    onBack: () -> Unit,
    viewModel: CardEditViewModel = viewModel(),
) {
    val context = LocalContext.current
    val canSave = viewModel.front.isNotBlank() && viewModel.back.isNotBlank()

    val frontLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { u -> ImageStore.save(context, u)?.let { viewModel.frontImage = it } }
    }
    val backLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { u -> ImageStore.save(context, u)?.let { viewModel.backImage = it } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.cardId == 0L) "新建卡片" else "编辑卡片") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.save(onBack) }, enabled = canSave) { Text("保存") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = viewModel.front,
                onValueChange = { viewModel.front = it },
                label = { Text("正面（问题）") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
            ImagePicker(
                name = viewModel.frontImage,
                onPick = { frontLauncher.launch(arrayOf("image/*")) },
                onClear = {
                    ImageStore.delete(context, viewModel.frontImage)
                    viewModel.frontImage = null
                },
            )
            OutlinedTextField(
                value = viewModel.back,
                onValueChange = { viewModel.back = it },
                label = { Text("背面（答案）") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
            )
            ImagePicker(
                name = viewModel.backImage,
                onPick = { backLauncher.launch(arrayOf("image/*")) },
                onClear = {
                    ImageStore.delete(context, viewModel.backImage)
                    viewModel.backImage = null
                },
            )
            OutlinedTextField(
                value = viewModel.tags,
                onValueChange = { viewModel.tags = it },
                label = { Text("标签（逗号或空格分隔）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("暂停此卡（不参与复习）", Modifier.weight(1f))
                Switch(checked = viewModel.suspended, onCheckedChange = { viewModel.suspended = it })
            }
        }
    }
}

@Composable
private fun ImagePicker(name: String?, onPick: () -> Unit, onClear: () -> Unit) {
    val context = LocalContext.current
    val bmp = remember(name) { name?.let { ImageStore.loadBitmap(context, it) } }
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        OutlinedButton(onClick = onPick, shape = RoundedCornerShape(10.dp)) {
            Text(if (name == null) "选择图片" else "更换图片")
        }
        if (bmp != null) {
            Spacer(Modifier.padding(horizontal = 6.dp))
            Image(
                bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.heightIn(max = 72.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit,
            )
            TextButton(onClick = onClear) { Text("移除") }
        }
    }
}