package org.github.ewt45.winemulator.ui.setting

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.github.ewt45.winemulator.inputcontrols.ControlsProfile
import org.github.ewt45.winemulator.inputcontrols.InputControlsManager
import org.github.ewt45.winemulator.ui.components.ConfirmDialog
import org.github.ewt45.winemulator.ui.components.rememberConfirmDialogState
import java.io.File

/**
 * Input Controls Settings UI for WinEmulator
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputControlsSettings(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dialogState = rememberConfirmDialogState()

    var profiles by remember { mutableStateOf<List<ControlsProfile>>(emptyList()) }
    var selectedProfile by remember { mutableStateOf<ControlsProfile?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showControlsEditor by remember { mutableStateOf(false) }

    val manager = remember { InputControlsManager(context) }

    LaunchedEffect(Unit) {
        manager.loadProfiles(ignoreTemplates = false)
        profiles = manager.getProfiles()
        selectedProfile = profiles.firstOrNull()
    }

    ConfirmDialog(dialogState)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "虚拟按键设置",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Enable/Disable toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = "启用虚拟按键",
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = selectedProfile != null,
                onCheckedChange = { enabled ->
                    if (!enabled) {
                        selectedProfile = null
                    } else if (profiles.isEmpty()) {
                        // Create default profile
                        val newProfile = manager.createProfile("默认配置")
                        profiles = manager.getProfiles()
                        selectedProfile = newProfile
                    } else {
                        selectedProfile = profiles.first()
                    }
                }
            )
        }

        if (selectedProfile != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Profile selector
            Text(
                text = "当前配置",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedProfile!!.getName(),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    profiles.forEach { profile ->
                        DropdownMenuItem(
                            text = { Text(profile.getName()) },
                            onClick = {
                                selectedProfile = profile
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Profile actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showProfileDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("新建")
                }

                if (profiles.size > 1) {
                    OutlinedButton(
                        onClick = {
                            selectedProfile?.let { profile ->
                                manager.removeProfile(profile)
                                profiles = manager.getProfiles()
                                selectedProfile = profiles.firstOrNull()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("删除")
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    selectedProfile?.let { profile ->
                        selectedProfile = manager.duplicateProfile(profile)
                        profiles = manager.getProfiles()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Star, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("复制当前配置")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Edit controls button
            Button(
                onClick = { showControlsEditor = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("编辑虚拟按键布局")
            }

            // Quick presets
            Text(
                text = "快捷预设",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.height(200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val presetNames = listOf(
                    "FPS游戏", "RPG游戏", "RTS游戏", "赛车游戏", "格斗游戏"
                )

                items(presetNames) { presetName ->
                    ListItem(
                        headlineContent = { Text(presetName) },
                        leadingContent = {
                            Icon(Icons.Default.Star, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            dialogState.showConfirm("加载预设 '$presetName'？这将替换当前配置的按键布局。") {
                                // Apply preset logic would go here
                            }
                        }
                    )
                }
            }

            // Export/Import
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        selectedProfile?.let { profile ->
                            val file = manager.exportProfile(profile)
                            if (file != null) {
                                dialogState.showConfirm("配置已导出到: ${file.absolutePath}")
                            } else {
                                dialogState.showConfirm("导出失败")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("导出")
                }

                OutlinedButton(
                    onClick = {
                        // Import logic would go here
                        dialogState.showConfirm("请选择要导入的.icp文件")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Home, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("导入")
                }
            }
        }
    }

    // Profile creation dialog
    if (showProfileDialog) {
        var newProfileName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("新建配置") },
            text = {
                OutlinedTextField(
                    value = newProfileName,
                    onValueChange = { newProfileName = it },
                    label = { Text("配置名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newProfileName.isNotBlank()) {
                            val newProfile = manager.createProfile(newProfileName)
                            profiles = manager.getProfiles()
                            selectedProfile = newProfile
                            showProfileDialog = false
                        }
                    }
                ) {
                    Text("创建")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Controls editor
    if (showControlsEditor) {
        selectedProfile?.let { profile ->
            ControlsEditorDialog(
                profile = profile,
                onDismiss = { showControlsEditor = false },
                onSave = {
                    profile.save()
                    showControlsEditor = false
                }
            )
        }
    }
}

/**
 * Dialog for editing control elements
 */
@Composable
fun ControlsEditorDialog(
    profile: ControlsProfile,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var selectedElement by remember { mutableStateOf<org.github.ewt45.winemulator.inputcontrols.ControlElement?>(null) }
    var showElementSettings by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑虚拟按键 - ${profile.getName()}") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "提示：点击下方按钮添加新的控件元素，长按拖动可以移动位置。",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Element list
                LazyColumn(
                    modifier = Modifier.height(200.dp)
                ) {
                    items(profile.getElements()) { element ->
                        ListItem(
                            headlineContent = {
                                Text("${element.type.name} - ${element.getBindingAt(0).name}")
                            },
                            supportingContent = {
                                Text("位置: (${element.x}, ${element.y})")
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = when (element.type) {
                                        org.github.ewt45.winemulator.inputcontrols.ControlElement.Type.BUTTON -> Icons.Default.Star
                                        org.github.ewt45.winemulator.inputcontrols.ControlElement.Type.D_PAD -> Icons.Default.ArrowForward
                                        org.github.ewt45.winemulator.inputcontrols.ControlElement.Type.STICK -> Icons.Default.Info
                                        org.github.ewt45.winemulator.inputcontrols.ControlElement.Type.RANGE_BUTTON -> Icons.Default.List
                                        org.github.ewt45.winemulator.inputcontrols.ControlElement.Type.TRACKPAD -> Icons.Default.Menu
                                    },
                                    contentDescription = null
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = {
                                    profile.removeElement(element)
                                    profile.save()
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除")
                                }
                            },
                            modifier = Modifier.clickable {
                                selectedElement = element
                                showElementSettings = true
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Add element buttons
                Text(
                    text = "添加控件",
                    style = MaterialTheme.typography.titleSmall
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            val element = org.github.ewt45.winemulator.inputcontrols.ControlElement(
                                profile.id as? org.github.ewt45.winemulator.inputcontrols.InputControlsView ?: return@FilterChip
                            ).apply {
                                setType(org.github.ewt45.winemulator.inputcontrols.ControlElement.Type.BUTTON)
                                x = 100
                                y = 100
                            }
                            profile.addElement(element)
                            profile.save()
                        },
                        label = { Text("按钮") },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) }
                    )

                    FilterChip(
                        selected = false,
                        onClick = {
                            // Add D-Pad
                        },
                        label = { Text("方向键") },
                        leadingIcon = { Icon(Icons.Default.ArrowForward, contentDescription = null) }
                    )

                    FilterChip(
                        selected = false,
                        onClick = {
                            // Add Stick
                        },
                        label = { Text("摇杆") },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("完成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    // Element settings dialog
    if (showElementSettings && selectedElement != null) {
        ElementSettingsDialog(
            element = selectedElement!!,
            onDismiss = { showElementSettings = false },
            onSave = {
                profile.save()
                showElementSettings = false
            }
        )
    }
}

/**
 * Dialog for editing a single control element
 */
@Composable
fun ElementSettingsDialog(
    element: org.github.ewt45.winemulator.inputcontrols.ControlElement,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var selectedType by remember { mutableStateOf(element.type) }
    var selectedShape by remember { mutableStateOf(element.shape) }
    var scale by remember { mutableStateOf(element.scale) }
    var text by remember { mutableStateOf(element.text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("控件设置") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type selector
                Text("类型", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    org.github.ewt45.winemulator.inputcontrols.ControlElement.Type.entries.forEach { type ->
                        FilterChip(
                            selected = element.type == type,
                            onClick = {
                                element.setType(type)
                                selectedType = type
                            },
                            label = { Text(type.name.replace("_", "-")) }
                        )
                    }
                }

                // Shape selector (for buttons)
                if (element.type == org.github.ewt45.winemulator.inputcontrols.ControlElement.Type.BUTTON) {
                    Text("形状", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        org.github.ewt45.winemulator.inputcontrols.ControlElement.Shape.entries.forEach { shape ->
                            FilterChip(
                                selected = element.shape == shape,
                                onClick = {
                                    element.shape = shape
                                    selectedShape = shape
                                },
                                label = { Text(shape.name.replace("_", " ")) }
                            )
                        }
                    }
                }

                // Scale slider
                Text("大小: ${(scale * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = scale,
                    onValueChange = {
                        scale = it
                        element.scale = it
                    },
                    valueRange = 0.5f..2f
                )

                // Custom text
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        element.text = it
                    },
                    label = { Text("自定义文字（可选）") },
                    singleLine = true
                )

                // Binding section
                Text("按键绑定", style = MaterialTheme.typography.labelMedium)
                Column {
                    for (i in 0 until element.getBindingCount()) {
                        val binding = element.getBindingAt(i)
                        ListItem(
                            headlineContent = { Text("绑定 ${i + 1}") },
                            supportingContent = { Text(binding.name) },
                            modifier = Modifier.clickable {
                                // Show binding picker
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
