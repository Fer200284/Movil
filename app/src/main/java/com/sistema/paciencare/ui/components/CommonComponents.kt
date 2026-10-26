package com.sistema.paciencare.ui.components

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onVisibilityToggle: (() -> Unit)? = null,
    accentColor: Color,
    isMandatory: Boolean = false,
    showError: Boolean = false,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            if (isMandatory && showError) {
                Text(
                    " *",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
            enabled = onClick == null,
            readOnly = readOnly,
            placeholder = { Text(placeholder, fontSize = 15.sp, color = Color(0xFF94A3B8)) },
            leadingIcon = {
                Icon(
                    icon,
                    null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            },
            trailingIcon = if (isPassword && (onVisibilityToggle != null)) {
                {
                    IconButton(onClick = onVisibilityToggle) {
                        Icon(
                            if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !passwordVisible)
                PasswordVisualTransformation() else VisualTransformation.None,
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (showError) Color.Red else accentColor,
                unfocusedBorderColor = if (showError) Color.Red else Color(0xFFE2E8F0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledBorderColor = Color(0xFFE2E8F0),
                disabledTextColor = Color(0xFF1E293B),
                disabledLeadingIconColor = accentColor
            )
        )
    }
}

@Composable
fun RoleItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(60.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) color else Color.White,
        shadowElevation = if (isSelected) 8.dp else 2.dp,
        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                if (isSelected) Icons.Filled.CheckCircle else icon,
                null,
                tint = if (isSelected) Color.White else Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                title,
                color = if (isSelected) Color.White else Color(0xFF1E293B),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun SuccessRegisterDialog(onConfirm: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = { }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0284C7))
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Cuenta creada",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE2F2FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎉", fontSize = 40.sp)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "¡Registro exitoso!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Tu cuenta ha sido creada. Ya puedes iniciar sesión con tus credenciales.",
                    textAlign = TextAlign.Center,
                    color = Color(0xFF64748B),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(horizontal = 24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Ir al inicio de sesión", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ConnectionStatusBadge(
    statusText: String,
    modifier: Modifier = Modifier
) {
    val isRemote = statusText.contains("remotamente", ignoreCase = true)
    val bgColor = if (isRemote) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
    val textColor = if (isRemote) Color(0xFF15803D) else Color(0xFFB45309)
    val icon = if (isRemote) Icons.Filled.CloudDone else Icons.Filled.Storage

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = statusText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}
