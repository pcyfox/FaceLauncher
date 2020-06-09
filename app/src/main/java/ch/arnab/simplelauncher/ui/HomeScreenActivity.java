package ch.arnab.simplelauncher.ui;

import android.os.Bundle;

import android.view.Menu;

import androidx.fragment.app.FragmentActivity;

import com.tk.facelauncher.R;

import ch.arnab.simplelauncher.App;

public class HomeScreenActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homescreen);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home_screen, menu);
        return true;
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //桌面被干死了，重置登录状态
        App.Companion.setRegister(false);
    }
}
