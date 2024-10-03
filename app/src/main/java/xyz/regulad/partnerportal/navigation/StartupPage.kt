package xyz.regulad.partnerportal.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.regulad.partnerportal.HandledAsyncImage
import xyz.regulad.partnerportal.MinecraftBackgroundImage
import xyz.regulad.partnerportal.MinecraftText

@Preview
@Composable
fun StartupPage(modifier: Modifier = Modifier) {
    MinecraftBackgroundImage("dirt.png")

    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.align(Alignment.Center)) {
            Column {
                HandledAsyncImage(
                    "splash.png",
                    modifier = Modifier
                        .height(40.dp)
                        .align(Alignment.CenterHorizontally)
                )

                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        MinecraftText("Enter configuration below:")

                        Spacer(modifier = Modifier.height(10.dp))

                        MinecraftText("Supabase Server URL:")

                        Spacer(modifier = Modifier.height(10.dp))

                        MinecraftText("Supabase Server Anon Key:")

                        Spacer(modifier = Modifier.height(10.dp))

                        MinecraftText("Room Code:")
                    }

                    Column {
                        MinecraftText("Connect")

                        Spacer(modifier = Modifier.height(10.dp))

                        MinecraftText("Connect at Startup")
                    }
                }
            }
        }
    }
}
