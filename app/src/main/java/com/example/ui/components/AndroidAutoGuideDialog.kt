package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ElectricCyan

@Composable
fun AndroidAutoGuideDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("android_auto_guide_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Android Auto -sovellus",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Sovellus on rakennettu Android Auto -yhteensopivaksi (Car App Library) ja se näyttää spottihinnat ja lataustunnit suoraan auton kosketusnäytöllä.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                GuideStepCard(
                    stepNumber = "1",
                    icon = Icons.Default.Settings,
                    title = "Android Auto: Kehittäjäasetukset",
                    description = "Avaa puhelimen Asetukset > Android Auto > napauta alalaidassa olevaa 'Versio'-kohtaa 10 kertaa nopeasti > hyväksy kehittäjätilan aktivointi. Paina sitten oikean yläkulman kolmea pistettä (⋮) > Kehittäjäasetukset."
                )

                Spacer(modifier = Modifier.height(10.dp))

                GuideStepCard(
                    stepNumber = "2",
                    icon = Icons.Default.Settings,
                    title = "Tuntemattomat lähteet & Sovellustila",
                    description = "Kehittäjäasetuksissa:\n1. Ruksaa 'Tuntemattomat lähteet' (Unknown sources).\n2. Etsi kohta 'Sovellustila' (Application Mode) ja valitse 'Kehittäjä' (Developer) tai 'Julkaisu' (Release)."
                )

                Spacer(modifier = Modifier.height(10.dp))

                GuideStepCard(
                    stepNumber = "3",
                    icon = Icons.Default.DirectionsCar,
                    title = "Sulje Android Auto välimuisti",
                    description = "Android Auto ei aina päivitä sovelluslistaa ennen kuin se käynnistetään uudelleen: Avaa puhelimen Asetukset > Sovellukset > Android Auto > valitse 'Pakota lopetus' (Force Stop) ja tyhjennä välimuisti (Clear cache). Tämän jälkeen avaa Android Auto -asetukset uudelleen ja tarkista 'Mukauta käynnistysohjelmaa'."
                )

                Spacer(modifier = Modifier.height(10.dp))

                GuideStepCard(
                    stepNumber = "4",
                    icon = Icons.Default.CheckCircle,
                    title = "Kytke autoon tai käytä puhelimen Autotilaa",
                    description = "Kun kytket puhelimen autoon, 'Nord Pool Sähkö' ilmestyy auton valikkoon. Voit myös milloin vain käyttää yläpalkin 'Autotila'-painiketta autonäytön simulointiin suoraan puhelimella."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
private fun GuideStepCard(
    stepNumber: String,
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.size(24.dp)
                ) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stepNumber,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
