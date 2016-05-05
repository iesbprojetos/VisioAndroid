package br.iesb.vismobile;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

public class PCAActivity extends AppCompatActivity {

    private SeekBar seekComponentes;
    private TextView txtComponentes;
    private RadioGroup radioGroupMatrizes;
    private RadioButton radioMatrizesL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pca);

        txtComponentes = (TextView) findViewById(R.id.txtComponentes);
        txtComponentes.setText("20");

        seekComponentes = (SeekBar) findViewById(R.id.seekComponentes);
        seekComponentes.setProgress(20);
        seekComponentes.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 0) {
                    seekBar.setProgress(1);
                    progress = 1;
                }
                txtComponentes.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO
            }
        });

        radioGroupMatrizes = (RadioGroup) findViewById(R.id.radioGroupMatrizes);
        radioMatrizesL = (RadioButton) findViewById(R.id.radioMatrizesL);
        radioMatrizesL.setChecked(true);
    }
}
