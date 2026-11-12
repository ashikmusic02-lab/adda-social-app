package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PlusJakartaSansFont = FontFamily(
  Font(R.font.plus_jakarta_sans, FontWeight.Normal),
  Font(R.font.plus_jakarta_sans, FontWeight.Medium),
  Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val NotoSansBengaliFont = FontFamily(
  Font(R.font.noto_sans_bengali, FontWeight.Normal),
  Font(R.font.noto_sans_bengali, FontWeight.Medium),
  Font(R.font.noto_sans_bengali, FontWeight.Bold)
)

fun appTypography(isBengali: Boolean): Typography {
  val primaryFont = if (isBengali) NotoSansBengaliFont else PlusJakartaSansFont
  return Typography(
    displaySmall = TextStyle(
      fontFamily = primaryFont,
      fontWeight = FontWeight.Bold,
      fontSize = 30.sp,
      lineHeight = 38.sp
    ),
    headlineMedium = TextStyle(
      fontFamily = primaryFont,
      fontWeight = FontWeight.Bold,
      fontSize = 24.sp,
      lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
      fontFamily = primaryFont,
      fontWeight = FontWeight.Bold,
      fontSize = 20.sp,
      lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
      fontFamily = primaryFont,
      fontWeight = FontWeight.Medium,
      fontSize = 16.sp,
      lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
      fontFamily = primaryFont,
      fontWeight = FontWeight.Normal,
      fontSize = 16.sp,
      lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
      fontFamily = primaryFont,
      fontWeight = FontWeight.Normal,
      fontSize = 14.sp,
      lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
      fontFamily = primaryFont,
      fontWeight = FontWeight.Medium,
      fontSize = 14.sp,
      lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
      fontFamily = primaryFont,
      fontWeight = FontWeight.Medium,
      fontSize = 12.sp,
      lineHeight = 16.sp
    )
  )
}
