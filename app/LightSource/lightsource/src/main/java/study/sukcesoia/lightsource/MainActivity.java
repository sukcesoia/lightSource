package study.sukcesoia.lightsource;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                final Intent intent = new Intent(MainActivity.this, LightControlActivity.class);
                intent.putExtra(LightControlActivity.EXTRAS_DEVICE_NAME, "LightSource");
                intent.putExtra(LightControlActivity.EXTRAS_DEVICE_ADDRESS, "A4:D5:78:05:52:32");
                startActivity(intent);
            }
        });

        /* Start Activity LightControlActivity */
        startActivity(new Intent(MainActivity.this, LightControlActivity.class));

        finish();
    }
}
