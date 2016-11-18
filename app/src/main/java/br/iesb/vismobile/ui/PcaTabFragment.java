package br.iesb.vismobile.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Jama.Matrix;
import br.iesb.vismobile.R;
import br.iesb.vismobile.ChartCollection;
import br.iesb.vismobile.ChartData;
import br.iesb.vismobile.file.FileManager;
import br.iesb.vismobile.Pca;

/**
 * Created by dfcarvalho on 6/8/16.
 */
public class PcaTabFragment extends Fragment implements ActivityCompat.OnRequestPermissionsResultCallback, LoaderManager.LoaderCallbacks<Map<String, ChartCollection>> {
    private static final int REQUEST_FILE_PERMISSION = 1;
    private static final int LOADER_PCA = 1;
    private static final String ARG_FILENAMES = "FILENAMES";

    private View view;

    private ListView listFiles;
    private SeekBar seekComponentes;
    private TextView txtComponentes;
    private Button btnPCA;
    private RadioGroup radioGroupMatrizes;
    private RadioButton radioMatrizesL;
    private Spinner spinX;
    private Spinner spinY;
    private Button btnPlotPCA;

    private View layoutProgress;
    private View layoutPCAGraphOptions;

    private int mShortAnimationDuration;

    private int numComponentes;
    private Map<String, ChartCollection> chartCollections;

    private FileManager fileManager;

    private OnFragmentInteractionListener listener;

    public PcaTabFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ConnectionTabFragment.
     */
    public static PcaTabFragment newInstance() {
        return new PcaTabFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: needed?
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            listener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view == null) {
            createView(inflater, container, savedInstanceState);
        }

        return view;
    }

    private void createView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_pca_tab, container, false);

        layoutProgress = view.findViewById(R.id.layoutProgress);
        layoutProgress.setVisibility(View.GONE);

        layoutPCAGraphOptions = view.findViewById(R.id.layoutPCAGraphOptions);
        layoutPCAGraphOptions.setVisibility(View.GONE);

        listFiles = (ListView) view.findViewById(R.id.listArquivos);

        txtComponentes = (TextView) view.findViewById(R.id.txtComponentes);
        txtComponentes.setText("20");

        seekComponentes = (SeekBar) view.findViewById(R.id.seekComponentes);
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
        seekComponentes.setProgress(20);
        numComponentes = 20;

        btnPCA = (Button) view.findViewById(R.id.btnPCA);
        btnPCA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO:
                btnPCA.setEnabled(false);

                ArrayList<String> files = new ArrayList<>();

                SparseBooleanArray checkedPositions = listFiles.getCheckedItemPositions();
                for (int i = 0; i < listFiles.getCount(); i++) {
                    if (checkedPositions.get(i)) {
                        String strFile = (String) listFiles.getItemAtPosition(i);
                        if (!TextUtils.isEmpty(strFile)) {
                            files.add(strFile);
                        } else {
                            // TODO:
                        }
                    }
                }

                if (files.isEmpty()) {
                    // TODO: show alert, need to select at least one file
                    return;
                }

                Bundle args = new Bundle();
                args.putStringArrayList(ARG_FILENAMES, files);
                getLoaderManager().restartLoader(LOADER_PCA, args, PcaTabFragment.this).forceLoad();
            }
        });

        radioGroupMatrizes = (RadioGroup) view.findViewById(R.id.radioGroupMatrizes);
        radioMatrizesL = (RadioButton) view.findViewById(R.id.radioMatrizesL);
        radioMatrizesL.setChecked(true);

        spinX = (Spinner) view.findViewById(R.id.spinX);
        spinY = (Spinner) view.findViewById(R.id.spinY);

        btnPlotPCA = (Button) view.findViewById(R.id.btnPlotPCA);
        btnPlotPCA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChartCollection collection = null;

                switch (radioGroupMatrizes.getCheckedRadioButtonId()) {
                    case R.id.radioMatrizesL:
                        collection = chartCollections.get("L");
                        break;
                    case R.id.radioMatrizesT:
                        collection = chartCollections.get("T");
                        break;
                }

                if (collection != null) {
                    int x = spinX.getSelectedItemPosition();
                    int y = spinY.getSelectedItemPosition();

                    ChartCollection finalCollection = new ChartCollection(String.format("PCA - (%d, %d)", x, y));
                    Map<Double, Double> data = new HashMap<>();
                    for (ChartData chartData : collection) {
                        // TODO:
                        Number numX = chartData.getData().get((double)x);
                        data.put(numX.doubleValue(), chartData.getData().get((double)y));
                    }
                    ChartData chartData = new ChartData("PCA", "PCA", new Date().getTime(), data);
                    finalCollection.addChartData(chartData);

                    listener.onPlotChart(finalCollection);
                }
            }
        });

        fileManager = FileManager.getSingleton(getContext());

        if (fileManager.askForStoragePermission(getActivity(), REQUEST_FILE_PERMISSION, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            fillFilesListView();
        }

        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    private void fillFilesListView() {
        File defaultPath = fileManager.getDefaultFolder();

        String[] filesInPath = defaultPath.list();

        ListAdapter filesAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_multiple_choice, android.R.id.text1, filesInPath);
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
                        layoutPCAGraphOptions.setVisibility(show ? View.VISIBLE : View.GONE);
                    }
                });
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

                    Toast.makeText(getContext(), "Permissão para acesso à arquivos negada.", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    public Loader<Map<String, ChartCollection>> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_PCA:
                showGraphOptions(false);
                showProgressBar(true);
                List<String> files = args.getStringArrayList(ARG_FILENAMES);
                return new PcaAsyncTask(getContext(), files, seekComponentes.getProgress());
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Map<String, ChartCollection>> loader, Map<String, ChartCollection> data) {
        switch (loader.getId()) {
            case LOADER_PCA:
                btnPCA.setEnabled(true);
                chartCollections = data;
                boolean showComponentsMsg = ((PcaAsyncTask)loader).mShowComponentsMsg;
                if (showComponentsMsg) {
                    int realNumComponents = ((PcaAsyncTask)loader).mNumComponents;

                    String msg = "Número de amostras selecionadas (" + realNumComponents +
                            ") é menor que o número de componentes do PCA selecionado (" + numComponentes +
                            "). O número de componentes foi alterado para " + realNumComponents + ".";

                    numComponentes = realNumComponents;
                    seekComponentes.setProgress(numComponentes);

                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Alerta")
                            .setMessage(msg)
                            .setPositiveButton("OK", null)
                            .show();

                    ((PcaAsyncTask)loader).mShowComponentsMsg = false;
                }

                List<String> strComponentes = new ArrayList<>();
                for (int i = 0; i < numComponentes; i++) {
                    strComponentes.add(String.valueOf(i+1));
                }
                SpinnerAdapter spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, android.R.id.text1, strComponentes);
                spinX.setAdapter(spinnerAdapter);
                spinY.setAdapter(spinnerAdapter);

                showProgressBar(false);
                showGraphOptions(data != null);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Map<String, ChartCollection>> loader) {
        switch (loader.getId()) {
            case LOADER_PCA:
                // TODO: ?
                btnPCA.setEnabled(true);
                showProgressBar(false);
                showGraphOptions(false);
                break;
        }
    }

    private static class PcaAsyncTask extends AsyncTaskLoader<Map<String, ChartCollection>> {
        private FileManager mFileManager;
        private List<String> mFiles;
        public int mNumComponents;
        public boolean mShowComponentsMsg;

        public PcaAsyncTask(Context context, List<String> files, int numComponents) {
            super(context);
            mFiles = files;
            mFileManager = FileManager.getSingleton(context);
            mNumComponents = numComponents;
            mShowComponentsMsg = false;
        }

        @Override
        public Map<String, ChartCollection> loadInBackground() {
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

            if (mNumComponents > collection.size()) {
                mShowComponentsMsg = true;
                mNumComponents = collection.size();
            }

            double[][] array = new double[collection.size()][2048];
            for (int i = 0; i < collection.size(); i++) {
                for (int j = 0; j < 2048; j++) {
                    array[i][j] = collection.getCharData(i).getData().get((double)j);
                }
            }
            Matrix matrix = new Matrix(array);

//            SingularValueDecomposition svd = matrix.svd();
//            SingularValueDecomposition svd = new SingularValueDecomposition(matrix);
            Pca pca = new Pca(matrix, mNumComponents);

            StringWriter strWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(strWriter);
            matrix.print(writer, 10, 0);
            Log.d("Visio", "Matrix = " + strWriter.toString());

            Map<String, ChartCollection> pcaCollections = new HashMap<>(2);

            ChartCollection lCollection = new ChartCollection("PCA - L");

            Matrix lMatrix = pca.getL();
            double[][] lArray = lMatrix.getArray();
            for (int i = 0; i < lMatrix.getRowDimension(); i++) {
                Map<Double, Double> data = new HashMap<>();

                for (int j = 0; j < lMatrix.getColumnDimension(); j++) {
                    data.put((double)j, lArray[i][j]);
                }

                ChartData chartData = new ChartData("PCA", "PCA", new Date().getTime(), data);
                lCollection.addChartData(chartData);

            }
            pcaCollections.put("L", lCollection);

            ChartCollection tCollection = new ChartCollection("PCA - T");

            Matrix tMatrix = pca.getT();
            double[][] tArray = tMatrix.getArray();
            for (int i = 0; i < tMatrix.getRowDimension(); i++) {
                Map<Double, Double> data = new HashMap<>();

                for (int j = 0; j < tMatrix.getColumnDimension(); j++) {
                    data.put((double)j, tArray[i][j]);
                }

                ChartData chartData = new ChartData("PCA", "PCA", new Date().getTime(), data);
                tCollection.addChartData(chartData);
            }
            pcaCollections.put("T", tCollection);

            return pcaCollections;
        }
    }

    public interface OnFragmentInteractionListener {
        // TODO:
        void onPlotChart(ChartCollection collection);
    }
}
