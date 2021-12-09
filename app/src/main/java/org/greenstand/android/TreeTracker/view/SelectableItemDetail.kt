package org.greenstand.android.TreeTracker.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.user.User

@Composable
fun SelectableImageDetail(
    photoPath: String,
    isSelected: Boolean,
    buttonColors: DepthButtonColors,
    selectedColor: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    DepthButton(
        onClick = onClick,
        isSelected = isSelected,
        modifier = Modifier
            .padding(8.dp)
            .width(156.dp)
            .wrapContentHeight(),
        contentAlignment = Alignment.TopCenter,
        colors = DepthButtonColors(
            color = buttonColors.color,
            shadowColor = if (isSelected) selectedColor else buttonColors.shadowColor,
            disabledColor = buttonColors.disabledColor,
            disabledShadowColor = buttonColors.disabledShadowColor
        )
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = 12.dp) // Bottom side can be clipped by the button.
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LocalImage(
                imagePath = photoPath,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .aspectRatio(1.0f)
                    .clip(RoundedCornerShape(percent = 10)),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun UserButton(
    user: User,
    isSelected: Boolean,
    buttonColors: DepthButtonColors,
    selectedColor: Color,
    onClick: () -> Unit
) {
    SelectableImageDetail(
        user.photoPath,
        isSelected,
        buttonColors,
        selectedColor,
        onClick
    ) {
        Text(
            text = "${user.firstName} ${user.lastName}\n${user.wallet}",
            color = AppColors.LightGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
        )
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.white_leaf),
                contentDescription = "",
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(width = 20.dp,height = 22.dp)
            )
            Text(
                text = user.numberOfTrees, // TODO: Fetch user's token count.
                modifier = Modifier.padding(start = 4.dp),
                color = AppColors.LightGray,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
            )
        }
    }
}
