package org.greenstand.android.TreeTracker.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.theme.CustomTheme


@Composable
fun SelfieTutorial(onCompleteClick: (() -> Unit)) {
    val selfieIllustration: Painter = painterResource(id = R.drawable.selfie_illustration)
    TutorialDialog(
        content = {
            ImageCapturing(
                image = selfieIllustration,
                text = "to take a selfie to continue your registration."
            )
        },
        onCompleteClick = onCompleteClick
    )

}

@Composable
fun ImageCapturing(
    image: Painter,
    text: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Click on",
            color = CustomTheme.textColors.primaryText,
            style = CustomTheme.typography.medium,
            fontWeight = FontWeight.Bold,
        )
        CaptureButton(
            onClick = {},
            isEnabled = false
        )
        Text(
            text = text,
            color = CustomTheme.textColors.primaryText,
            textAlign = TextAlign.Center,
            style = CustomTheme.typography.medium,
            fontWeight = FontWeight.Bold,
        )
        Image(
            painter = image,
            contentDescription = null
        )
    }
}

@Composable
fun TutorialDialog(
    content: @Composable() (() -> Unit)? = null,
    onCompleteClick: (() -> Unit)
) {
    AlertDialog(
        onDismissRequest = { },
        title = null,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .alpha(0.4f)
            .padding(2.dp)
            .border(1.dp, color = AppColors.Green, shape = RoundedCornerShape(percent = 10))
            .clip(RoundedCornerShape(percent = 10)),
        backgroundColor = AppColors.Gray,
        text = content,
        buttons = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ApprovalButton(
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.CenterHorizontally),
                    onClick = onCompleteClick,
                    approval = true
                )
            }
        }
    )
}