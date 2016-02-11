package br.iesb.vismobile;

import android.app.AlertDialog;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;

import br.iesb.vismobile.usb.DeviceConnectionListener;
import br.iesb.vismobile.usb.DevicePickerDialogFragment;
import br.iesb.vismobile.usb.UsbConnection;


public class MainActivity extends AppCompatActivity implements
        TabFragment.OnFragmentInteractionListener,
        DeviceConnectionListener {
    private TabsPagerAdapter mTabsPagerAdapter;
    private ViewPager mViewPager;

    private UsbConnection usbConn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTabsPagerAdapter = new TabsPagerAdapter(getSupportFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mTabsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // TODO: Implementar DeviceConnectionListener e passar no getSingleton()
        usbConn = UsbConnection.getSingleton(getApplicationContext(), this);
        DevicePickerDialogFragment.newInstance()
                .show(getSupportFragmentManager(), "DevicePickerDialogFragment");

    }

    @Override
    protected void onDestroy() {
        usbConn.removeListener(this);

        super.onDestroy();
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

    @Override
    public void onFragmentInteraction(Uri uri) {
        // TODO:
    }

    @Override
    public void onDevicePermissionGranted(UsbDevice device) {

    }

    @Override
    public void onDevicePermissionDenied() {

    }

    @Override
    public void onDeviceConnected(UsbDevice device) {

    }

    @Override
    public void onDeviceWriteOperationFailed() {

    }

    @Override
    public void onDeviceReadOperationFailed() {

    }

    @Override
    public void onDeviceShowInfo(UsbDevice device) {
        UsbInterface usbInterface = device.getInterface(0);
        String strInterface = String.format("toString = %s\nID = %d\n" +
                        "Class = %d\nProtocol = %d",
                usbInterface.toString(), usbInterface.getId(),
                usbInterface.getInterfaceClass(),
                usbInterface.getInterfaceProtocol());
        StringBuilder strEndpoints = new StringBuilder();

        for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
            UsbEndpoint endpoint = usbInterface.getEndpoint(i);
            String strEndpoint = String.format("toString = %s\nType = %d\n" +
                            "Address = %d\nAttributes = %d",
                    endpoint.toString(), endpoint.getType(),
                    endpoint.getAddress(), endpoint.getAttributes());
            strEndpoints.append("ENDPOINT:\n").append(strEndpoint)
                    .append("------");
        }
        String msg = strInterface + "\n" + strEndpoints.toString();
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("USB Interfaces")
                .setMessage(msg)
                .show();
    }

    public class TabsPagerAdapter extends FragmentPagerAdapter {
        private final int PAGE_COUNT = 2;
        private final String PAGE_TITLES[] = new String[] {"Tab1", "Tab2"};

        public TabsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = null;

            switch (position) {
                case 0:
                    fragment = TabFragment.newInstance("tab1", "tab1");
                    break;
                case 1:
                    fragment = TabFragment.newInstance("tab2", "tab2");
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
    }
}
