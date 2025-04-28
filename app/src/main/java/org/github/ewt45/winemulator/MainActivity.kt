package org.github.ewt45.winemulator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.github.ewt45.winemulator.ui.theme.MainTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val terminalViewModel :TerminalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Consts.init(this)

        setContent {
            MainTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }

        lifecycleScope.launch {
            viewModel.showBlockDialog("解压alpine rootfs")
            try {
                Rootfs.ensure(this@MainActivity)
                viewModel.setDebugInfo("rootfs解压完成")
            } catch (e:Exception) {
                e.printStackTrace()
                viewModel.setDebugInfo("rootfs解压失败")
            }

            viewModel.closeBlockDialog()

            terminalViewModel.startTerminal()
//            viewModel.runProotCommand(applicationContext)

//            // 假设你已经把 `proot` 可执行文件放到了 filesDir 并设置了执行权限
//            val result = runProotCommand(applicationContext, rootfs, "ls -la /")
//            Log.d("PROOT", result)
        }



    }
}



@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MainTheme {
        ProotTerminalScreen()
    }
}