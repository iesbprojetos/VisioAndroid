package br.iesb.vismobile.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.nononsenseapps.filepicker.FilePickerActivity;

import br.iesb.vismobile.R;
import br.iesb.vismobile.file.FileManager;

public class MainActivity extends AppCompatActivity
        implements SaveFileDialogFragment.OnFragmentInteractionListener,
        ConnectionTabFragment.OnFragmentInteractionListener,
        ActivityCompat.OnRequestPermissionsResultCallback {
    private static final int REQUEST_FILE_PERMISSION = 1;
    private static final int REQUEST_FILE_OPEN = 2;

    private TabsPagerAdapter mTabsPagerAdapter;
    private ViewPager mViewPager;
    private FileManager fileManager;
    private View mLayoutProgress;

//    private Fragment mFragmentChart;

    private int mShortAnimationDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTabsPagerAdapter = new TabsPagerAdapter(getSupportFragmentManager());

        mLayoutProgress = findViewById(R.id.layoutProgress);

        mViewPager = (NoSwipeViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mTabsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        fileManager = FileManager.getSingleton(this);

        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

//    @Override
//    protected void onDestroy() {
//        try {
//            unregisterReceiver(UsbConnection.getSingleton(this, null).getUsbPermissionReceiver());
//        } catch (IllegalArgumentException e) {
//            // ignorar
//        }
//
//        super.onDestroy();
//    }

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
        switch (id) {
            case R.id.action_settings:
                Intent optionsIntent = new Intent(this, OptionsActivity.class);
                startActivity(optionsIntent, Bundle.EMPTY);
                break;
            case R.id.action_save:
                if (fileManager.askForStoragePermission(this, REQUEST_FILE_PERMISSION,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    startSaveDialogFragment();
                }
                break;
            case R.id.action_delete:
                fileManager.deleteCollection();
                redrawChartTabFragment(true);
                Snackbar.make(mViewPager, "Dados deletados", Snackbar.LENGTH_LONG)
                        .setAction("Desfazer", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                fileManager.restoreCollection();
                                redrawChartTabFragment(true);

                            }
                        })
                        .setCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                fileManager.deleteCollection();
                            }
                        }).show();
                break;
            case R.id.action_pca:
                Intent intent = new Intent(this, PcaTabActivity.class);
                startActivity(intent);
                break;
            case R.id.action_open:
                // This always works
                Intent i = new Intent(this, FilePickerActivity.class);
                // This works if you defined the intent filter
                // Intent i = new Intent(Intent.ACTION_GET_CONTENT);

                // Set these depending on your use case. These are the defaults.
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);

                // Configure initial directory by specifying a String.
                // You could specify a String like "/storage/emulated/0/", but that can
                // dangerous. Always use Android's API calls to get paths to the SD-card or
                // internal memory.

                i.putExtra(FilePickerActivity.EXTRA_START_PATH, fileManager.getDefaultFolder().getAbsolutePath());

                startActivityForResult(i, REQUEST_FILE_OPEN);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startSaveDialogFragment() {
        String defaultName = fileManager.getDefaultFilename();
        SaveFileDialogFragment.newInstance(defaultName)
                .show(getSupportFragmentManager(), "SaveFileDialogFragment");
    }

    private void saveCollection(final String filename, final boolean overwrite) {
        new AsyncTask<String, Void, Bundle>() {
            @Override
            protected void onPreExecute() {
                showProgressBar(true);
            }

            @Override
            protected Bundle doInBackground(String... params) {
                String filename = params[0];
                return fileManager.createFile(filename, overwrite);
            }

            @Override
            protected void onPostExecute(Bundle result) {
                boolean hideProgress = true;

                int resultCode = result.getInt(FileManager.ARG_RESULT_CODE);
                switch (resultCode) {
                    case FileManager.RESULT_OK:
                        Uri backupUri = result.getParcelable(FileManager.ARG_BACKUP_URI);
                        if (backupUri != null) {
                            AlertDialog.Builder okDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                            okDialogBuilder.setTitle("Sucesso")
                                    .setMessage(String.format("Arquivo criado: %s", backupUri.getPath()))
                                    .setPositiveButton("OK", null);
                            okDialogBuilder.show();
                            break;
                        }
                    case FileManager.RESULT_STORAGE_NOT_READY:
                    case FileManager.RESULT_FAILED_TO_CREATE_DIRECTORY:
                    case FileManager.RESULT_FAILED_TO_CREATE_FILE:
                    case FileManager.RESULT_FAILED_WRITE:
                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
                        alertBuilder.setTitle("Erro")
                                .setMessage(String.format("Não foi possível salvar o arquivo. (Erro %d)", resultCode))
                                .setPositiveButton("OK", null);
                        alertBuilder.show();
                        break;
                    case FileManager.RESULT_WAITING_USER:
                        hideProgress = false;

                        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("Alerta!")
                                .setMessage("Arquivo já existe. Deseja sobrescrever?")
                                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        saveCollection(filename, true);
                                    }
                                })
                                .setNegativeButton("Não", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        showProgressBar(false);
                                    }
                                });

                        builder.show();
                        break;
                }

                if (hideProgress) {
                    showProgressBar(false);
                }
            }
        }.execute(filename);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_FILE_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startSaveDialogFragment();
                } else {
                    // TODO:
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                    Toast.makeText(MainActivity.this, "Permissão para acesso à arquivos negada.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_FILE_OPEN) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();

                new AsyncTask<Uri, Void, Boolean>() {
                    @Override
                    protected void onPreExecute() {
                        showProgressBar(true);
                    }

                    @Override
                    protected Boolean doInBackground(Uri... params) {
                        Uri uri = params[0];
                        return fileManager.readFile(uri);
                    }

                    @Override
                    protected void onPostExecute(Boolean aBoolean) {
                        showProgressBar(false);

                        redrawChartTabFragment(true);
                    }
                }.execute(uri);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showProgressBar(final boolean show) {
        mLayoutProgress.setVisibility(View.VISIBLE);
        mLayoutProgress.animate()
                .setDuration(mShortAnimationDuration)
                .alpha(show ? 1 : 0)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mLayoutProgress.setVisibility(show ? View.VISIBLE : View.GONE);
                    }
                });

        mViewPager.setVisibility(View.VISIBLE);
        mViewPager.animate()
                .setDuration(mShortAnimationDuration)
                .alpha(show ? 0 : 1)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mViewPager.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
                    }
                });
    }

    private void redrawChartTabFragment(boolean reload) {
        // draw graph
        Fragment fragment = mTabsPagerAdapter.getFragment(1);
        if (fragment != null && fragment instanceof ChartTabFragment2) {
            ChartTabFragment2 tabFragment = (ChartTabFragment2) fragment;
            tabFragment.onRedrawGraph(reload);
        }
    }

    public class TabsPagerAdapter extends FragmentPagerAdapter {
        private final int PAGE_COUNT = 2;
        private final String PAGE_TITLES[] = new String[] {"Conexão", "Gráfico"};
        private SparseArray<Fragment> fragments = new SparseArray<>();

        public TabsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = null;

            switch (position) {
                case 0:
                    fragment = ConnectionTabFragment.newInstance();
                    break;
                case 1:
                    fragment = ChartTabFragment2.newInstance(true);
//                    mFragmentChart = fragment;
                    break;
            }

            return fragment;
        }

        @Override
        public int getCount() {
            return PAGE_COUNT;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return PAGE_TITLES[position];
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            fragments.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            fragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public Fragment getFragment(int position) {
            return fragments.get(position);
        }
    }

    @Override
    public void onOk(String filename) {
        saveCollection(filename, false);
    }

    @Override
    public void onUnitChanged() {
        redrawChartTabFragment(false);
    }
}
