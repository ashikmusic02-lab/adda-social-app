package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.i18n.LocalAppStrings

data class FeatureReportRow(
  val featureBn: String,
  val status: String,
  val noteBn: String
)

private val phase1ReportRows = listOf(
  FeatureReportRow(
    "১. Splash ও Welcome স্ক্রিন",
    "Completed",
    "অ্যানিমেটেড Splash এবং হিরো ব্যানারসহ Welcome স্ক্রিন"
  ),
  FeatureReportRow(
    "২. ইমেইল-পাসওয়ার্ড স্ক্রিন (রেজিস্টার, লগইন, রিসেট, ইমেইল যাচাই)",
    "Completed (Firebase Console-এ Enable আবশ্যিক)",
    "AuthService-এর বাস্তব Firebase কলের মাধ্যমে লগইন, নতুন অ্যাকাউন্ট, পাসওয়ার্ড রিসেট ও ইমেইল যাচাই যুক্ত"
  ),
  FeatureReportRow(
    "৩. Google লগইন ও স্থির debug.keystore",
    "Completed",
    "Credential Manager ও রিপোতে থাকা স্থির debug.keystore-এর মাধ্যমে প্রতি বিল্ডে অভিন্ন SHA-1"
  ),
  FeatureReportRow(
    "৪. অনবোর্ডিং ও ইউনিক ইউজারনেম ট্রানজ্যাকশন (getAfter)",
    "Completed",
    "usernames/{username} ও users/{uid} একত্রে অ্যাটমিক Firestore Transaction ও getAfter যাচাই"
  ),
  FeatureReportRow(
    "৫. মূল নেভিগেশন বার (৫টি ট্যাব)",
    "Completed",
    "Home, Explore, Create, Notifications ('শীঘ্রই আসছে' ব্যাজসহ) ও Profile"
  ),
  FeatureReportRow(
    "৬. সেটিংস (ভাষা, থিম ও লগআউট)",
    "Completed",
    "বাংলা/ইংরেজি i18n, ডার্ক/লাইট/সিস্টেম থিম (DataStore ও Firestore সিঙ্ক) এবং নিরাপদ লগআউট"
  ),
  FeatureReportRow(
    "৭. প্রতিটি স্ক্রিনে Loading, Error, Empty ও Offline state",
    "Completed",
    "রিয়েল-টাইম নেটওয়ার্ক মনিটর ও চারটি স্টেট কম্পোনেন্ট সক্রিয়"
  ),
  FeatureReportRow(
    "৮. পোস্ট, কমেন্ট, ফলো, লাইক, নোটিফিকেশন, রিপোর্ট, ব্লক",
    "Future Feature",
    "শুধুমাত্র services/ ইন্টারফেস ও পূর্ণাঙ্গ Firestore স্কিমা তৈরি (ধাপ ২–৫ এ বাস্তবায়িত হবে)"
  )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupAndArchitectureScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onBack)
  val strings = LocalAppStrings.current

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(strings.setupAndGithubGuideTitle) },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("close_setup_guide_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back"
            )
          }
        }
      )
    },
    modifier = modifier
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 600.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
          )
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Text(
              text = "Feature Completion Report (ধাপ ১)",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            phase1ReportRows.forEach { row ->
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp)
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = row.featureBn,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.weight(1f)
                    )
                    Surface(
                      shape = RoundedCornerShape(50),
                      color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                      Text(
                        text = row.status,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = row.noteBn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }

        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
          )
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Text(
              text = "ফোন থেকে GitHub Actions ও Secrets সেটআপ গাইড",
              style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "১. নেটিভ Android (`app/`) একমাত্র ইউজার অ্যাপ। রিপোজিটরিতে স্থির `debug.keystore` থাকায় প্রতি বিল্ডে একই SHA-1 (`13:EE:07:37:10:AE:FC:86:1A:59:F7:51:69:56:C6:64:A7:D9:62:6E`) পাওয়া যাবে।\n" +
                "২. ফোনের ব্রাউজার থেকে GitHub Repo -> Settings -> Secrets and variables -> Actions-এ গিয়ে মাত্র ৩টি Secret যোগ করুন:\n" +
                "   • GOOGLE_SERVICES_JSON_BASE64 (build-user-debug-apk.yml-এর জন্য)\n" +
                "   • FIREBASE_PROJECT_ID (deploy-firebase-rules.yml-এর জন্য)\n" +
                "   • FIREBASE_SERVICE_ACCOUNT (deploy-firebase-rules.yml-এর জন্য)\n" +
                "৩. বিস্তারিত বাংলা নির্দেশিকা `docs/SETUP_GUIDE_BN.md` এবং `docs/FIRESTORE_SCHEMA.md` ফাইলে রয়েছে।",
              style = MaterialTheme.typography.bodyMedium,
              fontFamily = FontFamily.Default
            )
          }
        }
      }
    }
  }
}
