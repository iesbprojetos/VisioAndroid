package br.iesb.vismobile;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private static RadioGroup radioGroup;

    private static RadioButton radioContinuo, radioUnico, radiomV, radioCounts;

    private static Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        onClickListenerButton();
    }

    public void onClickListenerButton() {
        radioGroup = (RadioGroup) findViewById(R.id.rgroup);
        button = (Button) findViewById(R.id.rb_adquirir);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int select_id = radioGroup.getCheckedRadioButtonId();
                radioContinuo = (RadioButton) findViewById(select_id);
                Toast.makeText(MainActivity.this, radioContinuo.getText().toString(), Toast.LENGTH_SHORT);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
