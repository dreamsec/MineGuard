package com.example.mineguard;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化 Fragment
        fragments[0] = new HomeFragment();      // 数据统计
        fragments[1] = new AlarmFragment();     // 报警管理
        fragments[2] = new PreviewFragment();   // 实时预览
        fragments[3] = new ProfileFragment();   // 我的

        // 默认显示第一个
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, fragments[0])
                .commit();

        // 设置 BottomNavigationView 监听
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            int newIndex = -1;

            if (id == R.id.nav_home) {
                newIndex = 0;
            } else if (id == R.id.nav_alarms) {
                newIndex = 1;
            } else if (id == R.id.nav_preview) {
                newIndex = 2;
            } else if (id == R.id.nav_profile) {
                newIndex = 3;
            }

            if (newIndex != -1 && newIndex != currentIndex) {
                switchFragment(currentIndex, newIndex);
                currentIndex = newIndex;
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
}