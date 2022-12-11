package org.greenstand.android.TreeTracker.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
                text = stringResource(R.string.selfie_instruction)
            )
        },
        onCompleteClick = onCompleteClick
    )

}

@Composable
fun TreeCaptureTutorial(onCompleteClick: (() -> Unit)) {
    val treeCapturingIllustration: Painter = painterResource(id = R.drawable.tree_capturing_illustration)
    TutorialDialog(
        content = {
            ImageCapturing(
                image =treeCapturingIllustration,
                text = stringResource(R.string.tracking_instruction)
            )
        },
        onCompleteClick = onCompleteClick
    )

}

@Composable
fun TreeCaptureReviewTutorial(onCompleteClick: (() -> Unit)) {
    val treeCapturingIllustration: Painter = painterResource(id = R.drawable.tree_capturing_illustration)
    TutorialDialog(
        content = {
          TreeCaptureReview()
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
            text = stringResource(R.string.click_on),
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
fun TreeCaptureReview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = CenterVertically
        ) {
            Text(
                text = stringResource(R.string.tree_capture_review_text),
                color = CustomTheme.textColors.primaryText,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .padding(end = 30.dp),
                style = CustomTheme.typography.medium,
                fontWeight = FontWeight.Bold,
            )

            Box(
                modifier = Modifier
                    .wrapContentHeight(),
                contentAlignment = Center
            ) {
                TreeTrackerButton(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(width = 100.dp, 60.dp),
                    isEnabled = false,
                    onClick = { }
                ) {
                    Text(stringResource(R.string.note))
                }
                Image(
                    modifier = Modifier.padding(top = 30.dp),
                    painter = painterResource(id = R.drawable.touch_gesture),
                    contentDescription = null
                )
            }

        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = CenterVertically
        ) {
            Text(
                text = stringResource(R.string.capture_tutorial),
                color = CustomTheme.textColors.primaryText,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .padding(end = 30.dp),
                style = CustomTheme.typography.medium,
                fontWeight = FontWeight.Bold,
            )
            Box(
                modifier = Modifier
                    .wrapContentHeight(),
                contentAlignment = Center
            ) {
                ApprovalButton(onClick = { /*TODO*/ }, approval = true)
                Image(
                    modifier = Modifier.padding(top = 30.dp),
                    painter = painterResource(id = R.drawable.touch_gesture),
                    contentDescription = null
                )
            }

        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = CenterVertically
        ) {
            Text(
                text = stringResource(R.string.recapture_tutorial),
                color = CustomTheme.textColors.primaryText,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .padding(end = 30.dp),
                style = CustomTheme.typography.medium,
                fontWeight = FontWeight.Bold,
            )
            Box(
                modifier = Modifier
                    .wrapContentHeight(),
                contentAlignment = Center
            ) {
                ApprovalButton(onClick = { /*TODO*/ }, approval = false)
                Image(
                    modifier = Modifier.padding(top = 30.dp),
                    painter = painterResource(id = R.drawable.touch_gesture),
                    contentDescription = null
                )
            }
        }

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
            .alpha(0.8f)
            .padding(2.dp)
            .border(1.dp, color = AppColors.Green, shape = RoundedCornerShape(percent = 10))
            .clip(RoundedCornerShape(percent = 10)),
        backgroundColor = AppColors.Gray,
        text = content,
        buttons = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
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