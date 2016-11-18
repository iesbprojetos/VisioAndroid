package br.iesb.vismobile.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
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

import br.iesb.vismobile.R;
import br.iesb.vismobile.ChartCollection;
import br.iesb.vismobile.file.FileManager;

public class PcaTabActivity extends AppCompatActivity implements PcaTabFragment.OnFragmentInteractionListener {
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    private TabsPagerAdapter mTabsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private View mLayoutProgress;
    private int mShortAnimationDuration;

    private FileManager fileManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pca_tab);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mTabsPagerAdapter = new TabsPagerAdapter(getSupportFragmentManager());

        mLayoutProgress = findViewById(R.id.layoutProgress);

        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mTabsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        fileManager = FileManager.getSingleton(this);

        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_pca_tab, menu);
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

    @Override
    public void onPlotChart(ChartCollection collection) {
        // TODO: get charttabfragment and draw collection
        fileManager.setPcaCollection(collection);
        Fragment frag = mTabsPagerAdapter.getFragment(1);
        if (frag != null && frag instanceof ChartTabFragment2) {
            ChartTabFragment2 chartTabFragment = (ChartTabFragment2) frag;
            chartTabFragment.onRedrawGraph(true);

            mViewPager.setCurrentItem(1, true);
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class TabsPagerAdapter extends FragmentPagerAdapter {
        private final int PAGE_COUNT = 2;
        private final String PAGE_TITLES[] = new String[] {"Opções", "Gráfico"};
        private SparseArray<Fragment> fragments = new SparseArray<>();

        public TabsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = null;

            switch (position) {
                case 0:
                    fragment = PcaTabFragment.newInstance();
                    break;
                case 1:
                    fragment = ChartTabFragment2.newInstance(false);
                    break;
            }

            return fragment;
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return PAGE_COUNT;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position < PAGE_COUNT) {
                return PAGE_TITLES[position];
            }

            return null;
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
}

/**
 * Jama - biblioteca para decompor matriz (T e L)
 * svd() - metodo para decompor - singular value decomposition
 * V^t = L^t
 * U * S (vetor de matriz diagonal) = T
 * PCA: plotar
 *  - 2 componentes = 2 primeira colunas da matriz T
 *  -
 */