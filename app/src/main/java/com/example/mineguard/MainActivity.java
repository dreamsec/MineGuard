package com.example.mineguard;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.mineguard.alarm.AlarmFragment;
import com.example.mineguard.home.HomeFragment;
import com.example.mineguard.preview.PreviewFragment;
import com.example.mineguard.profile.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private Fragment[] fragments = new Fragment[4];
    private int currentIndex = 0;
    private WindowInsetsControllerCompat windowInsetsController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. 设置全屏沉浸
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);

        // 初始化控制器
        windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        // 2. 修改 Insets 监听
        // 注意：这里只处理 Bottom (导航栏) 和 Left/Right，
        // 也就是【不要】设置 systemBars.top 的 padding，这样内容才能延伸到状态栏背后。
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 初始化 Fragment
        fragments[0] = HomeFragment.newInstance();
        fragments[1] = new AlarmFragment();
        fragments[2] = new PreviewFragment();
        fragments[3] = new ProfileFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, fragments[0])
                .commit();

        // 初始化状态栏样式（首页默认）
        updateStatusBarForIndex(0);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            int newIndex = -1;

            if (id == R.id.nav_home) newIndex = 0;
            else if (id == R.id.nav_alarms) newIndex = 1;
            else if (id == R.id.nav_preview) newIndex = 2;
            else if (id == R.id.nav_profile) newIndex = 3;

            if (newIndex != -1 && newIndex != currentIndex) {
                switchFragment(currentIndex, newIndex);
                currentIndex = newIndex;
                // 3. 切换页面时更新状态栏样式
                updateStatusBarForIndex(newIndex);
                return true;
            }
            return false;
        });
    }

    private void switchFragment(int oldIndex, int newIndex) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Fragment oldFrag = fragments[oldIndex];
        Fragment newFrag = fragments[newIndex];

        if (!newFrag.isAdded()) {
            transaction.hide(oldFrag).add(R.id.fragment_container, newFrag);
        } else {
            transaction.hide(oldFrag).show(newFrag);
        }
        transaction.commit();
    }

    // 辅助方法：根据页面索引调整状态栏颜色
    private void updateStatusBarForIndex(int index) {
        if (windowInsetsController == null) return;

        if (index == 0) {
            // === 首页 (Home) ===
            // 你的背景是蓝色/深色图片，所以状态栏文字图标应该是“白色”
            // setAppearanceLightStatusBars(false) 表示图标是浅色(白色)
            windowInsetsController.setAppearanceLightStatusBars(false);
        } else {
            // === 其他页面 (Alarm, Preview, Profile) ===
            // 这种页面通常背景是白色，所以状态栏文字图标应该是“黑色”
            // setAppearanceLightStatusBars(true) 表示图标是深色(黑色)
            windowInsetsController.setAppearanceLightStatusBars(true);
        }
    }
}