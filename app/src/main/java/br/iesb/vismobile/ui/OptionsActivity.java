package br.iesb.vismobile.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import br.iesb.vismobile.R;
import br.iesb.vismobile.usb.UsbConnection;

public class OptionsActivity extends AppCompatActivity {
    private EditText editBaudRate;
    private EditText editDataBits;
    private EditText editStopBits;
    private EditText editParidade;
    private EditText editNumAmostras;
    private Button btnLimpar;
    private Button btnAplicar;

    private UsbConnection usbConn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        usbConn = UsbConnection.getSingleton(this, null);

        editBaudRate = (EditText) findViewById(R.id.editBaudRate);
        editBaudRate.setText(String.valueOf(usbConn.getBaudRate()));
        editDataBits = (EditText) findViewById(R.id.editDataBits);
        editDataBits.setText(String.valueOf(usbConn.getDataBits()));
        editStopBits = (EditText) findViewById(R.id.editStopBits);
        editStopBits.setText(String.valueOf(usbConn.getStopBits()));
        editParidade = (EditText) findViewById(R.id.editParidade);
        editParidade.setText(String.valueOf(usbConn.getParidade()));
        editNumAmostras = (EditText) findViewById(R.id.editNumAmostras);
        editNumAmostras.setText(String.valueOf(usbConn.getSampleSize()));

        btnLimpar = (Button) findViewById(R.id.btnLimpar);
        btnLimpar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editBaudRate.setText(String.valueOf(UsbConnection.STD_BAUD_RATE));
                editDataBits.setText(String.valueOf(UsbConnection.STD_DATA_BITS));
                editStopBits.setText(String.valueOf(UsbConnection.STD_STOP_BITS));
                editParidade.setText(String.valueOf(UsbConnection.STD_PARIDADE));
                editNumAmostras.setText(String.valueOf(UsbConnection.STD_SAMPLE_SIZE));

                saveOptions();
            }
        });

        btnAplicar = (Button) findViewById(R.id.btnAplicar);
        btnAplicar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveOptions();
            }
        });
    }

    @Override
    protected void onDestroy() {
//        usbConn.release();
        usbConn = null;

        super.onDestroy();
    }

    private void saveOptions() {
        try {
            try {
                int baudRate = Integer.parseInt(editBaudRate.getText().toString());
                usbConn.setBaudRate(baudRate);
            } catch (NumberFormatException e) {
                throw new NotANumberException(editBaudRate);
            }

            try {
                int dataBits = Integer.parseInt(editDataBits.getText().toString());
                usbConn.setDataBits(dataBits);
            } catch (NumberFormatException e) {
                throw new NotANumberException(editDataBits);
            }

            try {
                int stopBits = Integer.parseInt(editStopBits.getText().toString());
                usbConn.setStopBits(stopBits);
            } catch (NumberFormatException e) {
                throw new NotANumberException(editStopBits);
            }

            try {
                int paridade = Integer.parseInt(editParidade.getText().toString());
                usbConn.setParidade(paridade);
            } catch (NumberFormatException e) {
                throw new NotANumberException(editParidade);
            }

            try {
                int numAmostras = Integer.parseInt(editNumAmostras.getText().toString());
                usbConn.setSampleSize(numAmostras);
            } catch (NumberFormatException e) {
                throw new NotANumberException(editNumAmostras);
            }

            Toast.makeText(this, "Opções salvas com sucesso", Toast.LENGTH_SHORT).show();
        } catch (NotANumberException e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Erro!")
                    .setPositiveButton("OK", null);
            String msg = "Valor em ";

            EditText errorView = e.getErrorView();
            if (errorView == editBaudRate) {
                msg += "Baud Rate";
            } else if (errorView == editDataBits) {
                msg += "Data Bits";
            } else if (errorView == editStopBits) {
                msg += "Stop Bits";
            } else if (errorView == editParidade) {
                msg += "Paridade";
            } else if (errorView == editNumAmostras) {
                msg += "Número de Amostras";
            }

            msg += " não é um número";

            builder.setMessage(msg);
            builder.show();
        }
    }

    private class NotANumberException extends NumberFormatException {
        private EditText errorView;

        public NotANumberException(EditText errorView) {
            this.errorView = errorView;
        }

        public EditText getErrorView() {
            return errorView;
        }
    }
}
