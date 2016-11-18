package br.iesb.vismobile.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
import br.iesb.vismobile.R;
import br.iesb.vismobile.ChartCollection;
import br.iesb.vismobile.ChartData;
import br.iesb.vismobile.file.FileManager;


/**
 * Jama - biblioteca para decompor matriz (T e L)
 * svd() - metodo para decompor - singular value decomposition
 * U^t = L^t
 * U * S (vetor de matriz diagonal) = T
 * PCA: plotar
 *  - 2 componentes = 2 primeira colunas da matriz T
 *  -
 */
public class PCAActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, LoaderManager.LoaderCallbacks<SingularValueDecomposition> {
    private static final int REQUEST_FILE_PERMISSION = 1;
    private static final int LOADER_PCA = 1;
    private static final String ARG_FILENAMES = "FILENAMES";

    private FileManager fileManager;

    private ListView listFiles;
    private SeekBar seekComponentes;
    private TextView txtComponentes;
    private Button btnPCA;
    private RadioGroup radioGroupMatrizes;
    private RadioButton radioMatrizesL;
    private Spinner spinX;
    private Spinner spinY;

    private View layoutProgress;
    private View layoutPCAGraphOptions;

    private int mShortAnimationDuration;

    private int numComponentes;
    private SingularValueDecomposition mSvd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pca);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        layoutProgress = findViewById(R.id.layoutProgress);
        layoutProgress.setVisibility(View.GONE);

        layoutPCAGraphOptions = findViewById(R.id.layoutPCAGraphOptions);
        layoutPCAGraphOptions.setVisibility(View.GONE);

        listFiles = (ListView) findViewById(R.id.listArquivos);

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
                numComponentes = seekBar.getProgress();
            }
        });

        btnPCA = (Button) findViewById(R.id.btnPCA);
        btnPCA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO:
                ArrayList<String> files = new ArrayList<>();

                SparseBooleanArray checkedPositions = listFiles.getCheckedItemPositions();
                for (int i = 0; i < checkedPositions.size(); i++) {
                    if (checkedPositions.get(i)) {
                        String strFile = (String) listFiles.getItemAtPosition(i);
                        if (!TextUtils.isEmpty(strFile)) {
                            files.add(strFile);
                        } else {
                            // TODO:
                        }
                    }
                }

                Bundle args = new Bundle();
                args.putStringArrayList(ARG_FILENAMES, files);
                getSupportLoaderManager().initLoader(LOADER_PCA, args, PCAActivity.this).forceLoad();
            }
        });

        radioGroupMatrizes = (RadioGroup) findViewById(R.id.radioGroupMatrizes);
        radioMatrizesL = (RadioButton) findViewById(R.id.radioMatrizesL);
        radioMatrizesL.setChecked(true);

        spinX = (Spinner) findViewById(R.id.spinX);
        spinY = (Spinner) findViewById(R.id.spinY);

        fileManager = FileManager.getSingleton(this);

        if (fileManager.askForStoragePermission(this, REQUEST_FILE_PERMISSION, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            fillFilesListView();
        }

        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_FILE_PERMISSION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    fillFilesListView();
                } else {
                    // TODO:
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                    Toast.makeText(PCAActivity.this, "Permissão para acesso à arquivos negada.", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    private void fillFilesListView() {
        File defaultPath = fileManager.getDefaultFolder();

        String[] filesInPath = defaultPath.list();

        ListAdapter filesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, android.R.id.text1, filesInPath);
        listFiles.setAdapter(filesAdapter);
        listFiles.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    }

    private void showProgressBar(final boolean show) {
//        layoutProgress.setVisibility(View.VISIBLE);
        layoutProgress.animate()
                .setDuration(mShortAnimationDuration)
                .alpha(show ? 1 : 0)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        layoutProgress.setVisibility(show ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private void showGraphOptions(final boolean show) {
//        layoutPCAGraphOptions.setVisibility(View.VISIBLE);
        layoutPCAGraphOptions.animate()
                .setDuration(mShortAnimationDuration)
                .alpha(show ? 1 : 0)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        layoutPCAGraphOptions.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
                    }
                });
    }

    @Override
    public Loader<SingularValueDecomposition> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_PCA:
                showProgressBar(true);
                List<String> files = args.getStringArrayList(ARG_FILENAMES);
                return new PcaAsyncTask(this, files);
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<SingularValueDecomposition> loader, SingularValueDecomposition data) {
        switch (loader.getId()) {
            case LOADER_PCA:
                mSvd = data;

                List<String> strComponentes = new ArrayList<>();
                for (int i = 0; i < numComponentes; i++) {
                    strComponentes.add(String.valueOf(i+1));
                }
                SpinnerAdapter spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, strComponentes);
                spinX.setAdapter(spinnerAdapter);
                spinY.setAdapter(spinnerAdapter);

                showProgressBar(false);
                showGraphOptions(data != null);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<SingularValueDecomposition> loader) {
        switch (loader.getId()) {
            case LOADER_PCA:
                // TODO: ?
                showProgressBar(false);
                showGraphOptions(false);
                break;
        }
    }

    private static class PcaAsyncTask extends AsyncTaskLoader<SingularValueDecomposition> {
        private FileManager mFileManager;
        private List<String> mFiles;

        public PcaAsyncTask(Context context, List<String> files) {
            super(context);
            mFiles = files;
            mFileManager = FileManager.getSingleton(context);
        }

        @Override
        public SingularValueDecomposition loadInBackground() {
            ChartCollection collection = new ChartCollection("PCA");

            File defaultFolder = mFileManager.getDefaultFolder();
            Uri uriDefaultFolder = Uri.parse("file://" + defaultFolder.getAbsolutePath());

            for (String filename : mFiles) {
                Uri uriFile = uriDefaultFolder.buildUpon()
                        .appendPath(filename)
                        .build();
                ChartCollection fileCollection = mFileManager.readFileForPca(uriFile);
                if (fileCollection != null) {
                    for (ChartData chartData : fileCollection) {
                        collection.addChartData(chartData);
                    }
                }
            }

            double[][] array = new double[collection.size()][2048];
            for (int i = 0; i < collection.size(); i++) {
                for (int j = 0; j < 2048; j++) {
                    array[i][j] = collection.getCharData(i).getData().get(j);
                }
            }
            Matrix matrix = new Matrix(array);
            SingularValueDecomposition svd = matrix.svd();

            StringWriter strWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(strWriter);
            matrix.print(writer, 10, 0);
            Log.d("Visio", "Matrix = " + strWriter.toString());

            return svd;
        }
    }
}
