package bose.ankush.commonui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Premium bottom sheet content UI. Stateless composable for multiplatform (CMP) compatibility.
 *
 * This is a pure presentation component with no internal state, allowing it to be used
 * across Android, iOS, and other Compose Multiplatform targets.
 *
 * @param title Header title text (e.g., "Premium")
 * @param features List of premium feature descriptions
 * @param priceText Pricing text (e.g., "$4.99/month")
 * @param trialText Trial information text (e.g., "7-day free trial, cancel anytime")
 * @param subscribeButtonText Text for subscribe button (e.g., "Subscribe")
 * @param startingText Text shown while loading (e.g., "Starting...")
 * @param cancelText Text for cancel button (e.g., "No Thanks")
 * @param isLoading Whether subscription is in progress - drives button state (disable/loading indicator)
 * @param onDismiss Callback when user cancels
 * @param onSubscribe Callback when user clicks subscribe
 */
@Composable
fun PremiumBottomSheetContent(
    title: String = "Premium",
    features: List<String> = listOf(
        "Ad-Free Experience",
        "Extended 15-day Forecasts",
        "Severe Weather Alerts",
        "Detailed Air Quality Data"
    ),
    priceText: String = "$4.99/month",
    trialText: String = "7-day free trial, cancel anytime",
    subscribeButtonText: String = "Subscribe",
    startingText: String = "Starting...",
    cancelText: String = "No Thanks",
    isLoading: Boolean = false,
    onDismiss: () -> Unit,
    onSubscribe: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Features
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                features.forEach { feature ->
                    SimplePremiumFeature(feature)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Pricing
        Text(
            text = priceText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = trialText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Subscribe Button - Stateless, driven by isLoading parameter
        // ViewModel controls the loading state and calls onSubscribe() when needed
        Button(
            onClick = onSubscribe,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFB74D)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = startingText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            } else {
                Text(
                    text = subscribeButtonText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Cancel Button
        Text(
            text = cancelText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { onDismiss() }
                .padding(vertical = 8.dp)
        )
    }
}

/**
 * Stateless feature item for premium features list
 */
@Composable
fun SimplePremiumFeature(
    feature: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFB74D))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = feature,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
