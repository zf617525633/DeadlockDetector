package com.expo.threadtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expo.threadtest.ui.theme.ThreadTestTheme

class MainActivity : ComponentActivity() {

    private val synchronizedDeadlockExample = DeadlockExample()
    private var reentrantLockDeadlockExample: ReentrantLockDeadlockExample? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 延迟初始化，确保 DeadlockDetector 已经初始化
        reentrantLockDeadlockExample = ReentrantLockDeadlockExample()

        setContent {
            ThreadTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DeadlockTestScreen(
                        modifier = Modifier.padding(innerPadding),
                        onSynchronizedDeadlock = {
                            synchronizedDeadlockExample.triggerSynchronizedDeadlock()
                        },
                        onReentrantLockDeadlock = {
                            reentrantLockDeadlockExample?.triggerReentrantLockDeadlock()
                        },
                        onTriggerANR = {
                            // 在主线程执行耗时操作，触发 ANR
                            Thread.sleep(10000)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DeadlockTestScreen(
    modifier: Modifier = Modifier,
    onSynchronizedDeadlock: () -> Unit,
    onReentrantLockDeadlock: () -> Unit,
    onTriggerANR: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "死锁检测测试",
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSynchronizedDeadlock,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
        ) {
            Text("触发 Synchronized 死锁")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onReentrantLockDeadlock,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
        ) {
            Text("触发 ReentrantLock 死锁")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onTriggerANR,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
        ) {
            Text("触发 ANR (主线程阻塞)")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "点击按钮后查看 Logcat\n过滤 Tag: DeadlockDetector",
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}
